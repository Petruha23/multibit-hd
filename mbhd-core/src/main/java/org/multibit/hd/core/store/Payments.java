package org.multibit.hd.core.store;

import com.google.common.collect.Lists;
import org.multibit.hd.core.dto.PaymentRequestData;

import java.util.Collection;

/**
 *  <p>DTO to provide the following to WalletService:<br>
 *  <ul>
 *  <li>Top level encapsulating class around payment requests and transaction info</li>
 *  </p>
 *  
 */

public class Payments {
  private int lastIndexUsed;

  private Collection<PaymentRequestData> paymentRequestDatas;

  private Collection<TransactionInfo> transactionInfos;

  public Payments(int lastIndexUsed) {
    this.lastIndexUsed = lastIndexUsed;
    this.paymentRequestDatas = Lists.newArrayList();
    this.transactionInfos = Lists.newArrayList();
  }

  public int getLastIndexUsed() {
    return lastIndexUsed;
  }

  public void setLastIndexUsed(int lastIndexUsed) {
    this.lastIndexUsed = lastIndexUsed;
  }

  public Collection<PaymentRequestData> getPaymentRequestDatas() {
    return paymentRequestDatas;
  }

  public void setPaymentRequestDatas(Collection<PaymentRequestData> paymentRequestDatas) {
    this.paymentRequestDatas = paymentRequestDatas;
  }

  public Collection<TransactionInfo> getTransactionInfos() {
    return transactionInfos;
  }

  public void setTransactionInfos(Collection<TransactionInfo> transactionInfos) {
    this.transactionInfos = transactionInfos;
  }
}
