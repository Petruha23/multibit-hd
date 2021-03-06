package org.multibit.hd.ui.views.components;

import com.google.bitcoin.core.NetworkParameters;
import com.google.common.base.Preconditions;
import org.multibit.hd.core.config.BitcoinConfiguration;
import org.multibit.hd.core.dto.BackupSummary;
import org.multibit.hd.core.dto.PaymentRequestData;
import org.multibit.hd.core.dto.Recipient;
import org.multibit.hd.core.dto.WalletSummary;
import org.multibit.hd.core.exchanges.ExchangeKey;
import org.multibit.hd.core.services.ContactService;
import org.multibit.hd.core.utils.BitcoinSymbol;
import org.multibit.hd.core.utils.CurrencyUtils;
import org.multibit.hd.ui.MultiBitUI;
import org.multibit.hd.ui.languages.LanguageKey;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.auto_complete.AutoCompleteDecorator;
import org.multibit.hd.ui.views.components.auto_complete.AutoCompleteFilter;
import org.multibit.hd.ui.views.components.auto_complete.AutoCompleteFilters;
import org.multibit.hd.ui.views.components.display_amount.BitcoinSymbolListCellRenderer;
import org.multibit.hd.ui.views.components.renderers.BackupSummaryListCellRenderer;
import org.multibit.hd.ui.views.components.renderers.LanguageListCellRenderer;
import org.multibit.hd.ui.views.components.renderers.WalletSummaryListCellRenderer;
import org.multibit.hd.ui.views.components.select_recipient.RecipientComboBoxEditor;
import org.multibit.hd.ui.views.components.select_recipient.RecipientListCellRenderer;
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
   * The "show balance" combo box action command
   */
  public static final String SHOW_BALANCE_COMMAND = "showBalance";
  /**
   * The "themes" combo box action command
   */
  public static final String THEMES_COMMAND = "themes";
  /**
   * The "paymentRequests" combo box action command
   */
  public static final String PAYMENT_REQUESTS_COMMAND = "paymentRequests";

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
   * The "Tor" combo box action command
   */
  public static final String TOR_COMMAND = "tor";

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

    // Increase border insets to create better visual clarity
    comboBox.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 0));

    // Push out the standard scrollbar beyond the default
    comboBox.setMaximumRowCount(10);

    // Adjust the scrollbar UI for the popup menu
    Object popupComponent = comboBox.getUI().getAccessibleChild(comboBox, 0);
    if (popupComponent instanceof JPopupMenu) {

      JPopupMenu popupMenu = (JPopupMenu) popupComponent;
      for (Component component : popupMenu.getComponents()) {
        if ((component instanceof JScrollPane)) {
          JScrollPane scrollPane = (JScrollPane) component;

          // Ensure we maintain the overall theme
          ScrollBarUIDecorator.apply(scrollPane, true);

        }
      }
    }

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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.YES);

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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.ALERT_SOUND);

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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.RECEIVE_SOUND);

    comboBox.setActionCommand(RECEIVE_SOUND_COMMAND);

    return comboBox;
  }

  /**
   * @param listener The action listener to alert when the selection is made
   * @param useTor   True if the "yes" option should be pre-selected
   *
   * @return A new "yes/no" combo box
   */
  public static JComboBox<String> newTorYesNoComboBox(ActionListener listener, boolean useTor) {

    JComboBox<String> comboBox = newYesNoComboBox(listener, useTor);

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_TOR);

    comboBox.setActionCommand(TOR_COMMAND);

    return comboBox;
  }

  /**
   * @param listener    The action listener to alert when the selection is made
   * @param showBalance True if the "yes" option should be pre-selected
   *
   * @return A new "yes/no" combo box
   */
  public static JComboBox<String> newShowBalanceYesNoComboBox(ActionListener listener, boolean showBalance) {

    JComboBox<String> comboBox = newYesNoComboBox(listener, showBalance);

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SHOW_BALANCE);

    comboBox.setActionCommand(SHOW_BALANCE_COMMAND);

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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.CONTACTS);

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

    JComboBox<String> comboBox = newContactsCheckboxComboBox(listener);

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.HISTORY);

    return comboBox;

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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_LANGUAGE);

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
   * @param listener The action listener to alert when the selection is made
   *
   * @return A new "themes" combo box containing all supported languages and variants
   */
  public static JComboBox<String> newThemesComboBox(ActionListener listener) {

    // Get the current themes
    // Populate the combo box and declare a suitable renderer
    JComboBox<String> comboBox = newReadOnlyComboBox(ThemeKey.localisedNames());

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_THEME);

    // Can use the ordinal due to the declaration ordering
    comboBox.setSelectedIndex(ThemeKey.fromTheme(Themes.currentTheme).ordinal());

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(THEMES_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * @param listener               The action listener to alert when the selection is made
   * @param paymentRequestDataList The list of paymentRequestData to put in the combo box
   *
   * @return A new "payment requests" combo box containing all supported languages and variants
   */
  public static JComboBox<PaymentRequestData> newPaymentRequestsComboBox(ActionListener listener, List<PaymentRequestData> paymentRequestDataList) {

    // Populate the combo box and declare a suitable renderer
    JComboBox<PaymentRequestData> comboBox = newReadOnlyComboBox(paymentRequestDataList.toArray(new PaymentRequestData[paymentRequestDataList.size()]));

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.CHOOSE_PAYMENT_REQUEST);

    // Can use the ordinal due to the declaration ordering
    if (paymentRequestDataList.size() > 0) {
      comboBox.setSelectedIndex(0);
    }

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(PAYMENT_REQUESTS_COMMAND);
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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_DECIMAL_SEPARATOR);

    // Determine the first matching separator
    String decimal = bitcoinConfiguration.getDecimalSeparator();
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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_GROUPING_SEPARATOR);

    // Determine the first matching separator
    String grouping = bitcoinConfiguration.getGroupingSeparator();
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
      CurrencyUtils.symbolFor(bitcoinConfiguration.getLocalCurrencyCode()),
      bitcoinConfiguration.getLocalCurrencyCode()
    };
    JComboBox<String> comboBox = newReadOnlyComboBox(localSymbols);

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_LOCAL_SYMBOL);

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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_BITCOIN_SYMBOL);

    // Increase default font size
    comboBox.setFont(comboBox.getFont().deriveFont(MultiBitUI.COMBO_BOX_TEXT_FONT_SIZE));

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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_PLACEMENT);

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
   * @param contactService    The contact service
   * @param networkParameters The Bitcoin network parameters
   *
   * @return A new "recipient" combo box with auto-complete functionality
   */
  public static JComboBox<Recipient> newRecipientComboBox(ContactService contactService, NetworkParameters networkParameters) {

    Preconditions.checkNotNull(contactService, "'contactService' must be present");
    Preconditions.checkNotNull(networkParameters, "'networkParameters' must be present");

    AutoCompleteFilter<Recipient> filter = AutoCompleteFilters.newRecipientFilter(contactService, networkParameters);

    JComboBox<Recipient> comboBox = newComboBox(filter.create());

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.RECIPIENT);

    // Remove border
    comboBox.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

    // Ensure we start with nothing selected (must come before editor is set)
    comboBox.setSelectedIndex(-1);

    // Allow editing
    comboBox.setEditable(true);

    // Use a recipient editor to force use of the name field
    comboBox.setEditor(new RecipientComboBoxEditor(contactService, networkParameters));

    // Use a recipient list cell renderer to ensure recipient is set on selection
    ListCellRenderer<Recipient> renderer = new RecipientListCellRenderer((JTextField) comboBox.getEditor().getEditorComponent());
    comboBox.setRenderer(renderer);

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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_BACKUP_NOTE_1);

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
   * @param wallets  The wallet data entries
   *
   * @return A new "select wallet" combo box
   */
  public static JComboBox<WalletSummary> newSelectWalletComboBox(ActionListener listener, List<WalletSummary> wallets) {

    Preconditions.checkNotNull(listener, "'listener' must be present");
    Preconditions.checkNotNull(wallets, "'wallets' must be present");

    // Convert the wallet data entries to an array
    WalletSummary[] walletSummaryArray = new WalletSummary[wallets.size()];

    JComboBox<WalletSummary> comboBox = newReadOnlyComboBox(walletSummaryArray);

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_WALLET);

    // Use a wallet list cell renderer to ensure the correct fields are displayed
    ListCellRenderer<WalletSummary> renderer = new WalletSummaryListCellRenderer();
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
    allExchangeNames[0] = Languages.safeText(MessageKey.NONE);
    JComboBox<String> comboBox = newReadOnlyComboBox(allExchangeNames);
    comboBox.setMaximumRowCount(MultiBitUI.COMBOBOX_MAX_ROW_COUNT);

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.EXCHANGE_RATE_PROVIDER);

    // Determine the selected index
    ExchangeKey exchangeKey = ExchangeKey.valueOf(bitcoinConfiguration.getCurrentExchange());
    comboBox.setSelectedIndex(exchangeKey.ordinal());

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(EXCHANGE_RATE_PROVIDER_COMMAND);
    comboBox.addActionListener(listener);

    return comboBox;

  }

  /**
   * <p>Creates a combo that is populated asynchronously from the currency and exchange provided</p>
   *
   * @param listener The action listener
   *
   * @return A new "currency code" combo box
   */
  public static JComboBox<String> newCurrencyCodeComboBox(ActionListener listener) {

    final JComboBox<String> comboBox = newReadOnlyComboBox(new String[]{});

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SELECT_LOCAL_CURRENCY);

    // Add the listener at the end to avoid false events
    comboBox.setActionCommand(ComboBoxes.CURRENCY_COMMAND);
    comboBox.addActionListener(listener);

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

    // Ensure it is accessible
    AccessibilityDecorator.apply(comboBox, MessageKey.SEED_SIZE);

    comboBox.setSelectedIndex(0);

    // Add the listener at the end to avoid false events
    comboBox.addActionListener(listener);

    return comboBox;
  }

  /**
   * @param comboBox The combo box to set the selection on
   * @param items    The items in the model
   * @param item     the item that should be matched using a case-sensitive "starts with" approach
   */
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