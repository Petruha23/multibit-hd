package org.multibit.hd.ui.views.wizards.welcome;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import org.multibit.hd.brit.exceptions.SeedPhraseException;
import org.multibit.hd.brit.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.seed_phrase.SeedPhraseGenerator;
import org.multibit.hd.brit.seed_phrase.SeedPhraseSize;
import org.multibit.hd.core.dto.BackupSummary;
import org.multibit.hd.core.dto.WalletId;
import org.multibit.hd.core.dto.WalletSummary;
import org.multibit.hd.core.managers.BackupManager;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.ui.events.view.VerificationStatusChangedEvent;
import org.multibit.hd.ui.events.view.ViewEvents;
import org.multibit.hd.ui.views.components.confirm_password.ConfirmPasswordModel;
import org.multibit.hd.ui.views.components.enter_seed_phrase.EnterSeedPhraseModel;
import org.multibit.hd.ui.views.components.select_backup_summary.SelectBackupSummaryModel;
import org.multibit.hd.ui.views.components.select_file.SelectFileModel;
import org.multibit.hd.ui.views.wizards.AbstractWizardModel;
import org.multibit.hd.ui.views.wizards.WizardButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

import static org.multibit.hd.ui.views.wizards.welcome.WelcomeWizardState.*;

/**
 * <p>Model object to provide the following to welcome wizard:</p>
 * <ul>
 * <li>Storage of panel data</li>
 * <li>State transition management</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class WelcomeWizardModel extends AbstractWizardModel<WelcomeWizardState> {

  private static final Logger log = LoggerFactory.getLogger(WelcomeWizardModel.class);

  /**
   * The "select wallet" radio button choice (as a state)
   */
  private WelcomeWizardState selectWalletChoice = WelcomeWizardState.CREATE_WALLET_SEED_PHRASE;

  /**
   * The optional "selected wallet ID" choice
   */
  private Optional<WalletId> walletId = Optional.absent();

  /**
   * The "restore method" indicates if a backup location or timestamp was selected
   */
  private WelcomeWizardState restoreMethod = WelcomeWizardState.RESTORE_WALLET_SELECT_BACKUP_LOCATION;

  /**
   * The seed phrase generator
   */
  private final SeedPhraseGenerator seedPhraseGenerator;

  /**
   * The confirm password model
   */
  private ConfirmPasswordModel confirmPasswordModel;

  private SelectFileModel cloudBackupLocationSelectFileModel;
  private SelectFileModel restoreLocationSelectFileModel;

  private EnterSeedPhraseModel createWalletEnterSeedPhraseModel;
  private EnterSeedPhraseModel restorePasswordEnterSeedPhraseModel;
  private EnterSeedPhraseModel restoreWalletEnterSeedPhraseModel;
  private EnterSeedPhraseModel restoreWalletBackupSeedPhraseModel;

  private List<String> createWalletSeedPhrase = Lists.newArrayList();
  private List<String> restoreWalletSeedPhrase = Lists.newArrayList();

  private final boolean restoring;

  private String actualSeedTimestamp;

  // Backup summaries for restoring a wallet
  private List<BackupSummary> backupSummaries = Lists.newArrayList();
  private SelectBackupSummaryModel selectBackupSummaryModel;
  private EnterSeedPhraseModel restoreWalletEnterTimestampModel;
  private ConfirmPasswordModel restoreWalletConfirmPasswordModel;

  private List<WalletSummary> walletList = Lists.newArrayList();

  /**
   * @param state The state object
   */
  public WelcomeWizardModel(WelcomeWizardState state) {
    super(state);

    log.debug("Welcome wizard starting in state '{}'", state.name());

    this.seedPhraseGenerator = CoreServices.newSeedPhraseGenerator();
    this.restoring = WelcomeWizardState.WELCOME_SELECT_WALLET.equals(state);

  }

  @Override
  public void showNext() {

    switch (state) {
      case WELCOME_LICENCE:
        state = WELCOME_SELECT_LANGUAGE;
        break;
      case WELCOME_SELECT_LANGUAGE:
        state = WELCOME_SELECT_WALLET;
        break;
      case WELCOME_SELECT_WALLET:
        state = selectWalletChoice;
        break;
      case CREATE_WALLET_SELECT_BACKUP_LOCATION:
        log.debug("Cloud backup location = " + getCloudBackupLocation());
        state = CREATE_WALLET_SEED_PHRASE;
        break;
      case CREATE_WALLET_SEED_PHRASE:
        state = CREATE_WALLET_CONFIRM_SEED_PHRASE;
        // Fail safe to ensure that the generator hasn't gone screwy
        Preconditions.checkState(SeedPhraseSize.isValid(getCreateWalletSeedPhrase().size()), "'actualSeedPhrase' is not a valid length");
        break;
      case CREATE_WALLET_CONFIRM_SEED_PHRASE:
        state = CREATE_WALLET_CREATE_PASSWORD;
        break;
      case CREATE_WALLET_CREATE_PASSWORD:
        state = CREATE_WALLET_REPORT;
        break;
      case CREATE_WALLET_REPORT:
        throw new IllegalStateException("'Next' is not permitted here");
      case RESTORE_PASSWORD_SEED_PHRASE:
        state = RESTORE_PASSWORD_REPORT;
        break;
      case RESTORE_WALLET_SEED_PHRASE:
        if (!isLocalZipBackupPresent()) {
          restoreMethod = RESTORE_WALLET_SELECT_BACKUP_LOCATION;
        } else {
          restoreMethod = RESTORE_WALLET_SELECT_BACKUP;
        }
        state = restoreMethod;
        break;
      case RESTORE_WALLET_SELECT_BACKUP_LOCATION:
        if (isCloudBackupPresent()) {
          restoreMethod = RESTORE_WALLET_SELECT_BACKUP;
        } else {
          restoreMethod = RESTORE_WALLET_TIMESTAMP;
        }
        state = restoreMethod;
        break;
      case RESTORE_WALLET_SELECT_BACKUP:
        state = RESTORE_WALLET_REPORT;
        break;
      case RESTORE_WALLET_TIMESTAMP:
        state = RESTORE_WALLET_REPORT;
        break;
      case RESTORE_WALLET_REPORT:
        throw new IllegalStateException("'Next' is not permitted here");
      case SELECT_WALLET_HARDWARE:
        // TODO Requires implementation
        state = selectWalletChoice;
        break;
      default:
        throw new IllegalStateException("Unknown state: " + state.name());
    }

  }

  @Override
  public void showPrevious() {

    switch (state) {
      case WELCOME_LICENCE:
        state = WELCOME_LICENCE;
        break;
      case WELCOME_SELECT_LANGUAGE:
        state = WELCOME_LICENCE;
        break;
      case WELCOME_SELECT_WALLET:
        state = WELCOME_SELECT_LANGUAGE;
        break;
      case CREATE_WALLET_SELECT_BACKUP_LOCATION:
        state = WELCOME_SELECT_WALLET;
        break;
      case CREATE_WALLET_SEED_PHRASE:
        state = CREATE_WALLET_SELECT_BACKUP_LOCATION;
        break;
      case CREATE_WALLET_REPORT:
        throw new IllegalStateException("'Previous' is not permitted here");
      case RESTORE_PASSWORD_SEED_PHRASE:
        state = WELCOME_SELECT_WALLET;
        break;
      case RESTORE_PASSWORD_REPORT:
        state = RESTORE_PASSWORD_SEED_PHRASE;
        break;
      case RESTORE_WALLET_SEED_PHRASE:
        state = WELCOME_SELECT_WALLET;
        break;
      case RESTORE_WALLET_SELECT_BACKUP_LOCATION:
        state = RESTORE_WALLET_SEED_PHRASE;
        break;
      case RESTORE_WALLET_SELECT_BACKUP:
        state = RESTORE_WALLET_SELECT_BACKUP_LOCATION;
        break;
      case RESTORE_WALLET_TIMESTAMP:
        state = RESTORE_WALLET_SELECT_BACKUP_LOCATION;
        break;
      case RESTORE_WALLET_REPORT:
        state = restoreMethod;
        break;
      case SELECT_WALLET_HARDWARE:
        state = WELCOME_SELECT_WALLET;
        break;
      default:
        throw new IllegalStateException("Unknown state: " + state.name());
    }
  }

  @Override
  public String getPanelName() {
    return state.name();
  }

  @Subscribe
  public void onVerificationStatusChangedEvent(VerificationStatusChangedEvent event) {

    ViewEvents.fireWizardButtonEnabledEvent(event.getPanelName(), WizardButton.NEXT, event.isOK());

  }

  /**
   * @return True if backups are present in the local zip backup location
   */
  private boolean isLocalZipBackupPresent() {

    // Ensure we start from a fresh list
    backupSummaries.clear();

    // Get the local backups
    Optional<WalletSummary> currentWalletSummary = WalletManager.INSTANCE.getCurrentWalletSummary();
    if (currentWalletSummary.isPresent()) {
      backupSummaries = BackupManager.INSTANCE.getLocalZipBackups(currentWalletSummary.get().getWalletId());
    }

    return !backupSummaries.isEmpty();
  }

  /**
   * @return True if backups are present in the cloud backup location given by the user
   */
  private boolean isCloudBackupPresent() {

    // Ensure we start from a fresh list
    backupSummaries.clear();

    // Get the cloud backups matching the entered seed
    EnterSeedPhraseModel restoreWalletEnterSeedPhraseModel = getRestoreWalletEnterSeedPhraseModel();

    SeedPhraseGenerator seedGenerator = new Bip39SeedPhraseGenerator();
    try {
      byte[] seed = seedGenerator.convertToSeed(restoreWalletEnterSeedPhraseModel.getSeedPhrase());
      WalletId walletId = new WalletId(seed);
      backupSummaries = BackupManager.INSTANCE.getCloudBackups(walletId, new File(getRestoreLocation()));

      return !backupSummaries.isEmpty();
    } catch (SeedPhraseException spe) {
      log.error("The seed phrase is incorrect.");
      return false;
    }
  }

  /**
   * @return The "select wallet" radio button choice
   */
  public WelcomeWizardState getSelectWalletChoice() {
    return selectWalletChoice;
  }

  /**
   * @return The selected wallet ID from the list
   */
  public Optional<WalletId> getWalletId() {
    return walletId;
  }

  /**
   * @return The "create wallet" seed phrase (generated)
   */
  public List<String> getCreateWalletSeedPhrase() {
    return createWalletSeedPhrase;
  }

  /**
   * @return The "restore wallet" seed phrase (user entered)
   */
  public List<String> getRestoreWalletSeedPhrase() {
    return restoreWalletSeedPhrase;
  }

  /**
   * The seed phrase entered on the RestoreWalletSeedPhrasePanelView
   */
  public EnterSeedPhraseModel getRestoreWalletEnterSeedPhraseModel() {
    return restoreWalletEnterSeedPhraseModel;
  }

  /**
   * The seed phrase entered on the RestoreWalletBackupPanelView
   */
  public EnterSeedPhraseModel getRestoreWalletBackupSeedPhraseModel() {
    return restoreWalletBackupSeedPhraseModel;
  }

  public WelcomeWizardState getRestoreMethod() {
    return restoreMethod;
  }

  /**
   * @return The actual generated seed timestamp (e.g. "1850/07")
   */
  public String getActualSeedTimestamp() {
    return actualSeedTimestamp;
  }

  /**
   * @return The user entered password for the creation process
   */
  public String getCreateWalletUserPassword() {
    return confirmPasswordModel.getValue();
  }

  /**
   * @return The user entered "cloud backup" location
   */
  public String getCloudBackupLocation() {
    return cloudBackupLocationSelectFileModel.getValue();
  }

  /**
   * @return The user entered restore location
   */
  public String getRestoreLocation() {
    return restoreLocationSelectFileModel.getValue();
  }

  /**
   * @return The seed phrase generator
   */
  public SeedPhraseGenerator getSeedPhraseGenerator() {
    return seedPhraseGenerator;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param confirmPasswordModel The "confirm password" model
   */
  void setConfirmPasswordModel(ConfirmPasswordModel confirmPasswordModel) {
    this.confirmPasswordModel = confirmPasswordModel;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param cloudBackupLocationSelectFileModel The "cloud backup location" select file model
   */
  void setCloudBackupLocationSelectFileModel(SelectFileModel cloudBackupLocationSelectFileModel) {
    this.cloudBackupLocationSelectFileModel = cloudBackupLocationSelectFileModel;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param restoreLocationSelectFileModel The "restore location" select file model
   */
  void setRestoreLocationSelectFileModel(SelectFileModel restoreLocationSelectFileModel) {
    this.restoreLocationSelectFileModel = restoreLocationSelectFileModel;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param selectWalletChoice The wallet selection from the radio buttons
   */
  void setSelectWalletChoice(WelcomeWizardState selectWalletChoice) {
    this.selectWalletChoice = selectWalletChoice;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param walletId The wallet ID selection from the list
   */
  void setWalletId(WalletId walletId) {
    this.walletId = Optional.fromNullable(walletId);
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param createWalletEnterSeedPhraseModel The "create wallet enter seed phrase" model
   */
  void setCreateWalletEnterSeedPhraseModel(EnterSeedPhraseModel createWalletEnterSeedPhraseModel) {
    this.createWalletEnterSeedPhraseModel = createWalletEnterSeedPhraseModel;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param restoreWalletEnterSeedPhraseModel The "restore wallet enter seed phrase" model
   */
  void setRestoreWalletEnterSeedPhraseModel(EnterSeedPhraseModel restoreWalletEnterSeedPhraseModel) {
    this.restoreWalletEnterSeedPhraseModel = restoreWalletEnterSeedPhraseModel;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param restoreWalletBackupSeedPhraseModel The "restore wallet backup seed phrase" model
   */
  void setRestoreWalletBackupSeedPhraseModel(EnterSeedPhraseModel restoreWalletBackupSeedPhraseModel) {
    this.restoreWalletBackupSeedPhraseModel = restoreWalletBackupSeedPhraseModel;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param createWalletSeedPhrase The actual seed phrase generated by the panel model
   */
  void setCreateWalletSeedPhrase(List<String> createWalletSeedPhrase) {
    this.createWalletSeedPhrase = createWalletSeedPhrase;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param actualSeedTimestamp The actual seed timestamp generated by the panel model
   */
  void setActualSeedTimestamp(String actualSeedTimestamp) {
    this.actualSeedTimestamp = actualSeedTimestamp;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param selectBackupSummaryModel The selected backup summary
   */
  void setSelectBackupSummaryModel(SelectBackupSummaryModel selectBackupSummaryModel) {
    this.selectBackupSummaryModel = selectBackupSummaryModel;
  }

  public SelectBackupSummaryModel getSelectBackupSummaryModel() {
    return selectBackupSummaryModel;
  }

  /**
   * @return The discovered backup summaries
   */
  public List<BackupSummary> getBackupSummaries() {
    return backupSummaries;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param restoreWalletEnterTimestampModel The "enter seed phrase" model for the restore wallet panel
   */
  void setRestoreWalletEnterTimestampModel(EnterSeedPhraseModel restoreWalletEnterTimestampModel) {
    this.restoreWalletEnterTimestampModel = restoreWalletEnterTimestampModel;
  }

  public EnterSeedPhraseModel getRestoreWalletEnterTimestampModel() {
    return restoreWalletEnterTimestampModel;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param restoreWalletConfirmPasswordModel The "enter password" model for the restore wallet panel
   */
  void setRestoreWalletConfirmPasswordModel(ConfirmPasswordModel restoreWalletConfirmPasswordModel) {
    this.restoreWalletConfirmPasswordModel = restoreWalletConfirmPasswordModel;
  }

  public ConfirmPasswordModel getRestoreWalletConfirmPasswordModel() {
    return restoreWalletConfirmPasswordModel;
  }

  /**
   * <p>Reduced visibility for panel models</p>
   *
   * @param enterSeedPhraseModel The "restore password" seed phrase mode
   */
  void setRestorePasswordEnterSeedPhraseModel(EnterSeedPhraseModel enterSeedPhraseModel) {
    this.restorePasswordEnterSeedPhraseModel = enterSeedPhraseModel;
  }

  public EnterSeedPhraseModel getRestorePasswordEnterSeedPhraseModel() {
    return restorePasswordEnterSeedPhraseModel;
  }

  /**
   * @return True if this wizard was created as the result of a restore operation
   */
  public boolean isRestoring() {
    return restoring;
  }

}
