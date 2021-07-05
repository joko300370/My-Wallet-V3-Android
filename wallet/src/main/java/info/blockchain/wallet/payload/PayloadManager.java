package info.blockchain.wallet.payload;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.crypto.MnemonicException.MnemonicChecksumException;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import org.bitcoinj.crypto.MnemonicException.MnemonicWordException;
import org.bitcoinj.params.MainNetParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.blockchain.api.ApiException;
import com.blockchain.api.NonCustodialBitcoinService;
import com.blockchain.api.bitcoin.data.BalanceDto;
import info.blockchain.balance.CryptoValue;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.bip44.HDAccount;
import info.blockchain.wallet.exceptions.AccountLockedException;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.EncryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.NoSuchAddressException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.keys.MasterKey;
import info.blockchain.wallet.keys.SigningKey;
import info.blockchain.wallet.keys.SigningKeyImpl;
import info.blockchain.wallet.multiaddress.MultiAddressFactory;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.pairing.Pairing;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.Derivation;
import info.blockchain.wallet.payload.data.WalletBody;
import info.blockchain.wallet.payload.data.ImportedAddress;
import info.blockchain.wallet.payload.data.Wallet;
import info.blockchain.wallet.payload.data.WalletBase;
import info.blockchain.wallet.payload.data.WalletExtensionsKt;
import info.blockchain.wallet.payload.data.WalletWrapper;
import info.blockchain.wallet.payload.data.XPub;
import info.blockchain.wallet.payload.data.XPubs;
import info.blockchain.wallet.payload.data.XPubxKt;
import info.blockchain.wallet.payload.model.Balance;
import info.blockchain.wallet.payload.model.BalanceKt;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.Tools;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

@SuppressWarnings("ALL")
public class PayloadManager {

    private static Logger log = LoggerFactory.getLogger(PayloadManager.class);

    private boolean isV4Enabled = false;
    private WalletBase walletBase;
    private String password;

    private final WalletApi walletApi;
    private final NonCustodialBitcoinService bitcoinApi;

    // Bitcoin
    private final MultiAddressFactory multiAddressFactory;
    private final BalanceManagerBtc balanceManagerBtc;
    // Bitcoin Cash
    private final BalanceManagerBch balanceManagerBch;

    public PayloadManager(
        WalletApi walletApi,
        NonCustodialBitcoinService bitcoinApi,
        MultiAddressFactory multiAddressFactory,
        BalanceManagerBtc balanceManagerBtc,
        BalanceManagerBch balanceManagerBch
    ) {
        this.walletApi = walletApi;
        this.bitcoinApi = bitcoinApi;
        // Bitcoin
        this.multiAddressFactory = multiAddressFactory;
        this.balanceManagerBtc = balanceManagerBtc;
        // Bitcoin Cash
        this.balanceManagerBch  = balanceManagerBch;
    }

    @Nullable
    public Wallet getPayload() {
        return walletBase != null ? walletBase.getWalletBody() : null;
    }

    public String getPayloadChecksum() {
        return walletBase.getPayloadChecksum();
    }

    public String getTempPassword() {
        return password;
    }

    public void setTempPassword(String password) {
        this.password = password;
    }

    ///////////////////////////////////////////////////////////////////////////
    // WALLET INITIALIZATION, CREATION, RECOVERY, SYNCING
    ///////////////////////////////////////////////////////////////////////////

    /**
     * NB! When called from Android - First apply PRNGFixes
     * Creates a new Blockchain wallet and saves it to the server.
     *
     * @param email Used to send GUID link to user
     */
    public Wallet create(
        @Nonnull String defaultAccountName,
        @Nonnull String email,
        @Nonnull String password,
        boolean isPayloadV4Enabled
    ) throws Exception {
        this.isV4Enabled = isPayloadV4Enabled;
        this.password = password;
        walletBase = new WalletBase();
        walletBase.setWalletBody(
            new Wallet(
                defaultAccountName,
                isPayloadV4Enabled
            )
        );

        saveNewWallet(email);

        updateAllBalances();

        return getPayload();
    }

