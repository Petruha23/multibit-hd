package org.multibit.hd.ui.i18n;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.dto.CoreMessageKey;
import org.multibit.hd.ui.exceptions.UIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * <p>Utility to provide the following to Views:</p>
 * <ul>
 * <li>Access to internationalised text strings</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class Languages {

  private static final Logger log = LoggerFactory.getLogger(Languages.class);

  private static final String BASE_NAME = "i18n.viewer";

  private static final String LANGUAGE_CHOICE_TEMPLATE = "showPreferencesPanel.language.%d";

  /**
   * The 2-letter short language code suitable for use with Locale
   * The ordering is consistent with the presentation of the language
   * list within the resource bundle, but this list is 0-based
   */
  private static final String[] LANGUAGE_CODES = new String[]{
    // 0 - 9
    "en", "es", "ru", "sv", "no", "it", "fr", "de", "pt", "hi",
    // 10 - 19
    "th", "nl", "zh", "ja", "ar", "ko", "lv", "tr", "fi", "pl",
    // 20 - 29
    "hr", "hu", "el", "da", "eo", "fa", "he", "sk", "cs", "id",
    // 30 - 35
    "sl", "ta", "ro", "af", "tl", "sw",
  };

  /**
   * Utilities have private constructors
   */
  private Languages() {
  }

  /**
   * @param includeCodes True if the list should prefix the names with the ISO language code
   *
   * @return An unsorted array of the available languages
   */
  public static String[] getLanguageNames(boolean includeCodes) {

    ResourceBundle rb = currentResourceBundle();

    String[] items = new String[LANGUAGE_CODES.length];

    for (int i = 0; i < LANGUAGE_CODES.length; i++) {

      // Use a 1-based lookup for the resource bundle
      String key = String.format(LANGUAGE_CHOICE_TEMPLATE, i + 1);
      String value = rb.getString(key);

      if (includeCodes) {
        items[i] = LANGUAGE_CODES[i] + ": " + value;
      } else {
        items[i] = value;
      }

    }

    return items;

  }

  /**
   * @return Current locale
   */
  public static Locale currentLocale() {

    Preconditions.checkNotNull(Configurations.currentConfiguration, "'currentConfiguration' must be present");

    return Configurations.currentConfiguration.getLocale();
  }

  /**
   * @param code The 2 letter ISO code of the language
   *
   * @return A new resource bundle based on the locale
   */
  public static Locale newLocaleFromCode(String code) {

    Preconditions.checkNotNull(code, "'code' must be present");

    return new Locale(code);
  }

  /**
   * @param locale The locale
   *
   * @return The matching index (0-based)
   *
   * @throws org.multibit.hd.ui.exceptions.UIException If there is no match
   */
  public static int getIndexFromLocale(Locale locale) {

    Preconditions.checkNotNull(locale, "'locale' must be present");

    String language = locale.getLanguage().toLowerCase().substring(0, 2);

    for (int i = 0; i < LANGUAGE_CODES.length; i++) {

      if (LANGUAGE_CODES[i].startsWith(language)) {
        return i;
      }

    }

    throw new UIException("Unknown locale: " + locale);
  }

  /**
   * @param key    The key (treated as a direct format string if not present)
   * @param values An optional collection of value substitutions for {@link MessageFormat}
   *
   * @return The localised text with any substitutions made
   */
  public static String safeText(MessageKey key, Object... values) {

    ResourceBundle rb = currentResourceBundle();

    final String message;

    if (!rb.containsKey(key.getKey())) {
      // If no key is present then use it direct
      message = key.getKey();
    } else {
      // Must have the key to be here
      message = rb.getString(key.getKey());
    }

    return MessageFormat.format(message, values);
  }

  /**
   * @param key    The key (treated as a direct format string if not present)
   * @param values An optional collection of value substitutions for {@link MessageFormat}
   *
   * @return The localised text with any substitutions made
   */
  public static String safeText(CoreMessageKey key, Object... values) {

    ResourceBundle rb = currentResourceBundle();

    final String message;

    if (!rb.containsKey(key.getKey())) {
      // If no key is present then use it direct
      message = key.getKey();
    } else {
      // Must have the key to be here
      message = rb.getString(key.getKey());
    }

    return MessageFormat.format(message, values);
  }

  /**
   * @param key    The key (must be present in the bundle)
   * @param values An optional collection of value substitutions for {@link MessageFormat}
   *
   * @return The localised text with any substitutions made
   */
  public static String safeText(String key, Object... values) {

    ResourceBundle rb = currentResourceBundle();

    final String message;

    if (!rb.containsKey(key)) {
      // If no key is present then use it direct
      message = "Key '" + key + "' is not localised!";
    } else {
      // Must have the key to be here
      message = rb.getString(key);
    }

    return MessageFormat.format(message, values);
  }

  /**
   * @param contents  The contents to join into a localised comma-separated list
   * @param maxLength The maximum length of the result single string
   *
   * @return The localised comma-separated list with ellipsis appended if truncated
   */
  public static String truncatedList(Collection<String> contents, int maxLength) {

    String joinedContents = Joiner
      .on(Languages.safeText(MessageKey.LIST_COMMA)+ " ")
      .join(contents);

    String ellipsis = Languages.safeText(MessageKey.LIST_ELLIPSIS);

    // Determine the truncation point (if required)
    int maxIndex = Math.min(joinedContents.length() - 1, maxLength - ellipsis.length() - 1);

    if (maxIndex == joinedContents.length() - 1) {
      // No truncation
      return joinedContents;
    } else {
      // Apply truncation (with ellipsis)
      return joinedContents.substring(0, maxIndex) + ellipsis;
    }

  }

  /**
   * <p>Internal access only - external consumers should use safeText()</p>
   *
   * @return The resource bundle based on the current locale
   */
  private static ResourceBundle currentResourceBundle() {

    return ResourceBundle.getBundle(BASE_NAME, currentLocale());
  }

  /**
   * @return The component orientation based on the current locale
   */
  public static ComponentOrientation currentComponentOrientation() {
    return ComponentOrientation.getOrientation(Languages.currentLocale());
  }

  /**
   * @return True if text is to be placed left to right (standard Western language presentation)
   */
  public static boolean isLeftToRight() {
    return ComponentOrientation.getOrientation(currentLocale()).isLeftToRight();
  }
}
