package org.multibit.hd.ui.views.wizards.send_bitcoin;

import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import net.miginfocom.swing.MigLayout;
import org.multibit.hd.core.dto.CoreMessageKey;
import org.multibit.hd.core.events.BitcoinSentEvent;
import org.multibit.hd.core.events.TransactionCreationEvent;
import org.multibit.hd.core.events.TransactionSeenEvent;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.Labels;
import org.multibit.hd.ui.views.components.Panels;
import org.multibit.hd.ui.views.components.panels.PanelDecorator;
import org.multibit.hd.ui.views.fonts.AwesomeIcon;
import org.multibit.hd.ui.views.themes.Themes;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardPanelView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * <p>View to provide the following to UI:</p>
 * <ul>
 * <li>Show send Bitcoin progress report</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class SendBitcoinReportPanelView extends AbstractWizardPanelView<SendBitcoinWizardModel, SendBitcoinReportPanelModel> {

  private static final Logger log = LoggerFactory.getLogger(SendBitcoinReportPanelView.class);

  private JLabel transactionConstructionStatusSummary;
  private JLabel transactionConstructionStatusDetail;

  private JLabel transactionBroadcastStatusSummary;
  private JLabel transactionBroadcastStatusDetail;

  private JLabel transactionConfirmationStatus;

  /**
   * @param wizard The wizard managing the states
   */
  public SendBitcoinReportPanelView(AbstractWizard<SendBitcoinWizardModel> wizard, String panelName) {

    super(wizard, panelName, MessageKey.SEND_PROGRESS_TITLE, AwesomeIcon.CLOUD_UPLOAD);

  }

  @Override
  public void newPanelModel() {

    // Configure the panel model
    SendBitcoinReportPanelModel panelModel = new SendBitcoinReportPanelModel(
      getPanelName()
    );
    setPanelModel(panelModel);

    // Bind it to the wizard model
    getWizardModel().setReportPanelModel(panelModel);

  }

  @Override
  public void initialiseContent(JPanel contentPanel) {

    contentPanel.setLayout(new MigLayout(
      Panels.migXYLayout(),
      "[][][]", // Column constraints
      "[]10[]10[]" // Row constraints
    ));

    // Apply the theme
    contentPanel.setBackground(Themes.currentTheme.detailPanelBackground());

    transactionConstructionStatusSummary = Labels.newStatusLabel(Optional.<MessageKey>absent(), null, Optional.<Boolean>absent());
    transactionConstructionStatusDetail = Labels.newStatusLabel(Optional.<MessageKey>absent(), null, Optional.<Boolean>absent());

    transactionBroadcastStatusSummary = Labels.newStatusLabel(Optional.<MessageKey>absent(), null, Optional.<Boolean>absent());
    transactionBroadcastStatusDetail = Labels.newStatusLabel(Optional.<MessageKey>absent(), null, Optional.<Boolean>absent());
    transactionConfirmationStatus = Labels.newStatusLabel(Optional.<MessageKey>absent(), null, Optional.<Boolean>absent());

    contentPanel.add(transactionConstructionStatusSummary, "wrap");
    contentPanel.add(transactionConstructionStatusDetail, "wrap");
    contentPanel.add(transactionBroadcastStatusSummary, "wrap");
    contentPanel.add(transactionBroadcastStatusDetail, "wrap");
    contentPanel.add(transactionConfirmationStatus, "wrap");

  }

  @Override
  protected void initialiseButtons(AbstractWizard<SendBitcoinWizardModel> wizard) {

    PanelDecorator.addFinish(this, wizard);

  }

  @Override
  public void afterShow() {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        getFinishButton().requestFocusInWindow();
      }
    });

  }

  @Override
  public void updateFromComponentModels(Optional componentModel) {
    // Do nothing - panel model is updated via an action and wizard model is not applicable
  }

  @Subscribe
  public void onTransactionCreationEvent(TransactionCreationEvent transactionCreationEvent) {
    log.debug("Received the TransactionCreationEvent: " + transactionCreationEvent.toString());
    if (transactionCreationEvent.isTransactionCreationWasSuccessful()) {
      // We now have a transactionId so keep that in the panel model for filtering TransactionSeenEvents later
      getPanelModel().get().setTransactionId(transactionCreationEvent.getTransactionId());

      transactionConstructionStatusSummary.setText(Languages.safeText(CoreMessageKey.TRANSACTION_CREATED_OK));
      transactionConstructionStatusDetail.setText("");
      Labels.decorateStatusLabel(transactionConstructionStatusSummary, Optional.of(Boolean.TRUE));
    } else {
      String detailMessage = Languages.safeText(transactionCreationEvent.getTransactionCreationFailureReasonKey(),
        (Object[]) transactionCreationEvent.getTransactionCreationFailureReasonData());
      transactionConstructionStatusSummary.setText(Languages.safeText(CoreMessageKey.TRANSACTION_CREATION_FAILED));
      transactionConstructionStatusDetail.setText(detailMessage);
      Labels.decorateStatusLabel(transactionConstructionStatusSummary, Optional.of(Boolean.FALSE));
    }
  }

  @Subscribe
  public void onBitcoinSentEvent(BitcoinSentEvent bitcoinSentEvent) {
    log.debug("Received the BitcoinSentEvent: " + bitcoinSentEvent.toString());
    if (bitcoinSentEvent.isSendWasSuccessful()) {
      transactionBroadcastStatusSummary.setText(Languages.safeText(CoreMessageKey.BITCOIN_SENT_OK));
      Labels.decorateStatusLabel(transactionBroadcastStatusSummary, Optional.of(Boolean.TRUE));
    } else {
      String summaryMessage = Languages.safeText(CoreMessageKey.BITCOIN_SEND_FAILED);
      String detailMessage = Languages.safeText(bitcoinSentEvent.getSendFailureReasonKey(), (Object[]) bitcoinSentEvent.getSendFailureReasonData());
      transactionBroadcastStatusSummary.setText(summaryMessage);
      transactionBroadcastStatusDetail.setText(detailMessage);
      Labels.decorateStatusLabel(transactionBroadcastStatusSummary, Optional.of(Boolean.FALSE));
    }
  }

  @Subscribe
  public void onTransactionSeenEvent(TransactionSeenEvent transactionSeenEvent) {
    log.debug("Received the TransactionSeenEvent: " + transactionSeenEvent.toString());

    // Is this an event about the transaction that was just sent ?
    // If so, update the UI
    if (getPanelModel().get() != null) {
      String currentTransactionId = getPanelModel().get().getTransactionId();
      if (transactionSeenEvent.getTransactionId().equals(currentTransactionId)) {
        transactionConfirmationStatus.setText("Transaction id = " + transactionSeenEvent.getTransactionId());
      }
    }
  }
}