    /**
     * Creates a new Blockchain wallet based on provided mnemonic and saves it to the server.
     *
     * @param mnemonic 12 word recovery phrase - space separated
     * @param email    Used to send GUID link to user
     */
    public Wallet recoverFromMnemonic(
        @Nonnull String mnemonic,
        @Nonnull String defaultAccountName,
        @Nonnull String email,
        @Nonnull String password,
        boolean isPayloadV4Enabled
    ) throws Exception {
        this.isV4Enabled = isPayloadV4Enabled;
        this.password = password;
        walletBase = new WalletBase();

        Wallet wallet = new Wallet();
        WalletBody walletBody = WalletBody.recoverFromMnemonic(
            mnemonic,
            defaultAccountName,
            bitcoinApi,
            isPayloadV4Enabled
        );
        wallet.setWalletBody(walletBody);

        walletBase.setWalletBody(wallet);

        saveNewWallet(email);

        updateAllBalances();

        return getPayload();
    }

    public boolean isWalletBackedUp() {
        Wallet payload = getPayload();
        if (payload != null) {
            List<WalletBody> wallets = payload.getWalletBodies();
            if (!wallets.isEmpty()) {
                return wallets.get(0).isMnemonicVerified();
            }
        }
        return false;
    }

    /**
     * Upgrades a V2 wallet to a V3 HD wallet and saves it to the server
     * NB! When called from Android - First apply PRNGFixes
     */
    public void upgradeV2PayloadToV3(
        @Nullable String secondPassword,
        @Nonnull String defaultAccountName
    ) throws Exception {
        try {
            getPayload().upgradeV2PayloadToV3(secondPassword, defaultAccountName);
            boolean success = save();
            if (!success) {
                throw new Exception("Save failed");
            }
        } catch (Throwable t) {
            // Revert on fail
            getPayload().setWalletBodies(null);
            throw t;
        }
        updateAllBalances();
    }

    /**
     * Upgrades a V3 wallet to V4 wallet format and saves it to the server
     */
    public void upgradeV3PayloadToV4(@Nullable String secondPassword) throws Exception {
        final Wallet payload = getPayload();

        if (payload.isDoubleEncryption()) {
            payload.decryptHDWallet(secondPassword);
        }

        // Prepare backup in case of failure
        final int wrapperVersionBackup = payload.getWrapperVersion();
        final List<List<Account>> hdWalletsAccountsBackup = new ArrayList<>();

        for (WalletBody walletBody : payload.getWalletBodies()) {
            hdWalletsAccountsBackup.add(walletBody.getAccounts());
        }

        try {
            for (WalletBody walletBody : payload.getWalletBodies()) {
                List<Account> upgraded = walletBody.upgradeAccountsToV4();
                encryptUpgradedAccounts(payload, upgraded, secondPassword);
                walletBody.setAccounts(upgraded);
            }
            payload.setWrapperVersion(WalletWrapper.V4);

            if (!save()) {
                throw new Exception("Save failed");
            }
        } catch (Throwable t) {
            // Revert on fail
            for (int i = 0; i < hdWalletsAccountsBackup.size(); i++) {
                WalletBody walletBody = getPayload().getWalletBodies().get(i);
                walletBody.setWrapperVersion(wrapperVersionBackup);
                walletBody.setAccounts(hdWalletsAccountsBackup.get(i));
            }
            getPayload().setWrapperVersion(wrapperVersionBackup);
            throw t;
        }
        updateAllBalances();
    }

    private void encryptUpgradedAccounts(
        Wallet payload,
        List<Account> upgraded,
        @Nonnull String secondPassword
    ) throws UnsupportedEncodingException, EncryptionException {
        for (Account a : upgraded) {
            payload.encryptAccount(a, secondPassword);
        }
    }

