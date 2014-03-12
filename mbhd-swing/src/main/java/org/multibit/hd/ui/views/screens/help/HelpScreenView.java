package org.multibit.hd.ui.views.screens.help;

import com.google.common.collect.Lists;
import net.miginfocom.swing.MigLayout;
import org.multibit.hd.core.dto.RAGStatus;
import org.multibit.hd.core.exceptions.ExceptionHandler;
import org.multibit.hd.ui.audio.Sounds;
import org.multibit.hd.ui.events.controller.ControllerEvents;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.models.Models;
import org.multibit.hd.ui.views.components.Buttons;
import org.multibit.hd.ui.views.components.Labels;
import org.multibit.hd.ui.views.components.Panels;
import org.multibit.hd.ui.views.screens.AbstractScreenView;
import org.multibit.hd.ui.views.screens.Screen;
import org.multibit.hd.ui.views.themes.Themes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedList;

/**
 * <p>View to provide the following to application:</p>
 * <ul>
 * <li>Provision of components and layout for the help detail display</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class HelpScreenView extends AbstractScreenView<HelpScreenModel> {

  private static final Logger log = LoggerFactory.getLogger(HelpScreenView.class);

  private static final String HELP_BASE_URL = "http://www.multibit.org/v0.5/";

  /**
   * A primitive form of "browser history"
   */
  private int currentPageIndex = 0;
  private final LinkedList<URL> pageList = Lists.newLinkedList();
  private JEditorPane editorPane;

  private JButton backButton;
  private JButton forwardButton;
  private JButton launchBrowserButton;

  // View components

  /**
   * @param panelModel The model backing this panel view
   * @param screen     The screen to filter events from components
   * @param title      The key to the main title of this panel view
   */
  public HelpScreenView(HelpScreenModel panelModel, Screen screen, MessageKey title) {
    super(panelModel, screen, title);
  }

  @Override
  public void newScreenModel() {

  }

  @Override
  public JPanel initialiseScreenViewPanel() {

    MigLayout layout = new MigLayout(
      Panels.migLayout("fillx,insets 10 5 0 0"),
      "[][][]push[]", // Column constraints
      "[shrink][grow]" // Row constraints
    );

    // Create the content panel
    JPanel contentPanel = Panels.newPanel(layout);

    backButton = Buttons.newBackButton(getBackAction());
    forwardButton = Buttons.newForwardButton(getForwardAction());
    launchBrowserButton = Buttons.newLaunchBrowserButton(getLaunchBrowserAction());

    // Control visibility and availability
    launchBrowserButton.setEnabled(Desktop.isDesktopSupported());

    // Create the browser
    final JEditorPane editorPane = createBrowser();

    // Create the scroll pane and add the HTML editor pane to it
    JScrollPane scrollPane = new JScrollPane(editorPane);
    scrollPane.setViewportBorder(null);

    // Add to the panel
    contentPanel.add(backButton, "shrink");
    contentPanel.add(forwardButton, "shrink");
    contentPanel.add(launchBrowserButton, "shrink");
    contentPanel.add(Labels.newBlankLabel(), "grow,push,wrap"); // Empty label to pack buttons
    contentPanel.add(scrollPane, "span 4,grow,push");

    return contentPanel;
  }

  @Override
  public void afterShow() {

  }

  /**
   * @return An editor pane with support for basic HTML (v3.2)
   */
  private JEditorPane createBrowser() {

    // Create an HTML editor kit
    HTMLEditorKit kit = new HTMLEditorKit();

    // Set a basic style sheet
    StyleSheet styleSheet = kit.getStyleSheet();

    // Avoid setting the background here since it can bleed through the look and feel
    styleSheet.addRule("body{font-family:\"Helvetica Neue\",\"Liberation Sans\",Arial,sans-serif;margin:0;padding:0;}");
    styleSheet.addRule("h1,h2{font-family:\"Helvetica Neue\",\"Liberation Sans\",Arial,sans-serif;font-weight:normal;}");
    styleSheet.addRule("h1{color:#973131;font-size:150%;}");
    styleSheet.addRule("h2{color:#973131;font-size:125%;}");
    styleSheet.addRule("h3{color:#973131;font-size:100%;}");
    styleSheet.addRule("h1 img,h2 img,h3 img{vertical-align:middle;margin-right:5px;}");
    styleSheet.addRule("a:link,a:visited,a:active{color:#973131;}");
    styleSheet.addRule("a:link:hover,a:visited:hover,a:active:hover{color:#973131;}");
    styleSheet.addRule("a img{border:0;}");

    // TODO More robust error handling required
    try {
      // Look up the standard MultiBit help (via HTTP)
      URL helpBaseUrl = URI.create(HELP_BASE_URL).toURL();

      // Create an editor pane to wrap the HTML editor kit
      editorPane = new JEditorPane();

      // Make it read-only to allow links to be followed
      editorPane.setEditable(false);

      // Apply theme
      editorPane.setBackground(Themes.currentTheme.detailPanelBackground());

      // Apply style
      editorPane.setEditorKit(kit);

      // Create a default document to manage HTML
      HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
      doc.setBase(helpBaseUrl);
      editorPane.setDocument(doc);

      // Create the starting page
      addPage(URI.create(HELP_BASE_URL + "help_contents.html").toURL());

      // Load the current page in the history
      editorPane.setPage(currentPage());

    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    editorPane.addHyperlinkListener(new HyperlinkListener() {
      @Override
      public void hyperlinkUpdate(HyperlinkEvent e) {

        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

          URL url = e.getURL();

          boolean isMultiBit = url.toString().startsWith("/") || url.toString().startsWith(HELP_BASE_URL);

          // Ignore off site links
          if (!isMultiBit) {

            // User is clicking on an external link so hint at proper browser
            Sounds.playBeep();
            launchBrowserButton.setBackground(Themes.currentTheme.readOnlyBackground());

          } else {

            // User has clicked on the link so treat as a new page
            addPage(e.getURL());

            // We are allowed to browse to this page
            browse(currentPage());
          }
        }

      }
    });
    return editorPane;
  }

  /**
   * <p>Point the editor pane to the given URL for rendering</p>
   *
   * @param url The URL to render
   */
  private void browse(URL url) {

    try {

      editorPane.setPage(url);
      launchBrowserButton.setBackground(Themes.currentTheme.buttonBackground());

    } catch (IOException e) {
      // Log the error and report a failure to the user via the alerts
      log.error(e.getMessage(), e);
      ControllerEvents.fireAddAlertEvent(Models.newAlertModel(
        Languages.safeText(MessageKey.NETWORK_CONFIGURATION_ERROR),
        RAGStatus.AMBER
      ));
    }
  }

  /**
   * <p>Adds the page to the list, inserting it so that a "back" operation will </p>
   *
   * @param url The URL of the page to add
   */
  private void addPage(URL url) {

    if (currentPageIndex >= pageList.size() - 1) {
      // We're at the end of the list so append the new page
      pageList.add(url);
      // Make this the current page
      currentPageIndex = pageList.size() - 1;
    } else {
      // We're not at the end so insert after current position
      // to maintain back button, then update to current
      pageList.add(currentPageIndex + 1, url);
      currentPageIndex++;
    }

    handleNavigationButtons();

  }

  /**
   * @return The URL for the current page in the history
   */
  private URL currentPage() {
    return pageList.get(currentPageIndex);
  }

  /**
   * @return The URL for the previous in the history
   */
  private URL previousPage() {

    // Limit current page index to first index or decrement
    currentPageIndex = (currentPageIndex <= 0) ? 0 : currentPageIndex - 1;

    // Enable/disable the navigation buttons
    handleNavigationButtons();

    return pageList.get(currentPageIndex);
  }

  /**
   * @return The URL for the next page in the history
   */
  private URL nextPage() {

    // Limit current page index to last index or increment
    currentPageIndex = (currentPageIndex >= pageList.size() - 1) ? pageList.size() - 1 : currentPageIndex + 1;

    return pageList.get(currentPageIndex);

  }

  /**
   * <p>Control how the navigation buttons are presented depending on the history</p>
   */
  private void handleNavigationButtons() {

    backButton.setEnabled(currentPageIndex > 0);
    forwardButton.setEnabled(currentPageIndex < pageList.size() - 1);

  }

  /**
   * @return The "launch browser" action
   */
  private Action getLaunchBrowserAction() {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        try {
          Desktop.getDesktop().browse(currentPage().toURI());
        } catch (IOException | URISyntaxException e1) {
          ExceptionHandler.handleThrowable(e1);
        }

      }
    };
  }

  /**
   * @return The "forward" action
   */
  private Action getForwardAction() {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        browse(nextPage());

      }
    };

  }

  /**
   * @return The "back" action
   */
  private Action getBackAction() {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        browse(previousPage());

      }
    };
  }

}
