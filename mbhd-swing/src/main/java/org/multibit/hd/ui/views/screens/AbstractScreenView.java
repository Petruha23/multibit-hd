package org.multibit.hd.ui.views.screens;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.ui.events.view.ScreenComponentModelChangedEvent;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.Panels;
import org.multibit.hd.ui.views.components.panels.PanelDecorator;

import javax.swing.*;

/**
 * <p>Abstract base class providing the following to screens:</p>
 * <ul>
 * <li>Standard methods common to screens</li>
 * </ul>
 * <p>A screen contains the title and components. It relies on
 * its implementers to provide the panel containing the specific
 * components for the user interaction.</p>
 *
 * @param <M> The screen model
 *
 * @since 0.0.1
 *  
 */
public abstract class AbstractScreenView<M extends ScreenModel> {

  private final M screenModel;

  private final Screen screen;

  /**
   * The current screen view panel with the specific components for the view
   */
  private JPanel currentScreenViewPanel;

  /**
   * True if the components making up this screen have been populated
   */
  private boolean initialised;

  /**
   * @param screenModel The screen model managing the states
   * @param screen      The screen to filter events from components
   * @param title       The key to the main title of the wizard panel
   */
  public AbstractScreenView(M screenModel, Screen screen, MessageKey title) {

    Preconditions.checkNotNull(screenModel, "'screenModel' must be present");
    Preconditions.checkNotNull(screen, "'screen' must be present");
    Preconditions.checkNotNull(title, "'title' must be present");

    this.screenModel = screenModel;
    this.screen = screen;

    // All detail views can receive events
    CoreServices.uiEventBus.register(this);

    // All screens are decorated with the same theme and layout at creation
    // so just need a vanilla panel to begin with
    JPanel screenPanel = Panels.newPanel();

    // All detail panels require a backing model
    newScreenModel();

    // Apply the screen theme to the panel
    PanelDecorator.applyScreenTheme(screenPanel, title);

  }

  /**
   * @return The screen model providing aggregated state information
   */
  public M getScreenModel() {
    return screenModel;
  }

  /**
   * @return The screen
   */
  public Screen getScreen() {
    return screen;
  }

  /**
   * <p>Called when the screen is first created to initialise the model and subsequently on a locale change event.</p>
   *
   * <p>This is called before {@link AbstractScreenView#initialiseScreenViewPanel()}</p>
   *
   * <p>Implementers must create a new panel model and bind it to the overall screen</p>
   */
  public abstract void newScreenModel();

  /**
   * @return The current screen view panel with lazy initialisation
   */
  public JPanel getScreenViewPanel() {

    if (!isInitialised()) {

      currentScreenViewPanel = initialiseScreenViewPanel();

      setInitialised(true);

    }

    return currentScreenViewPanel;

  }

  /**
   * <p>Called when the screen is first created to initialise the panel and subsequently on a locale change event.</p>
   *
   * <p>This is called after {@link AbstractScreenView#newScreenModel()}</p>
   *
   * <p>Implementers must create a new panel</p>
   *
   * @return A new panel containing the data components specific to this screen (e.g. contacts or payments)
   */
  protected abstract JPanel initialiseScreenViewPanel();

  /**
   * <p>Update the view with any required view events to create a clean initial state (all initialisation will have completed)</p>
   *
   * <p>Default implementation is to do nothing</p>
   */
  public void fireInitialStateViewEvents() {

    // Do nothing

  }

  /**
   * <p>Called before this screen is about to be shown</p>
   *
   * <p>Typically this is where a view would reference the model to obtain latest values for display</p>
   *
   * @return True if the panel can be shown, false if the show operation should be aborted
   */
  public boolean beforeShow() {

    return true;

  }

  /**
   * <p>Called after this screen has been shown</p>
   *
   * <p>Typically this is where a view would attempt to set the focus for its primary component using
   * the Swing thread as follows:</p>
   *
   * <pre>
   * SwingUtilities.invokeLater(new Runnable() {
   *
   * {@literal @}Override public void run() {
   *   myComponent.requestFocusInWindow();
   * }
   *
   * });
   *
   * </pre>
   */
  public void afterShow() {

    // Do nothing

  }

  /**
   * @return True if the implementer has configured all the components for display
   */
  public boolean isInitialised() {
    return initialised;
  }

  public void setInitialised(boolean initialised) {
    this.initialised = initialised;
  }

  /**
   * <p>React to a "screen component model changed" event</p>
   *
   * @param event The event
   */
  @Subscribe
  public void onScreenComponentModelChangedEvent(ScreenComponentModelChangedEvent event) {

  }

}
