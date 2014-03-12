package org.multibit.hd.ui.views.wizards.exchange_settings;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.xeiam.xchange.dto.marketdata.Ticker;
import net.miginfocom.swing.MigLayout;
import org.joda.money.CurrencyUnit;
import org.multibit.hd.core.config.BitcoinConfiguration;
import org.multibit.hd.core.config.Configuration;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.config.LanguageConfiguration;
import org.multibit.hd.core.exceptions.ExceptionHandler;
import org.multibit.hd.core.exchanges.ExchangeKey;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.core.services.ExchangeTickerService;
import org.multibit.hd.ui.audio.Sounds;
import org.multibit.hd.ui.events.view.ViewEvents;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.*;
import org.multibit.hd.ui.views.components.panels.PanelDecorator;
import org.multibit.hd.ui.views.fonts.AwesomeIcon;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardPanelView;
import org.multibit.hd.ui.views.wizards.WizardButton;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * <p>View to provide the following to UI:</p>
 * <ul>
 * <li>Settings: Exchange rate provider display</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class ExchangeSettingsPanelView extends AbstractWizardPanelView<ExchangeSettingsWizardModel, ExchangeSettingsPanelModel> implements ActionListener {

  private JComboBox<String> exchangeRateProviderComboBox;

  private JLabel apiKeyLabel;
  private JTextField apiKeyTextField;

  private JLabel currencyCodeLabel;
  private JComboBox<String> currencyCodeComboBox;

  private JLabel tickerVerifiedStatus;

  // Prevent early events from triggering NPEs etc
  private boolean componentsReady = false;

  /**
   * @param wizard    The wizard managing the states
   * @param panelName The panel name
   */
  public ExchangeSettingsPanelView(AbstractWizard<ExchangeSettingsWizardModel> wizard, String panelName) {

    super(wizard, panelName, MessageKey.SHOW_EXCHANGE_WIZARD, AwesomeIcon.DOLLAR);

  }

  @Override
  public void newPanelModel() {

    // Use a deep copy to avoid reference leaks
    Configuration configuration = Configurations.currentConfiguration.deepCopy();

    // Configure the panel model
    setPanelModel(new ExchangeSettingsPanelModel(
      getPanelName(),
      configuration
    ));

  }

  @Override
  public void initialiseContent(JPanel contentPanel) {

    contentPanel.setLayout(new MigLayout(
      Panels.migXYLayout(),
      "[][][]", // Column constraints
      "[][][]" // Row constraints
    ));

    LanguageConfiguration languageConfiguration = Configurations.currentConfiguration.getLanguageConfiguration().deepCopy();
    BitcoinConfiguration bitcoinConfiguration = Configurations.currentConfiguration.getBitcoinConfiguration().deepCopy();
    Locale locale = languageConfiguration.getLocale();
    ExchangeKey exchangeKey = ExchangeKey.valueOf(bitcoinConfiguration.getExchangeKey());

    Preconditions.checkNotNull(locale, "'locale' cannot be empty");

    JButton exchangeRateProviderBrowserButton = Buttons.newLaunchBrowserButton(getExchangeRateProviderBrowserAction());

    exchangeRateProviderComboBox = ComboBoxes.newExchangeRateProviderComboBox(this, bitcoinConfiguration);
    currencyCodeComboBox = ComboBoxes.newCurrencyCodeComboBox(this, bitcoinConfiguration);

    // API key
    apiKeyTextField = TextBoxes.newEnterApiKey(getApiKeyDocumentListener());
    apiKeyLabel = Labels.newApiKeyLabel();

    // API key visibility
    boolean isOERExchange = ExchangeKey.OPEN_EXCHANGE_RATES.equals(exchangeKey);

    // API key value
    if (bitcoinConfiguration.getExchangeApiKeys().isPresent()) {
      apiKeyTextField.setText(bitcoinConfiguration.getExchangeApiKeys().get());
    }

    // Ticker verification status
    tickerVerifiedStatus = Labels.newVerificationStatus(true);
    tickerVerifiedStatus.setVisible(false);

    // Local currency
    currencyCodeLabel = Labels.newLocalCurrencyLabel();

    // All components are initialised
    componentsReady = true;

    ///////////////////////////// Components ready ////////////////////////////////

    // Show the API key if OER
    setApiKeyVisibility(isOERExchange);

    // Show the currency code by default
    setCurrencyCodeVisibility(true);

    // Hide the currency code if OER and no API key is filled in
    if (isOERExchange && Strings.isNullOrEmpty(apiKeyTextField.getText())) {
      // No API key so hide currency code
      setCurrencyCodeVisibility(false);
    }

    contentPanel.add(Labels.newExchangeSettingsNote(), "growx,push,span 3,wrap");

    contentPanel.add(Labels.newSelectExchangeRateProviderLabel(), "shrink");
    contentPanel.add(exchangeRateProviderComboBox, "growx,push");
    contentPanel.add(exchangeRateProviderBrowserButton, "shrink,wrap");

    contentPanel.add(apiKeyLabel, "shrink");
    contentPanel.add(apiKeyTextField, "growx,push,wrap");

    contentPanel.add(currencyCodeLabel, "shrink");
    contentPanel.add(currencyCodeComboBox, "growx,push,wrap");
    contentPanel.add(tickerVerifiedStatus, "grow,cell 1 4,push,wrap");


  }

  @Override
  protected void initialiseButtons(AbstractWizard<ExchangeSettingsWizardModel> wizard) {

    PanelDecorator.addCancelApply(this, wizard);

  }

  @Override
  public void fireInitialStateViewEvents() {

    // Apply button starts off enabled
    ViewEvents.fireWizardButtonEnabledEvent(getPanelName(), WizardButton.APPLY, true);

  }

  @Override
  public void afterShow() {

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        exchangeRateProviderComboBox.requestFocusInWindow();

      }
    });

  }

  @Override
  public boolean beforeHide(boolean isExitCancel) {

    if (!isExitCancel) {

      // If the user has selected OER then update the API key
      if (apiKeyTextField.isVisible() && !Strings.isNullOrEmpty(apiKeyTextField.getText())) {
        getWizardModel().getConfiguration().getBitcoinConfiguration().setExchangeApiKeys(apiKeyTextField.getText());
      }

      // Switch the main configuration over to the new one
      Configurations.switchConfiguration(getWizardModel().getConfiguration());

    }

    // Must be OK to proceed
    return true;

  }

  @Override
  public void updateFromComponentModels(Optional componentModel) {


  }


  /**
   * <p>Handle one of the combo boxes changing</p>
   *
   * @param e The action event
   */
  @Override
  public void actionPerformed(ActionEvent e) {

    if (ComboBoxes.EXCHANGE_RATE_PROVIDER_COMMAND.equals(e.getActionCommand())) {
      handleExchangeRateProviderSelection(e);
    }
    if (ComboBoxes.CURRENCY_COMMAND.equals(e.getActionCommand())) {
      handleCurrencySelection(e);
    }

  }

  /**
   * @return The "exchange rate provider browser" action
   */
  private Action getExchangeRateProviderBrowserAction() {

    return new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        int selectedIndex = exchangeRateProviderComboBox.getSelectedIndex();
        if (selectedIndex == -1) {
          return;
        }

        ExchangeKey exchangeKey = ExchangeKey.values()[selectedIndex];

        try {
          final URI exchangeUri;
          if (ExchangeKey.OPEN_EXCHANGE_RATES.equals(exchangeKey)) {
            // Ensure MultiBit customers go straight to the free API key page with referral
            exchangeUri = URI.create("https://openexchangerates.org/signup/free?r=multibit");
          } else {
            // All other exchanges go to the main host (not API root)
            exchangeUri = URI.create("http://" + exchangeKey.getExchange().getExchangeSpecification().getHost());
          }
          Desktop.getDesktop().browse(exchangeUri);
        } catch (IOException ex) {
          ExceptionHandler.handleThrowable(ex);
        }

      }
    };
  }

  /**
   * <p>The exchange rate provider selection has changed</p>
   *
   * @param e The action event
   */
  private void handleExchangeRateProviderSelection(ActionEvent e) {

    JComboBox source = (JComboBox) e.getSource();
    int exchangeIndex = source.getSelectedIndex();

    // Exchanges are presented in the same order as they are declared in the enum
    ExchangeKey exchangeKey = ExchangeKey.values()[exchangeIndex];

    // Test for Open Exchange Rates
    if (ExchangeKey.OPEN_EXCHANGE_RATES.equals(exchangeKey)) {
      // Show the API key
      setApiKeyVisibility(true);

      // Hide the currency code to start with
      setCurrencyCodeVisibility(false);

    } else {
      // Hide the API key
      setApiKeyVisibility(false);

      // Show the currency code
      setCurrencyCodeVisibility(true);
    }

    // Hide the ticker verification
    tickerVerifiedStatus.setVisible(false);

    // Update the model (even if in error)
    getWizardModel().getConfiguration().getBitcoinConfiguration().setExchangeKey(exchangeKey.name());

    // Reset the available currencies
    ExchangeTickerService exchangeTickerService = CoreServices.newExchangeService(getWizardModel().getConfiguration().getBitcoinConfiguration());
    ListenableFuture<String[]> futureAllCurrencies = exchangeTickerService.allCurrencies();
    Futures.addCallback(futureAllCurrencies, new FutureCallback<String[]>() {
      @Override
      public void onSuccess(String[] allCurrencies) {

        currencyCodeComboBox.setModel(new DefaultComboBoxModel<>(allCurrencies));
        currencyCodeComboBox.setSelectedIndex(-1);

        // Prevent application until the currency is selected (to allow ticker check)
        ViewEvents.fireWizardButtonEnabledEvent(
          getPanelName(),
          WizardButton.APPLY,
          false
        );

      }

      @Override
      public void onFailure(Throwable t) {

        // Exchange currency look up failed
        ExceptionHandler.handleThrowable(t);
      }
    });


  }

  /**
   * <p>The currency selection has changed</p>
   *
   * @param e The action event
   */
  private void handleCurrencySelection(ActionEvent e) {

    // Always disable the Apply after a currency change
    ViewEvents.fireWizardButtonEnabledEvent(
      getPanelName(),
      WizardButton.APPLY,
      false
    );

    JComboBox source = (JComboBox) e.getSource();

    // Ignore cascading events from an exchange selection
    if (source.getSelectedIndex() == -1) {
      return;
    }

    String isoCounterCode = String.valueOf(source.getSelectedItem()).substring(0, 3);

    // Immediately update the model while we wait for the results
    CurrencyUnit currencyUnit = CurrencyUnit.getInstance(isoCounterCode);

    // Update the model (even if in error)
    getWizardModel().getConfiguration().getBitcoinConfiguration().setLocalCurrencySymbol(isoCounterCode);
    getWizardModel().getConfiguration().getBitcoinConfiguration().setLocalCurrencyUnit(currencyUnit);

    // Test the new settings
    handleTestTicker();

  }

  /**
   * <p>Handles the process of testing the currency selection against the exchange</p>
   */
  private void handleTestTicker() {

    // Hide the ticker verification
    tickerVerifiedStatus.setVisible(false);

    BitcoinConfiguration bitcoinConfiguration = getWizardModel().getConfiguration().getBitcoinConfiguration();

    // Build a custom exchange ticker service from the wizard model
    ExchangeTickerService exchangeTickerService = CoreServices.newExchangeService(bitcoinConfiguration);
    ListenableFuture<Ticker> futureTicker = exchangeTickerService.latestTicker();

    // Avoid freezing the UI
    Futures.addCallback(futureTicker, new FutureCallback<Ticker>() {
      @Override
      public void onSuccess(Ticker ticker) {

        // Show the ticker verification
        tickerVerifiedStatus.setVisible(true);

        ViewEvents.fireWizardButtonEnabledEvent(
          getPanelName(),
          WizardButton.APPLY,
          true
        );

      }

      @Override
      public void onFailure(Throwable t) {

        Sounds.playBeep();
        ViewEvents.fireWizardButtonEnabledEvent(
          getPanelName(),
          WizardButton.APPLY,
          false
        );

      }
    });

  }

  /**
   * @return A document listener for interacting with the API key field
   */
  public DocumentListener getApiKeyDocumentListener() {

    return new DocumentListener() {
      @Override
      public void insertUpdate(DocumentEvent e) {
        verify();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        verify();
      }

      @Override
      public void changedUpdate(DocumentEvent e) {
        verify();
      }

      public void verify() {

        if (!componentsReady) {
          return;
        }

        // Some exchanges will offer a variety of API key lengths so cannot verify beyond a presence
        boolean notEmpty = apiKeyTextField.getText().length() > 0;

        // Update the wizard model so that the ticker can be tested if a currency combo selection is made
        getWizardModel().getConfiguration().getBitcoinConfiguration().setExchangeApiKeys(apiKeyTextField.getText());

        // Handle currency combo
        setCurrencyCodeVisibility(notEmpty);

        // Handle currency code (always deselected after someone fiddles with the API key)
        currencyCodeComboBox.setSelectedIndex(-1);

        // Hide the ticker verification
        tickerVerifiedStatus.setVisible(false);

        // Apply is always disabled when the currency code combo is not selected
        ViewEvents.fireWizardButtonEnabledEvent(
          getPanelName(),
          WizardButton.APPLY,
          false
        );

      }
    };
  }

  /**
   * @param visible True to make the label and text field visible
   */
  private void setApiKeyVisibility(boolean visible) {

    if (!componentsReady) {
      return;
    }
    if (apiKeyLabel == null || apiKeyTextField == null) {
      return;
    }
    apiKeyLabel.setVisible(visible);
    apiKeyTextField.setVisible(visible);

  }

  /**
   * @param visible True to make the label and combo box visible
   */
  private void setCurrencyCodeVisibility(boolean visible) {

    if (!componentsReady) {
      return;
    }
    currencyCodeLabel.setVisible(visible);
    currencyCodeComboBox.setVisible(visible);

  }


}