    /**
     * Initializes a wallet from provided credentials.
     * Calls balance api to show wallet balances on wallet load.
     *
     * @throws InvalidCredentialsException GUID might be incorrect
     * @throws AccountLockedException      Account has been locked, contact support
     * @throws ServerConnectionException   Unknown server error
     * @throws DecryptionException         Password not able to decrypt payload
     * @throws InvalidCipherTextException  Decryption issue
     * @throws UnsupportedVersionException Payload version newer than current supported
     * @throws MnemonicLengthException     Initializing HD issue
     * @throws MnemonicWordException       Initializing HD issue
     * @throws MnemonicChecksumException   Initializing HD issue
     * @throws DecoderException            Decryption issue
     */
    public void initializeAndDecrypt(
        @Nonnull String sharedKey,
        @Nonnull String guid,
        @Nonnull String password,
        boolean isV4Enabled
    ) throws IOException,
        InvalidCredentialsException,
        AccountLockedException,
        ServerConnectionException,
        DecryptionException,
        InvalidCipherTextException,
        UnsupportedVersionException,
        MnemonicLengthException,
        MnemonicWordException,
        MnemonicChecksumException,
        DecoderException,
        HDWalletException {

        this.isV4Enabled = isV4Enabled;
        this.password = password;

        Call<ResponseBody> call = walletApi.fetchWalletData(guid, sharedKey);
        Response<ResponseBody> exe = call.execute();

        if (exe.isSuccessful()) {
            final String response = exe.body().string();
            final WalletBase base = WalletBase.fromJson(response);

            base.decryptPayload(this.password);
            walletBase = base;
        } else {
            log.warn("Fetching wallet data failed with provided credentials");
            String errorMessage = exe.errorBody().string();
            log.warn("", errorMessage);
            if (errorMessage != null && errorMessage.contains("Unknown Wallet Identifier")) {
                throw new InvalidCredentialsException();
            }
            else if (errorMessage != null && errorMessage.contains("locked")) {
                throw new AccountLockedException(errorMessage);
            }
            else {
                throw new ServerConnectionException(errorMessage);
            }
        }
        updateAllBalances();
    }

    public void initializeAndDecryptFromQR(
        @Nonnull String qrData,
        boolean isV4Enabled
    ) throws Exception {
        MainNetParams netParams = MainNetParams.get();
        Pair qrComponents = Pairing.getQRComponentsFromRawString(qrData);
        Call<ResponseBody> call = walletApi.fetchPairingEncryptionPasswordCall((String) qrComponents.getLeft());

        Response<ResponseBody> exe = call.execute();

        if (exe.isSuccessful()) {
            String encryptionPassword = exe.body().string();
            String encryptionPairingCode = (String) qrComponents.getRight();
            String guid = (String) qrComponents.getLeft();

            String[] sharedKeyAndPassword = Pairing.getSharedKeyAndPassword(encryptionPairingCode, encryptionPassword);
            String sharedKey = sharedKeyAndPassword[0];
            String hexEncodedPassword = sharedKeyAndPassword[1];
            String password = new String(Hex.decode(hexEncodedPassword), "UTF-8");

            initializeAndDecrypt(
                sharedKey,
                guid,
                password,
                isV4Enabled
            );
        } else {
            log.error("", exe.code() + " - " + exe.errorBody().string());
            throw new ServerConnectionException(exe.code() + " - " + exe.errorBody().string());
        }

        updateAllBalances();
    }

    /**
     * Initializes a wallet from a Payload string from manual pairing. Should decode both V3 and V1 wallets successfully.
     *
     * @param networkParameters The parameters for the network - TestNet or MainNet
     * @param payload           The Payload in String format that you wish to decrypt and initialise
     * @param password          The password for the payload
     * @throws HDWalletException   Thrown for a variety of reasons, wraps actual exception and is fatal
     * @throws DecryptionException Thrown if the password is incorrect
     */
    public void initializeAndDecryptFromPayload(
        String payload,
        String password
    ) throws HDWalletException, DecryptionException {
        try {
            walletBase = WalletBase.fromJson(payload);
            walletBase.decryptPayload(password);
            setTempPassword(password);

            updateAllBalances();
        } catch (DecryptionException decryptionException) {
            log.warn("", decryptionException);
            throw decryptionException;
        } catch (Exception e) {
            log.error("", e);
            throw new HDWalletException(e);
        }
    }

    private void validateSave() throws HDWalletException {
        if (walletBase == null) {
            throw new HDWalletException("Save aborted - HDWallet not initialized.");
        }
        else if (!getPayload().isEncryptionConsistent()) {
            throw new HDWalletException("Save aborted - Payload corrupted. Key encryption not consistent.");
        }
        else if (BlockchainFramework.getDevice() == null) {
            throw new HDWalletException("Save aborted - Device name not specified in FrameWork.");
        }
    }

    private void saveNewWallet(String email) throws Exception {
        validateSave();
        // Encrypt and wrap payload
        ObjectMapper mapper = WalletWrapper.getMapperForVersion(getPayload().getWrapperVersion());
        Pair pair = walletBase.encryptAndWrapPayload(password);
        WalletWrapper payloadWrapper = (WalletWrapper) pair.getRight();
        String newPayloadChecksum = (String) pair.getLeft();

        // Save to server
        Call<ResponseBody> call = walletApi.insertWallet(
            getPayload().getGuid(),
            getPayload().getSharedKey(),
            null,
            payloadWrapper.toJson(mapper),
            newPayloadChecksum,
            email,
            BlockchainFramework.getDevice()
        );

        Response<ResponseBody> exe = call.execute();
        if (exe.isSuccessful()) {
            //set new checksum
            walletBase.setPayloadChecksum(newPayloadChecksum);
        } else {
            log.error("", exe.code() + " - " + exe.errorBody().string());
            throw new ServerConnectionException(exe.code() + " - " + exe.errorBody().string());
        }
    }

