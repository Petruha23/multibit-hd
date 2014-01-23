package org.multibit.hd.ui.views.components;

import com.google.common.base.Preconditions;
import net.miginfocom.swing.MigLayout;
import org.multibit.hd.core.api.MessageKey;
import org.multibit.hd.ui.views.themes.Themes;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardView;
import org.multibit.hd.ui.views.wizards.WizardModel;

import javax.swing.*;
import java.awt.*;

/**
 * <p>Decorator to provide the following to panels:</p>
 * <ul>
 * <li>Application of various themed styles to panels</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class PanelDecorator {

  /**
   * Utilities have a private constructor
   */
  private PanelDecorator() {
  }

  /**
   * <p>Create the standard "wizard" theme</p>
   *
   * @param wizardPanel The wizard panel to decorate (arranged as [title][dataPanel][buttons])
   * @param dataPanel   The data panel sandwiched into the wizard
   */
  public static void applyWizardTheme(JPanel wizardPanel, JPanel dataPanel, MessageKey titleKey) {

    Preconditions.checkNotNull(wizardPanel, "'wizardPanel' must be present");
    Preconditions.checkNotNull(dataPanel, "'dataPanel' must be present");
    Preconditions.checkNotNull(titleKey, "'titleKey' must be present");

    // Standard wizard layout
    MigLayout layout = new MigLayout(
      "fillx,insets 5", // Layout constraints
      "[][][][]", // Column constraints
      "[shrink]10[grow]10[]" // Row constraints
    );
    wizardPanel.setLayout(layout);

    // Apply the theme
    wizardPanel.setBackground(Themes.currentTheme.detailPanelBackground());

    // Add the wizard components
    wizardPanel.add(Labels.newTitleLabel(titleKey), "span 4,shrink,wrap,aligny top");
    wizardPanel.add(dataPanel, "span 4,grow,wrap");

  }

  /**
   * <p>Add an exit, cancel combination</p>
   *
   * @param view   The view containing the panel to decorate
   * @param wizard The wizard providing the actions
   * @param <M>    The wizard model type
   * @param <P>    The wizard panel model type
   */
  public static <M extends WizardModel, P> void addExitCancel(AbstractWizardView<M, P> view, AbstractWizard<M> wizard) {

    Preconditions.checkNotNull(view, "'view' must be present");
    Preconditions.checkNotNull(view, "'wizard' must be present");
    Preconditions.checkNotNull(view.getWizardPanel(), "'wizardPanel' must be present");

    // Use the wizard panel
    JPanel wizardPanel = view.getWizardPanel();

    addExit(view, wizard, wizardPanel);
    addCancel(view, wizard, wizardPanel);

  }

  /**
   * <p>Add a finish only button</p>
   *
   * @param view   The view containing the panel to decorate
   * @param wizard The wizard providing the actions
   * @param <M>    The wizard model type
   * @param <P>    The wizard panel model type
   */
  public static <M extends WizardModel, P> void addFinish(AbstractWizardView<M, P> view, AbstractWizard<M> wizard) {

    Preconditions.checkNotNull(view, "'view' must be present");
    Preconditions.checkNotNull(view, "'wizard' must be present");

    // Use the wizard panel
    JPanel wizardPanel = view.getWizardPanel();

    // Add an invisible button to push the finish
    JButton empty = Buttons.newExitButton(null);
    empty.setVisible(false);

    wizardPanel.add(empty,"cell 0 2,push");

    addFinish(view, wizard, wizardPanel);

  }

  /**
   * <p>Add an exit/cancel, previous, finish combination</p>
   *
   * @param view   The view containing the panel to decorate
   * @param wizard The wizard providing the actions
   * @param <M>    The wizard model type
   * @param <P>    The wizard panel model type
   */
  public static <M extends WizardModel, P> void addExitCancelPreviousFinish(AbstractWizardView<M, P> view, AbstractWizard<M> wizard) {

    Preconditions.checkNotNull(view, "'view' must be present");
    Preconditions.checkNotNull(view, "'wizard' must be present");
    Preconditions.checkNotNull(view.getWizardPanel(), "'wizardPanel' must be present");

    // Use the wizard panel
    JPanel wizardPanel = view.getWizardPanel();

    addExitCancel(view, wizard, wizardPanel);
    addPrevious(view, wizard, wizardPanel);
    addFinish(view, wizard, wizardPanel);

  }

  /**
   * <p>Add an exit/cancel, next button combination</p>
   *
   * @param view   The view containing the panel to decorate
   * @param wizard The wizard providing the actions
   * @param <M>    The wizard model type
   * @param <P>    The wizard panel model type
   */
  public static <M extends WizardModel, P> void addExitCancelNext(AbstractWizardView<M, P> view, AbstractWizard<M> wizard) {

    Preconditions.checkNotNull(view, "'view' must be present");
    Preconditions.checkNotNull(view, "'wizard' must be present");
    Preconditions.checkNotNull(view.getWizardPanel(), "'wizardPanel' must be present");

    // Use the wizard panel
    JPanel wizardPanel = view.getWizardPanel();

    addExitCancel(view, wizard, wizardPanel);
    addNext(view, wizard, wizardPanel);

  }

  /**
   * <p>Add an exit/cancel, previous, next button combination</p>
   *
   * @param view   The view containing the panel to decorate
   * @param wizard The wizard providing the actions
   * @param <M>    The wizard model type
   * @param <P>    The wizard panel model type
   */
  public static <M extends WizardModel, P> void addExitCancelPreviousNext(AbstractWizardView<M, P> view, AbstractWizard<M> wizard) {

    Preconditions.checkNotNull(view, "'view' must be present");
    Preconditions.checkNotNull(view, "'wizard' must be present");
    Preconditions.checkNotNull(view.getWizardPanel(), "'wizardPanel' must be present");

    // Use the current panel
    JPanel wizardPanel = view.getWizardPanel();

    addExitCancel(view, wizard, wizardPanel);
    addPrevious(view, wizard, wizardPanel);
    addNext(view, wizard, wizardPanel);

  }

  /**
   * <p>Add a cancel, previous, send(next) button combination</p>
   *
   * @param view   The view containing the panel to decorate
   * @param wizard The wizard providing the actions
   * @param <M>    The wizard model type
   * @param <P>    The wizard panel model type
   */
  public static <M extends WizardModel, P> void addCancelPreviousSend(AbstractWizardView<M, P> view, AbstractWizard<M> wizard) {

    Preconditions.checkNotNull(view, "'view' must be present");
    Preconditions.checkNotNull(view, "'wizard' must be present");
    Preconditions.checkNotNull(view.getWizardPanel(), "'wizardPanel' must be present");

    // Use the current panel
    JPanel wizardPanel = view.getWizardPanel();

    addCancel(view, wizard, wizardPanel);
    addPrevious(view, wizard, wizardPanel);

    // Replace next with send
    view.setNextButton(Buttons.newSendButton(wizard.getNextAction(view)));
    wizardPanel.add(view.getNextButton(), "cell 3 2");

  }

  /**
   * <p>Add a cancel, previous, next button combination</p>
   *
   * @param view   The view containing the panel to decorate
   * @param wizard The wizard providing the actions
   * @param <M>    The wizard model type
   * @param <P>    The wizard panel model type
   */
  public static <M extends WizardModel, P> void addCancelPreviousNext(AbstractWizardView<M, P> view, AbstractWizard<M> wizard) {

    Preconditions.checkNotNull(view, "'view' must be present");
    Preconditions.checkNotNull(view, "'wizard' must be present");
    Preconditions.checkNotNull(view.getWizardPanel(), "'wizardPanel' must be present");

    // Use the wizard panel
    JPanel wizardPanel = view.getWizardPanel();

    addCancel(view, wizard, wizardPanel);
    addPrevious(view, wizard, wizardPanel);
    addNext(view, wizard, wizardPanel);

  }

  /**
   * <p>Make the panel have the "danger" theme</p>
   *
   * @param panel The panel to decorate
   */
  public static void applyDangerTheme(JPanel panel) {

    Preconditions.checkNotNull(panel, "'panel' must be present");

    Color background = Themes.currentTheme.dangerAlertBackground();
    Color border = Themes.currentTheme.dangerAlertBorder();
    Color text = Themes.currentTheme.dangerAlertText();

    applyTheme(panel, background, border, text);

  }

  /**
   * <p>Make the panel have the "danger faded" theme</p>
   *
   * @param panel The panel to decorate
   */
  public static void applyDangerFadedTheme(JPanel panel) {

    Preconditions.checkNotNull(panel, "'panel' must be present");

    Color background = Themes.currentTheme.dangerAlertFadedBackground();
    Color border = Themes.currentTheme.dangerAlertBorder();
    Color text = Themes.currentTheme.dangerAlertText();

    applyTheme(panel, background, border, text);

  }

  /**
   * <p>Make the panel have the "warning" theme</p>
   *
   * @param panel The panel to decorate
   */
  public static void applyWarningTheme(JPanel panel) {

    Preconditions.checkNotNull(panel, "'panel' must be present");

    Color background = Themes.currentTheme.warningAlertBackground();
    Color border = Themes.currentTheme.warningAlertBorder();
    Color text = Themes.currentTheme.warningAlertText();

    applyTheme(panel, background, border, text);

  }

  /**
   * <p>Make the panel have the "success" theme</p>
   *
   * @param panel The panel to decorate
   */
  public static void applySuccessTheme(JPanel panel) {

    Preconditions.checkNotNull(panel, "'panel' must be present");

    Color background = Themes.currentTheme.successAlertBackground();
    Color border = Themes.currentTheme.successAlertBorder();
    Color text = Themes.currentTheme.successAlertText();

    applyTheme(panel, background, border, text);

  }

  /**
   * <p>Make the panel have the "success faded" theme</p>
   *
   * @param panel The panel to decorate
   */
  public static void applySuccessFadedTheme(JPanel panel) {

    Preconditions.checkNotNull(panel, "'panel' must be present");

    Color background = Themes.currentTheme.successAlertFadedBackground();
    Color border = Themes.currentTheme.successAlertBorder();
    Color text = Themes.currentTheme.successAlertText();

    applyTheme(panel, background, border, text);

  }

  /**
   * <p>Apply panel colours</p>
   *
   * @param panel      The target panel
   * @param background The background colour
   * @param border     The border colour
   * @param text       The text colour
   */
  private static void applyTheme(JPanel panel, Color background, Color border, Color text) {

    Preconditions.checkNotNull(panel, "'panel' must be present");

    panel.setBackground(background);
    panel.setForeground(text);

    // Use a simple rounded border
    panel.setBorder(new TextBubbleBorder(border));

    for (Component component : panel.getComponents()) {
      if (component instanceof JLabel) {
        component.setForeground(text);
      }
    }

  }

  /**
   * <p>Add a "next" button into the standard cell</p>
   *
   * @param view        The view containing the panel to decorate
   * @param wizard      The wizard providing the actions
   * @param wizardPanel The wizard panel providing the layout
   * @param <M>         The wizard model type
   * @param <P>         The wizard panel model type
   */
  private static <M extends WizardModel, P> void addNext(AbstractWizardView<M, P> view, AbstractWizard<M> wizard, JPanel wizardPanel) {
    view.setNextButton(Buttons.newNextButton(wizard.getNextAction(view)));
    wizardPanel.add(view.getNextButton(), "cell 3 2");
  }

  /**
   * <p>Add a "previous" button into the standard cell</p>
   *
   * @param view        The view containing the panel to decorate
   * @param wizard      The wizard providing the actions
   * @param wizardPanel The wizard panel providing the layout
   * @param <M>         The wizard model type
   * @param <P>         The wizard panel model type
   */
  private static <M extends WizardModel, P> void addPrevious(AbstractWizardView<M, P> view, AbstractWizard<M> wizard, JPanel wizardPanel) {
    view.setPreviousButton(Buttons.newPreviousButton(wizard.getPreviousAction(view)));
    wizardPanel.add(view.getPreviousButton(), "cell 2 2");
  }

  /**
   * <p>Add an "exit/cancel" button into the standard cell</p>
   *
   * @param view        The view containing the panel to decorate
   * @param wizard      The wizard providing the actions
   * @param wizardPanel The wizard panel providing the layout
   * @param <M>         The wizard model type
   * @param <P>         The wizard panel model type
   */
  private static <M extends WizardModel, P> void addExitCancel(AbstractWizardView<M, P> view, AbstractWizard<M> wizard, JPanel wizardPanel) {

    if (wizard.isExiting()) {

      view.setExitButton(Buttons.newExitButton(wizard.getExitAction()));
      wizardPanel.add(view.getExitButton(), "cell 0 2,push");

    } else {

      view.setCancelButton(Buttons.newCancelButton(wizard.getCancelAction()));
      wizardPanel.add(view.getCancelButton(), "cell 0 2,push");

    }

  }

  /**
   * <p>Add "exit" button into the standard cell</p>
   *
   * @param view        The view containing the panel to decorate
   * @param wizard      The wizard providing the actions
   * @param wizardPanel The wizard panel providing the layout
   * @param <M>         The wizard model type
   * @param <P>         The wizard panel model type
   */
  private static <M extends WizardModel, P> void addExit(AbstractWizardView<M, P> view, AbstractWizard<M> wizard, JPanel wizardPanel) {

      view.setExitButton(Buttons.newExitButton(wizard.getExitAction()));
      wizardPanel.add(view.getExitButton(), "cell 0 2,push");

  }

  /**
   * <p>Add an "cancel" button into an appropriate cell (will detect exiting wizard)</p>
   *
   * @param view        The view containing the panel to decorate
   * @param wizard      The wizard providing the actions
   * @param wizardPanel The wizard panel providing the layout
   * @param <M>         The wizard model type
   * @param <P>         The wizard panel model type
   */
  private static <M extends WizardModel, P> void addCancel(AbstractWizardView<M, P> view, AbstractWizard<M> wizard, JPanel wizardPanel) {

    if (wizard.isExiting()) {

      view.setCancelButton(Buttons.newCancelButton(wizard.getCancelAction()));
      wizardPanel.add(view.getCancelButton(), "cell 3 2");

    } else {

      view.setCancelButton(Buttons.newCancelButton(wizard.getCancelAction()));
      wizardPanel.add(view.getCancelButton(), "cell 0 2,push");

    }

  }

  /**
   * <p>Add a "finish" button into an appropriate cell</p>
   *
   * @param view        The view containing the panel to decorate
   * @param wizard      The wizard providing the actions
   * @param wizardPanel The wizard panel providing the layout
   * @param <M>         The wizard model type
   * @param <P>         The wizard panel model type
   */
  private static <M extends WizardModel, P> void addFinish(AbstractWizardView<M, P> view, AbstractWizard<M> wizard, JPanel wizardPanel) {

    Preconditions.checkNotNull(view, "'view' must be present");
    Preconditions.checkNotNull(view, "'wizard' must be present");
    Preconditions.checkNotNull(view.getWizardPanel(), "'wizardPanel' must be present");

    view.setFinishButton(Buttons.newFinishButton(wizard.getFinishAction(view)));
    wizardPanel.add(view.getFinishButton(), "cell 3 2");
  }

}
