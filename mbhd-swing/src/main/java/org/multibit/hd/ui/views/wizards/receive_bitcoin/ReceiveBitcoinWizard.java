package org.multibit.hd.ui.views.wizards.receive_bitcoin;

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
public class ReceiveBitcoinWizard extends AbstractWizard<ReceiveBitcoinWizardModel> {

  public ReceiveBitcoinWizard(ReceiveBitcoinWizardModel model, boolean isExiting) {
    super(model, isExiting);
  }

  @Override
  protected void populateWizardViewMap(Map<String, AbstractWizardView> wizardViewMap) {

    wizardViewMap.put(ReceiveBitcoinState.ENTER_AMOUNT.name(), new ReceiveBitcoinEnterAmountView(this, ReceiveBitcoinState.ENTER_AMOUNT.name()));

  }

}
