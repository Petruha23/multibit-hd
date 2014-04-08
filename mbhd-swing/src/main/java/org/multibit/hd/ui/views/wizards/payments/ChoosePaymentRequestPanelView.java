package org.multibit.hd.ui.views.wizards.payments;

import com.google.common.base.Optional;
import net.miginfocom.swing.MigLayout;
import org.multibit.hd.core.dto.PaymentRequestData;
import org.multibit.hd.ui.MultiBitUI;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.ComboBoxes;
import org.multibit.hd.ui.views.components.Labels;
import org.multibit.hd.ui.views.components.Panels;
import org.multibit.hd.ui.views.components.panels.PanelDecorator;
import org.multibit.hd.ui.views.components.renderers.PaymentRequestDataListCellRenderer;
import org.multibit.hd.ui.views.fonts.AwesomeDecorator;
import org.multibit.hd.ui.views.fonts.AwesomeIcon;
import org.multibit.hd.ui.views.themes.Themes;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardPanelView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * <p>View to provide the following to UI:</p>
 * <ul>
 * <li>Choose payment request overview</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class ChoosePaymentRequestPanelView extends AbstractWizardPanelView<PaymentsWizardModel, ChoosePaymentRequestPanelModel> implements ActionListener {

  private static final Logger log = LoggerFactory.getLogger(ChoosePaymentRequestPanelView.class);

  private JComboBox<PaymentRequestData> paymentRequestDataJComboBox;
  private JLabel paymentRequestInfoLabel;
  private JLabel paymentRequestSelectLabel;

  /**
   * @param wizard The wizard managing the states
   */
  public ChoosePaymentRequestPanelView(AbstractWizard<PaymentsWizardModel> wizard, String panelName) {

    super(wizard, panelName, MessageKey.CHOOSE_PAYMENT_REQUEST, AwesomeIcon.FILE_TEXT_ALT);
  }

  @Override
  public void newPanelModel() {

    // Configure the panel model
    ChoosePaymentRequestPanelModel panelModel = new ChoosePaymentRequestPanelModel(
            getPanelName()
    );
    setPanelModel(panelModel);
  }

  @Override
  public void initialiseContent(JPanel contentPanel) {

    contentPanel.setLayout(new MigLayout(
            Panels.migXYLayout(),
            "[][][]", // Column constraints
            "[]30[]30[]" // Row constraints
    ));

    // Apply the theme
    contentPanel.setBackground(Themes.currentTheme.detailPanelBackground());

    List<PaymentRequestData> matchingPaymentRequestDataList = getWizardModel().getMatchingPaymentRequestList();
    paymentRequestDataJComboBox = ComboBoxes.newPaymentRequestsComboBox(this, matchingPaymentRequestDataList);
    paymentRequestDataJComboBox.setRenderer(new PaymentRequestDataListCellRenderer());
    // initialiseAndLoadWalletFromConfig model to first payment request in case the user uses the initial setting
    if (matchingPaymentRequestDataList.size() > 0) {
      getWizardModel().setPaymentRequestData(matchingPaymentRequestDataList.get(0));
    }

    paymentRequestInfoLabel = Labels.newBlankLabel();
    paymentRequestSelectLabel = Labels.newLabel(MessageKey.CHOOSE_PAYMENT_REQUEST_LABEL);
    AwesomeDecorator.bindIcon(AwesomeIcon.FILE_TEXT_ALT, paymentRequestSelectLabel, true, MultiBitUI.NORMAL_ICON_SIZE + 12);
    contentPanel.add(paymentRequestInfoLabel, "growx,span 2,wrap");

    contentPanel.add(paymentRequestSelectLabel, "shrink,aligny top");
    contentPanel.add(paymentRequestDataJComboBox, "growx,width min:350:,push,aligny top,wrap");
  }

  @Override
  protected void initialiseButtons(AbstractWizard<PaymentsWizardModel> wizard) {

    PanelDecorator.addExitCancelPreviousNext(this, wizard);
  }

  @Override
  public void afterShow() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        paymentRequestDataJComboBox.requestFocusInWindow();
        getNextButton().setEnabled(true);
      }
    });
  }

  @Override
  public boolean beforeShow() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        int size = getWizardModel().getMatchingPaymentRequestList().size();
        if (size == 1) {
          paymentRequestInfoLabel.setText(Languages.safeText(MessageKey.PAYMENT_REQUEST_INFO_SINGULAR));
        } else {
          paymentRequestInfoLabel.setText(Languages.safeText(MessageKey.PAYMENT_REQUEST_INFO_PLURAL, size));
        }

        boolean showComboBox = (size != 0);
        paymentRequestSelectLabel.setVisible(showComboBox);
        paymentRequestDataJComboBox.setVisible(showComboBox);
      }
    });
    return true;
  }

  @Override
  public void updateFromComponentModels(Optional componentModel) {
    // Do nothing - panel model is updated via an action and wizard model is not applicable
  }

  @SuppressWarnings("unchecked")
  @Override
  public void actionPerformed(ActionEvent e) {
    JComboBox<PaymentRequestData> source = (JComboBox<PaymentRequestData>) e.getSource();
    PaymentRequestData paymentRequestData = (PaymentRequestData) source.getSelectedItem();

    // Update the model
    getWizardModel().setPaymentRequestData(paymentRequestData);
  }
}
