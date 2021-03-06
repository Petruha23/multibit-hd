package org.multibit.hd.ui.views.wizards.empty_wallet;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import net.miginfocom.swing.MigLayout;
import org.multibit.hd.ui.events.view.ViewEvents;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.Components;
import org.multibit.hd.ui.views.components.ModelAndView;
import org.multibit.hd.ui.views.components.Panels;
import org.multibit.hd.ui.views.components.enter_password.EnterPasswordModel;
import org.multibit.hd.ui.views.components.enter_password.EnterPasswordView;
import org.multibit.hd.ui.views.components.enter_recipient.EnterRecipientModel;
import org.multibit.hd.ui.views.components.enter_recipient.EnterRecipientView;
import org.multibit.hd.ui.views.components.panels.PanelDecorator;
import org.multibit.hd.ui.views.fonts.AwesomeIcon;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardPanelView;
import org.multibit.hd.ui.views.wizards.WizardButton;

import javax.swing.*;

/**
 * <p>Wizard to provide the following to UI:</p>
 * <ul>
 * <li>Empty wallet: Enter details</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class EmptyWalletEnterDetailsPanelView extends AbstractWizardPanelView<EmptyWalletWizardModel, EmptyWalletEnterDetailsPanelModel> {

  // Panel specific components
  private ModelAndView<EnterRecipientModel, EnterRecipientView> enterRecipientMaV;
  private ModelAndView<EnterPasswordModel, EnterPasswordView> enterPasswordMaV;

  /**
   * @param wizard    The wizard managing the states
   * @param panelName The panel name
   */
  public EmptyWalletEnterDetailsPanelView(AbstractWizard<EmptyWalletWizardModel> wizard, String panelName) {

    super(wizard, panelName, MessageKey.EMPTY_WALLET_TITLE, AwesomeIcon.FIRE);

  }

  @Override
  public void newPanelModel() {

    // Require a reference for the model
    enterRecipientMaV = Components.newEnterRecipientMaV(getPanelName());
    enterPasswordMaV = Components.newEnterPasswordMaV(getPanelName());

    // Configure the panel model
    final EmptyWalletEnterDetailsPanelModel panelModel = new EmptyWalletEnterDetailsPanelModel(
      getPanelName(),
      enterRecipientMaV.getModel(),
      enterPasswordMaV.getModel()
    );
    setPanelModel(panelModel);

    // Bind it to the wizard model
    getWizardModel().setEnterDetailsPanelModel(panelModel);

    // Register components
    registerComponents(enterRecipientMaV, enterPasswordMaV);

  }

  @Override
  public void initialiseContent(JPanel contentPanel) {

    contentPanel.setLayout(new MigLayout(
      Panels.migXYLayout(),
      "[]", // Column constraints
      "[]10[]" // Row constraints
    ));

    contentPanel.add(enterRecipientMaV.getView().newComponentPanel(), "wrap");
    contentPanel.add(enterPasswordMaV.getView().newComponentPanel(), "wrap");

  }

  @Override
  protected void initialiseButtons(AbstractWizard<EmptyWalletWizardModel> wizard) {

    PanelDecorator.addExitCancelNext(this, wizard);

  }

  @Override
  public void fireInitialStateViewEvents() {

    // Next button starts off disabled
    ViewEvents.fireWizardButtonEnabledEvent(getPanelName(), WizardButton.NEXT, false);

  }

  @Override
  public void afterShow() {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        enterRecipientMaV.getView().requestInitialFocus();
      }
    });

  }

  @Override
  public void updateFromComponentModels(Optional componentModel) {

    // No need to update the wizard it has the references

    // Determine any events
    ViewEvents.fireWizardButtonEnabledEvent(
      getPanelName(),
      WizardButton.NEXT,
      isNextEnabled()
    );

  }

  /**
   * @return True if the "next" button should be enabled
   */
  private boolean isNextEnabled() {

    // Recipient must be present
    boolean recipientOK = getPanelModel().get()
      .getEnterRecipientModel()
      .getRecipient()
      .isPresent();

    // Password only has to contain something to qualify
    boolean passwordOK = !Strings.isNullOrEmpty(getPanelModel().get()
      .getEnterPasswordModel()
      .getValue());

    return recipientOK && passwordOK;
  }

}