    /**
     * Saves wallet to server and forces the upload of the user's addresses to allow notifications
     * to work correctly.
     *
     * @return True if save successful
     */
    public boolean saveAndSyncPubKeys() throws
        HDWalletException,
        EncryptionException,
        NoSuchAlgorithmException,
        IOException {
        return save(true);
    }

    /**
     * Saves wallet to server.
     *
     * @return True if save successful
     */
    public boolean save() throws
        HDWalletException,
        EncryptionException,
        NoSuchAlgorithmException,
        IOException {
        return save(false);
    }

    private synchronized boolean save(
        boolean forcePubKeySync
    ) throws HDWalletException,
        NoSuchAlgorithmException,
        EncryptionException,
        IOException {

        validateSave();

        // Encrypt and wrap payload
        int payloadVersion = getPayload().getWrapperVersion();
        ObjectMapper mapper = WalletWrapper.getMapperForVersion(payloadVersion);
        Pair pair = walletBase.encryptAndWrapPayload(password);
        WalletWrapper payloadWrapper = (WalletWrapper) pair.getRight();
        String newPayloadChecksum = (String) pair.getLeft();
        String oldPayloadChecksum = walletBase.getPayloadChecksum();

        // Save to server
        List<String> syncAddresses;
        if (walletBase.isSyncPubkeys() || forcePubKeySync) {
            syncAddresses = makePubKeySyncList(getPayload().getWalletBody(), payloadVersion);
        } else {
            syncAddresses = new ArrayList<>();
        }

        Call<ResponseBody> call = walletApi.updateWallet(
            getPayload().getGuid(),
            getPayload().getSharedKey(),
            syncAddresses,
            payloadWrapper.toJson(mapper),
            newPayloadChecksum,
            oldPayloadChecksum,
            BlockchainFramework.getDevice()
        );

        Response<ResponseBody> exe = call.execute();
        if (exe.isSuccessful()) {
            //set new checksum
            walletBase.setPayloadChecksum(newPayloadChecksum);
            return true;
        }
        else {
            log.error("Save unsuccessful: " + exe.errorBody().string());
            return false;
        }
    }

    private List<String> makePubKeySyncList(WalletBody walletBody, int payloadVersion)
        throws HDWalletException {

        final List<String> syncAddresses = new ArrayList<>();

        // This matches what iOS is doing, but it seems to be massive overkill for mobile
        // devices. I'm also filtering out archived accounts here because I don't see the point
        // in sending them.
        int derivationPurpose = (payloadVersion == WalletWrapper.V4)
            ? Derivation.SEGWIT_BECH32_PURPOSE
            : Derivation.LEGACY_PURPOSE;

        for (Account account : walletBody.getAccounts()) {
            if (!account.isArchived()) {
                HDAccount hdAccount = walletBody.getHDAccountFromAccountBody(account).get(0);
                int nextIndex = getNextReceiveAddressIndexBtc(account);

                syncAddresses.addAll(
                    Tools.getReceiveAddressList(
                        hdAccount,
                        nextIndex,
                        nextIndex + 20,
                        derivationPurpose
                    )
                );
            }
        }

        syncAddresses.addAll(
            Tools.filterImportedAddress(
                ImportedAddress.NORMAL_ADDRESS,
                getPayload().getImportedAddressList()
            )
        );

        return syncAddresses;
    }

    ///////////////////////////////////////////////////////////////////////////
    // ACCOUNT AND IMPORTED HDADDRESS CREATION
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Adds a new account to hd wallet and saves to server.
     * Reverts on save failure.
     */
    public Account addAccount(
        String label,
        @Nullable String secondPassword
    ) throws Exception {
        int version = getPayload().getWrapperVersion();
        Account accountBody = getPayload().addAccount(
            label,
            secondPassword,
            version
        );

        boolean success = save();

        if (!success) {
            //Revert on save fail
            getPayload().getWalletBody().getAccounts().remove(accountBody);
            throw new Exception("Failed to save added account.");
        }

        updateAllBalances();

        return accountBody;
    }

