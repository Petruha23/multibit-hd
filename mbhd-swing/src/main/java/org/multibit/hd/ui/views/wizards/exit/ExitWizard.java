package org.multibit.hd.ui.views.wizards.exit;

import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardView;

import java.util.Map;

/**
 * <p>Wizard to provide the following to UI for "Exit":</p>
 * <ol>
 * <li>Confirm choice</li>
 * </ol>
 *
 * @since 0.0.1
 *  
 */
public class ExitWizard extends AbstractWizard<ExitWizardModel> {

  public ExitWizard(ExitWizardModel model, boolean isExiting) {
    super(model, isExiting);
  }

  @Override
  protected void populateWizardViewMap(Map<String, AbstractWizardView> wizardViewMap) {

    wizardViewMap.put(ExitState.CONFIRM_EXIT.name(), new ExitView(this, ExitState.CONFIRM_EXIT.name()));

  }

}
