package info.blockchain.wallet.payload.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.google.common.annotations.VisibleForTesting;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.EncryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.NoSuchAddressException;
import info.blockchain.wallet.keys.SigningKey;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.FormatsUtil;
import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.params.MainNetParams;
import org.spongycastle.crypto.InvalidCipherTextException;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = Visibility.NONE,
        getterVisibility = Visibility.NONE,
        setterVisibility = Visibility.NONE,
        creatorVisibility = Visibility.NONE,
        isGetterVisibility = Visibility.NONE)
public class Wallet {

    @JsonProperty("guid")
    private String guid;

    @JsonProperty("sharedKey")
    private String sharedKey;

    @JsonProperty("double_encryption")
    private boolean doubleEncryption;

    @JsonProperty("dpasswordhash")
    private String dpasswordhash;

    @JsonProperty("metadataHDNode")
    private String metadataHDNode;

    @JsonProperty("tx_notes")
    private Map<String, String> txNotes;

    @JsonProperty("tx_tags")
    private Map<String, List<Integer>> txTags;

    @JsonProperty("tag_names")
    private List<Map<Integer, String>> tagNames;

    @JsonProperty("options")
    private Options options;

    @JsonProperty("wallet_options")
    private Options walletOptions;

    @JsonProperty("hd_wallets")
    private List<WalletBody> walletBodies;

    @JsonProperty("keys")
    private List<ImportedAddress> imported;

    @JsonProperty("address_book")
    private List<AddressBook> addressBook;

    @JsonIgnore
    private int wrapperVersion;

    public Wallet() {
        guid           = UUID.randomUUID().toString();
        sharedKey      = UUID.randomUUID().toString();
        txNotes        = new HashMap<>();
        imported       = new ArrayList<>();
        options        = Options.getDefaultOptions();
        wrapperVersion = WalletWrapper.V4;
        walletBodies   = new ArrayList<>();
    }

    public Wallet(String defaultAccountName) throws Exception {
        this(defaultAccountName, false);
    }

    public Wallet(String defaultAccountName, boolean createV4) throws Exception {
        guid      = UUID.randomUUID().toString();
        sharedKey = UUID.randomUUID().toString();
        txNotes   = new HashMap<>();
        imported  = new ArrayList<>();
        options   = Options.getDefaultOptions();

        WalletBody walletBodyBody = new WalletBody(defaultAccountName, createV4);
        wrapperVersion = createV4 ? WalletWrapper.V4 : WalletWrapper.V3;
        walletBodies   = new ArrayList<>();
        walletBodies.add(walletBodyBody);
    }

