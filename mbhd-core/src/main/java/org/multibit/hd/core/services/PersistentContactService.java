package org.multibit.hd.core.services;

import com.google.bitcoin.core.Address;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.multibit.hd.core.crypto.EncryptedFileReaderWriter;
import org.multibit.hd.core.dto.Contact;
import org.multibit.hd.core.dto.WalletId;
import org.multibit.hd.core.exceptions.ContactsLoadException;
import org.multibit.hd.core.exceptions.ContactsSaveException;
import org.multibit.hd.core.exceptions.EncryptedFileReaderWriterException;
import org.multibit.hd.core.managers.InstallationManager;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.store.ContactsProtobufSerializer;
import org.multibit.hd.core.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>Service to provide the following to application:</p>
 * <ul>
 * <li>CRUD operations on Contacts</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class PersistentContactService implements ContactService {

  private static final Logger log = LoggerFactory.getLogger(PersistentContactService.class);

  /**
   * The in-memory cache of contacts for the current wallet
   */
  private final Set<Contact> contacts = Sets.newHashSet();

  /**
   * The location of the backing writeContacts for the contacts
   */
  private File backingStoreFile;

  /**
   * The serializer for the backing writeContacts
   */
  private ContactsProtobufSerializer protobufSerializer;

  /**
   * <p>Create a ContactService for a Wallet with the given walletId</p>
   *
   * <p>Reduced visibility constructor to prevent accidental instance creation outside of CoreServices.</p>
   */
  PersistentContactService(WalletId walletId) {

    Preconditions.checkNotNull(walletId, "'walletId' must be present");

    // Register for events
    CoreServices.uiEventBus.register(this);

    // Work out where to writeContacts the contacts for this wallet id.
    File applicationDataDirectory = InstallationManager.getOrCreateApplicationDataDirectory();
    String walletRoot = WalletManager.createWalletRoot(walletId);

    File walletDirectory = WalletManager.getWalletDirectory(applicationDataDirectory.getAbsolutePath(), walletRoot);

    File contactsDirectory = new File(walletDirectory.getAbsolutePath() + File.separator + CONTACTS_DIRECTORY_NAME);
    FileUtils.createDirectoryIfNecessary(contactsDirectory);

    this.backingStoreFile = new File(contactsDirectory.getAbsolutePath() + File.separator + CONTACTS_DATABASE_NAME);

    initialise();
  }

  /**
   * <p>Create a ContactService with the specified File as the backing writeContacts. (This exists primarily for testing where you just run things in a temporary directory)</p>
   * <p>Reduced visibility constructor to prevent accidental instance creation outside of CoreServices.</p>
   */
  PersistentContactService(File backingStoreFile) {

    this.backingStoreFile = backingStoreFile;

    initialise();
  }

  private void initialise() {

    protobufSerializer = new ContactsProtobufSerializer();

    // Load the contact data from the backing writeContacts if it exists
    if (backingStoreFile.exists()) {
      loadContacts();
    }

  }

  /**
   * <p>Create a new contact and add it to the internal cache</p>
   *
   * @param name A name (normally first name and last name)
   *
   * @return A new contact with a fresh ID
   */
  @Override
  public Contact newContact(String name) {

    return new Contact(UUID.randomUUID(), name);

  }

  /**
   * @return A list of all Contacts for the given page
   */
  @Override
  public List<Contact> allContacts() {

    return Lists.newArrayList(contacts);

  }

  @Override
  public List<Contact> filterContactsByBitcoinAddress(Address address) {

    Preconditions.checkNotNull(address,"'address' must be present");

    String queryAddress = address.toString();

    List<Contact> filteredContacts = Lists.newArrayList();

    for (Contact contact : contacts) {

      if (contact.getBitcoinAddress().or("").equals(queryAddress)) {
        filteredContacts.add(contact);
      }
    }

    return filteredContacts;
  }

  @Override
  public List<Contact> filterContactsByContent(String query) {

    String lowerQuery = query.toLowerCase();

    List<Contact> filteredContacts = Lists.newArrayList();

    for (Contact contact : contacts) {

      // Note: Do not include a Bitcoin address or EPK in this search
      // because vanity addresses can cause an attack vector
      // Instead use the dedicated methods for those fields
      boolean isNameMatched = contact.getName().toLowerCase().contains(lowerQuery);
      boolean isEmailMatched = contact.getEmail().or("").toLowerCase().contains(lowerQuery);
      boolean isNoteMatched = contact.getNotes().or("").toLowerCase().contains(lowerQuery);

      boolean isTagMatched = false;
      for (String tag : contact.getTags()) {
        if (tag.toLowerCase().contains(lowerQuery)) {
          isTagMatched = true;
          break;
        }
      }

      if (isNameMatched
        || isEmailMatched
        || isNoteMatched
        || isTagMatched
        ) {
        filteredContacts.add(contact);
      }
    }

    return filteredContacts;
  }

  @Override
  public void addAll(Collection<Contact> selectedContacts) {

    contacts.addAll(selectedContacts);

  }

  @Override
  public void loadContacts() throws ContactsLoadException {

    log.debug("Loading contacts from '{}'", backingStoreFile.getAbsolutePath());

    try {
      ByteArrayInputStream decryptedInputStream = EncryptedFileReaderWriter.readAndDecrypt(backingStoreFile, WalletManager.INSTANCE.getCurrentWalletData().get().getPassword());
      Set<Contact> loadedContacts = protobufSerializer.readContacts(decryptedInputStream);
      contacts.clear();
      contacts.addAll(loadedContacts);

    } catch (EncryptedFileReaderWriterException e) {
      throw new ContactsLoadException("Could not loadContacts contacts db '" + backingStoreFile.getAbsolutePath() + "'. Error was '" + e.getMessage() + "'.");
    }
  }

  /**
   * <p>Clear all contact data</p>
   * <p>Reduced visibility for testing</p>
   */
  void clear() {
    contacts.clear();
  }

  @Override
  public void removeAll(Collection<Contact> selectedContacts) {

    Preconditions.checkNotNull(selectedContacts, "'selectedContacts' must be present");

    log.debug("Removing {} contact(s)", selectedContacts.size());

    contacts.removeAll(selectedContacts);

  }

  @Override
  public void updateContacts(Collection<Contact> editedContacts) {

    Preconditions.checkNotNull(editedContacts, "'editedContacts' must be present");

    log.debug("Updating {} contact(s)", editedContacts.size());

    for (Contact editedContact : editedContacts) {

      if (!contacts.contains(editedContact)) {

        contacts.add(editedContact);

      }

    }

  }

  @Override
  public void writeContacts() throws ContactsSaveException {

    log.debug("Writing {} contact(s)", contacts.size());

    try (FileOutputStream fos = new FileOutputStream(backingStoreFile)) {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(1024);
      protobufSerializer.writeContacts(contacts, byteArrayOutputStream);
      EncryptedFileReaderWriter.encryptAndWrite(byteArrayOutputStream.toByteArray(), WalletManager.INSTANCE.getCurrentWalletData().get().getPassword(), fos);

    } catch (IOException e) {
      throw new ContactsSaveException("Could not save contacts db '" + backingStoreFile.getAbsolutePath() + "'. Error was '" + e.getMessage() + "'.");
    }
  }

  @Override
  public void addDemoContacts() {

    // Only add the demo contacts if there are none present
    if (!contacts.isEmpty()) {
      return;
    }

    Contact contact1 = newContact("Alice Capital");
    contact1.setEmail("g.rowe@froot.co.uk");
    contact1.getTags().add("VIP");
    contact1.getTags().add("Family");
    contact1.setNotes("This is a really long note that should span over several lines when finally rendered to the screen. It began with Alice Capital.");
    contacts.add(contact1);

    Contact contact2 = newContact("Bob Capital");
    contact2.setEmail("bob.capital@example.org");
    contact2.setNotes("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
    contact2.getTags().add("VIP");
    contact2.getTags().add("Merchandise");
    contacts.add(contact2);

    Contact contact3 = newContact("Charles Capital");
    contact2.setNotes("Charles Capital's note 1\n\nCharles Capital's note 2");
    contact3.setEmail("charles.capital@example.org");
    contacts.add(contact3);

    // No email for Derek
    Contact contact4 = newContact("Derek Capital");
    contact2.setNotes("Derek Capital's note 1\n\nDerek Capital's note 2");
    contact4.getTags().add("Family");
    contacts.add(contact4);

    Contact contact5 = newContact("alice Lower");
    contact5.setEmail("alice.lower@example.org");
    contacts.add(contact5);

    Contact contact6 = newContact("alicia Lower");
    contact6.setEmail("alicia.lower@example.org");
    contacts.add(contact6);

  }

}
