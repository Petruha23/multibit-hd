package org.multibit.hd.core.managers;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.multibit.hd.core.api.WalletId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


/**
 * Class to manage creation and reading back of the wallet backups.
 */
public enum BackupManager {
  INSTANCE;

  public static final String BACKUP_SUFFIX_FORMAT = "yyyyMMddHHmmss";
  public static final String BACKUP_ZIP_FILE_EXTENSION = ".zip";
  public static final String BACKUP_ZIP_FILE_EXTENSION_REGEX = "\\.zip";
  public static final String LOCAL_BACKUP_DIRECTORY_NAME = "zip-backups";
  public static final int MAXIMUM_NUMBER_OF_BACKUPS = 60; // Chosen so that you will have about weekly backups for a year, fortnightly over two years.
  public static final int NUMBER_OF_FIRST_WALLETS_TO_ALWAYS_KEEP = 2;
  public static final int NUMBER_OF_LAST_WALLETS_TO_ALWAYS_KEEP = 8; // Must be at least 1.
  private static final Logger log = LoggerFactory.getLogger(BackupManager.class);
  // Where wallets are stored
  private File applicationDataDirectory;
  // Where the cloud backups are stored (this is typically specified by the user and is a SpiderOak etc sync directory)
  private File cloudBackupDirectory;
  private SimpleDateFormat dateFormat;

  /**
   * Initialise the backup manager to use the specified cloudBackupDirectory.
   * All backups will be written and read from this directory
   */
  public void initialise(File applicationDataDirectory, File cloudBackupDirectory) {
    this.applicationDataDirectory = applicationDataDirectory;
    this.cloudBackupDirectory = cloudBackupDirectory;
  }