    /**
     * Inserts a {@link ImportedAddress} into the user's {@link Wallet} and then syncs the wallet with
     * the server. Will remove/revert the ImportedAddress if the sync was unsuccessful.
     *
     * @param importedAddress The {@link ImportedAddress} to be added
     * @throws Exception Possible if saving the Wallet fails
     */
    public void addImportedAddress(ImportedAddress importedAddress) throws Exception {
        List<ImportedAddress> backup = new ArrayList<>(getPayload().getImportedAddressList());
        getPayload().getImportedAddressList().add(importedAddress);

        if (!save()) {
            // Revert on sync fail
            getPayload().setImportedAddressList(backup);
            throw new Exception("Failed to save added Imported Address.");
        }
        updateAllBalances();
    }

    /**
     * Replaces an old {@link ImportedAddress} with a newer one if found and then syncs the wallet
     * with the server. Will remove/revert the ImportedAddress if the sync was unsuccessful.
     *
     * @param importedAddress The {@link ImportedAddress} to be added
     * @throws Exception            Possible if saving the Wallet fails
     * @throws NullPointerException Thrown if the address to be updated is not found
     */
    public void updateImportedAddress(ImportedAddress importedAddress) throws Exception {
        boolean found = false;

        final List<ImportedAddress> backup = new ArrayList<>(getPayload().getImportedAddressList());

        final List<ImportedAddress> importedList = getPayload().getImportedAddressList();
        for (int i = 0; i < importedList.size(); i++) {
            final ImportedAddress address = importedList.get(i);
            if (address.getAddress().equals(importedAddress.getAddress())) {
                // Replace object with updated version
                importedList.set(i, importedAddress);
                found = true;
                break;
            }
        }

        if (!found) {
            throw new NullPointerException("Imported address not found");
        }

        if (!save()) {
            // Revert on sync fail
            getPayload().setImportedAddressList(backup);
            throw new Exception("Failed to save added imported Address.");
        }
        updateAllBalances();
    }

    /**
     * Sets private key to existing matching imported address. If no match is found the key will be added
     * to the wallet non the less.
     *
     * @param key            ECKey for existing imported address
     * @param secondPassword Double encryption password if applicable.
     */
    public ImportedAddress setKeyForImportedAddress(SigningKey key, @Nullable String secondPassword) throws Exception {
        ImportedAddress matchingImportedAddress;
        try {
            matchingImportedAddress = getPayload().setKeyForImportedAddress(key, secondPassword);
        } catch (NoSuchAddressException e) {
            e.printStackTrace();
            //If no match found, save as new
            return addImportedAddressFromKey(key, secondPassword);
        }

        boolean success = save();

        if (!success) {
            //Revert on save fail
            matchingImportedAddress.setPrivateKey(null);
        }

        return matchingImportedAddress;

    }

    public ImportedAddress addImportedAddressFromKey(
        SigningKey key,
        @Nullable String secondPassword
    ) throws Exception {
        ImportedAddress newlyAdded = getPayload().addImportedAddressFromKey(key, secondPassword);

        boolean success = save();

        if (!success) {
            //Revert on save fail
            newlyAdded.setPrivateKey(null);
        }
        updateAllBalances();
        return newlyAdded;
    }

    ///////////////////////////////////////////////////////////////////////////
    // SHORTCUT METHODS
    ///////////////////////////////////////////////////////////////////////////

    public boolean validateSecondPassword(@Nullable String secondPassword) {
        try {
            getPayload().validateSecondPassword(secondPassword);
            return true;
        } catch (Exception e) {
            log.warn("", e);
            e.printStackTrace();
            return false;
        }
    }

    public boolean isV3UpgradeRequired() {
        return getPayload() != null && !getPayload().isUpgradedToV3();
    }

    public boolean isV4UpgradeRequired() {
        int payloadVersion = getPayload().getWrapperVersion();
        return payloadVersion < WalletWrapper.V4 && isV4Enabled;
    }

