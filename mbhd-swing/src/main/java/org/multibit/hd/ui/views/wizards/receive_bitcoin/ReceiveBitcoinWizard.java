package org.multibit.hd.ui.views.wizards.receive_bitcoin;

import com.google.common.base.Optional;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardPanelView;

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
    super(model, isExiting, Optional.absent());
  }

  @Override
  protected void populateWizardViewMap(Map<String, AbstractWizardPanelView> wizardViewMap) {

    wizardViewMap.put(ReceiveBitcoinState.RECEIVE_ENTER_AMOUNT.name(), new ReceiveBitcoinEnterAmountPanelView(this, ReceiveBitcoinState.RECEIVE_ENTER_AMOUNT.name()));

  }



}
