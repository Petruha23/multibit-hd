package org.multibit.hd.ui.views.components;

import org.multibit.hd.core.api.MessageKey;
import org.multibit.hd.ui.i18n.Languages;
import org.multibit.hd.ui.views.fonts.AwesomeDecorator;
import org.multibit.hd.ui.views.fonts.AwesomeIcon;
import org.multibit.hd.ui.views.themes.NimbusDecorator;
import org.multibit.hd.ui.views.themes.Themes;

import javax.swing.*;
import java.awt.*;

/**
 * <p>Utility to provide the following to UI:</p>
 * <ul>
 * <li>Provision of localised buttons</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class Buttons {

  /**
   * Utilities have no public constructor
   */
  private Buttons() {
  }

  /**
   * @return A new JButton with default styling
   */
  public static JButton newButton(Action action) {

    // The action resets all text
    JButton button = new JButton(action);

    // Ensure borders render smoothly
    button.setOpaque(false);

    // Reinforce the idea of clicking
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    // Ensure we use the correct component orientation
    button.applyComponentOrientation(Languages.currentComponentOrientation());

    return button;
  }

  /**
   * @param key    The resource key for the i18n string
   * @param values The values to apply to the string (can be null)
   *
   * @return A new JButton with default styling
   */
  public static JButton newButton(Action action, MessageKey key, Object... values) {

    // The action resets all text
    JButton button = newButton(action);

    button.setText(Languages.safeText(key, values));

    // TODO Accessibility API - append _ACCESSIBILITY to .name() ?


    return button;
  }

  /**
   * @param key    The resource key for the i18n string
   * @param values The values to apply to the string (can be null)
   *
   * @return A new JButton with default styling and text arranged below the icon
   */
  public static JButton newLargeButton(Action action, MessageKey key, Object... values) {

    JButton button = newButton(action, key, values);

    button.setVerticalTextPosition(SwingConstants.BOTTOM);
    button.setHorizontalTextPosition(SwingConstants.CENTER);

    return button;
  }

  /**
   * @param action The click action
   *
   * @return A new "Yes" button with icon
   */
  public static JButton newYesButton(Action action) {

    JButton button = newButton(action, MessageKey.YES);
    button.setAction(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.CHECK, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "No" button with icon
   */
  public static JButton newNoButton(Action action) {

    JButton button = newButton(action, MessageKey.NO);
    button.setAction(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.TIMES, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }


  /**
   * @param action The click action
   *
   * @return A new "Apply" button with icon
   */
  public static JButton newApplyButton(Action action) {

    JButton button = newButton(action, MessageKey.APPLY);
    button.setAction(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.EDIT, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "Undo" button with icon
   */
  public static JButton newUndoButton(Action action) {

    JButton button = newButton(action, MessageKey.UNDO);
    button.setAction(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.UNDO, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "Cancel" button with icon
   */
  public static JButton newCancelButton(Action action) {

    JButton button = newButton(action, MessageKey.CANCEL);
    button.setAction(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.TIMES, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "Exit" button with icon
   */
  public static JButton newExitButton(Action action) {

    JButton button = newButton(action, MessageKey.EXIT);
    button.setAction(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.SIGN_OUT, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    NimbusDecorator.applyThemeColor(Themes.currentTheme.dangerAlertBackground(), button);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "Next" button with icon (not prefixed)
   */
  public static JButton newNextButton(Action action) {

    JButton button = newButton(action, MessageKey.NEXT);
    button.setAction(action);

    AwesomeIcon icon = AwesomeDecorator.select(AwesomeIcon.ANGLE_DOUBLE_RIGHT, AwesomeIcon.ANGLE_DOUBLE_LEFT);

    AwesomeDecorator.applyIcon(icon, button, false, AwesomeDecorator.NORMAL_ICON_SIZE);


    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "Previous" button with icon
   */
  public static JButton newPreviousButton(Action action) {

    JButton button = newButton(action, MessageKey.PREVIOUS);
    button.setAction(action);

    AwesomeIcon icon = AwesomeDecorator.select(AwesomeIcon.ANGLE_DOUBLE_LEFT, AwesomeIcon.ANGLE_DOUBLE_RIGHT);

    AwesomeDecorator.applyIcon(icon, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "Finish" button with icon
   */
  public static JButton newFinishButton(Action action) {

    JButton button = newButton(action, MessageKey.FINISH);
    button.setAction(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.FLAG_CHECKERED, button, false, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "Send" button with icon
   */
  public static JButton newSendButton(Action action) {

    JButton button = newButton(action, MessageKey.SEND);
    button.setAction(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.CLOUD_UPLOAD, button, false, AwesomeDecorator.NORMAL_ICON_SIZE);

    NimbusDecorator.applyThemeColor(Themes.currentTheme.dangerAlertBackground(), button);

    return button;
  }

  /**
   * @param action The click action
   *
   * @return A new "Receive" button with icon
   */
  public static JButton newReceiveButton(Action action) {

    JButton button = newButton(action, MessageKey.RECEIVE);
    button.setAction(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.CLOUD_DOWNLOAD, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    NimbusDecorator.applyThemeColor(Themes.currentTheme.infoAlertBackground(), button);

    return button;
  }


  /**
   * @param action The click action
   *
   * @return A new "Refresh" button with icon
   */
  public static JButton newRefreshButton(Action action) {

    JButton button = newButton(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.UNDO, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "show" button with icon
   */
  public static JButton newShowButton(Action action) {

    JButton button = newButton(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.EYE, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "hide" button with icon
   */
  public static JButton newHideButton(Action action) {

    JButton button = newButton(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.EYE_SLASH, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "copy" button with icon
   */
  public static JButton newCopyButton(Action action) {

    JButton button = newButton(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.COPY, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "select file" button with icon
   */
  public static JButton newSelectFileButton(Action action) {

    JButton button = newButton(action);

    AwesomeDecorator.applyIcon(AwesomeIcon.FOLDER_OPEN, button, true, AwesomeDecorator.NORMAL_ICON_SIZE);

    return button;
  }

  /**
   * @param action The click action
   *
   * @return A new "send Bitcoin" wizard button with icon
   */
  public static JButton newSendBitcoinWizardButton(Action action) {

    JButton button = newLargeButton(action, MessageKey.SEND);

    AwesomeDecorator.applyIcon(AwesomeIcon.CLOUD_UPLOAD, button, true, JLabel.BOTTOM, AwesomeDecorator.LARGE_ICON_SIZE);

    return button;
  }

  /**
   * @param action The click action
   *
   * @return A new "Receive Bitcoin" wizard button with icon
   */
  public static JButton newReceiveBitcoinWizardButton(Action action) {

    JButton button = newLargeButton(action, MessageKey.RECEIVE);

    AwesomeDecorator.applyIcon(AwesomeIcon.CLOUD_DOWNLOAD, button, true, JLabel.BOTTOM, AwesomeDecorator.LARGE_ICON_SIZE);

    return button;

  }

  /**
   * @param action The click action
   *
   * @return A new "Welcome" wizard button with icon
   */
  public static JButton newShowWelcomeWizardButton(Action action) {

    JButton button = newLargeButton(action, MessageKey.SHOW_WELCOME_WIZARD);

    AwesomeDecorator.applyIcon(AwesomeIcon.WRENCH, button, true, JLabel.BOTTOM, AwesomeDecorator.LARGE_ICON_SIZE);

    return button;
  }
}