    public SigningKey getAddressSigningKey(
        @Nonnull ImportedAddress importedAddress,
        @Nullable String secondPassword
    ) throws DecryptionException, UnsupportedEncodingException, InvalidCipherTextException {

        getPayload().validateSecondPassword(secondPassword);

        String decryptedPrivateKey = importedAddress.getPrivateKey();

        if (secondPassword != null) {
            decryptedPrivateKey = DoubleEncryptionFactory
                .decrypt(
                    importedAddress.getPrivateKey(),
                    getPayload().getSharedKey(),
                    secondPassword,
                    getPayload().getOptions().getPbkdf2Iterations());
        }

        return new SigningKeyImpl(
            Tools.getECKeyFromKeyAndAddress(
                decryptedPrivateKey, importedAddress.getAddress())
        );
    }

    private Balance accountTotalBalance(HashMap<String, Balance> balanceHashMap, String legacyXpub, String segwitXpub) {
        Balance totalBalance = null;
        if (balanceHashMap.containsKey(legacyXpub)) {
            totalBalance = balanceHashMap.get(legacyXpub);
        }
        if (balanceHashMap.containsKey(segwitXpub)) {
            if (totalBalance != null) {
                totalBalance.setFinalBalance(totalBalance.getFinalBalance().add(balanceHashMap.get(segwitXpub).getFinalBalance()));
                totalBalance.setTotalReceived(totalBalance.getTotalReceived().add(balanceHashMap.get(segwitXpub).getTotalReceived()));
                totalBalance.setTxCount(totalBalance.getTxCount() + balanceHashMap.get(segwitXpub).getTxCount());
            }
            else {
                totalBalance = balanceHashMap.get(segwitXpub);
            }
        }
        return totalBalance;
    }

