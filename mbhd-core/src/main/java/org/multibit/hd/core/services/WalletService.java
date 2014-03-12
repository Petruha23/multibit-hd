package org.multibit.hd.core.services;

import com.google.bitcoin.core.*;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.joda.money.BigMoney;
import org.joda.time.DateTime;
import org.multibit.hd.core.dto.*;
import org.multibit.hd.core.events.ExchangeRateChangedEvent;
import org.multibit.hd.core.exceptions.PaymentsLoadException;
import org.multibit.hd.core.exceptions.PaymentsSaveException;
import org.multibit.hd.core.exchanges.ExchangeKey;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.store.Payments;
import org.multibit.hd.core.store.PaymentsProtobufSerializer;
import org.multibit.hd.core.store.TransactionInfo;
import org.multibit.hd.core.utils.FileUtils;
import org.multibit.hd.core.utils.Satoshis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

/**
 *  <p>Service to provide the following to GUI classes:<br>
 *  <ul>
 *  <li>list Transactions in the current wallet</li>
 *  </ul>
 * <p/>
 * Most of the functionality is provided by WalletManager and BackupManager.
 */
public class WalletService {

  private static final Logger log = LoggerFactory.getLogger(WalletService.class);

  /**
   * The name of the directory (within the wallet directory) that contains the payments database
   */
  public final static String PAYMENTS_DIRECTORY_NAME = "payments";

  /**
   * The name of the protobuf file containing additional payments information
   */
  public static final String PAYMENTS_DATABASE_NAME = "payments.db";

  /**
   * The text separator used in localising To: and By: prefices
   */
  public static final String PREFIX_SEPARATOR = ": ";

  /**
   * The location of the main user data directory where wallets etc are stored.
   * This is typically created from InstallationManager.getOrCreateApplicationDataDirectory() but
   * is passed in to make this service easier to test
   */
  private File applicationDataDirectory;

  /**
   * The location of the backing writeContacts for the payments
   */
  private File backingStoreFile;

  /**
   * The serializer for the backing writeContacts
   */
  private PaymentsProtobufSerializer protobufSerializer;

  /**
   * The payment requests in a map, indexed by the bitcoin address
   */
  private Map<String, PaymentRequestData> paymentRequestMap;

  /**
   * The additional transaction information, in the form of a map, index by the transaction hash
   */
  private Map<String, TransactionInfo> transactionInfoMap;

  /**
   * The last index used for address generation
   */
  private int lastIndexUsed;

  /**
   * The wallet id that this WalletService is using
   */
  private WalletId walletId;

  /**
   * The undo stack for undeleting payment requests
   */
  private final Stack<PaymentRequestData> undoDeletePaymentRequestStack = new Stack<>();

  /**
   * Initialise the wallet service with a user data directory and a wallet id so that it knows where to put files etc
   *
   * @param walletId the walletId to use for this WalletService
   */
  public void initialise(File applicationDataDirectory, WalletId walletId) {
    Preconditions.checkNotNull(applicationDataDirectory, "'applicationDataDirectory' must be present");
    Preconditions.checkNotNull(walletId, "'walletId' must be present");

    this.applicationDataDirectory = applicationDataDirectory;
    this.walletId = walletId;

    // Work out where to writeContacts the contacts for this wallet id.
    String walletRoot = WalletManager.createWalletRoot(walletId);

    File walletDirectory = WalletManager.getWalletDirectory(applicationDataDirectory.getAbsolutePath(), walletRoot);

    File paymentsDirectory = new File(walletDirectory.getAbsolutePath() + File.separator + PAYMENTS_DIRECTORY_NAME);
    FileUtils.createDirectoryIfNecessary(paymentsDirectory);

    this.backingStoreFile = new File(paymentsDirectory.getAbsolutePath() + File.separator + PAYMENTS_DATABASE_NAME);

    protobufSerializer = new PaymentsProtobufSerializer();

    // Load the payment request data from the backing store if it exists
    // Initial values
    lastIndexUsed = 0;
    paymentRequestMap = Maps.newHashMap();
    transactionInfoMap = Maps.newHashMap();
    if (backingStoreFile.exists()) {
      readPayments();
    }
  }

