package org.multibit.hd.core.managers;

import com.google.bitcoin.wallet.WalletFiles;
import com.google.common.base.Optional;
import org.multibit.hd.core.dto.WalletData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 *  <p>Listener to provide the following to WalletManager:</p>
 *  <ul>
 *  <li>Saving of rolling wallet backups and zip backups</li>
 *  </ul>
 *  </p>
 *  
 */
public class WalletAutoSaveListener implements WalletFiles.Listener {
  private static final Logger log = LoggerFactory.getLogger(WalletManager.class);

  @Override
  public void onBeforeAutoSave(File tempFile) {
    log.debug("Just about to save wallet to tempFile '" + tempFile.getAbsolutePath() + "'");
  }

  @Override
  public void onAfterAutoSave(File newlySavedFile) {
    log.debug("Have just saved wallet to newlySavedFile '" + newlySavedFile.getAbsolutePath() + "'");

    Optional<WalletData> walletData = WalletManager.INSTANCE.getCurrentWalletData();
    if (walletData.isPresent()) {
      try {
        // Save an encrypted copy of the wallet
        CharSequence password = walletData.get().getPassword();
        File encryptedWalletFile = WalletManager.makeAESEncryptedCopyAndDeleteOriginal(newlySavedFile, password);
        if (encryptedWalletFile != null && encryptedWalletFile.exists()) {
          log.debug("Save encrypted copy of wallet as '{}'. Size was {} bytes.", encryptedWalletFile.getAbsolutePath(), encryptedWalletFile.length());
        } else {
          log.debug("No encrypted copy of wallet '{}' made.", newlySavedFile.getAbsolutePath());
        }

        BackupManager.INSTANCE.createRollingBackup(walletData.get(), password);

        BackupManager.INSTANCE.createLocalAndCloudBackup(walletData.get().getWalletId());
        // TODO save the cloud backups at a slower rate than the local backups to save bandwidth - say a factor of 2 or 3
      } catch (IOException ioe) {
        log.error("No backups created. The error was '" + ioe.getMessage() + "'.");
      }
    } else {
      log.error("No AES wallet encryption nor backups created as there was no wallet data to backup.");
    }
  }
}
