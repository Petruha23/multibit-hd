package org.multibit.hd.core.services;

import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.wallet.DeterministicSeed;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.multibit.hd.brit.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.seed_phrase.SeedPhraseGenerator;
import org.multibit.hd.core.config.BitcoinNetwork;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.dto.*;
import org.multibit.hd.core.managers.BackupManager;
import org.multibit.hd.core.managers.InstallationManager;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.managers.WalletManagerTest;
import org.multibit.hd.core.utils.Dates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class WalletServiceTest {

  private static NetworkParameters networkParameters;

  private WalletService walletService;

  private WalletId walletId;

  private WalletSummary walletSummary;

  public static final String PASSWORD = "1throckSplockChockAdock";

  public static final String CHANGED_PASSWORD1 = "2orinocoFlow";

  public static final String CHANGED_PASSWORD2 = "3the quick brown fox jumps over the lazy dog";

  private static final Logger log = LoggerFactory.getLogger(WalletServiceTest.class);

  private String firstAddress;


  @Before
  public void setUp() throws Exception {

    Configurations.currentConfiguration = Configurations.newDefaultConfiguration();
    networkParameters = BitcoinNetwork.current().get();

    // Create a random temporary directory where the wallet directory will be written
    File temporaryDirectory = WalletManagerTest.makeRandomTemporaryApplicationDirectory();

    // Create a wallet from a seed
    SeedPhraseGenerator seedGenerator = new Bip39SeedPhraseGenerator();
    byte[] seed1 = seedGenerator.convertToSeed(Bip39SeedPhraseGenerator.split(WalletIdTest.SEED_PHRASE_1));
    walletId = new WalletId(seed1);

    BackupManager.INSTANCE.initialise(temporaryDirectory, null);
    InstallationManager.setCurrentApplicationDataDirectory(temporaryDirectory);

    long nowInSeconds = Dates.nowInSeconds();
    walletSummary = WalletManager
            .INSTANCE
            .getOrCreateWalletSummary(
                    temporaryDirectory,
                    seed1,
                    nowInSeconds,
                    PASSWORD,
                    "Example",
                    "Example"
            );
    WalletManager.INSTANCE.setCurrentWalletSummary(walletSummary);

    firstAddress = walletSummary.getWallet().freshReceiveKey().toString();

    walletService = new WalletService(networkParameters);

    walletService.initialise(temporaryDirectory, walletId);
  }

  @Test
  public void testCreatePaymentRequest() throws Exception {
    // Initially there are no payment requests
    assertThat(walletService.getPaymentRequests().size()).isEqualTo(0);

    // Create a new payment request
    PaymentRequestData paymentRequestData1 = new PaymentRequestData();

    paymentRequestData1.setAddress("1abc");
    paymentRequestData1.setAmountCoin(Coin.valueOf(245));
    DateTime date1 = new DateTime();
    paymentRequestData1.setDate(date1);
    paymentRequestData1.setLabel("label1");
    paymentRequestData1.setNote("note1");

    FiatPayment fiatPayment1 = new FiatPayment();
    paymentRequestData1.setAmountFiat(fiatPayment1);
    fiatPayment1.setAmount(Optional.of(new BigDecimal("12345.6")));
    fiatPayment1.setCurrency(Optional.of(Currency.getInstance("USD")));
    fiatPayment1.setRate(Optional.of("10.0"));
    fiatPayment1.setExchangeName(Optional.of("Bitstamp"));

    walletService.addPaymentRequest(paymentRequestData1);

    // Write the payment requests to the backing store
    walletService.writePayments();

    // Read the payment requests
    walletService.readPayments();

    // Check the new payment request is present
    Collection<PaymentRequestData> newPaymentRequestDatas = walletService.getPaymentRequests();
    assertThat(newPaymentRequestDatas.size()).isEqualTo(1);

    checkPaymentRequest(paymentRequestData1, newPaymentRequestDatas.iterator().next());
  }

  private void checkPaymentRequest(PaymentRequestData paymentRequestData, PaymentRequestData other) {
    assertThat(other.getAddress()).isEqualTo(paymentRequestData.getAddress());
    assertThat(other.getLabel()).isEqualTo(paymentRequestData.getLabel());
    assertThat(other.getNote()).isEqualTo(paymentRequestData.getNote());
    assertThat(other.getAmountCoin()).isEqualTo(paymentRequestData.getAmountCoin());
    assertThat(other.getDate()).isEqualTo(paymentRequestData.getDate());

    FiatPayment fiatPayment = other.getAmountFiat();
    FiatPayment otherFiatPayment = paymentRequestData.getAmountFiat();
    assertThat(fiatPayment.getAmount()).isEqualTo(otherFiatPayment.getAmount());
    assertThat(fiatPayment.getRate()).isEqualTo(otherFiatPayment.getRate());
    assertThat(fiatPayment.getExchangeName()).isEqualTo(otherFiatPayment.getExchangeName());
  }

  @Test
  public void testChangePassword() throws Exception {
    log.debug("Start of testChangePassword");

    assertThat(walletSummary.getWallet().checkPassword(PASSWORD)).isTrue();

    // Change the password once
    WalletService.changeWalletPasswordInternal(walletSummary, PASSWORD, CHANGED_PASSWORD1);
    assertThat(walletSummary.getWallet().checkPassword(CHANGED_PASSWORD1)).isTrue();

    // Change the password again
    WalletService.changeWalletPasswordInternal(walletSummary, CHANGED_PASSWORD1, CHANGED_PASSWORD2);
    assertThat(walletSummary.getWallet().checkPassword(CHANGED_PASSWORD2)).isTrue();

    // And change ti back to the original value just for good measure
    WalletService.changeWalletPasswordInternal(walletSummary, CHANGED_PASSWORD2, PASSWORD);
    assertThat(walletSummary.getWallet().checkPassword(PASSWORD)).isTrue();
  }

  @Test
  /**
   * Simple test to check decryption of a bitcoinj wallet - referenced in bitcoinj issue:
   * https://code.google.com/p/bitcoinj/issues/detail?id=573&thanks=573&ts=1406733004
   */
  public void testChangePasswordSimple() throws Exception {
    NetworkParameters networkParameters = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

    long creationTimeSecs = MnemonicCode.BIP39_STANDARDISATION_TIME_SECS;
    String seedStr = "letter advice cage absurd amount doctor acoustic avoid letter advice cage above";

    // Parse as mnemonic code.
    final List<String> split = ImmutableList.copyOf(Splitter.on(" ").omitEmptyStrings().split(seedStr));


    // Test encrypt / decrypt with empty passphrase
    DeterministicSeed seed1 = new DeterministicSeed(split, "", creationTimeSecs);

    Wallet wallet1 = Wallet.fromSeed(networkParameters, seed1);

    // Encrypt wallet
    wallet1.encrypt(PASSWORD);

    // Decrypt the wallet
    wallet1.decrypt(PASSWORD);
  }
}