  /**
   * Get all the payments (payments and payment requests) in the current wallet.
   * (This is a bit expensive so don't call it indiscriminately)
   *
   * @param locale the locale to use when localising text
   */
  public List<PaymentData> getPaymentDataList(Locale locale) {
    // See if there is a current wallet
    WalletManager walletManager = WalletManager.INSTANCE;

    Optional<WalletData> walletDataOptional = walletManager.getCurrentWalletData();
    if (!walletDataOptional.isPresent()) {
      // No wallet is present
      return Lists.newArrayList();
    }

    // Wallet is present
    WalletData walletData = walletDataOptional.get();
    Wallet wallet = walletData.getWallet();

    // There should be a wallet
    Preconditions.checkNotNull(wallet, "There is no wallet to process");

    // Get and adapt all the payments in the wallet
    Set<Transaction> transactions = wallet.getTransactions(true);

    // Adapted transaction data to return
    Set<TransactionData> transactionDatas = Sets.newHashSet();

    if (transactions != null) {
      for (Transaction transaction : transactions) {
        TransactionData transactionData = adaptTransaction(wallet, transaction);
        transactionDatas.add(transactionData);
      }
    }

    // Determine which paymentRequests have not been fully funded
    Set<PaymentRequestData> paymentRequestsNotFullyFunded = Sets.newHashSet();
    for (PaymentRequestData basePaymentRequestData : paymentRequestMap.values()) {
      if (basePaymentRequestData.getPaidAmountBTC().compareTo(basePaymentRequestData.getAmountBTC()) < 0) {
        paymentRequestsNotFullyFunded.add(basePaymentRequestData);
      }
    }
    // Union all the transactionDatas and paymentDatas
    return Lists.newArrayList(Sets.union(transactionDatas, paymentRequestsNotFullyFunded));
  }

  /**
   * Adapt a bitcoinj transaction to a TransactionData DTO.
   * Also merges in any transactionInfo available.
   * Also checks if this transaction funds any payment requests
   *
   * @param wallet      the current wallet
   * @param transaction the transaction to adapt
   * @return TransactionData the transaction data
   */
  public TransactionData adaptTransaction(Wallet wallet, Transaction transaction) {

    // Tx id
    String transactionHashAsString = transaction.getHashAsString();

    // UpdateTime
    Date updateTime = transaction.getUpdateTime();

    // Amount BTC
    BigInteger amountBTC = transaction.getValue(wallet);

    // Fiat amount
    FiatPayment amountFiat = calculateFiatPayment(amountBTC);

    TransactionConfidence transactionConfidence = transaction.getConfidence();

    // Depth
    int depth = 0; // By default not in a block
    TransactionConfidence.ConfidenceType confidenceType = TransactionConfidence.ConfidenceType.UNKNOWN;

    if (transactionConfidence != null) {
      confidenceType = transaction.getConfidence().getConfidenceType();
      if (TransactionConfidence.ConfidenceType.BUILDING.equals(confidenceType)) {
        depth = transaction.getConfidence().getDepthInBlocks();
      }
    }

    // Payment status
    PaymentStatus paymentStatus = calculateStatus(transaction);

    // Payment type
    PaymentType paymentType = calculatePaymentType(amountBTC, depth);

    // Fee on send
    Optional<BigInteger> feeOnSend = calculateFeeOnSend(paymentType, wallet, transaction);

    // Description
    String description = calculateDescription(wallet, transaction, transactionHashAsString,paymentType, amountBTC);


    // Create the DTO from the raw transaction info
    TransactionData transactionData = new TransactionData(transactionHashAsString, new DateTime(updateTime), paymentStatus, amountBTC, amountFiat,
            feeOnSend, confidenceType, paymentType, description, transaction.isCoinBase());

    // Note - from the transactionInfo (if present)
    calculateNote(transactionData, transactionHashAsString);

    // Related payment requests - from the transactionInfo (if present)
    copyRelatedPaymentRequests(transactionData, transactionHashAsString);

    return transactionData;
  }

