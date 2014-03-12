package org.multibit.hd.ui.views.components;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.multibit.hd.core.config.ApplicationConfiguration;
import org.multibit.hd.core.config.BitcoinConfiguration;
import org.multibit.hd.core.dto.BackupSummary;
import org.multibit.hd.core.dto.Recipient;
import org.multibit.hd.core.exceptions.ExceptionHandler;
import org.multibit.hd.core.exchanges.ExchangeKey;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.core.services.ExchangeTickerService;
import org.multibit.hd.core.utils.BitcoinSymbol;
import org.multibit.hd.core.utils.CurrencyUtils;
import org.multibit.hd.ui.MultiBitUI;
import org.multibit.hd.ui.languages.LanguageKey;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.auto_complete.AutoCompleteDecorator;
import org.multibit.hd.ui.views.components.auto_complete.AutoCompleteFilter;
import org.multibit.hd.ui.views.components.display_amount.BitcoinSymbolListCellRenderer;
import org.multibit.hd.ui.views.components.renderers.BackupSummaryListCellRenderer;
import org.multibit.hd.ui.views.components.renderers.LanguageListCellRenderer;
import org.multibit.hd.ui.views.components.select_contact.RecipientComboBoxEditor;
import org.multibit.hd.ui.views.components.select_contact.RecipientListCellRenderer;
import org.multibit.hd.ui.views.themes.ThemeKey;
import org.multibit.hd.ui.views.themes.Themes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Locale;

