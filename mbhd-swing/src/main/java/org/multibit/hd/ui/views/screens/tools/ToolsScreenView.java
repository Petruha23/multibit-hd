package org.multibit.hd.ui.views.screens.tools;

import net.miginfocom.swing.MigLayout;
import org.multibit.hd.core.dto.RAGStatus;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.multibit.hd.ui.MultiBitUI;
import org.multibit.hd.ui.events.controller.ControllerEvents;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.models.AlertModel;
import org.multibit.hd.ui.models.Models;
import org.multibit.hd.ui.views.components.Buttons;
import org.multibit.hd.ui.views.components.Panels;
import org.multibit.hd.ui.views.fonts.AwesomeIcon;
import org.multibit.hd.ui.views.screens.AbstractScreenView;
import org.multibit.hd.ui.views.screens.Screen;
import org.multibit.hd.ui.views.wizards.Wizards;
import org.multibit.hd.ui.views.wizards.welcome.WelcomeWizardState;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * <p>View to provide the following to application:</p>
 * <ul>
 * <li>Provision of components and layout for the tools detail display</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class ToolsScreenView extends AbstractScreenView<ToolsScreenModel> {

  private JButton welcomeWizard;

  /**
   * @param panelModel The model backing this panel view
   * @param screen     The screen to filter events from components
   * @param title      The key to the main title of this panel view
   */
  public ToolsScreenView(ToolsScreenModel panelModel, Screen screen, MessageKey title) {
    super(panelModel, screen, title);
  }

  @Override
  public void newScreenModel() {

  }

  @Override
  public JPanel initialiseScreenViewPanel() {

    CoreServices.uiEventBus.register(this);

    MigLayout layout = new MigLayout(
      Panels.migXYLayout(),
      "[]10[]", // Column constraints
      "[]50[]" // Row constraints
    );

    JPanel contentPanel = Panels.newPanel(layout);

    Action showWelcomeWizardAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        Panels.showLightBox(Wizards.newClosingWelcomeWizard(WelcomeWizardState.WELCOME_SELECT_LANGUAGE).getWizardScreenHolder());
      }
    };

    Action fireDemoAlertAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        AlertModel alertModel = Models.newAlertModel("Demonstrate alert", RAGStatus.RED);

        // Demonstrate a button
        AbstractAction action = new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            System.out.println("Done something");
          }
        };
        JButton button = Buttons.newAlertPanelButton(action, MessageKey.YES, AwesomeIcon.CHECK);
        alertModel.setButton(button);

        ControllerEvents.fireAddAlertEvent(alertModel);
      }
    };

    Action fireRedAlertAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        ControllerEvents.fireAddAlertEvent(Models.newAlertModel("Red alert", RAGStatus.RED));
      }
    };

    Action fireAmberAlertAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        ControllerEvents.fireAddAlertEvent(Models.newAlertModel("Amber alert", RAGStatus.AMBER));
      }
    };

    Action fireGreenAlertAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        ControllerEvents.fireAddAlertEvent(Models.newAlertModel("Green alert", RAGStatus.GREEN));
      }
    };

    Action fireHardwareWalletConnectedAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_CONNECTED);
      }
    };

    Action fireHardwareWalletDisconnectedAction = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_DISCONNECTED);
      }
    };

    contentPanel.add(
      Buttons.newShowWelcomeWizardButton(showWelcomeWizardAction),
      MultiBitUI.LARGE_BUTTON_MIG + ",align center,push,wrap");
    contentPanel.add(
      Buttons.newGenericToolButton(fireRedAlertAction, "Red alert"),
      MultiBitUI.LARGE_BUTTON_MIG + ",align center, push");
    contentPanel.add(
      Buttons.newGenericToolButton(fireAmberAlertAction, "Amber alert"),
      MultiBitUI.LARGE_BUTTON_MIG + ",align center, push");
    contentPanel.add(
      Buttons.newGenericToolButton(fireGreenAlertAction,"Green alert"),
      MultiBitUI.LARGE_BUTTON_MIG + ",align center, push,wrap");
    contentPanel.add(
      Buttons.newGenericToolButton(fireHardwareWalletConnectedAction,"Trezor connect"),
      MultiBitUI.LARGE_BUTTON_MIG + ",align center, push");
    contentPanel.add(
      Buttons.newGenericToolButton(fireHardwareWalletDisconnectedAction,"Trezor disconnect"),
      MultiBitUI.LARGE_BUTTON_MIG + ",align center, push,wrap");

    return contentPanel;
  }

  @Override
  public void afterShow() {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        welcomeWizard.requestFocusInWindow();
      }
    });

  }

}