    /**
     * Returns a {@link Map} of {@link Balance} objects keyed to their respective Bitcoin
     * Cash addresses.
     *
     * @param addresses A List of Bitcoin Cash addresses as Strings
     * @return A {@link LinkedHashMap} where they key is the address String, and the value is a
     * {@link Balance} object
     * @throws IOException  Thrown if there are network issues
     * @throws ApiException Thrown if the call isn't successful
     */
    public Map<String, Balance> getBalanceOfBchAccounts(List<XPubs> xpubs) throws
        IOException,
        ApiException {
        final Response<Map<String, BalanceDto>> response = balanceManagerBch.getBalanceOfAddresses(xpubs)
            .execute();
        if (response.isSuccessful()) {
            final Map<String, BalanceDto> result = response.body();
            final Map<String, Balance> balanceHashMap = BalanceKt.toBalanceMap(result);
            return balanceHashMap;
        } else {
            throw new ApiException(response.code() + ": " + response.errorBody().string());
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // MULTIADDRESS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Gets BTC transaction list for all wallet accounts/addresses
     *
     * @param limit  Amount of transactions per page
     * @param offset Page offset
     * @return List of tx summaries for all wallet transactions
     */
    public List<TransactionSummary> getAllTransactions(int limit, int offset) throws
        IOException,
        ApiException {

        return getAccountTransactions(null, limit, offset);
    }

    /**
     * Updates internal balance and transaction list for imported BTC addresses
     *
     * @param limit  Amount of transactions per page
     * @param offset Page offset
     * @return Consolidated list of tx summaries for specified imported transactions
     */
    public List<TransactionSummary> getImportedAddressesTransactions(int limit, int offset)
        throws IOException, ApiException {

        Wallet wallet = getPayload();
        List<XPubs> xpubs = WalletExtensionsKt.activeXpubs(wallet);
        List<String> allImported = WalletExtensionsKt.nonArchivedImportedAddressStrings(wallet);

      return multiAddressFactory.getAccountTransactions(
            xpubs,
            allImported,
            null,
            limit,
            offset,
            0
        );
    }

    public MasterKey masterKey() throws HDWalletException {
        try {
            if (getPayload().isDoubleEncryption() && getPayload().getWalletBody().getMasterKey() == null) {
                throw new HDWalletException("Wallet private key unavailable. First decrypt with second password.");
            }
            return getPayload().getWalletBody().getMasterKey();
        } catch (HDWalletException e) {
            throw new HDWalletException("Wallet private key unavailable. First decrypt with second password.");
        }
    }

    /**
     * Gets BTC transaction list for an {@link Account}.
     *
     * @param xpub   The xPub to get transactions from
     * @param limit  Amount of transactions per page
     * @param offset Page offset
     * @return List of BTC tx summaries for specified xpubs transactions
     */
    public List<TransactionSummary> getAccountTransactions(String xpub, int limit, int offset)
        throws IOException, ApiException {

        List<XPubs> activeXpubs = getPayload().getWalletBody().getActiveXpubs();
        List<String> activeImported = getPayload().getImportedAddressStringList(ImportedAddress.NORMAL_ADDRESS);

        return multiAddressFactory.getAccountTransactions(
            activeXpubs,
            null,
            XPubxKt.allAddresses(activeXpubs),
            limit,
            offset,
            0
        );
    }

    /**
     * Calculates if an address belongs to any xpubs in wallet. Accepts both BTC and BCH addresses.
     * Make sure multi address is up to date before executing this method.
     *
     * @param address Either a BTC or BCH address
     * @return A boolean, true if the address belongs to an xPub
     */
    public boolean isOwnHDAddress(String address) {
        return multiAddressFactory.isOwnHDAddress(address);
    }

    /**
     * Converts any Bitcoin address to a label.
     *
     * @param address Accepts account receive or change chain address, as well as imported address.
     * @return Account or imported address label
     */
    public String getLabelFromAddress(String address) {
        String label;
        String xpub = multiAddressFactory.getXpubFromAddress(address);

        if (xpub != null) {
            label = getPayload().getWalletBody().getLabelFromXpub(xpub);
        } else {
            label = getPayload().getLabelFromImportedAddress(address);
        }

        if (label == null || label.isEmpty()) {
            label = address;
        }

        return label;
    }

    /**
     * Returns an xPub from an address if the address belongs to this wallet.
     *
     * @param address The address you want to query
     * @return An xPub as a String
     */
    @Nullable
    public String getXpubFromAddress(String address) {
        return multiAddressFactory.getXpubFromAddress(address);
    }

    /**
     * Gets next BTC receive address. Excludes reserved BTC addresses.
     *
     * @param account The account from which to derive an address
     * @return A BTC address
     */
    public String getNextReceiveAddress(Account account) throws HDWalletException {
        String derivationType = derivationTypeFromXPub(account.getXpubs().getDefault());
        int nextIndex = getNextReceiveAddressIndexBtc(account);
        return getReceiveAddress(account, nextIndex, derivationType);
    }

    /**
     * Allows you to generate a BTC receive address at an arbitrary number of positions on the chain
     * from the next valid unused address. For example, the passing 5 as the position will generate
     * an address which correlates with the next available address + 5 positions.
     *
     * @param account  The {@link Account} you wish to generate an address from
     * @param position Represents how many positions on the chain beyond what is already used that
     *                 you wish to generate
     * @return A Bitcoin address
     */
    @Nullable
    public String getReceiveAddressAtPosition(Account account, int position) {
        String derivationType = derivationTypeFromXPub(account.getXpubs().getDefault());
        int nextIndex = getNextReceiveAddressIndexBtc(account);
        return getReceiveAddressAtArbitraryPosition(account, nextIndex + position, derivationType);
    }

    private String derivationTypeFromXPub(XPub xpub) {
        switch (xpub.getDerivation()) {
            case LEGACY: return Derivation.LEGACY_TYPE;
            case SEGWIT: return Derivation.SEGWIT_BECH32_TYPE;
        }
        throw new IllegalStateException("Unknown derivation type");
    }

    /**
     * Returns the position on the receive chain of the next available receive address.
     *
     * @param account The {@link Account} you wish to generate an address from
     * @return The position of the next available receive address
     */
    public int getPositionOfNextReceiveAddress(Account account) {
        return getNextReceiveAddressIndexBtc(account);
    }

    /**
     * Allows you to generate a BTC or BCH address from any given point on the receive chain.
     *
     * @param account  The {@link Account} you wish to generate an address from
     * @param position What position on the chain the address you wish to create is
     * @return A Bitcoin or Bitcoin Cash address
     */
    @Nullable
    public String getReceiveAddressAtArbitraryPosition(Account account, int position, String derivationType) {
        try {
            return getReceiveAddress(account, position, derivationType);
        } catch (HDWalletException e) {
            return null;
        }
    }

    private int getNextReceiveAddressIndexBtc(Account account) {
        return multiAddressFactory.getNextReceiveAddressIndex(
            account.getXpubs().getDefault().getAddress(),
            account.getAddressLabels()
        );
    }

    private int getNextChangeAddressIndexBtc(Account account, String derivation) {
        return multiAddressFactory.getNextChangeAddressIndex(account.xpubForDerivation(derivation));
    }

    private String getReceiveAddress(
        Account account,
        int position,
        String derivationType
    ) throws HDWalletException {
        HDAccount hdAccount = getPayload().getWalletBody()
            .getHDAccountFromAccountBody(account)
            .get(derivationType == Derivation.LEGACY_TYPE ? 0 : 1);

        return hdAccount.getReceive()
            .getAddressAt(
                position,
                derivationType == Derivation.LEGACY_TYPE ? Derivation.LEGACY_PURPOSE : Derivation.SEGWIT_BECH32_PURPOSE
            ).getFormattedAddress();
    }

    private String getChangeAddress(Account account, int position, String derivationType) throws HDWalletException {
        HDAccount hdAccount = getPayload()
            .getWalletBody()
            .getHDAccountFromAccountBody(account)
            .get(derivationType == Derivation.LEGACY_TYPE ? 0 : 1);

        return hdAccount.getChange()
            .getAddressAt(
                position,
                derivationType == Derivation.LEGACY_TYPE ? Derivation.LEGACY_PURPOSE : Derivation.SEGWIT_BECH32_PURPOSE
            ).getFormattedAddress();
    }

    /**
     * Gets next BTC change address in the chain.
     *
     * @param account The {@link Account} from which you wish to derive a change address
     * @return A Bitcoin change address
     */
    public String getNextChangeAddress(Account account) throws HDWalletException {
        String derivationType = derivationTypeFromXPub(account.getXpubs().getDefault());
        int nextIndex = getNextChangeAddressIndexBtc(account, derivationType);
        return getChangeAddress(account, nextIndex, derivationType);
    }

    public void incrementNextReceiveAddress(Account account) {
        multiAddressFactory.incrementNextReceiveAddress(
            account.getXpubs().getDefault(),
            account.getAddressLabels()
        );
    }

    public void incrementNextChangeAddress(Account account) {
        multiAddressFactory.incrementNextChangeAddress(account.getXpubs().getDefault().getAddress());
    }

    public String getNextReceiveAddressAndReserve(Account account, String reserveLabel)
        throws
        HDWalletException,
        EncryptionException,
        NoSuchAlgorithmException,
        IOException,
        ServerConnectionException {
        String derivationType = derivationTypeFromXPub(account.getXpubs().getDefault());
        int nextIndex = getNextReceiveAddressIndexBtc(account);

        reserveAddress(account, nextIndex, reserveLabel);

        return getReceiveAddress(account, nextIndex, derivationType);
    }

    public void reserveAddress(Account account, int index, String label)
        throws
        HDWalletException,
        EncryptionException,
        NoSuchAlgorithmException,
        IOException,
        ServerConnectionException {

        account.addAddressLabel(index, label);
        if (!save()) {
            throw new ServerConnectionException("Unable to reserve address.");
        }
    }

///////////////////////////////////////////////////////////////////////////
// BALANCE BITCOIN
///////////////////////////////////////////////////////////////////////////

    public CryptoValue getAddressBalance(XPubs xpubs) {
        return balanceManagerBtc.getAddressBalance(xpubs);
    }

    /**
     * Balance API - Final balance for all accounts + addresses.
     */
    public BigInteger getWalletBalance() {
        return balanceManagerBtc.getWalletBalance();
    }

    /**
     * Balance API - Final balance imported addresses.
     */
    public BigInteger getImportedAddressesBalance() {
        return balanceManagerBtc.getImportedAddressesBalance();
    }

    /**
     * Updates all account and address balances and transaction counts.
     * API call uses the Balance endpoint and is much quicker than multiaddress.
     * This will allow the wallet to display wallet/account totals while transactions are still being fetched.
     * This also stores the amount of transactions per address which we can use to limit the calls to multiaddress
     * when the limit is reached.
     */
    public void updateAllBalances() throws ServerConnectionException, IOException {
        Wallet wallet = getPayload();
        List<XPubs> xpubs = WalletExtensionsKt.activeXpubs(wallet);
        List<String> allLegacy = WalletExtensionsKt.nonArchivedImportedAddressStrings(wallet);

        balanceManagerBtc.updateAllBalances(xpubs, allLegacy);
    }

    /**
     * Updates address balance as well as wallet balance.
     * This is used to immediately update balances after a successful transaction which speeds
     * up the balance the UI reflects without the need to wait for incoming websocket notification.
     */
    public void subtractAmountFromAddressBalance(String address, BigInteger amount) throws
        Exception {
            balanceManagerBtc.subtractAmountFromAddressBalance(address, amount);
    }
}