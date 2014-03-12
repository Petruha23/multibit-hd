package org.multibit.hd.core.dto;

import org.joda.time.DateTime;

import java.math.BigInteger;

/**
 *  <p>Interface to provide the following to payments view:<br>
 *  <ul>
 *  <li>Unified common interface for TransactionData and PaymentRequestData </li>
 *  </ul>
 *  
 */
public interface PaymentData {

  public PaymentType getType();

  public PaymentStatus getStatus();

  public DateTime getDate();

  public BigInteger getAmountBTC();

  public FiatPayment getAmountFiat();

  public String getNote();

  public String getDescription();

  /**
   * @return true if transaction is a coinbase, false if not coinbase or not a transaction
   */
  public boolean isCoinBase();
}
