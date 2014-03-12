/**
 * Copyright 2014 multibit.org
 *
 * Licensed under the MIT license (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://opensource.org/licenses/mit-license.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Based on the WalletProtobufSerialiser written by Miron Cuperman, copyright Google (also MIT licence)
 */

package org.multibit.hd.core.store;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.multibit.hd.core.dto.Contact;
import org.multibit.hd.core.exceptions.ContactsLoadException;
import org.multibit.hd.core.protobuf.MBHDContactsProtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * <p>
 * Serialize and de-serialize contacts to a byte stream containing a
 * <a href="http://code.google.com/apis/protocolbuffers/docs/overview.html">protocol buffer</a>.</p>
 *
 * <p>Protocol buffers are a data interchange format developed by Google with an efficient binary representation, a type safe specification
 * language and compilers that generate code to work with those data structures for many languages. Protocol buffers
 * can have their format evolved over time: conceptually they represent data using (tag, length, value) tuples.</p>
 *
 * <p>The format is defined by the <tt>contacts.proto</tt> file in the MBHD source distribution.</p>
 *
 * <p>This class is used through its static methods. The most common operations are <code>writeContacts</code> and <code>readContacts</code>, which do
 * the obvious operations on Output/InputStreams. You can use a {@link java.io.ByteArrayInputStream} and equivalent
 * {@link java.io.ByteArrayOutputStream} if byte arrays are preferred. The protocol buffer can also be manipulated
 * in its object form if you'd like to modify the flattened data structure before serialization to binary.</p>
 *
 * <p>Based on the original work by Miron Cuperman for the Bitcoinj project</p>
 */
public class ContactsProtobufSerializer {

  private static final Logger log = LoggerFactory.getLogger(ContactsProtobufSerializer.class);

  public ContactsProtobufSerializer() {
  }

  /**
   * Formats the given Contacts to the given output stream in protocol buffer format.<p>
   */
  public void writeContacts(Set<Contact> contacts, OutputStream output) throws IOException {
    MBHDContactsProtos.Contacts contactsProto = contactsToProto(contacts);
    contactsProto.writeTo(output);
  }

  /**
   * Converts the given contacts to the object representation of the protocol buffers. This can be modified, or
   * additional data fields set, before serialization takes place.
   */
  public MBHDContactsProtos.Contacts contactsToProto(Set<Contact> contacts) {
    MBHDContactsProtos.Contacts.Builder contactsBuilder = MBHDContactsProtos.Contacts.newBuilder();

    Preconditions.checkNotNull(contacts, "Contacts must be specified");

    for (Contact contact : contacts) {
      MBHDContactsProtos.Contact contactProto = makeContactProto(contact);
      contactsBuilder.addContact(contactProto);
    }

    return contactsBuilder.build();
  }

  private static MBHDContactsProtos.Contact makeContactProto(Contact contact) {
    MBHDContactsProtos.Contact.Builder contactBuilder = MBHDContactsProtos.Contact.newBuilder();
    contactBuilder.setId(contact.getId().toString());
    contactBuilder.setName(contact.getName());
    contactBuilder.setBitcoinAddress(contact.getBitcoinAddress().or(""));
    contactBuilder.setEmail(contact.getEmail().or(""));
    contactBuilder.setImagePath(contact.getImagePath().or(""));
    contactBuilder.setExtendedPublicKey(contact.getExtendedPublicKey().or(""));
    contactBuilder.setNotes(contact.getNotes().or(""));

    // Construct tags
    List<String> tags = contact.getTags();
    if (tags != null) {
      int tagIndex = 0;
      for (String tag : tags) {
        MBHDContactsProtos.Tag tagProto = makeTagProto(tag);
        contactBuilder.addTag(tagIndex, tagProto);
        tagIndex++;
      }
    }

    return contactBuilder.build();
  }

  private static MBHDContactsProtos.Tag makeTagProto(String tag) {
    MBHDContactsProtos.Tag.Builder tagBuilder = MBHDContactsProtos.Tag.newBuilder();
    tagBuilder.setTagValue(tag);
    return tagBuilder.build();
  }

  /**
   * <p>Parses a Contacts from the given stream, using the provided Contacts instance to loadContacts data into.
   * <p>A Contacts db can be unreadable for various reasons, such as inability to open the file, corrupt data, internally
   * inconsistent data, You should always
   * handle {@link org.multibit.hd.core.exceptions.ContactsLoadException} and communicate failure to the user in an appropriate manner.</p>
   *
   * @throws ContactsLoadException thrown in various error conditions (see description).
   */
  public Set<Contact> readContacts(InputStream input) throws ContactsLoadException {
    try {
      MBHDContactsProtos.Contacts contactsProto = parseToProto(input);
      Set<Contact> contacts = Sets.newHashSet();
      readContacts(contactsProto, contacts);
      return contacts;
    } catch (IOException e) {
      throw new ContactsLoadException("Could not parse input stream to protobuf", e);
    }
  }

  /**
   * <p>Loads contacts data from the given protocol buffer and inserts it into the given Set of Contact object.
   *
   * <p>A contact db can be unreadable for various reasons, such as inability to open the file, corrupt data, internally
   * inconsistent data, a wallet extension marked as mandatory that cannot be handled and so on. You should always
   * handle {@link ContactsLoadException} and communicate failure to the user in an appropriate manner.</p>
   *
   * @throws ContactsLoadException thrown in various error conditions (see description).
   */
  private void readContacts(MBHDContactsProtos.Contacts contactsProto, Set<Contact> contacts) throws ContactsLoadException {
    Set<Contact> readContacts = Sets.newHashSet();

    List<MBHDContactsProtos.Contact> contactProtos = contactsProto.getContactList();

    if (contactProtos != null) {
      for (MBHDContactsProtos.Contact contactProto : contactProtos) {
        String idAsString = contactProto.getId();
        UUID id = UUID.fromString(idAsString);

        String name = contactProto.getName();

        Contact contact = new Contact(id, name);

        contact.setEmail(contactProto.getEmail());
        contact.setBitcoinAddress(contactProto.getBitcoinAddress());
        contact.setImagePath(contactProto.getImagePath());
        contact.setExtendedPublicKey(contactProto.getExtendedPublicKey());
        contact.setNotes(contactProto.getNotes());

        // Create tags
        List<String> tags = Lists.newArrayList();
        List<MBHDContactsProtos.Tag> tagProtos = contactProto.getTagList();
        if (tagProtos != null) {
          for (MBHDContactsProtos.Tag tagProto : tagProtos) {
            tags.add(tagProto.getTagValue());
          }
        }
        contact.setTags(tags);
        readContacts.add(contact);
      }
    }

    // Everything read ok - put the new contacts into the passed in contacts object
    contacts.clear();
    contacts.addAll(readContacts);
  }

  /**
   * Returns the loaded protocol buffer from the given byte stream. This method is designed for low level work involving the
   * wallet file format itself.
   */
  public static MBHDContactsProtos.Contacts parseToProto(InputStream input) throws IOException {
    return MBHDContactsProtos.Contacts.parseFrom(input);
  }
}