/**
 * <p>Utility to provide the following to UI:</p>
 * <ul>
 * <li>Provision of localised combo boxes</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class ComboBoxes {

  /**
   * The "languages" combo box action command
   */
  public static final String LANGUAGES_COMMAND = "languages";
  /**
   * The "themes" combo box action command
   */
  public static final String THEMES_COMMAND = "themes";
  /**
   * The "alert sound" combo box action command
   */
  public static final String ALERT_SOUND_COMMAND = "alertSound";
  /**
   * The "receive sound" combo box action command
   */
  public static final String RECEIVE_SOUND_COMMAND = "receiveSound";
  /**
   * The "Bitcoin symbol" combo box action command
   */
  public static final String BITCOIN_SYMBOL_COMMAND = "bitcoinSymbol";
  /**
   * The "local symbol" combo box action command
   */
  public static final String LOCAL_SYMBOL_COMMAND = "localSymbol";
  /**
   * The "placement" combo box action command
   */
  public static final String PLACEMENT_COMMAND = "placement";
  /**
   * The "grouping separator" combo box action command
   */
  public static final String GROUPING_COMMAND = "grouping";
  /**
   * The "decimal separator" combo box action command
   */
  public static final String DECIMAL_COMMAND = "decimal";
  /**
   * The "exchange rate provider" combo box action command
   */
  public static final String EXCHANGE_RATE_PROVIDER_COMMAND = "exchange";
  /**
   * The "currency" combo box action command
   */
  public static final String CURRENCY_COMMAND = "currency";

  /**
   * Utilities have no public constructor
   */
  private ComboBoxes() {
  }

  /**
   * @param items The items for the combo box model
   *
   * @return A new editable combo box with default styling (no listener since it will cause early event triggers during set up)
   */
  public static <T> JComboBox<T> newComboBox(T[] items) {

    JComboBox<T> comboBox = new JComboBox<>(items);

    // Required to match icon button heights
    comboBox.setMinimumSize(new Dimension(25, MultiBitUI.NORMAL_ICON_SIZE + 14));

    // Required to blend in with panel
    comboBox.setBackground(Themes.currentTheme.detailPanelBackground());

    // Ensure we use the correct component orientation
    comboBox.applyComponentOrientation(Languages.currentComponentOrientation());

    // Ensure that keyboard navigation does not trigger action events
    comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);

    return comboBox;

  }

  /**
   * @return A new read only combo box (no listeners attached)
   */
  public static <T> JComboBox<T> newReadOnlyComboBox(T[] items) {

    JComboBox<T> comboBox = newComboBox(items);

    comboBox.setEditable(false);

    // Apply theme
    comboBox.setBackground(Themes.currentTheme.readOnlyComboBox());

    return comboBox;

  }

  /**
   * @param listener  The action listener to alert when the selection is made
   * @param selectYes True if the "yes" option [0] should be selected, otherwise "no" is selected [1]
   *
   * @return A new "yes/no" read only combo box
   */
  public static JComboBox<String> newYesNoComboBox(ActionListener listener, boolean selectYes) {

    JComboBox<String> comboBox = newReadOnlyComboBox(new String[]{
      Languages.safeText(MessageKey.YES),
      Languages.safeText(MessageKey.NO)
    });

    comboBox.setEditable(false);

    comboBox.setSelectedIndex(selectYes ? 0 : 1);

    // Apply theme
    comboBox.setBackground(Themes.currentTheme.readOnlyComboBox());

    // Set the listener at the end to avoid spurious events
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * @param listener   The action listener to alert when the selection is made
   * @param alertSound True if the "yes" option should be pre-selected
   *
   * @return A new "yes/no" combo box
   */
  public static JComboBox<String> newAlertSoundYesNoComboBox(ActionListener listener, boolean alertSound) {

    JComboBox<String> comboBox = newYesNoComboBox(listener, alertSound);
    comboBox.setActionCommand(ALERT_SOUND_COMMAND);

    return comboBox;
  }

  /**
   * @param listener     The action listener to alert when the selection is made
   * @param receiveSound True if the "yes" option should be pre-selected
   *
   * @return A new "yes/no" combo box
   */
  public static JComboBox<String> newReceiveSoundYesNoComboBox(ActionListener listener, boolean receiveSound) {

    JComboBox<String> comboBox = newYesNoComboBox(listener, receiveSound);
    comboBox.setActionCommand(RECEIVE_SOUND_COMMAND);

    return comboBox;
  }

  /**
   * @param listener The action listener to alert when the selection is made
   *
   * @return A new "contact checkbox" combo box (all, none)
   */
  public static JComboBox<String> newContactsCheckboxComboBox(ActionListener listener) {

    String[] items = new String[]{
      Languages.safeText(MessageKey.ALL),
      Languages.safeText(MessageKey.NONE),
    };

    JComboBox<String> comboBox = newReadOnlyComboBox(items);

    // Add the listener at the end to avoid false events
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * @param listener The action listener to alert when the selection is made
   *
   * @return A new "history checkbox" combo box (all, none) - kept separate from contacts
   */
  public static JComboBox<String> newHistoryCheckboxComboBox(ActionListener listener) {

    return newContactsCheckboxComboBox(listener);

  }

  /**
   * @param listener The action listener to alert when the selection is made
   * @param locale   The locale to use for initial selection
   *
   * @return A new "language" combo box containing all supported languages and variants
   */
  public static JComboBox<String> newLanguagesComboBox(ActionListener listener, Locale locale) {

    // Populate the combo box and declare a suitable renderer
    JComboBox<String> comboBox = newReadOnlyComboBox(LanguageKey.localisedNames());
    comboBox.setRenderer(new LanguageListCellRenderer());
    comboBox.setMaximumRowCount(MultiBitUI.COMBOBOX_MAX_ROW_COUNT);

    // Can use the ordinal due to the declaration ordering
    comboBox.setSelectedIndex(LanguageKey.fromLocale(locale).ordinal());

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(LANGUAGES_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * @param listener                 The action listener to alert when the selection is made
   * @param applicationConfiguration The application configuration providing
   *
   * @return A new "language" combo box containing all supported languages and variants
   */
  public static JComboBox<String> newThemesComboBox(ActionListener listener, ApplicationConfiguration applicationConfiguration) {

    // Get the current themes
    // Populate the combo box and declare a suitable renderer
    JComboBox<String> comboBox = newReadOnlyComboBox(ThemeKey.localisedNames());

    // Can use the ordinal due to the declaration ordering
    comboBox.setSelectedIndex(ThemeKey.fromTheme(Themes.currentTheme).ordinal());

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(THEMES_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * @param listener             The action listener to alert when the selection is made
   * @param bitcoinConfiguration The Bitcoin configuration to use
   *
   * @return A new "decimal" combo box
   */
  public static JComboBox<String> newDecimalComboBox(ActionListener listener, BitcoinConfiguration bitcoinConfiguration) {

    String[] decimalSeparators = Languages.getCurrencySeparators(false);
    JComboBox<String> comboBox = newReadOnlyComboBox(decimalSeparators);

    // Determine the first matching separator
    String decimal = bitcoinConfiguration.getDecimalSeparator().toString();
    selectFirstMatch(comboBox, decimalSeparators, decimal);

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(DECIMAL_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * @param listener             The action listener to alert when the selection is made
   * @param bitcoinConfiguration The Bitcoin configuration to use
   *
   * @return A new "decimal" combo box
   */
  public static JComboBox<String> newGroupingComboBox(ActionListener listener, BitcoinConfiguration bitcoinConfiguration) {

    String[] groupingSeparators = Languages.getCurrencySeparators(true);
    JComboBox<String> comboBox = newReadOnlyComboBox(groupingSeparators);

    // Determine the first matching separator
    String grouping = bitcoinConfiguration.getGroupingSeparator().toString();
    selectFirstMatch(comboBox, groupingSeparators, grouping);

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(GROUPING_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * <p>Provide a choice between the local currency symbol, or its 3-letter code</p>
   *
   * @param listener             The action listener to alert when the selection is made
   * @param bitcoinConfiguration The Bitcoin configuration to use
   *
   * @return A new "local symbol" combo box (e.g. ["$", "USD"] or ["£","GBP"] etc)
   */
  public static JComboBox<String> newLocalSymbolComboBox(ActionListener listener, BitcoinConfiguration bitcoinConfiguration) {

    String[] localSymbols = new String[]{
      CurrencyUtils.symbolFor(bitcoinConfiguration.getLocalCurrencyUnit().getCurrencyCode()),
      bitcoinConfiguration.getLocalCurrencyUnit().getCurrencyCode(),
    };
    JComboBox<String> comboBox = newReadOnlyComboBox(localSymbols);
    selectFirstMatch(comboBox, localSymbols, bitcoinConfiguration.getLocalCurrencySymbol());

    // Ensure we have no ugly scrollbar
    comboBox.setMaximumRowCount(localSymbols.length);

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(LOCAL_SYMBOL_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;
  }

  /**
   * @param listener             The action listener to alert when the selection is made
   * @param bitcoinConfiguration The Bitcoin configuration to use
   *
   * @return A new "Bitcoin symbol" combo box (e.g. "mB", "XBT" etc)
   */
  public static JComboBox<BitcoinSymbol> newBitcoinSymbolComboBox(ActionListener listener, BitcoinConfiguration bitcoinConfiguration) {

    // Order of insertion is important here
    JComboBox<BitcoinSymbol> comboBox = newReadOnlyComboBox(BitcoinSymbol.values());

    comboBox.setEditable(false);

    // Ensure we have no ugly scrollbar
    comboBox.setMaximumRowCount(BitcoinSymbol.values().length);

    // Use a list cell renderer to ensure Bitcoin symbols are correctly presented
    ListCellRenderer<BitcoinSymbol> renderer = new BitcoinSymbolListCellRenderer();
    comboBox.setRenderer(renderer);

    // Ensure we start with the given symbol selected
    BitcoinSymbol bitcoinSymbol = BitcoinSymbol.of(bitcoinConfiguration.getBitcoinSymbol());
    comboBox.setSelectedIndex(bitcoinSymbol.ordinal());

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(BITCOIN_SYMBOL_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;
  }

  /**
   * @param listener             The action listener to alert when the selection is made
   * @param bitcoinConfiguration The Bitcoin configuration to use
   *
   * @return A new "placement" combo box (e.g. "Leading", "Trailing" etc)
   */
  public static JComboBox<String> newPlacementComboBox(ActionListener listener, BitcoinConfiguration bitcoinConfiguration) {

    // Order of insertion is important here
    String[] positions = new String[]{
      Languages.safeText(MessageKey.LEADING),
      Languages.safeText(MessageKey.TRAILING),
    };
    JComboBox<String> comboBox = newReadOnlyComboBox(positions);
    if (bitcoinConfiguration.isCurrencySymbolLeading()) {
      comboBox.setSelectedIndex(0);
    } else {
      comboBox.setSelectedIndex(1);
    }

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(PLACEMENT_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;
  }

  /**
   * @param filter The contact auto-complete filter
   *
   * @return A new "recipient" combo box with auto-complete functionality
   */
  public static JComboBox<Recipient> newRecipientComboBox(AutoCompleteFilter<Recipient> filter) {

    Preconditions.checkNotNull(filter, "'filter' must be present");

    JComboBox<Recipient> comboBox = newComboBox(filter.create());

    comboBox.setEditable(true);

    // Use a contact editor to force use of the name field
    comboBox.setEditor(new RecipientComboBoxEditor());

    // Use a contact list cell renderer to ensure thumbnails are maintained
    ListCellRenderer<Recipient> renderer = new RecipientListCellRenderer((JTextField) comboBox.getEditor().getEditorComponent());
    comboBox.setRenderer(renderer);

    // Ensure we start with nothing selected
    comboBox.setSelectedIndex(-1);

    AutoCompleteDecorator.apply(comboBox, filter);

    return comboBox;

  }

  /**
   * @param listener        The action listener
   * @param backupSummaries The backup summary entries
   *
   * @return A new "backup summary" combo box
   */
  public static JComboBox<BackupSummary> newBackupSummaryComboBox(ActionListener listener, List<BackupSummary> backupSummaries) {

    Preconditions.checkNotNull(listener, "'listener' must be present");
    Preconditions.checkNotNull(backupSummaries, "'backupSummaries' must be present");

    // Convert the backup summaries to an array
    BackupSummary[] backupSummaryArray = new BackupSummary[backupSummaries.size()];

    JComboBox<BackupSummary> comboBox = newReadOnlyComboBox(backupSummaryArray);

    // Use a backup summary list cell renderer to ensure the correct fields are displayed
    ListCellRenderer<BackupSummary> renderer = new BackupSummaryListCellRenderer();
    comboBox.setRenderer(renderer);

    // Ensure we start with nothing selected
    comboBox.setSelectedIndex(-1);

    // Add the listener at the end to avoid false events
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * @param listener The action listener
   *
   * @return A new "exchange rate provider" combo box
   */
  public static JComboBox<String> newExchangeRateProviderComboBox(ActionListener listener, BitcoinConfiguration bitcoinConfiguration) {

    Preconditions.checkNotNull(listener, "'listener' must be present");

    // Get all the exchange names
    String[] allExchangeNames = ExchangeKey.allExchangeNames();
    JComboBox<String> comboBox = newReadOnlyComboBox(allExchangeNames);
    comboBox.setMaximumRowCount(MultiBitUI.COMBOBOX_MAX_ROW_COUNT);

    // Determine the selected index
    ExchangeKey exchangeKey = ExchangeKey.valueOf(bitcoinConfiguration.getExchangeKey());
    comboBox.setSelectedIndex(exchangeKey.ordinal());

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(EXCHANGE_RATE_PROVIDER_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * <p>Creates a combo that is populated asynchronously from the currency and exchange provided</p>
   *
   * @param listener             The action listener
   * @param bitcoinConfiguration The Bitcoin configuration to use
   *
   * @return A new "currency code" combo box
   */
  public static JComboBox<String> newCurrencyCodeComboBox(final ActionListener listener, final BitcoinConfiguration bitcoinConfiguration) {

    Preconditions.checkNotNull(listener, "'listener' must be present");
    Preconditions.checkNotNull(bitcoinConfiguration, "'bitcoinConfiguration' must be present");

    final JComboBox<String> comboBox = newReadOnlyComboBox(new String[] {});

    // Get all the currencies available at the exchange
    ExchangeTickerService exchangeTickerService = CoreServices.newExchangeService(bitcoinConfiguration);
    ListenableFuture<String[]> futureAllCurrencies = exchangeTickerService.allCurrencies();
    Futures.addCallback(futureAllCurrencies, new FutureCallback<String[]>() {
      @Override
      public void onSuccess(String[] allCurrencies) {

        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(allCurrencies);
        comboBox.setModel(model);
        comboBox.setMaximumRowCount(MultiBitUI.COMBOBOX_MAX_ROW_COUNT);

        selectFirstMatch(comboBox, allCurrencies, bitcoinConfiguration.getLocalCurrencyUnit().getCode());

        // Add the listener at the end to avoid false events
        comboBox.setActionCommand(CURRENCY_COMMAND);
        comboBox.addActionListener(listener);
      }

      @Override
      public void onFailure(Throwable t) {
        ExceptionHandler.handleThrowable(t);
      }
    });

    return comboBox;

  }

  /**
   * @param listener The action listener
   *
   * @return A new "seed size" combo box
   */
  public static JComboBox<String> newSeedSizeComboBox(ActionListener listener) {

    JComboBox<String> comboBox = newReadOnlyComboBox(new String[]{
      "12",
      "18",
      "24"
    });
    comboBox.setSelectedIndex(0);

    // Add the listener at the end to avoid false events
    comboBox.addActionListener(listener);

    return comboBox;
  }

  public static void selectFirstMatch(JComboBox<String> comboBox, String[] items, String item) {

    // Avoid working with nulls
    if (item == null) {
      comboBox.setSelectedIndex(-1);
      return;
    }

    // Determine the first matching separator
    for (int i = 0; i < items.length; i++) {
      Preconditions.checkNotNull(items[i], "'items[" + i + "]' must be present");
      if (items[i].startsWith(item)) {
        comboBox.setSelectedIndex(i);
        break;
      }
    }

  }

}