  /**
   * Calculate the PaymentStatus of the transaction:
   * + RED   = tx is dead, double spend, failed to be transmitted to the network
   * + AMBER = tx is unconfirmed
   * + GREEN = tx is confirmed
   *
   * @param transaction the bitcoinj transaction to use to work out the status
   * @return status of the transaction
   */
  private static PaymentStatus calculateStatus(Transaction transaction) {
    if (transaction.getConfidence() != null) {
      TransactionConfidence.ConfidenceType confidenceType = transaction.getConfidence().getConfidenceType();

      if (TransactionConfidence.ConfidenceType.BUILDING.equals(confidenceType)) {
        // Confirmed
        PaymentStatus paymentStatus = new PaymentStatus(RAGStatus.GREEN);
        int depth = transaction.getConfidence().getDepthInBlocks();
        paymentStatus.setDepth(depth);
        if (depth == 1) {
          paymentStatus.setStatusKey(CoreMessageKey.CONFIRMED_BY_ONE_BLOCK);
        } else {
          paymentStatus.setStatusKey(CoreMessageKey.CONFIRMED_BY_SEVERAL_BLOCKS);
          paymentStatus.setStatusData(new Object[]{depth});
        }
        return paymentStatus;
      } else if (TransactionConfidence.ConfidenceType.PENDING.equals(confidenceType)) {
        int numberOfPeers = transaction.getConfidence().numBroadcastPeers();
        if (numberOfPeers >= 2) {
          // Seen by the network but not confirmed yet
          PaymentStatus paymentStatus = new PaymentStatus(RAGStatus.AMBER);
          paymentStatus.setStatusKey(CoreMessageKey.BROADCAST);
          paymentStatus.setStatusData(new Object[]{numberOfPeers});
          return paymentStatus;
        } else {
          // Not out in the network
          PaymentStatus paymentStatus = new PaymentStatus(RAGStatus.RED);
          paymentStatus.setStatusKey(CoreMessageKey.NOT_BROADCAST);
          return paymentStatus;
        }
      } else if (TransactionConfidence.ConfidenceType.DEAD.equals(confidenceType)) {
        // Dead
        PaymentStatus paymentStatus = new PaymentStatus(RAGStatus.RED);
        paymentStatus.setStatusKey(CoreMessageKey.DEAD);
        return paymentStatus;
      } else if (TransactionConfidence.ConfidenceType.UNKNOWN.equals(confidenceType)) {
        // Unknown
        PaymentStatus paymentStatus = new PaymentStatus(RAGStatus.AMBER);
        paymentStatus.setStatusKey(CoreMessageKey.UNKNOWN);
        return paymentStatus;
      }
    } else {
      // No transaction status - don't know
      PaymentStatus paymentStatus = new PaymentStatus(RAGStatus.AMBER);
      paymentStatus.setStatusKey(CoreMessageKey.UNKNOWN);
      return paymentStatus;
    }
    // Unknown
    PaymentStatus paymentStatus = new PaymentStatus(RAGStatus.AMBER);
    paymentStatus.setStatusKey(CoreMessageKey.UNKNOWN);
    return paymentStatus;
  }

  private PaymentType calculatePaymentType(BigInteger amountBTC, int depth) {
    PaymentType paymentType;
    if (amountBTC.compareTo(BigInteger.ZERO) < 0) {
      // Debit
      if (depth == 0) {
        paymentType = PaymentType.SENDING;
      } else {
        paymentType = PaymentType.SENT;
      }
    } else {
      // Credit
      if (depth == 0) {
        paymentType = PaymentType.RECEIVING;
      } else {
        paymentType = PaymentType.RECEIVED;
      }
    }
    return paymentType;
  }

