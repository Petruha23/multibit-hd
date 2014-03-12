package org.multibit.hd.ui.languages;

import com.google.common.base.Preconditions;
import org.joda.money.BigMoney;
import org.multibit.hd.core.config.BitcoinConfiguration;
import org.multibit.hd.core.config.LanguageConfiguration;
import org.multibit.hd.core.utils.BitcoinSymbol;
import org.multibit.hd.core.utils.Satoshis;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * <p>Utility to provide the following to controllers:</p>
 * <ul>
 * <li>Access to international formats for date/time and decimal data</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class Formats {

  /**
   * <p>Provide a split representation for the Bitcoin balance display.</p>
   * <p>For example, 12345.6789 becomes "12,345.67", "89" </p>
   * <p>The amount will be adjusted by the symbolic multiplier from the current confiuration</p>
   *
   * @param satoshis              The amount in satoshis
   * @param languageConfiguration The  language configuration to use as the basis for presentation
   * @param bitcoinConfiguration  The Bitcoin configuration to use as the basis for the symbol
   *
   * @return The left [0] and right [1] components suitable for presentation as a balance with no symbolic decoration
   */
  public static String[] formatSatoshisAsSymbolic(
    BigInteger satoshis,
    LanguageConfiguration languageConfiguration,
    BitcoinConfiguration bitcoinConfiguration
  ) {

    Preconditions.checkNotNull(satoshis, "'satoshis' must be present");
    Preconditions.checkNotNull(languageConfiguration, "'languageConfiguration' must be present");

    Locale currentLocale = languageConfiguration.getLocale();
    BitcoinSymbol bitcoinSymbol = BitcoinSymbol.of(bitcoinConfiguration.getBitcoinSymbol());

    DecimalFormatSymbols dfs = configureDecimalFormatSymbols(bitcoinConfiguration, currentLocale);
    DecimalFormat localFormat = configureBitcoinDecimalFormat(dfs, bitcoinSymbol);

    // Apply formatting to the symbolic amount
    String formattedAmount = localFormat.format(Satoshis.toSymbolicAmount(satoshis, bitcoinSymbol));

    // The Satoshi symbol does not have decimals
    if (BitcoinSymbol.SATOSHI.equals(bitcoinSymbol)) {

      return new String[]{
        formattedAmount,
        ""
      };

    }

    // All other representations require a decimal

    int decimalIndex = formattedAmount.lastIndexOf(dfs.getDecimalSeparator());

    if (decimalIndex == -1) {
      formattedAmount += dfs.getDecimalSeparator() + "00";
      decimalIndex = formattedAmount.lastIndexOf(dfs.getDecimalSeparator());
    }

    return new String[]{
      formattedAmount.substring(0, decimalIndex + 3), // 12,345.67 (significant figures)
      formattedAmount.substring(decimalIndex + 3) // 89 (lesser figures truncated )
    };

  }

  /**
   * <p>Provide a simple representation for a local currency amount.</p>
   *
   * @param amount               The amount as a plain number (no multipliers)
   * @param locale               The locale to use
   * @param bitcoinConfiguration The Bitcoin configuration to use as the basis for the symbol
   *
   * @return The local currency representation with no symbolic decoration
   */
  public static String formatLocalAmount(BigMoney amount, Locale locale, BitcoinConfiguration bitcoinConfiguration) {

    if (amount == null) {
      return "";
    }

    DecimalFormatSymbols dfs = configureDecimalFormatSymbols(bitcoinConfiguration, locale);
    DecimalFormat localFormat = configureLocalDecimalFormat(dfs, bitcoinConfiguration);

    return localFormat.format(amount.getAmount());

  }

  /**
   * @param dfs The decimal format symbols
   *
   * @return A decimal format suitable for Bitcoin balance representation
   */
  private static DecimalFormat configureBitcoinDecimalFormat(DecimalFormatSymbols dfs, BitcoinSymbol bitcoinSymbol) {

    DecimalFormat format = new DecimalFormat();

    format.setDecimalFormatSymbols(dfs);

    format.setMaximumIntegerDigits(16);
    format.setMinimumIntegerDigits(1);

    format.setMaximumFractionDigits(bitcoinSymbol.decimalPlaces());
    format.setMinimumFractionDigits(bitcoinSymbol.decimalPlaces());

    format.setDecimalSeparatorAlwaysShown(false);

    return format;
  }

  /**
   * @param dfs                  The decimal format symbols
   * @param bitcoinConfiguration The Bitcoin configuration to use
   *
   * @return A decimal format suitable for local currency balance representation
   */
  private static DecimalFormat configureLocalDecimalFormat(DecimalFormatSymbols dfs, BitcoinConfiguration bitcoinConfiguration) {

    DecimalFormat format = new DecimalFormat();

    format.setDecimalFormatSymbols(dfs);

    format.setMinimumIntegerDigits(1);
    format.setMaximumFractionDigits(bitcoinConfiguration.getLocalDecimalPlaces());
    format.setMinimumFractionDigits(bitcoinConfiguration.getLocalDecimalPlaces());

    format.setDecimalSeparatorAlwaysShown(true);

    return format;
  }

  /**
   * @param bitcoinConfiguration The Bitcoin configuration
   * @param currentLocale        The current locale
   *
   * @return The decimal format symbols to use based on the configuration and locale
   */
  private static DecimalFormatSymbols configureDecimalFormatSymbols(BitcoinConfiguration bitcoinConfiguration, Locale currentLocale) {

    DecimalFormatSymbols dfs = new DecimalFormatSymbols(currentLocale);

    dfs.setDecimalSeparator(bitcoinConfiguration.getDecimalSeparator());
    dfs.setGroupingSeparator(bitcoinConfiguration.getGroupingSeparator());

    return dfs;

  }
}
