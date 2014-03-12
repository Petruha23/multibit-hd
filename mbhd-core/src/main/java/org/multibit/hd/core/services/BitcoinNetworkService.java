package org.multibit.hd.core.services;

import com.google.bitcoin.core.*;
import com.google.bitcoin.crypto.KeyCrypterException;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.store.BlockStore;
import com.google.bitcoin.store.BlockStoreException;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import org.joda.time.DateTime;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.dto.BitcoinNetworkSummary;
import org.multibit.hd.core.dto.CoreMessageKey;
import org.multibit.hd.core.dto.WalletData;
import org.multibit.hd.core.dto.WalletId;
import org.multibit.hd.core.events.BitcoinSentEvent;
import org.multibit.hd.core.events.CoreEvents;
import org.multibit.hd.core.events.TransactionCreationEvent;
import org.multibit.hd.core.managers.BackupManager;
import org.multibit.hd.core.managers.BlockStoreManager;
import org.multibit.hd.core.managers.InstallationManager;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.network.MultiBitPeerEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * <p>Service to provide access to the Bitcoin network, including:</p>
 * <ul>
 * <li>Initialisation of bitcoin network connection</li>
 * <li>Ability to send bitcoin</li>
 * </ul>
 * <p/>
 * <p>Emits the following events:</p>
 * <ul>
 * <li><code>BitcoinNetworkChangeEvent</code></li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class BitcoinNetworkService extends AbstractService {

  public static final BigInteger DEFAULT_FEE_PER_KB = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE; // Currently 10,000 satoshi
  public static final MainNetParams NETWORK_PARAMETERS = MainNetParams.get();
  public static final int MAXIMUM_NUMBER_OF_PEERS = 6;
  private static final Logger log = LoggerFactory.getLogger(BitcoinNetworkService.class);
  private BlockStore blockStore;
  private PeerGroup peerGroup;  // May need to add listener as in MultiBitPeerGroup
  private BlockChain blockChain;
  private MultiBitPeerEventListener peerEventListener;

  private NetworkParameters MAINNET = NetworkParameters.fromID(NetworkParameters.ID_MAINNET);

  private boolean startedOk = false;

  @Override
  public void start() {

    CoreEvents.fireBitcoinNetworkChangedEvent(BitcoinNetworkSummary.newNetworkNotInitialised());

    try {

      // Check if there is a wallet - if there is no wallet the network will not start (there's nowhere to put the blockchain)
      if (!WalletManager.INSTANCE.getCurrentWalletData().isPresent()) {
        log.debug("Not starting bitcoin network service as there is currently no wallet.");
        return;
      }
      String walletRoot = WalletManager.INSTANCE.getCurrentWalletFilename().get().getParentFile().getAbsolutePath();
      String blockchainFilename = walletRoot + File.separator + InstallationManager.MBHD_PREFIX + InstallationManager.SPV_BLOCKCHAIN_SUFFIX;
      String checkpointsFilename = walletRoot + File.separator + InstallationManager.MBHD_PREFIX + InstallationManager.CHECKPOINTS_SUFFIX;

      // Load or create the blockStore..
      log.debug("Loading/ creating blockstore ...");
      blockStore = BlockStoreManager.createBlockStore(blockchainFilename, checkpointsFilename, null, false);
      log.debug("Blockstore is '{}'", blockStore);

      restartNetwork();

    } catch (Exception e) {
      log.error(e.getClass().getName() + " " + e.getMessage());
      CoreEvents.fireBitcoinNetworkChangedEvent(
        BitcoinNetworkSummary.newNetworkStartupFailed(
          CoreMessageKey.START_NETWORK_CONNECTION_ERROR,
          Optional.of(new Object[]{})
        ));
    }
  }

  /**
   * Restart the network, using the current wallet (specifically the blockstore)
   *
   * @throws BlockStoreException
   * @throws IOException
   */
  private void restartNetwork() throws BlockStoreException, IOException {

    requireSingleThreadExecutor();

    // Check if there is a network connection
    if (!isNetworkPresent()) {
      return;
    }

    log.debug("Creating blockchain ...");
    blockChain = new BlockChain(NETWORK_PARAMETERS, blockStore);
    if (WalletManager.INSTANCE.getCurrentWalletData().isPresent()) {
      blockChain.addWallet(WalletManager.INSTANCE.getCurrentWalletData().get().getWallet());
    }
    log.debug("Created blockchain '{}' with height '{}'", blockChain, blockChain.getBestChainHeight());

    log.debug("Creating peergroup ...");
    createNewPeerGroup();
    log.debug("Created peergroup '{}'", peerGroup);

    log.debug("Starting peergroup ...");
    peerGroup.start();
    log.debug("Started peergroup.");

    startedOk = true;
  }

  public boolean isStartedOk() {
    return startedOk;
  }

  @Override
  public void stopAndWait() {
    startedOk = false;
    stopPeerGroupAndCloseBlockstore();

    // Save the current wallet immediately
    if (WalletManager.INSTANCE.getCurrentWalletData().isPresent()) {
      WalletData walletData = WalletManager.INSTANCE.getCurrentWalletData().get();
      WalletId walletId = walletData.getWalletId();
      log.debug("Saving wallet with id '" + walletId + "'.");
      try {
        File currentWalletFile = WalletManager.INSTANCE.getCurrentWalletFilename().get();
        walletData.getWallet().saveToFile(currentWalletFile);
        log.debug("Wallet save completed ok. Wallet size is " + currentWalletFile.length() + " bytes.");

        BackupManager.INSTANCE.createRollingBackup(walletData);
        BackupManager.INSTANCE.createLocalAndCloudBackup(walletId);
      } catch (IOException ioe) {
        log.error("Could not write wallet and backups for wallet with id '" + walletId + "' successfully. The error was '" + ioe.getMessage() + "'");
      }
    }
  }

  public void recalculateFastCatchupAndFilter() {
    if (peerGroup != null) {
      peerGroup.recalculateFastCatchupAndFilter(PeerGroup.FilterRecalculateMode.FORCE_SEND);
    }
  }

  private void stopPeerGroupAndCloseBlockstore() {
    if (peerGroup != null) {
      log.debug("Stopping peerGroup service...");
      peerGroup.removeEventListener(peerEventListener);

      if (WalletManager.INSTANCE.getCurrentWalletData().isPresent()) {
        peerGroup.removeWallet(WalletManager.INSTANCE.getCurrentWalletData().get().getWallet());
      }

      peerGroup.stopAndWait();
      log.debug("Service peerGroup stopped");
    }

    // Shutdown any executor running a download
    if (getExecutorServiceOptional().isPresent()) {
      getExecutorService().shutdown();
    }

    // Remove the wallet from the blockChain
    if (WalletManager.INSTANCE.getCurrentWalletData().isPresent() && blockChain != null) {
      blockChain.removeWallet(WalletManager.INSTANCE.getCurrentWalletData().get().getWallet());
    }

    // Close the blockstore
    if (blockStore != null) {
      try {
        blockStore.close();
      } catch (BlockStoreException e) {
        log.error("Blockstore not closed successfully, error was '" + e.getClass().getName() + " " + e.getMessage() + "'");
      }
    }
  }

  /**
   * <p>Send bitcoin</p>
   * <p/>
   * <p>In the future will also need:</p>
   * <ul>
   * <li>the wallet to send from - when Trezor comes onstream</li>
   * <li>a CoinSelector - when HD subnodes are supported</li>
   * </ul>
   * <p>The result of the operation is sent to the CoreEventBus as a TransactionCreationEvent and, if the tx is created ok, a BitcoinSentEvent</p>
   *
   * @param destinationAddress The destination address to send to
   * @param amount             The amount to send (in satoshis)
   * @param changeAddress      The change address
   * @param feePerKB           The fee per Kb (in satoshis)
   * @param password           The wallet password
   */

  // TODO should be in executor
  public void send(String destinationAddress, BigInteger amount, String changeAddress, BigInteger feePerKB, CharSequence password) {

    if (!WalletManager.INSTANCE.getCurrentWalletData().isPresent()) {
      // Declare the transaction creation a failure - no wallet
      CoreEvents.fireTransactionCreationEvent(new TransactionCreationEvent(
        null,
        amount,
        BigInteger.ZERO,
        destinationAddress,
        changeAddress,
        false, "core_no_active_wallet",
        new String[]{""}
      ));

      // Prevent fall-through to success
      return;
    }

    log.debug("Just about to create send transaction");
    Wallet wallet = WalletManager.INSTANCE.getCurrentWalletData().get().getWallet();
    KeyParameter aesKey = wallet.getKeyCrypter().deriveKey(password);

    // Verify the destination address
    final Address destination;
    final Address change;
    try {
      destination = new Address(MAINNET, destinationAddress);
      change = new Address(MAINNET, changeAddress);
    } catch (NullPointerException | AddressFormatException e) {
      log.error(e.getMessage(), e);

      // Declare the transaction creation a failure
      CoreEvents.fireTransactionCreationEvent(new TransactionCreationEvent(
        null,
        amount,
        BigInteger.ZERO,
        destinationAddress,
        changeAddress,
        false,
        "core_the_error_was", // TODO Consider CoreMessageKey
        new String[]{e.getClass().getCanonicalName() + " " + e.getMessage()}));

      // Prevent fall-through to success
      return;
    }

    // Addresses must be OK to be here

    // Build the send request
    final Wallet.SendRequest sendRequest;
    sendRequest = Wallet.SendRequest.to(destination, amount);
    sendRequest.aesKey = aesKey;
    sendRequest.fee = BigInteger.ZERO;
    sendRequest.feePerKb = feePerKB;
    sendRequest.changeAddress = change;

    // Attempt to complete it

    try {

      // Complete it (works out fee and signs tx)
      wallet.completeTx(sendRequest);

      // Commit to the wallet
      wallet.commitTx(sendRequest.tx);

      // Fire a successful transaction creation event
      CoreEvents.fireTransactionCreationEvent(new TransactionCreationEvent(
        sendRequest.tx.getHashAsString(),
        amount,
        BigInteger.ZERO,
        destinationAddress,
        changeAddress,
        true,
        null,
        null
      ));

    } catch (KeyCrypterException | InsufficientMoneyException | VerificationException e) {

      log.error(e.getMessage(), e);

      String transactionId = sendRequest.tx != null ? sendRequest.tx.getHashAsString() : "?";

      // Fire a failed transaction creation event
      CoreEvents.fireTransactionCreationEvent(new TransactionCreationEvent(
        transactionId,
        amount,
        BigInteger.ZERO,
        destinationAddress,
        changeAddress,
        false,
        "core_the_error_was", // TODO Consider CoreMessageKey
        new String[]{e.getMessage()}));

      // We cannot proceed to broadcast
      return;
    }

    log.debug("Just about to broadcast transaction");
    try {
      // Ping the peers to check the bitcoin network connection
      if (!pingPeers()) {
        // Declare the send a failure
        CoreEvents.fireBitcoinSentEvent(new BitcoinSentEvent(
          amount,
          BigInteger.ZERO,
          destinationAddress,
          changeAddress,
          false,
          "core_could_not_connect_to_bitcoin_network",
          new String[]{"All pings failed"} // TODO Is this meaningful?
        ));

        // Prevent a fall-through to success
        return;
      }

      // Broadcast
      peerGroup.broadcastTransaction(sendRequest.tx);

      log.debug("Just broadcast transaction '" + Utils.bytesToHexString(sendRequest.tx.bitcoinSerialize()) + "'");

      // Declare the send a success
      CoreEvents.fireBitcoinSentEvent(new BitcoinSentEvent(
        amount,
        BigInteger.ZERO,
        destinationAddress,
        changeAddress,
        true,
        "core_bitcoin_sent_ok",
        null
      ));
    } catch (VerificationException e) {
      log.error(e.getMessage(), e);

      // Declare the send a failure
      CoreEvents.fireBitcoinSentEvent(new BitcoinSentEvent(
        amount,
        BigInteger.ZERO,
        destinationAddress,
        changeAddress,
        false,
        "core_the_error_was",
        new String[]{e.getMessage()}
      ));

      // Prevent a fall-through to success
      return;

    }

    log.debug("Send coins has completed");
  }

  /**
   * <p>Download the block chain</p>
   */
  public void downloadBlockChain() {

    getExecutorService().submit(new Runnable() {
      @Override
      public void run() {

        Preconditions.checkNotNull(peerGroup, "'peerGroup' must be present");

        log.debug("Downloading blockchain...");

        // This method blocks until completed but fires events along the way
        peerGroup.downloadBlockChain();

        log.debug("Blockchain downloaded.");

        CoreEvents.fireBitcoinNetworkChangedEvent(BitcoinNetworkSummary.newNetworkReady(peerGroup.numConnectedPeers()));

      }
    });
  }

  /**
   * Sync the current wallet from the date specified.
   * The blockstore is deleted and created anew, checkpointed and then the blockchain is downloaded.
   */
  // TODO once working should be in executor
  public void replayWallet(DateTime dateToReplayFrom) throws IOException, BlockStoreException {

    Preconditions.checkNotNull(dateToReplayFrom);
    Preconditions.checkState(WalletManager.INSTANCE.getCurrentWalletData().isPresent());

    log.info("Starting replay of wallet with id '" + WalletManager.INSTANCE.getCurrentWalletData().get().getWalletId()
      + "' from date " + dateToReplayFrom);

    // TODO the current best height should be remembered and used to generate percentage complete as then if the peer is replaced the percentage increases monotomically

    // Stop the peer group if it is running
    stopPeerGroupAndCloseBlockstore();

    // Close the blockstore and recreate a new one, checkpointed to the replay date
    String walletRoot = WalletManager.INSTANCE.getCurrentWalletFilename().get().getParentFile().getAbsolutePath();
    String blockchainFilename = walletRoot + File.separator + InstallationManager.MBHD_PREFIX + InstallationManager.SPV_BLOCKCHAIN_SUFFIX;
    String checkpointsFilename = walletRoot + File.separator + InstallationManager.MBHD_PREFIX + InstallationManager.CHECKPOINTS_SUFFIX;

    log.debug("Recreating blockstore with checkpoint date of " + dateToReplayFrom + " ...");
    blockStore = BlockStoreManager.createBlockStore(blockchainFilename, checkpointsFilename, dateToReplayFrom.toDate(), true);
    log.debug("Blockstore is '{}'", blockStore);

    restartNetwork();

    downloadBlockChain();
    log.debug("Blockchain download started.");
  }

  /**
   * <p>Create a new peer group</p>
   */
  private void createNewPeerGroup() {

    peerGroup = new PeerGroup(NETWORK_PARAMETERS, blockChain);
    peerGroup.setFastCatchupTimeSecs(0); // genesis block
    peerGroup.setUserAgent(InstallationManager.MBHD_APP_NAME, Configurations.APP_VERSION);
    peerGroup.setMaxConnections(MAXIMUM_NUMBER_OF_PEERS);

    peerGroup.addPeerDiscovery(new DnsDiscovery(NETWORK_PARAMETERS));

    peerEventListener = new MultiBitPeerEventListener();
    peerGroup.addEventListener(peerEventListener);

    if (WalletManager.INSTANCE.getCurrentWalletData().isPresent()) {
      peerGroup.addWallet(WalletManager.INSTANCE.getCurrentWalletData().get().getWallet());
    }
  }

  /**
   * Get the next available change address
   * TODO- this should be worked out deterministically but just use the first address on the current wallet for now
   *
   * @return changeAddress The next change address as a string
   */
  public String getNextChangeAddress() {

    Preconditions.checkState(WalletManager.INSTANCE.getCurrentWalletData().isPresent());
    Preconditions.checkNotNull(WalletManager.INSTANCE.getCurrentWalletData().get().getWallet());
    Preconditions.checkState(WalletManager.INSTANCE.getCurrentWalletData().get().getWallet().getKeychainSize() > 0);

    Wallet wallet = WalletManager.INSTANCE.getCurrentWalletData().get().getWallet();
    ECKey firstKey = wallet.getKeys().get(0);
    return firstKey.toAddress(MAINNET).toString();
  }

  /**
   * Ping all connected peers to see if there is an active network connection
   *
   * @return true is one or more peers respond to the ping
   */
  public boolean pingPeers() {

    List<Peer> connectedPeers = peerGroup.getConnectedPeers();
    boolean atLeastOnePingWorked = false;
    if (connectedPeers != null) {
      for (Peer peer : connectedPeers) {

        log.debug("Ping: {}", peer.getAddress().toString());

        try {
          ListenableFuture<Long> result = peer.ping();
          result.get(4, TimeUnit.SECONDS);
          atLeastOnePingWorked = true;
          break;
        } catch (ProtocolException | InterruptedException | ExecutionException | TimeoutException e) {
          log.warn("Peer '" + peer.getAddress().toString() + "' failed ping test. Message was " + e.getMessage());
        }
      }
    }

    return atLeastOnePingWorked;
  }

  private boolean isNetworkPresent() {

    final String dnsSeed = MainNetParams.get().getDnsSeeds()[0];

    // Attempt to lookup the address
    try {
      return InetAddress.getAllByName(dnsSeed) != null;
    } catch (UnknownHostException e) {
      return false;
    }
  }
}