  /**
   * Get all the backups available in the cloud backup directory for the wallet id specified.
   * Wallet backups are called mbhd-[formatted wallet id]-timestamp and the specified wallet id is used to subset all backups
   */
  // TODO would also be nice to return the dates of the backups (from the timestamp) or return them sorted by age
  // then the latest backup can be used easily
  public List<File> getCloudBackups(WalletId walletId) {

    Preconditions.checkNotNull(cloudBackupDirectory);
    List<File> walletBackups = Lists.newArrayList();

    if (!cloudBackupDirectory.exists()) {
      // No directory - no backups
      return walletBackups;
    }

    File[] listOfFiles = cloudBackupDirectory.listFiles();

    // Look for filenames with format "mbhd"-[formatted wallet id ] -YYYYMMDDHHMMSS.zip"
    String backupRegex = WalletManager.WALLET_DIRECTORY_PREFIX + WalletManager.SEPARATOR + walletId.toFormattedString() +
            WalletManager.SEPARATOR + "\\d{" + BACKUP_SUFFIX_FORMAT.length() + "}" + BACKUP_ZIP_FILE_EXTENSION_REGEX;
    if (listOfFiles != null) {
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
          if (listOfFiles[i].getName().matches(backupRegex)) {
            if (listOfFiles[i].length() > 0) {
              walletBackups.add(listOfFiles[i]);
            }
          }
        }
      }
    }

    return walletBackups;
  }

  /**
   * Get all the backups available in the local backup directory for the wallet id specified.
   * Wallet backups are called mbhd-[formatted wallet id]-timestamp and the specified wallet id is used to subset all backups
   */
  // TODO would also be nice to return the dates of the backups (from the timestamp) or return them sorted by age
  // then the latest backup can be used easily
  public List<File> getLocalBackups(WalletId walletId) {

    List<File> walletBackups = Lists.newArrayList();

    // Find the wallet root directory for this wallet id
    File walletRootDirectory = WalletManager.getWalletDirectory(applicationDataDirectory.getAbsolutePath(), WalletManager.createWalletRoot(walletId));

    if (!walletRootDirectory.exists()) {
      // No directory - no backups
      return walletBackups;
    }

    // Find the zip-backups directory containing the local backups
    File zipBackupsDirectory = new File(walletRootDirectory.getAbsoluteFile() + File.separator + LOCAL_BACKUP_DIRECTORY_NAME);
    if (!zipBackupsDirectory.exists()) {
      // No directory - no backups
      return walletBackups;
    }

    File[] listOfFiles = zipBackupsDirectory.listFiles();

    // Look for filenames with format "mbhd"-[formatted wallet id ] -YYYYMMDDHHMMSS.zip"
    String backupRegex = WalletManager.WALLET_DIRECTORY_PREFIX + WalletManager.SEPARATOR + walletId.toFormattedString() +
            WalletManager.SEPARATOR + "\\d{" + BACKUP_SUFFIX_FORMAT.length() + "}" + BACKUP_ZIP_FILE_EXTENSION_REGEX;
    if (listOfFiles != null) {
      for (int i = 0; i < listOfFiles.length; i++) {
        if (listOfFiles[i].isFile()) {
          if (listOfFiles[i].getName().matches(backupRegex)) {
            if (listOfFiles[i].length() > 0) {
              walletBackups.add(listOfFiles[i]);
            }
          }
        }
      }
    }

    return walletBackups;
  }

  /**
   * Create a backup of the specified wallet id.
   * The wallet manager is interrogated to find the physical directory where the wallet is stored.
   * The whole directory is then copied and zipped into a timestamped backup file which is then written to the local and cloud backup directories
   *
   * @return The created local backup as a file
   */
  public File createBackup(WalletId walletId) throws IOException {
    Preconditions.checkNotNull(applicationDataDirectory);
    Preconditions.checkNotNull(walletId);

    // Find the wallet root directory for this wallet id
    File walletRootDirectory = WalletManager.getWalletDirectory(applicationDataDirectory.getAbsolutePath(), WalletManager.createWalletRoot(walletId));

    if (!walletRootDirectory.exists()) {
      throw new IOException("Directory " + walletRootDirectory + " does not exist. Cannot backup.");
    }

    File localBackupsDirectory = new File(walletRootDirectory.getAbsoluteFile() + File.separator + LOCAL_BACKUP_DIRECTORY_NAME);

    if (!localBackupsDirectory.exists()) {
      if (!localBackupsDirectory.mkdir()) {
        throw new IOException("Could not create local backup directory '" + localBackupsDirectory + "'");
      }
    }

    // Work out the filename for the backup
    if (dateFormat == null) {
      dateFormat = new SimpleDateFormat(BACKUP_SUFFIX_FORMAT);
    }

    String localBackupFilename = localBackupsDirectory.getAbsolutePath() + File.separator + WalletManager.WALLET_DIRECTORY_PREFIX + WalletManager.SEPARATOR + walletId.toFormattedString() +
            WalletManager.SEPARATOR + dateFormat.format(new Date()) + BACKUP_ZIP_FILE_EXTENSION;

    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(localBackupFilename))) {
      zipDirectory(walletRootDirectory.getAbsolutePath(), zos);
    }

    String cloudBackupFilename = cloudBackupDirectory.getAbsolutePath() + File.separator + WalletManager.WALLET_DIRECTORY_PREFIX + WalletManager.SEPARATOR + walletId.toFormattedString() +
            WalletManager.SEPARATOR + dateFormat.format(new Date()) + BACKUP_ZIP_FILE_EXTENSION;

    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(cloudBackupFilename))) {
      zipDirectory(walletRootDirectory.getAbsolutePath(), zos);
    }
    return new File(localBackupFilename);
  }


  /**
   * Load a backup file, copying all the backup files to the appropriate wallet root directory
   */

  public WalletId loadBackup(File backupFileToLoad) throws IOException {
    // Work out the walletId of the backup file being loaded
    String backupFilename = backupFileToLoad.getName();

    // Remove "mbhd-" prefix
    String walletRoot = backupFilename.replace(WalletManager.WALLET_DIRECTORY_PREFIX + WalletManager.SEPARATOR, "");

    // Remove  ".zip" suffix
    walletRoot = walletRoot.replace(BACKUP_ZIP_FILE_EXTENSION, "");

    // Remove the timestamp
    if (walletRoot.length() > WalletId.LENGTH_OF_FORMATTED_WALLETID) {
      walletRoot = walletRoot.substring(0, WalletId.LENGTH_OF_FORMATTED_WALLETID);
    }
    WalletId walletId = new WalletId(walletRoot);

    // Make a backup of all the current file in the wallet root directory if it exists (except zip-backups)
    File walletRootDirectory = WalletManager.getWalletDirectory(applicationDataDirectory.getAbsolutePath(), WalletManager.createWalletRoot(walletId));

    if (walletRootDirectory.exists()) {
      createBackup(walletId);
    }

    // Unzip the backup into the wallet root directory - this overwrites files if already present (hence the backup just done)
    unzip(backupFileToLoad.getAbsolutePath(), walletRootDirectory.getAbsolutePath());

    return walletId;
  }

  /**
   * This method
   * --Reads an input stream
   * --Writes the value to the output stream
   * --Uses 1KB buffer.
   */
  private void writeFile(InputStream in, OutputStream out)
          throws IOException {
    byte[] buffer = new byte[1024];
    int len;

    while ((len = in.read(buffer)) >= 0)
      out.write(buffer, 0, len);

    in.close();
    out.close();
  }

  /**
   * Copy the files in the specified dir2zip to the ZipOutputStream
   * The zip-backups are not stored in the backup (as they are backups themselves)
   *
   * @param dir2zip The directory holding the files to zip
   * @param zos     the ZipOutputStream to copy files into
   * @throws IOException
   */
  private void zipDirectory(String dir2zip, ZipOutputStream zos) throws IOException {
    // Create a new File object based on the directory we have to zip File
    File zipDir = new File(dir2zip);
    // Get a listing of the directory content
    String[] dirList = zipDir.list();
    byte[] readBuffer = new byte[2156];
    int bytesIn = 0;
    // Loop through dirList, and zip the files
    for (int i = 0; i < dirList.length; i++) {
      File f = new File(zipDir, dirList[i]);
      // Do not include the zip-backups files
      boolean ignoreFile = dirList[i].contains(LOCAL_BACKUP_DIRECTORY_NAME);
      if (f.isDirectory() && !ignoreFile) {
        // If the File object is a directory, call this
        // function again to add its content recursively
        String filePath = f.getPath();
        zipDirectory(filePath, zos);
        // loop again
        continue;
      }

      // Not a directory
      if (!ignoreFile) {
        FileInputStream fis = new FileInputStream(f);
        // Create a new zip entry - note that only the filename is used - not the whole path
        ZipEntry anEntry = new ZipEntry(f.getName());
        // Place the zip entry in the ZipOutputStream object
        zos.putNextEntry(anEntry);
        // Now write the content of the file to the ZipOutputStream
        while ((bytesIn = fis.read(readBuffer)) != -1) {
          zos.write(readBuffer, 0, bytesIn);
        }
        //Close the Stream
        fis.close();
      }
    }
  }

  private void unzip(String zipFileName, String directoryToExtractTo) throws IOException {
    Enumeration entriesEnum;
    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(zipFileName);

      entriesEnum = zipFile.entries();

      File directory = new File(directoryToExtractTo);

      /**
       * Check if the directory to extract to exists
       */
      if (!directory.exists()) {
        /**
         * If not, create a new one.
         */
        if (new File(directoryToExtractTo).mkdir()) {
          log.debug("Created extraction directory '" + directoryToExtractTo + "'");
        } else {
          throw new IOException("Could not create extraction directory '" + directoryToExtractTo + "'");
        }
      }

      while (entriesEnum.hasMoreElements()) {

        ZipEntry entry = (ZipEntry) entriesEnum.nextElement();

        if (entry.isDirectory()) {
          // TODO need to save rolling backups when they get put in
          /**
           * Currently not unzipping the directory structure.
           * All the files will be unzipped in a Directory
           *
           **/
        } else {

          log.debug("Extracting file: " + entry.getName());
          /**
           * The following logic will just extract the file name
           * and discard the directory
           */
          String name = entry.getName();
          int index = entry.getName().lastIndexOf(File.separator);
          if (index > 0 && index != name.length()) {
            name = entry.getName().substring(index + 1);
          }

          writeFile(zipFile.getInputStream(entry),
                  new BufferedOutputStream(new FileOutputStream(
                          directoryToExtractTo + name)));
        }
      }

    } finally {

      if (zipFile != null) {
        zipFile.close();
      }
    }

  }
}

