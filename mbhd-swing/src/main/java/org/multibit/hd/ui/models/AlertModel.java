package org.multibit.hd.ui.models;

import org.multibit.hd.ui.i18n.MessageKey;
import org.multibit.hd.core.dto.RAGStatus;
import org.multibit.hd.ui.i18n.Languages;

/**
 * <p>Value object to provide the following to Alert API:</p>
 * <ul>
 * <li>Provision of state for an alert</li>
 * </ul>
 *
 * @since 0.0.1
 *         
 */
public class AlertModel implements Model<String> {

  private final RAGStatus severity;
  private String localisedMessage;

  private int remaining = 0;

  AlertModel(String localisedMessage, RAGStatus severity) {
    this.severity = severity;
    this.localisedMessage = localisedMessage;
  }

  public int getRemaining() {
    return remaining;
  }

  public void setRemaining(int remaining) {
    this.remaining = remaining;
  }

  public RAGStatus getSeverity() {
    return severity;
  }

  public String getLocalisedMessage() {
    return localisedMessage;
  }

  public String getRemainingText() {
    if (remaining > 0) {
      return Languages.safeText(MessageKey.ALERT_REMAINING,remaining);
    }
    return "";
  }

  @Override
  public String getValue() {
    return localisedMessage;
  }

  @Override
  public void setValue(String value) {
    this.localisedMessage = value;
  }

}