    public String getGuid() {
        return guid;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public boolean isDoubleEncryption() {
        return doubleEncryption;
    }

    public String getDpasswordhash() {
        return dpasswordhash;
    }

    public String getMetadataHDNode() {
        return metadataHDNode;
    }

    public Map<String, String> getTxNotes() {
        return txNotes;
    }

    public Map<String, List<Integer>> getTxTags() {
        return txTags;
    }

    public List<Map<Integer, String>> getTagNames() {
        return tagNames;
    }

    public Options getOptions() {
        fixPbkdf2Iterations();
        return options;
    }

    public Options getWalletOptions() {
        return walletOptions;
    }

    @Deprecated
    @Nullable
    public List<WalletBody> getWalletBodies() {
        return walletBodies;
    }

    @Nullable
    public WalletBody getWalletBody() {
        if (walletBodies == null || walletBodies.isEmpty()) {
            return null;
        }
        else {
            return walletBodies.get(HD_WALLET_INDEX);
        }
    }

    public List<ImportedAddress> getImportedAddressList() {
        return imported;
    }

    public List<AddressBook> getAddressBook() {
        return addressBook;
    }

    public int getWrapperVersion() {
        return wrapperVersion;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    @Deprecated
    public void setWalletBodies(List<WalletBody> walletBodies) {
        this.walletBodies = walletBodies;
    }

    public void setWalletBody(WalletBody walletBody) {
        this.walletBodies = Collections.singletonList(walletBody);
    }

    public void setImportedAddressList(List<ImportedAddress> keys) {
        this.imported = keys;
    }

    public void setWrapperVersion(int wrapperVersion) {
        this.wrapperVersion = wrapperVersion;
    }

    public boolean isUpgradedToV3() {
        return (walletBodies != null && walletBodies.size() > 0);
    }

    public static Wallet fromJson(String json)
        throws IOException, HDWalletException {
        ObjectMapper mapper = new ObjectMapper();

        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                                 .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                                 .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                                 .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                                 .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        KotlinModule module = new KotlinModule();
        module.addAbstractTypeMapping(Account.class, AccountV3.class);
        mapper.registerModule(module);

        return fromJson(json, mapper);
    }

    public static Wallet fromJson(
        String json,
        ObjectMapper mapper
    ) throws IOException, HDWalletException {
        Wallet wallet = mapper.readValue(json, Wallet.class);

        if (wallet.getWalletBodies() != null) {
            ArrayList<WalletBody> walletBodyList = new ArrayList<>();

            for (WalletBody walletBody : wallet.getWalletBodies()) {
                walletBodyList.add(
                    WalletBody.fromJson(
                        walletBody.toJson(mapper),
                        mapper
                    )
                );
            }

            wallet.setWalletBodies(walletBodyList);
        }

        return wallet;
    }

    public String toJson(ObjectMapper mapper) throws JsonProcessingException {
        return mapper.writeValueAsString(this);
    }

    void addHDWallet(WalletBody walletBody) {

        if (walletBodies == null) {
            walletBodies = new ArrayList<>();
        }

        walletBodies.add(walletBody);
    }

    /**
     * Checks imported address and hd keys for possible double encryption corruption
     */
    public boolean isEncryptionConsistent() {
        ArrayList<String> keyList = new ArrayList<>();

        if (getImportedAddressList() != null) {
            List<ImportedAddress> importedAddresses = getImportedAddressList();
            for (ImportedAddress importedAddress : importedAddresses) {
                String privateKey = importedAddress.getPrivateKey();
                // Filter watch-only addresses, which still exist in some wallets
                if (privateKey != null) {
                    keyList.add(privateKey);
                }
            }
        }

        if (getWalletBodies() != null && getWalletBodies().size() > 0) {
            for (WalletBody walletBody : getWalletBodies()) {
                List<Account> accounts = walletBody.getAccounts();
                for (Account account : accounts) {
                    keyList.add(account.getXpriv());
                }
            }
        }

        return isEncryptionConsistent(isDoubleEncryption(), keyList);
    }

    boolean isEncryptionConsistent(boolean isDoubleEncrypted, List<String> keyList) {
        boolean consistent = true;
        for (String key : keyList) {
            if (isDoubleEncrypted) {
                consistent = FormatsUtil.isKeyEncrypted(key);
            } else {
                consistent = FormatsUtil.isKeyUnencrypted(key);
            }

            if (!consistent) {
                break;
            }
        }
        return consistent;
    }

    public void validateSecondPassword(@Nullable String secondPassword) throws DecryptionException {

        if (isDoubleEncryption()) {
            DoubleEncryptionFactory.validateSecondPassword(
                getDpasswordhash(),
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations()
            );
        }
        else if (!isDoubleEncryption() && secondPassword != null) {
            throw new DecryptionException("Double encryption password specified on non double encrypted wallet.");
        }
    }

    public void upgradeV2PayloadToV3(@Nullable String secondPassword, String defaultAccountName) throws Exception {

        //Check if payload has 2nd password
        validateSecondPassword(secondPassword);

        if (!isUpgradedToV3()) {

            //Create new hd wallet
            WalletBody walletBodyBody = new WalletBody(defaultAccountName);
            walletBodyBody.setWrapperVersion(wrapperVersion);
            addHDWallet(walletBodyBody);

            //Double encrypt if need
            if (!StringUtils.isEmpty(secondPassword)) {

                //Double encrypt seedHex
                String doubleEncryptedSeedHex = DoubleEncryptionFactory.encrypt(
                    walletBodyBody.getSeedHex(),
                    getSharedKey(),
                    secondPassword,
                    getOptions().getPbkdf2Iterations()
                );
                walletBodyBody.setSeedHex(doubleEncryptedSeedHex);

                //Double encrypt private keys
                for (Account account : walletBodyBody.getAccounts()) {

                    String encryptedXPriv = DoubleEncryptionFactory.encrypt(
                        account.getXpriv(),
                        getSharedKey(),
                        secondPassword,
                        getOptions().getPbkdf2Iterations()
                    );

                    account.setXpriv(encryptedXPriv);

                }
            }

            setWrapperVersion(WalletWrapper.V3);
        }
    }

    @VisibleForTesting
    public ImportedAddress addImportedAddress(ImportedAddress address, @Nullable String secondPassword)
        throws Exception {

        validateSecondPassword(secondPassword);

        if (secondPassword != null) {
            //Double encryption
            String unencryptedKey = address.getPrivateKey();

            String encryptedKey = DoubleEncryptionFactory.encrypt(
                unencryptedKey,
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations()
            );

            address.setPrivateKey(encryptedKey);

        }
        imported.add(address);
        return address;
    }

    public ImportedAddress addImportedAddressFromKey(SigningKey key, @Nullable String secondPassword)
        throws Exception {
        return addImportedAddress(ImportedAddress.fromECKey(key.toECKey()), secondPassword);
    }

    public void decryptHDWallet(String secondPassword)
        throws DecryptionException,
        IOException,
        InvalidCipherTextException,
        HDWalletException {

        validateSecondPassword(secondPassword);

        WalletBody walletBody = walletBodies.get(HD_WALLET_INDEX);
        walletBody.decryptHDWallet(secondPassword, sharedKey, getOptions().getPbkdf2Iterations());
    }

    public void encryptAccount(Account account, String secondPassword)
        throws UnsupportedEncodingException, EncryptionException {
        //Double encryption
        if (secondPassword != null) {
            String encryptedPrivateKey = DoubleEncryptionFactory.encrypt(
                account.getXpriv(),
                sharedKey,
                secondPassword,
                getOptions().getPbkdf2Iterations()
            );
            account.setXpriv(encryptedPrivateKey);
        }
    }

    public Account addAccount(
        String label,
        @Nullable String secondPassword,
        int version
    ) throws Exception {

        validateSecondPassword(secondPassword);

        //Double decryption if need
        decryptHDWallet(secondPassword);

        WalletBody walletBody = walletBodies.get(HD_WALLET_INDEX);

        walletBody.setWrapperVersion(version);

        Account account = walletBody.addAccount(label);

        //Double encryption if need
        encryptAccount(account, secondPassword);

        return account;
    }

    public ImportedAddress setKeyForImportedAddress(
        SigningKey key,
        @Nullable String secondPassword
    ) throws DecryptionException,
        UnsupportedEncodingException,
        EncryptionException,
        NoSuchAddressException {

        ECKey ecKey = key.toECKey();
        validateSecondPassword(secondPassword);

        List<ImportedAddress> addressList = getImportedAddressList();

        String address = LegacyAddress.fromKey(
            MainNetParams.get(),
            ecKey
        ).toString();

        ImportedAddress matchingAddressBody = null;

        for (ImportedAddress addressBody : addressList) {
            if (addressBody.getAddress().equals(address)) {
                matchingAddressBody = addressBody;
            }
        }

        if (matchingAddressBody == null) {
            throw new NoSuchAddressException("No matching address found for key");
        }

        if (secondPassword != null) {
            //Double encryption
            String encryptedKey = Base58.encode(ecKey.getPrivKeyBytes());
            String encrypted2 = DoubleEncryptionFactory.encrypt(
                encryptedKey,
                getSharedKey(),
                secondPassword,
                getOptions().getPbkdf2Iterations()
            );

            matchingAddressBody.setPrivateKey(encrypted2);

        }
        else {
            matchingAddressBody.setPrivateKeyFromBytes(ecKey.getPrivKeyBytes());
        }
        return matchingAddressBody;
    }

    /**
     * @deprecated Use the kotlin extension: {@link WalletExtensionsKt#nonArchivedImportedAddressStrings}
     */
    @Deprecated
    public List<String> getImportedAddressStringList() {

        List<String> addrs = new ArrayList<>(imported.size());
        for (ImportedAddress importedAddress : imported) {
            if (!ImportedAddressExtensionsKt.isArchived(importedAddress)) {
                addrs.add(importedAddress.getAddress());
            }
        }

        return addrs;
    }

    public List<String> getImportedAddressStringList(long tag) {

        List<String> addrs = new ArrayList<>(imported.size());
        for (ImportedAddress importedAddress : imported) {
            if (importedAddress.getTag() == tag) {
                addrs.add(importedAddress.getAddress());
            }
        }

        return addrs;
    }

    public boolean containsImportedAddress(String addr) {
        for (ImportedAddress importedAddress : imported) {
            if (importedAddress.getAddress().equals(addr)) {
                return true;
            }
        }
        return false;
    }

    /**
     * In case wallet was encrypted with iterations other than what is specified in options, we
     * will ensure next encryption and options get updated accordingly.
     *
     * @return
     */
    private int fixPbkdf2Iterations() {

        //Use default initially
        int iterations = WalletWrapper.DEFAULT_PBKDF2_ITERATIONS_V2;

        //Old wallets may contain 'wallet_options' key - we'll use this now
        if (walletOptions != null && walletOptions.getPbkdf2Iterations() > 0) {
            iterations = walletOptions.getPbkdf2Iterations();
            options.setPbkdf2Iterations(iterations);
        }

        //'options' key override wallet_options key - we'll use this now
        if (options != null && options.getPbkdf2Iterations() > 0) {
            iterations = options.getPbkdf2Iterations();
        }

        //If wallet doesn't contain 'option' - use default
        if (options == null) {
            options = Options.getDefaultOptions();
        }

        //Set iterations
        options.setPbkdf2Iterations(iterations);

        return iterations;
    }

    /**
     * Returns label if match found, otherwise just returns address.
     *
     * @param address
     */
    public String getLabelFromImportedAddress(String address) {

        List<ImportedAddress> addresses = getImportedAddressList();

        for (ImportedAddress importedAddress : addresses) {
            if (importedAddress.getAddress().equals(address)) {
                String label = importedAddress.getLabel();
                if (label == null || label.isEmpty()) {
                    return address;
                }
                else {
                    return label;
                }
            }
        }

        return address;
    }

    //Assume we only support 1 hdWallet
    private static final int HD_WALLET_INDEX = 0;

    private boolean isKeyUnencrypted(String data) {
        if (data == null)
            return false;
        try {
            Base58.decode(data);
            return true;
        } catch (AddressFormatException e) {
            return false;
        }
    }
}