  private String calculateDescription(Wallet wallet, Transaction transaction, String transactionHashAsString, PaymentType paymentType, BigInteger amountBTC) {
    String description;
    if (paymentType == PaymentType.RECEIVING || paymentType == PaymentType.RECEIVED) {
      description = "";
      String addresses = "";

      boolean descriptiveTextIsAvailable = false;
      if (transaction.getOutputs() != null) {
        for (TransactionOutput transactionOutput : transaction.getOutputs()) {
          if (transactionOutput.isMine(wallet)) {
            String receivingAddress = transactionOutput.getScriptPubKey().getToAddress(NetworkParameters.fromID(NetworkParameters.ID_MAINNET)).toString();
            addresses = addresses + " " + receivingAddress;

            // Check if this output funds any payment requests;
            PaymentRequestData paymentRequestData = paymentRequestMap.get(receivingAddress);
            if (paymentRequestData != null) {
              // Yes - this output funds a payment address
              if (!paymentRequestData.getPayingTransactionHashes().contains(transactionHashAsString)) {
                // We have not yet added this tx to the total paid amount
                paymentRequestData.getPayingTransactionHashes().add(transactionHashAsString);
                paymentRequestData.setPaidAmountBTC(paymentRequestData.getPaidAmountBTC().add(amountBTC));
              }

              if (paymentRequestData.getLabel() != null && paymentRequestData.getLabel().length() > 0) {
                descriptiveTextIsAvailable = true;
                description = description + paymentRequestData.getLabel() + " ";
              }
              if (paymentRequestData.getNote() != null && paymentRequestData.getNote().length() > 0) {
                descriptiveTextIsAvailable = true;
                description = description + paymentRequestData.getNote() + " ";
              }
            }
          }
        }
      }

      if (!descriptiveTextIsAvailable) {
        // TODO localise
        description = "By" + PREFIX_SEPARATOR + addresses.trim();
      }
    } else {
      // Sent
      // TODO localise
      description = "To" + PREFIX_SEPARATOR;
      if (transaction.getOutputs() != null) {
        for (TransactionOutput transactionOutput : transaction.getOutputs()) {
          // TODO Beef up description for other cases
          description = description + " " + transactionOutput.getScriptPubKey().getToAddress(NetworkParameters.fromID(NetworkParameters.ID_MAINNET));
        }
      }
    }
    return description;
  }

  private FiatPayment calculateFiatPayment(BigInteger amountBTC) {
    FiatPayment amountFiat = new FiatPayment();
    amountFiat.setExchange(ExchangeKey.current().getExchangeName());
    Optional<ExchangeRateChangedEvent> exchangeRateChangedEvent = CoreServices.getApplicationEventService().getLatestExchangeRateChangedEvent();
    if (exchangeRateChangedEvent.isPresent() && exchangeRateChangedEvent.get().getRate() != null) {

      amountFiat.setRate(exchangeRateChangedEvent.get().getRate().toString());
      BigMoney localAmount = Satoshis.toLocalAmount(amountBTC, exchangeRateChangedEvent.get().getRate());
      amountFiat.setAmount(localAmount);
    } else {
      amountFiat.setRate("");
      amountFiat.setAmount(null);
    }
    return amountFiat;
  }

  private void calculateNote(TransactionData transactionData, String transactionHashAsString) {
    TransactionInfo transactionInfo = transactionInfoMap.get(transactionHashAsString);
     if (transactionInfo != null) {
       String note = transactionInfo.getNote();
       if (note != null) {
         transactionData.setNote(note);
         // if there is a real note use that as the description
         if (note.length() > 0) {
           transactionData.setDescription(note);
         }
       } else {
         transactionData.setNote("");
       }
       transactionData.setAmountFiat(transactionInfo.getAmountFiat());
     } else {
       transactionData.setNote("");
     }
  }

  private void copyRelatedPaymentRequests(TransactionData transactionData, String transactionHashAsString) {
     TransactionInfo transactionInfo = transactionInfoMap.get(transactionHashAsString);
      if (transactionInfo != null) {
        transactionData.setPaymentRequestAddresses(transactionInfo.getRequestAddresses());
      }
   }

  private Optional<BigInteger> calculateFeeOnSend(PaymentType paymentType, Wallet wallet, Transaction transaction) {
    Optional<BigInteger> feeOnSend = Optional.absent();

    if (paymentType == PaymentType.SENDING || paymentType == PaymentType.SENT) {
     // TODO - transaction.calculateFee(wallet) seems to have disappeared from transaction
    }

    return feeOnSend;
  }

  /**
   * <p>Populate the internal cache of Payments from the backing store</p>
   */
  public void readPayments() throws PaymentsLoadException {
    Preconditions.checkNotNull(backingStoreFile, "There is no backingStoreFile. Please initialise WalletService.");
    try (FileInputStream fis = new FileInputStream(backingStoreFile)) {

      Payments payments = protobufSerializer.readPayments(fis);

      lastIndexUsed = payments.getLastIndexUsed();

      // For quick access payment requests and transaction infos are stored in maps
      Collection<PaymentRequestData> paymentRequestDatas = payments.getPaymentRequestDatas();
      if (paymentRequestDatas != null) {
        paymentRequestMap.clear();
        for (PaymentRequestData paymentRequestData : paymentRequestDatas) {
          paymentRequestMap.put(paymentRequestData.getAddress(), paymentRequestData);
        }
      }

      Collection<TransactionInfo> transactionInfos = payments.getTransactionInfos();
      if (transactionInfos != null) {
        transactionInfoMap.clear();
        for (TransactionInfo transactionInfo : transactionInfos) {
          transactionInfoMap.put(transactionInfo.getHash(), transactionInfo);
        }
      }
    } catch (IOException | PaymentsLoadException e) {
      throw new PaymentsLoadException("Could not read payments db '" + backingStoreFile.getAbsolutePath() + "'. Error was '" + e.getMessage() + "'.");
    }
  }

