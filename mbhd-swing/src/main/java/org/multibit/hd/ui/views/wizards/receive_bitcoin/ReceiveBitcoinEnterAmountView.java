package org.multibit.hd.ui.views.wizards.receive_bitcoin;

import com.google.bitcoin.core.Utils;
import com.google.bitcoin.uri.BitcoinURI;
import net.miginfocom.swing.MigLayout;
import org.multibit.hd.core.api.MessageKey;
import org.multibit.hd.ui.events.view.ViewEvents;
import org.multibit.hd.ui.views.components.*;
import org.multibit.hd.ui.views.components.display_address.DisplayBitcoinAddressModel;
import org.multibit.hd.ui.views.components.display_address.DisplayBitcoinAddressView;
import org.multibit.hd.ui.views.components.display_qrcode.DisplayQRCodeModel;
import org.multibit.hd.ui.views.components.display_qrcode.DisplayQRCodeView;
import org.multibit.hd.ui.views.components.enter_amount.EnterAmountModel;
import org.multibit.hd.ui.views.components.enter_amount.EnterAmountView;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardView;
import org.multibit.hd.ui.views.wizards.WizardButton;
import org.multibit.hd.ui.views.wizards.welcome.WelcomeWizardState;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.math.BigInteger;

/**
 * <p>View to provide the following to UI:</p>
 * <ul>
 * <li>Receive bitcoin: Enter amount</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */

public class ReceiveBitcoinEnterAmountView extends AbstractWizardView<ReceiveBitcoinWizardModel, ReceiveBitcoinEnterAmountPanelModel> {

  // Panel specific components
  private ModelAndView<EnterAmountModel, EnterAmountView> enterAmountMaV;
  private ModelAndView<DisplayBitcoinAddressModel, DisplayBitcoinAddressView> displayBitcoinAddressMaV;
  private ModelAndView<DisplayQRCodeModel, DisplayQRCodeView> displayQRCodeMaV;

  private JTextField label;

  private JButton showQRCode;

  /**
   * @param wizard The wizard managing the states
   */
  public ReceiveBitcoinEnterAmountView(AbstractWizard<ReceiveBitcoinWizardModel> wizard, String panelName) {

    super(wizard.getWizardModel(), panelName, MessageKey.RECEIVE_BITCOIN_TITLE);

    PanelDecorator.addCancelFinish(this, wizard);

  }

  @Override
  public JPanel newWizardViewPanel() {

    enterAmountMaV = Components.newEnterAmountMaV(getWizardViewPanelName());

    // TODO Link this to the recipient address service
    displayBitcoinAddressMaV = Components.newDisplayBitcoinAddressMaV("1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty");

    // Create the QR code display
    displayQRCodeMaV = Components.newDisplayQRCodeMaV();

    label = TextBoxes.newEnterLabel();
    showQRCode = Buttons.newQRCodeButton(getShowQRCodePopoverAction());

    // Configure the panel model
    setPanelModel(new ReceiveBitcoinEnterAmountPanelModel(
      getWizardViewPanelName(),
      enterAmountMaV.getModel(),
      displayBitcoinAddressMaV.getModel()
    ));

    JPanel panel = Panels.newPanel(new MigLayout(
      "fillx,insets 0", // Layout constraints
      "[][][]", // Column constraints
      "[]10[]" // Row constraints
    ));

    panel.add(enterAmountMaV.getView().newPanel(),"span 3,wrap");
    panel.add(Labels.newRecipient());
    panel.add(displayBitcoinAddressMaV.getView().newPanel(),"growx,push");
    panel.add(showQRCode,"wrap");
    panel.add(Labels.newTransactionLabel());
    panel.add(label,"span 2,wrap");

    return panel;
  }

  @Override
  public void fireInitialStateViewEvents() {

    // Disable the finish button
    ViewEvents.fireWizardButtonEnabledEvent(WelcomeWizardState.CREATE_WALLET_REPORT.name(), WizardButton.FINISH, false);

  }

  /**
   * @return A new action for showing the QR code popover
   */
  private Action getShowQRCodePopoverAction() {

    // Show or hide the QR code
    return new AbstractAction() {

      @Override
      public void actionPerformed(ActionEvent e) {

        ReceiveBitcoinEnterAmountPanelModel model = getPanelModel().get();

        String bitcoinAddress = model.getDisplayBitcoinAddressModel().getValue();
        BigInteger amount = Utils.toNanoCoins(model.getEnterAmountModel().getRawBitcoinAmount().toPlainString());

        // Form a Bitcoin URI from the contents
        String bitcoinUri = BitcoinURI.convertToBitcoinURI(
          bitcoinAddress,
          amount,
          label.getText(),
          null
        );

        displayQRCodeMaV.getModel().setValue(bitcoinUri);

        // Show the QR code as a popover
        Panels.showLightBoxPopover(displayQRCodeMaV.getView().newPanel());

      }

    };
  }

  @Override
  public boolean updateFromComponentModels() {

    enterAmountMaV.getView().updateModel();

    // The panel model has changed so alert the wizard
    ViewEvents.fireWizardPanelModelChangedEvent(getWizardViewPanelName(), getPanelModel());

    return false;
  }

}