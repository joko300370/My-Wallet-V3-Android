package info.blockchain.wallet.payload.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import com.blockchain.api.NonCustodialBitcoinService;
import com.blockchain.api.bitcoin.data.BalanceDto;
import info.blockchain.wallet.bip44.HDAccount;
import info.blockchain.wallet.bip44.HDAddress;
import info.blockchain.wallet.bip44.HDWallet;
import info.blockchain.wallet.bip44.HDWalletFactory.Language;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.keys.MasterKey;
import info.blockchain.wallet.keys.SigningKey;
import info.blockchain.wallet.payload.HDWalletsContainer;
import info.blockchain.wallet.payload.model.Balance;
import info.blockchain.wallet.payload.model.BalanceKt;
import info.blockchain.wallet.payload.model.Utxo;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;
import info.blockchain.wallet.stx.STXAccount;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrivateKeyFactory;

import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.util.encoders.Hex;

import retrofit2.Response;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
        getterVisibility = Visibility.NONE,
        setterVisibility = Visibility.NONE,
        creatorVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE)
public class WalletBody {

    private static final int DEFAULT_MNEMONIC_LENGTH = 12;
    private static final int DEFAULT_NEW_WALLET_SIZE = 1;
    private static final String DEFAULT_PASSPHRASE = "";

    @JsonProperty("accounts")
    private List<Account> accounts;

    @JsonProperty("seed_hex")
    private String seedHex;

    @JsonProperty("passphrase")
    private String passphrase;

    @JsonProperty("mnemonic_verified")
    private boolean mnemonicVerified;

    @JsonProperty("default_account_idx")
    private int defaultAccountIdx;

    @JsonIgnore
    private int wrapperVersion;

    // Contains HDWallet implementations needed for address derivation
    private final HDWalletsContainer HD = new HDWalletsContainer();

    public void decryptHDWallet(
        @Nullable String validatedSecondPassword,
        String sharedKey,
        int iterations
    ) throws IOException,
        DecryptionException,
        InvalidCipherTextException,
        HDWalletException {

        if (!HD.isInstantiated()) {
            instantiateBip44Wallet();
        }

        if (validatedSecondPassword != null && !HD.isDecrypted()) {
            String encryptedSeedHex = getSeedHex();
            String decryptedSeedHex = DoubleEncryptionFactory.decrypt(
                encryptedSeedHex,
                sharedKey,
                validatedSecondPassword,
                iterations
            );

            HD.restoreWallets(
                Language.US,
                decryptedSeedHex,
                getPassphrase(),
                accounts.size()
            );
        }
    }

    private void instantiateBip44Wallet() throws HDWalletException {
        try {
            int walletSize = accounts == null
                    ? DEFAULT_NEW_WALLET_SIZE
                    : accounts.size();

            HD.restoreWallets(
                Language.US,
                getSeedHex(),
                getPassphrase(),
                walletSize
            );
        } catch (Exception e) {
            HD.restoreWatchOnly(getAccounts());
        }

        if (!HD.isInstantiated()) {
            throw new HDWalletException("HD instantiation failed");
        }
    }

    private void validateHD() throws HDWalletException {
        if (!HD.isInstantiated()) {
            throw new HDWalletException("HD wallet not instantiated");
        } else if (!HD.isDecrypted()) {
            throw new HDWalletException("Wallet private key unavailable. First decrypt with second password.");
        }
    }

    public WalletBody() {
        //parameterless constructor needed for jackson
    }

    public WalletBody(String defaultAccountName) {
        this(defaultAccountName, false);
    }