//  /**
//   * Thin the wallet backups when they reach the MAXIMUM_NUMBER_OF_BACKUPS setting.
//   * Thinning is done by removing the most quickly replaced backup, except for the first and last few
//   * (as they are considered to be more valuable backups).
//   *
//   * @param backupDirectoryName
//   */
//  void thinBackupDirectory(String walletFilename, String backupSuffixText) {
//    if (dateFormat == null) {
//      dateFormat = new SimpleDateFormat(BACKUP_SUFFIX_FORMAT);
//    }
//
//    if (walletFilename == null || backupSuffixText == null) {
//      return;
//    }
//
//    // Find out how many wallet backups there are.
//    List<File> backupWallets = getWalletsInBackupDirectory(walletFilename, backupSuffixText);
//
//    if (backupWallets.size() < MAXIMUM_NUMBER_OF_BACKUPS) {
//      // No thinning required.
//      return;
//    }
//
//    // Work out the date the backup was made for each of the wallet.
//    // This is done using the timestamp rather than the write time of the file.
//    Map<File, Date> mapOfFileToBackupTimes = new HashMap<File, Date>();
//    for (int i = 0; i < backupWallets.size(); i++) {
//      String filename = backupWallets.get(i).getName();
//      if (filename.length() > 22) { // 22 = 1 for hyphen + 14 for timestamp + 1 for dot + 6 for wallet.
//        int startOfTimestamp = filename.length() - 21; // 21 = 14 for timestamp + 1 for dot + 6 for wallet.
//        String timestampText = filename.substring(startOfTimestamp, startOfTimestamp + BACKUP_SUFFIX_FORMAT.length());
//        try {
//          Date parsedTimestamp = dateFormat.parse(timestampText);
//          mapOfFileToBackupTimes.put(backupWallets.get(i), parsedTimestamp);
//        } catch (ParseException pe) {
//          // Cannot parse text - may be some other type of file the user has put in the directory.
//          log.debug("For wallet '" + filename + " could not parse the timestamp of '" + timestampText + "'.");
//        }
//      }
//    }
//
//    // See which wallet is most quickly replaced by another backup - this will be thinned.
//    int walletBackupToDeleteIndex = -1; // Not set yet.
//    long walletBackupToDeleteReplacementTimeMillis = Integer.MAX_VALUE; // How quickly the wallet was replaced by a later one.
//
//    for (int i = 0; i < backupWallets.size(); i++) {
//      if ((i < NUMBER_OF_FIRST_WALLETS_TO_ALWAYS_KEEP)
//              || (i >= backupWallets.size() - NUMBER_OF_LAST_WALLETS_TO_ALWAYS_KEEP)) {
//        // Keep the very first and last wallets always.
//      } else {
//        // If there is a data directory for the backup then it may have been opened
//        // in MultiBit so we will skip considering it for deletion.
//        String possibleDataDirectory = calculateTopLevelBackupDirectoryName(backupWallets.get(i));
//        boolean theWalletHasADataDirectory = (new File(possibleDataDirectory)).exists();
//
//        // Work out how quickly the wallet is replaced by the next backup.
//        Date thisWalletTimestamp = mapOfFileToBackupTimes.get(backupWallets.get(i));
//        Date nextWalletTimestamp = mapOfFileToBackupTimes.get(backupWallets.get(i + 1));
//        if (thisWalletTimestamp != null && nextWalletTimestamp != null) {
//          long deltaTimeMillis = nextWalletTimestamp.getTime() - thisWalletTimestamp.getTime();
//          if (deltaTimeMillis < walletBackupToDeleteReplacementTimeMillis && !theWalletHasADataDirectory) {
//            // This is the best candidate for deletion so far.
//            walletBackupToDeleteIndex = i;
//            walletBackupToDeleteReplacementTimeMillis = deltaTimeMillis;
//          }
//        }
//      }
//    }
//
//    if (walletBackupToDeleteIndex > -1) {
//      try {
//        // Secure delete the chosen backup wallet and its info file if present.
//        log.debug("To save space, secure deleting backup wallet '"
//                + backupWallets.get(walletBackupToDeleteIndex).getAbsolutePath() + "'.");
//        FileHandler.secureDelete(backupWallets.get(walletBackupToDeleteIndex));
//
//        String walletInfoBackupFilename = backupWallets.get(walletBackupToDeleteIndex).getAbsolutePath()
//                .replaceAll(BitcoinModel.WALLET_FILE_EXTENSION + "$", INFO_FILE_SUFFIX_STRING);
//        File walletInfoBackup = new File(walletInfoBackupFilename);
//        if (walletInfoBackup.exists()) {
//          log.debug("To save space, secure deleting backup info file '" + walletInfoBackup.getAbsolutePath() + "'.");
//          FileHandler.secureDelete(walletInfoBackup);
//        }
//      } catch (IOException ioe) {
//        log.error(ioe.getClass().getName() + " " + ioe.getMessage());
//      }
//    }
//  }
//
//  void copyFileAndEncrypt(File sourceFile, File destinationFile, CharSequence passwordToUse) throws IOException {
//    if (passwordToUse == null || passwordToUse.length() == 0) {
//      throw new IllegalArgumentException("Password cannot be blank");
//    }
//
//    if (destinationFile.exists()) {
//      throw new IllegalArgumentException("The destination file '" + destinationFile.getAbsolutePath() + "' already exists.");
//    } else {
//      // Attempt to create it
//      if (!destinationFile.createNewFile()) {
//        throw new IllegalArgumentException("The destination file '" + destinationFile.getAbsolutePath() + "' could not be created. Check permissions.");
//      }
//    }
//
//    // Read in the source file.
//    byte[] sourceFileUnencrypted = FileHandler.read(sourceFile);
//
//    // Encrypt the data.
//    byte[] salt = new byte[KeyCrypterScrypt.SALT_LENGTH];
//    secureRandom.nextBytes(salt);
//
//    Protos.ScryptParameters.Builder scryptParametersBuilder = Protos.ScryptParameters.newBuilder()
//            .setSalt(ByteString.copyFrom(salt));
//    Protos.ScryptParameters scryptParameters = scryptParametersBuilder.build();
//    KeyCrypterScrypt keyCrypter = new KeyCrypterScrypt(scryptParameters);
//    EncryptedPrivateKey encryptedData = keyCrypter.encrypt(sourceFileUnencrypted, keyCrypter.deriveKey(passwordToUse));
//
//    // The format of the encrypted data is:
//    // 7 magic bytes 'mendoza' in ASCII.
//    // 1 byte version number of format - initially set to 0
//    // 8 bytes salt
//    // 16 bytes iv
//    // rest of file is the encrypted byte data
//
//    FileOutputStream fileOutputStream = null;
//    try {
//      fileOutputStream = new FileOutputStream(destinationFile);
//      fileOutputStream.write(ENCRYPTED_FILE_FORMAT_MAGIC_BYTES);
//
//      // file format version.
//      fileOutputStream.write(FILE_ENCRYPTED_VERSION_NUMBER);
//
//      fileOutputStream.write(salt); // 8 bytes.
//      fileOutputStream.write(encryptedData.getInitialisationVector()); // 16 bytes.
//      System.out.println(Utils.bytesToHexString(encryptedData.getInitialisationVector()));
//
//      fileOutputStream.write(encryptedData.getEncryptedBytes());
//      System.out.println(Utils.bytesToHexString(encryptedData.getEncryptedBytes()));
//    } finally {
//      if (fileOutputStream != null) {
//        fileOutputStream.flush();
//        fileOutputStream.close();
//      }
//    }
//
//    // Read in the file again and decrypt it to make sure everything was ok.
//    byte[] phoenix = readFileAndDecrypt(destinationFile, passwordToUse);
//
//    if (!org.spongycastle.util.Arrays.areEqual(sourceFileUnencrypted, phoenix)) {
//      throw new IOException("File '" + sourceFile.getAbsolutePath() + "' was not correctly encrypted to file '" + destinationFile.getAbsolutePath());
//    }
//  }
//
//  public byte[] readFileAndDecrypt(File encryptedFile, CharSequence passwordToUse) throws IOException {
//    // Read in the encrypted file.
//    byte[] sourceFileEncrypted = FileHandler.read(encryptedFile);
//
//    // Check the first bytes match the magic number.
//    if (!org.spongycastle.util.Arrays.areEqual(ENCRYPTED_FILE_FORMAT_MAGIC_BYTES, org.spongycastle.util.Arrays.copyOfRange(sourceFileEncrypted, 0, ENCRYPTED_FILE_FORMAT_MAGIC_BYTES.length))) {
//      throw new IOException("File '" + encryptedFile.getAbsolutePath() + "' did not start with the correct magic bytes.");
//    }
//
//    // If the file is too short don't process it.
//    if (sourceFileEncrypted.length < ENCRYPTED_FILE_FORMAT_MAGIC_BYTES.length + 1 + KeyCrypterScrypt.SALT_LENGTH + KeyCrypterScrypt.BLOCK_LENGTH) {
//      throw new IOException("File '" + encryptedFile.getAbsolutePath() + "' is too short to decrypt. It is " + sourceFileEncrypted.length + " bytes long.");
//    }
//
//    // Check the format version.
//    String versionNumber = "" + sourceFileEncrypted[ENCRYPTED_FILE_FORMAT_MAGIC_BYTES.length];
//    //System.out.println("FileHandler - versionNumber = " + versionNumber);
//    if (!("0".equals(versionNumber))) {
//      throw new IOException("File '" + encryptedFile.getAbsolutePath() + "' did not have the expected version number of 0. It was " + versionNumber);
//    }
//
//    // Extract the salt.
//    byte[] salt = org.spongycastle.util.Arrays.copyOfRange(sourceFileEncrypted, ENCRYPTED_FILE_FORMAT_MAGIC_BYTES.length + 1, ENCRYPTED_FILE_FORMAT_MAGIC_BYTES.length + 1 + KeyCrypterScrypt.SALT_LENGTH);
//    //System.out.println("FileHandler - salt = " + Utils.bytesToHexString(salt));
//
//    // Extract the IV.
//    byte[] iv = org.spongycastle.util.Arrays.copyOfRange(sourceFileEncrypted, ENCRYPTED_FILE_FORMAT_MAGIC_BYTES.length + 1 + KeyCrypterScrypt.SALT_LENGTH, ENCRYPTED_FILE_FORMAT_MAGIC_BYTES.length + 1 + KeyCrypterScrypt.SALT_LENGTH + KeyCrypterScrypt.BLOCK_LENGTH);
//    //System.out.println("FileHandler - iv = " + Utils.bytesToHexString(iv));
//
//    // Extract the encrypted bytes.
//    byte[] encryptedBytes = org.spongycastle.util.Arrays.copyOfRange(sourceFileEncrypted, ENCRYPTED_FILE_FORMAT_MAGIC_BYTES.length + 1 + KeyCrypterScrypt.SALT_LENGTH + KeyCrypterScrypt.BLOCK_LENGTH, sourceFileEncrypted.length);
//    //System.out.println("FileHandler - encryptedBytes = " + Utils.bytesToHexString(encryptedBytes));
//
//    // Decrypt the data.
//    Protos.ScryptParameters.Builder scryptParametersBuilder = Protos.ScryptParameters.newBuilder().setSalt(ByteString.copyFrom(salt));
//    Protos.ScryptParameters scryptParameters = scryptParametersBuilder.build();
//    KeyCrypter keyCrypter = new KeyCrypterScrypt(scryptParameters);
//    EncryptedPrivateKey encryptedPrivateKey = new EncryptedPrivateKey(iv, encryptedBytes);
//    return keyCrypter.decrypt(encryptedPrivateKey, keyCrypter.deriveKey(passwordToUse));
//  }
//
//  /**
//   * Work out the best wallet backups to try to load
//   *
//   * @param walletFile
//   * @return Collection<String> The best wallets to try to load, in order of goodness.
//   */
//  Collection<String> calculateBestWalletBackups(File walletFile, WalletInfoData walletInfo) {
//    Collection<String> backupWalletsToTry = new ArrayList<String>();
//
//    // Get the name of the rolling backup file.
//    String walletBackupFilenameLong = walletInfo.getProperty(BitcoinModel.WALLET_BACKUP_FILE);
//    String walletBackupFilenameShort = null;
//    if (walletBackupFilenameLong != null && !"".equals(walletBackupFilenameLong)) {
//      File walletBackupFile = new File(walletBackupFilenameLong);
//      walletBackupFilenameShort = walletBackupFile.getName();
//      if (!walletBackupFile.exists()) {
//        walletBackupFilenameLong = null;
//        walletBackupFilenameShort = null;
//      }
//    } else {
//      // No backup file was listed in the info file. Maybe it is damaged so take the most recent
//      // file in the rolling backup directory, if there is one.
//      Collection<File> rollingWalletBackups = getWalletsInBackupDirectory(walletFile.getAbsolutePath(),
//              ROLLING_WALLET_BACKUP_DIRECTORY_NAME);
//      if (rollingWalletBackups != null && !rollingWalletBackups.isEmpty()) {
//        List<String> rollingWalletBackupFilenames = new ArrayList<String>();
//        for (File file : rollingWalletBackups) {
//          rollingWalletBackupFilenames.add(file.getAbsolutePath());
//        }
//        Collections.sort(rollingWalletBackupFilenames);
//        walletBackupFilenameLong = rollingWalletBackupFilenames.get(rollingWalletBackupFilenames.size() - 1);
//        walletBackupFilenameShort = (new File(walletBackupFilenameLong)).getName();
//      }
//    }
//
//    Collection<File> unencryptedWalletBackups = getWalletsInBackupDirectory(walletFile.getAbsolutePath(),
//            UNENCRYPTED_WALLET_BACKUP_DIRECTORY_NAME);
//    Collection<File> encryptedWalletBackups = getWalletsInBackupDirectory(walletFile.getAbsolutePath(),
//            ENCRYPTED_WALLET_BACKUP_DIRECTORY_NAME);
//
//    // Make a list of ALL the unencrypted and encrypted backup names and sort them.
//    // Because the backups have a timestamp YYYYMMDDHHMMSS sort in ascending order gives most recent - we will use this one.
//    List<String> encryptedAndUnencryptedFilenames = new ArrayList<String>();
//
//    // Sorting is done by the filename, keep track of the corresponding absolute path.
//    Map<String, String> shortNamesToLongMap = new HashMap<String, String>();
//
//    if (unencryptedWalletBackups != null) {
//      for (File file : unencryptedWalletBackups) {
//        encryptedAndUnencryptedFilenames.add(file.getName());
//        shortNamesToLongMap.put(file.getName(), file.getAbsolutePath());
//      }
//    }
//    if (encryptedWalletBackups != null) {
//      for (File file : encryptedWalletBackups) {
//        encryptedAndUnencryptedFilenames.add(file.getName());
//        // If there is a duplicate, encrypted wallets are preferred.
//        shortNamesToLongMap.put(file.getName(), file.getAbsolutePath());
//      }
//    }
//
//    Collections.sort(encryptedAndUnencryptedFilenames);
//
//    String bestCandidateShort = null;
//    String bestCandidateLong = null;
//    if (encryptedAndUnencryptedFilenames.size() > 0) {
//      bestCandidateShort = encryptedAndUnencryptedFilenames.get(encryptedAndUnencryptedFilenames.size() - 1);
//      if (bestCandidateShort != null) {
//        bestCandidateLong = shortNamesToLongMap.get(bestCandidateShort);
//      }
//    }
//    log.debug("For wallet '" + walletFile + "' the rolling backup file was '" + walletBackupFilenameLong + "' and the best encrypted/ unencrypted backup was '" + bestCandidateLong + "'");
//
//    if (walletBackupFilenameLong == null) {
//      if (bestCandidateLong == null) {
//        // No backups to try.
//      } else {
//        // bestCandidate only.
//        backupWalletsToTry.add(bestCandidateLong);
//      }
//    } else {
//      if (bestCandidateLong == null) {
//        // WalletBackupFilename only.
//        backupWalletsToTry.add(walletBackupFilenameLong);
//      } else {
//        // Have both. Try the most recent first (preferring the backups to the rolling backups if there is a tie).
//        if (walletBackupFilenameShort.compareTo(bestCandidateShort) <= 0) {
//          backupWalletsToTry.add(bestCandidateLong);
//          backupWalletsToTry.add(walletBackupFilenameLong);//    return backupWalletsToTry;
//  }
//        } else {
//          backupWalletsToTry.add(walletBackupFilenameLong);
//          backupWalletsToTry.add(bestCandidateLong);
//        }
//      }
//    }



