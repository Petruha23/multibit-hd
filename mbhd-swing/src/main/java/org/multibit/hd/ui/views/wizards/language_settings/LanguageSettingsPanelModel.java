package org.multibit.hd.ui.views.wizards.language_settings;

import org.multibit.hd.core.config.Configuration;
import org.multibit.hd.ui.views.wizards.AbstractWizardPanelModel;

/**
 * <p>Panel model to provide the following to "internationalisation settings" wizard:</p>
 * <ul>
 * <li>Storage of state for the "formatting" panel</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class LanguageSettingsPanelModel extends AbstractWizardPanelModel {

  private final Configuration configuration;

  /**
   * @param panelName     The panel name
   * @param configuration The configuration to use
   */
  public LanguageSettingsPanelModel(String panelName, Configuration configuration) {
    super(panelName);
    this.configuration = configuration;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

}
