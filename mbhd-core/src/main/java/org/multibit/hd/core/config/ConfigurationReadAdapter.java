package org.multibit.hd.core.config;

import ch.qos.logback.classic.Level;
import com.google.common.base.Preconditions;
import org.joda.money.CurrencyUnit;

import java.util.Map;
import java.util.Properties;

/**
 * <p>Adapter to provide the following to application:</p>
 * <ul>
 * <li>Creates a Configuration from the given Properties performing validation</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public class ConfigurationReadAdapter {

  private final Properties properties;
  private final Configuration configuration = new Configuration();

  public ConfigurationReadAdapter(Properties properties) {
    this.properties = properties;
  }

  /**
   * @return A new Configuration based on the properties
   */
  public Configuration adapt() {

    for (Map.Entry<Object, Object> entry : properties.entrySet()) {

      String key = (String) entry.getKey();
      String value = (String) entry.getValue();

      Preconditions.checkNotNull(key, "'key' must be present");
      Preconditions.checkNotNull(value, "'value' must be present");

      // Application
      adaptApplication(key, value);

      // Sound
      adaptSound(key, value);

      // Bitcoin
      adaptBitcoin(key, value);

      // Language
      adaptLanguage(key, value);

      // Logging
      if (key.startsWith(Configurations.LOGGING)) {
        adaptLogging(key, value);
      }

    }

    return configuration;
  }

  private void adaptApplication(String key, String value) {

    if (Configurations.APP_CURRENT_WALLET_FILENAME.equalsIgnoreCase(key)) {
      configuration.getApplicationConfiguration().setCurrentWalletRoot(value);
    }
    if (Configurations.APP_CURRENT_THEME.equalsIgnoreCase(key)) {
      configuration.getApplicationConfiguration().setCurrentTheme(value);
    }
    if (Configurations.APP_VERSION.equalsIgnoreCase(key)) {
      configuration.getApplicationConfiguration().setVersion(value);
    }
    // TODO more application fields to adapt.

  }

  private void adaptSound(String key, String value) {

    if (Configurations.SOUND_ALERT.equalsIgnoreCase(key)) {
      configuration.getSoundConfiguration().setAlertSound(Boolean.valueOf(value));
    }
    if (Configurations.SOUND_RECEIVE.equalsIgnoreCase(key)) {
      configuration.getSoundConfiguration().setReceiveSound(Boolean.valueOf(value));
    }

  }

  private void adaptBitcoin(String key, String value) {

    if (Configurations.BITCOIN_SYMBOL.equalsIgnoreCase(key)) {
      configuration.getBitcoinConfiguration().setBitcoinSymbol(value);
    }
    if (Configurations.BITCOIN_DECIMAL_SEPARATOR.equalsIgnoreCase(key)) {
      configuration.getBitcoinConfiguration().setDecimalSeparator(value.charAt(0));
    }
    if (Configurations.BITCOIN_GROUPING_SEPARATOR.equalsIgnoreCase(key)) {
      configuration.getBitcoinConfiguration().setGroupingSeparator(value.charAt(0));
    }
    if (Configurations.BITCOIN_IS_CURRENCY_PREFIXED.equalsIgnoreCase(key)) {
      configuration.getBitcoinConfiguration().setCurrencySymbolLeading(Boolean.valueOf(value));
    }
    if (Configurations.BITCOIN_LOCAL_DECIMAL_PLACES.equalsIgnoreCase(key)) {
      configuration.getBitcoinConfiguration().setLocalDecimalPlaces(Integer.valueOf(value));
    }
    if (Configurations.BITCOIN_LOCAL_CURRENCY_UNIT.equalsIgnoreCase(key)) {
      configuration.getBitcoinConfiguration().setLocalCurrencyUnit(CurrencyUnit.of(value));
    }
    if (Configurations.BITCOIN_LOCAL_CURRENCY_SYMBOL.equalsIgnoreCase(key)) {
      configuration.getBitcoinConfiguration().setLocalCurrencySymbol(value);
    }

  }
  private void adaptLanguage(String key, String value) {

    if (Configurations.LANGUAGE_LOCALE.equalsIgnoreCase(key)) {
      configuration.getLanguageConfiguration().setLocale(value);
    }

  }

  /**
   * @param key The key
   * @param value The value
   */
  private void adaptLogging(String key, String value) {

    LoggingConfiguration logging = configuration.getLoggingConfiguration();

    if (Configurations.LOGGING_LEVEL.equalsIgnoreCase(key)) {
      logging.setLevel(Level.valueOf(value));
    }
    if (Configurations.LOGGING_FILE.equalsIgnoreCase(key)) {
      logging.getFileConfiguration().setCurrentLogFilename(value);
    }
    if (Configurations.LOGGING_ARCHIVE.equalsIgnoreCase(key)) {
      logging.getFileConfiguration().setArchivedLogFilenamePattern(value);
    }
    if (key.startsWith(Configurations.LOGGING_PACKAGE_PREFIX)) {
      String packageName = key.substring(Configurations.LOGGING_PACKAGE_PREFIX.length());
      logging.getLoggers().put(packageName, Level.valueOf(value));
    }
  }

}