  /**
   * <p>Save the payments data to the backing store</p>
   */
  public void writePayments() throws PaymentsSaveException {
    Preconditions.checkNotNull(backingStoreFile, "There is no backingStoreFile. Please initialise WalletService.");

    try (FileOutputStream fos = new FileOutputStream(backingStoreFile)) {

      Payments payments = new Payments(lastIndexUsed);
      payments.setTransactionInfos(transactionInfoMap.values());
      payments.setPaymentRequestDatas(paymentRequestMap.values());
      protobufSerializer.writePayments(payments, fos);

    } catch (IOException | PaymentsSaveException e) {
      throw new PaymentsSaveException("Could not write payments db '" + backingStoreFile.getAbsolutePath() + "'. Error was '" + e.getMessage() + "'.");
    }
  }

  public WalletId getWalletId() {
    return walletId;
  }

  public void addPaymentRequest(PaymentRequestData paymentRequestData) {
    paymentRequestMap.put(paymentRequestData.getAddress(), paymentRequestData);
  }

  public void addTransactionInfo(TransactionInfo transactionInfo) {
    transactionInfoMap.put(transactionInfo.getHash(), transactionInfo);
  }

  public Collection<PaymentRequestData> getPaymentRequests() {
    return paymentRequestMap.values();
  }

  public PaymentRequestData getPaymentRequestData(String paymentRequestAddress) {
    return paymentRequestMap.get(paymentRequestAddress);
  }

  /**
   * Create the next receiving address for the wallet.
   * This is either the first key's address in the wallet or is
   * worked out deterministically and uses the lastIndexUsed on the Payments so that each address is unique
   * <p/>
   * Remember to save both the dirty wallet and the payment requests backing store after calling this method when new keys are generated
   * TODO replace with proper HD algorithm
   *
   * @param walletPasswordOptional Either: Optional.absent() = just recycle the first address in the wallet or:  password of the wallet to which the new private key is added
   * @return Address the next generated address, as a String. The corresponding private key will be added to the wallet
   */
  public String generateNextReceivingAddress(Optional<CharSequence> walletPasswordOptional) {
    Optional<WalletData> walletDataOptional = WalletManager.INSTANCE.getCurrentWalletData();
    if (!walletDataOptional.isPresent()) {
      // No wallet is present
      throw new IllegalStateException("Trying to add a key to a non-existent wallet");
    } else {
      // If there is no password then recycle the first address in the wallet
      if (walletPasswordOptional.isPresent()) {
        // increment the lastIndexUsed
        lastIndexUsed++;
        log.debug("The last index used has been incremented to " + lastIndexUsed);
        ECKey newKey = WalletManager.INSTANCE.createAndAddNewWalletKey(walletDataOptional.get().getWallet(), walletPasswordOptional.get(), lastIndexUsed);
        return newKey.toAddress(NetworkParameters.fromID(NetworkParameters.ID_MAINNET)).toString();
      } else {
        return walletDataOptional.get().getWallet().getKeys().get(0).toAddress(NetworkParameters.fromID(NetworkParameters.ID_MAINNET)).toString();
      }
    }
  }

  /**
   * Delete a payment request
   */
  public void deletePaymentRequest(PaymentRequestData paymentRequestData) {
    undoDeletePaymentRequestStack.push(paymentRequestData);
    paymentRequestMap.remove(paymentRequestData.getAddress());
    writePayments();
  }

  /**
   * Undo the deletion of a payment request
   */
  public void undoDeletePaymentRequest() {
    if (!undoDeletePaymentRequestStack.isEmpty()) {
      PaymentRequestData deletedPaymentRequestData = undoDeletePaymentRequestStack.pop();
      addPaymentRequest(deletedPaymentRequestData);
      writePayments();
    }
  }
}