    public WalletBody(String defaultAccountName, boolean createV4) {
        HD.createWallets(
            Language.US,
            DEFAULT_MNEMONIC_LENGTH,
            DEFAULT_PASSPHRASE,
            DEFAULT_NEW_WALLET_SIZE
        );

        setWrapperVersion(createV4 ? WalletWrapper.V4 : WalletWrapper.V3);

        List<HDAccount> hdAccounts = HD.getLegacyAccounts();

        setAccounts(new ArrayList<>());

        for (int i = 0; i < hdAccounts.size(); i++) {
            String label = defaultAccountName;

            if (i > 0) {
                label = defaultAccountName + " " + (i + 1);
            }
            if (createV4)
                addAccount(label, HD.getLegacyAccount(i), HD.getSegwitAccount(i));
            else
                addAccount(label, HD.getLegacyAccount(i), null);
        }

        setSeedHex(this.HD.getSeedHex());
        setDefaultAccountIdx(0);
        setMnemonicVerified(false);
        setPassphrase(DEFAULT_PASSPHRASE);
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public Account getAccount(int accountId) {
        return accounts.get(accountId);
    }

    public String getSeedHex() {
        return seedHex;
    }

    public String getPassphrase() {
        return passphrase;
    }

    public boolean isMnemonicVerified() {
        return mnemonicVerified;
    }

    public int getDefaultAccountIdx() {
        return defaultAccountIdx;
    }

    public int getWrapperVersion() {
        return wrapperVersion;
    }

    public void setAccounts(List<Account> accounts) {
        this.accounts = accounts;
    }

    public void setSeedHex(String seedHex) {
        this.seedHex = seedHex;
    }

    public void setPassphrase(String passphrase) {
        this.passphrase = passphrase;
    }

    public void setMnemonicVerified(boolean mnemonicVerified) {
        this.mnemonicVerified = mnemonicVerified;
    }

    public void setDefaultAccountIdx(int defaultAccountIdx) {
        this.defaultAccountIdx = defaultAccountIdx;
    }

    public void setWrapperVersion(int wrapperVersion) {
        this.wrapperVersion = wrapperVersion;
    }

    public static WalletBody fromJson(
        String json,
        ObjectMapper mapper
    ) throws IOException, HDWalletException {
        WalletBody walletBody = mapper.readValue(json, WalletBody.class);
        walletBody.instantiateBip44Wallet();

        return walletBody;
    }

    public String toJson(ObjectMapper mapper) throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    public List<Account> upgradeAccountsToV4() throws HDWalletException {
        List<Account> upgradedAccounts = new ArrayList<>();
        setWrapperVersion(WalletWrapper.V4);
        for (Account account : getAccounts()) {
            int index = getAccounts().indexOf(account);
            AccountV4 accountV4 = account.upgradeToV4();
            HDAccount legacyHdAccount = HD.getLegacyAccount(index);

            accountV4.derivationForType(Derivation.LEGACY_TYPE).setCache(
                AddressCache.Companion.setCachedXPubs(legacyHdAccount)
            );
            addSegwitDerivation(accountV4, index);
            upgradedAccounts.add(accountV4);
        }
        return upgradedAccounts;
    }

    private void addSegwitDerivation(AccountV4 accountV4, int accountIdx) throws HDWalletException {
        validateHD();
        if (wrapperVersion != WalletWrapper.V4) {
            throw new HDWalletException("HD wallet has not been upgraded to version 4");
        }
        HDAccount hdAccount = HD.getSegwitAccount(accountIdx);
        accountV4.addSegwitDerivation(hdAccount, accountIdx);
        getAccounts().set(accountIdx, accountV4);
    }

    /**
     * @return Non-archived account xpubs
     */
    public List<XPubs> getActiveXpubs() {

        ArrayList<XPubs> result = new ArrayList<>();

        if (getAccounts() == null) {
            return result;
        }

        int nb_accounts = getAccounts().size();
        for (int i = 0; i < nb_accounts; i++) {

            Account account = getAccounts().get(i);
            boolean isArchived = account.isArchived();
            if (!isArchived) {
                result.add(account.getXpubs());
            }
        }
        return result;
    }

    public Account addAccount(String label) throws HDWalletException {
        validateHD();

        int accountIndex = HD.addAccount();
        HDAccount legacyAccount = HD.getLegacyAccount(accountIndex);
        HDAccount segwitAccount = HD.getSegwitAccount(accountIndex);

        return addAccount(label, legacyAccount, segwitAccount);
    }

    public Account addAccount(
        String label,
        @Nonnull HDAccount legacyAccount,
        @Nullable HDAccount segWit
    ) {
        Account accountBody;

        if (wrapperVersion == WalletWrapper.V4) {
            List<Derivation> derivations = new ArrayList<>();

            Derivation legacy = Derivation.create(
                legacyAccount.getXPriv(),
                legacyAccount.getXpub(),
                AddressCache.Companion.setCachedXPubs(legacyAccount)
            );
            Derivation segwit = Derivation.createSegwit(
                segWit.getXPriv(),
                segWit.getXpub(),
                AddressCache.Companion.setCachedXPubs(segWit)
            );

            derivations.add(legacy);
            derivations.add(segwit);
            accountBody = new AccountV4(
                label,
                Derivation.SEGWIT_BECH32_TYPE,
                false,
                derivations
            );
        } else {
            accountBody = new AccountV3(
                label,
                false,
                legacyAccount.getXPriv(),
                legacyAccount.getXpub()
            );
        }
        accounts.add(accountBody);

        return accountBody;
    }

    public static WalletBody recoverFromMnemonic(
        String mnemonic,
        String defaultAccountName,
        NonCustodialBitcoinService bitcoinApi,
        boolean recoverV4
    ) throws Exception {
        return recoverFromMnemonic(
            mnemonic,
            "",
            defaultAccountName,
            0,
            bitcoinApi,
            recoverV4
        );
    }

    public static WalletBody recoverFromMnemonic(
        String mnemonic,
        String passphrase,
        String defaultAccountName,
        NonCustodialBitcoinService bitcoinApi,
        boolean recoverV4
    ) throws Exception {
        return recoverFromMnemonic(
            mnemonic,
            passphrase,
            defaultAccountName,
            0,
            bitcoinApi,
            recoverV4
        );
    }

    public static WalletBody recoverFromMnemonic(
        String mnemonic,
        String passphrase,
        String defaultAccountName,
        int walletSize,
        NonCustodialBitcoinService bitcoinApi,
        boolean recoverV4
    ) throws Exception {
        int wrapperVersion = recoverV4 ? WalletWrapper.V4 : WalletWrapper.V3;

        HDWalletsContainer HD = new HDWalletsContainer();

        //Start with initial wallet size of 1.
        //After wallet is recovered we'll check how many accounts to restore
        HD.restoreWallets(
            Language.US,
            mnemonic,
            passphrase,
            DEFAULT_NEW_WALLET_SIZE
        );

        WalletBody walletBody = new WalletBody();
        walletBody.setWrapperVersion(wrapperVersion);
        walletBody.setAccounts(new ArrayList<>());

        if (walletSize <= 0) {
            int legacyWalletSize = getDeterminedSize(
                1,
                5,
                0,
                bitcoinApi,
                HD.getHDWallet(Derivation.LEGACY_PURPOSE),
                Derivation.LEGACY_PURPOSE
            );

            int segwitP2SHWalletSize = 0;

            if (wrapperVersion == WalletWrapper.V4) {
                segwitP2SHWalletSize = getDeterminedSize(
                    1,
                    5,
                    0,
                    bitcoinApi,
                    HD.getHDWallet(Derivation.SEGWIT_BECH32_PURPOSE),
                    Derivation.SEGWIT_BECH32_PURPOSE
                );
            }

            walletSize = Math.max(legacyWalletSize, segwitP2SHWalletSize);
        }

        HD.restoreWallets(
            Language.US,
            mnemonic,
            passphrase,
            walletSize
        );

        //Set accounts
        List<HDAccount> legacyAccounts = HD.getLegacyAccounts();
        List<HDAccount> segwitAccounts = HD.getSegwitAccounts();

        for (int i = 0; i < legacyAccounts.size(); i++) {
            String label = defaultAccountName;

            if (i > 0) {
                label = defaultAccountName + " " + (i + 1);
            }

            HDAccount legacyAccount = legacyAccounts.get(i);
            HDAccount segwitAccount = segwitAccounts.get(i);
            Account account;
            if (!recoverV4) {
                account = walletBody.addAccount(label, legacyAccount, null);
            } else {
                account = walletBody.addAccount(label, legacyAccount, segwitAccount);
            }

            if (wrapperVersion == WalletWrapper.V4) {
                AccountV4 accountV4 = account.upgradeToV4();
                accountV4.addSegwitDerivation(segwitAccounts.get(i), i);
                walletBody.getAccounts().set(i, accountV4);
            }
        }

        walletBody.setSeedHex(Hex.toHexString(HD.getSeed()));
        walletBody.setPassphrase(HD.getPassphrase());
        walletBody.setMnemonicVerified(false);
        walletBody.setDefaultAccountIdx(0);

        return walletBody;
    }

    private static int getDeterminedSize(
        int walletSize,
        int trySize,
        int currentGap,
        NonCustodialBitcoinService bitcoinApi,
        HDWallet bip44Wallet,
        int purpose
    ) throws Exception {

        LinkedList<String> xpubs = new LinkedList<>();

        for (int i = 0; i < trySize; i++) {
            HDAccount account = bip44Wallet.addAccount();
            xpubs.add(account.getXpub());
        }

        Response<Map<String, BalanceDto>> exe = bitcoinApi.getBalance(
            NonCustodialBitcoinService.BITCOIN,
            purpose == Derivation.LEGACY_PURPOSE ? xpubs : Collections.emptyList(),
            purpose == Derivation.SEGWIT_BECH32_PURPOSE ? xpubs : Collections.emptyList(),
            NonCustodialBitcoinService.BalanceFilter.RemoveUnspendable
        ).execute();

        if (!exe.isSuccessful()) {
            throw new Exception(exe.code() + " " + exe.errorBody().string());
        }

        final Map<String, Balance> map = BalanceKt.toBalanceMap(exe.body());

        final int lookAheadTotal = 10;
        for (String xpub : xpubs) {
            //If account has txs
            if (map.get(xpub).getTxCount() > 0L) {
                walletSize++;
                currentGap = 0;
            } else {
                currentGap++;
            }

            if (currentGap >= lookAheadTotal) {
                return walletSize;
            }
        }

        return getDeterminedSize(
            walletSize,
            trySize * 2,
            currentGap,
            bitcoinApi,
            bip44Wallet,
            purpose
        );
    }

    public List<SigningKey> getHDKeysForSigning(
        Account account,
        SpendableUnspentOutputs unspentOutputBundle
    ) throws Exception {

        validateHD();

        List<SigningKey> keys = new ArrayList<>();

        List<HDAccount> hdAccounts = getHDAccountFromAccountBody(account);

        for (Utxo unspent : unspentOutputBundle.getSpendableOutputs()) {
            HDAccount hdAccount = hdAccounts.get(unspent.isSegwit() ? 1 : 0);  // TODO: What are these constants?
            if (hdAccount != null && unspent.getXpub() != null) {
                String[] split = unspent.getXpub().getDerivationPath().split("/");
                int chain = Integer.parseInt(split[1]);
                int addressIndex = Integer.parseInt(split[2]);

                HDAddress hdAddress = hdAccount.getChain(chain)
                    .getAddressAt(addressIndex, Derivation.SEGWIT_BECH32_PURPOSE);

                SigningKey walletKey = new PrivateKeyFactory()
                        .getSigningKey(PrivateKeyFactory.WIF_COMPRESSED, hdAddress.getPrivateKeyString());

                keys.add(walletKey);
            }
        }

        return keys;
    }

    public List<HDAccount> getHDAccountFromAccountBody(Account accountBody) throws HDWalletException {
        if (!HD.isInstantiated()) {
            throw new HDWalletException("HD wallet not instantiated");
        }

        HDAccount legacyAccount = null;
        HDAccount segwitAccount = null;

        for (HDAccount account : HD.getLegacyAccounts()) {
            if (account.getXpub().equals(accountBody.xpubForDerivation(Derivation.LEGACY_TYPE))) {
                legacyAccount = account;
            }
        }

        for (HDAccount account : HD.getSegwitAccounts()) {
            if (account.getXpub().equals(accountBody.xpubForDerivation(Derivation.SEGWIT_BECH32_TYPE))) {
                segwitAccount = account;
            }
        }

        return Arrays.asList(legacyAccount, segwitAccount);
    }

    //no need for second pw. only using HD xpubs
    // TODO: 16/02/2017 Old. Investigate better way to do this
    public BiMap<String, Integer> getXpubToAccountIndexMap() throws HDWalletException {
        if (!HD.isInstantiated()) {
            throw new HDWalletException("HD wallet not instantiated");
        }

        BiMap<String, Integer> xpubToAccountIndexMap = HashBiMap.create();

        List<HDAccount> accountList = HD.getLegacyAccounts();

        for (HDAccount account : accountList) {
            xpubToAccountIndexMap.put(account.getXpub(), account.getId());
        }

        return xpubToAccountIndexMap;
    }

    /**
     * Bip44 master private key. Not to be confused with bci HDWallet seed
     */
    public MasterKey getMasterKey() throws HDWalletException {
        validateHD();
        return HD.getMasterKey();
    }

    public byte[] getHdSeed() throws HDWalletException {
        validateHD();
        return HD.getHdSeed();
    }

    public List<String> getMnemonic() throws HDWalletException {
        validateHD();
        return HD.getMnemonic();
    }

    @Nullable
    public String getLabelFromXpub(String xpub) {
        List<Account> accounts = getAccounts();

        for (Account account : accounts) {
            if (account.containsXpub(xpub)) {
                return account.getLabel();
            }
        }
        return null;
    }

    @Nullable
    public STXAccount getSTXAccount() {
        return HD.getStxAccount();
    }
}
