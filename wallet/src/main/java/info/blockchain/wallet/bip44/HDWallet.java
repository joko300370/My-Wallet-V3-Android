package info.blockchain.wallet.bip44;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.*;

import java.util.ArrayList;
import java.util.List;

import info.blockchain.wallet.keys.MasterKey;
import info.blockchain.wallet.keys.MasterKeyImpl;
import info.blockchain.wallet.payload.data.Derivation;
import info.blockchain.wallet.stx.STXAccount;
import info.blockchain.wallet.util.HexUtils;

/**
 * HDWalletx.java : BIP44 wallet
 */
public class HDWallet {

    private byte[] seed = null;
    private String strPassphrase = null;
    private List<String> wordList = null;

    private final byte[] hd_seed;
    private DeterministicKey dkKey = null;
    private DeterministicKey dkRoot = null;

    private final ArrayList<HDAccount> accounts;

    private String strPath = null;
    private STXAccount stxAccount = null;

    private final NetworkParameters params;

    /**
     * Constructor for wallet.
     *
     * @param mc         mnemonic code object
     * @param seed       seed for this wallet
     * @param passphrase optional BIP39 passphrase
     * @param nbAccounts number of accounts to create
     * @param purpose    BIP43 purpose
     */
    public HDWallet(
        MnemonicCode mc,
        NetworkParameters params,
        byte[] seed,
        String passphrase,
        int nbAccounts,
        int purpose
    ) throws MnemonicException.MnemonicLengthException {
        this.params = params;
        this.seed = seed;
        strPassphrase = passphrase;

        wordList = mc.toMnemonic(seed);
        hd_seed = MnemonicCode.toSeed(wordList, strPassphrase);
        dkKey = HDKeyDerivation.createMasterPrivateKey(hd_seed);
        DeterministicKey dKey = HDKeyDerivation.deriveChildKey(
            dkKey, purpose | ChildNumber.HARDENED_BIT
        );
        dkRoot = HDKeyDerivation.deriveChildKey(dKey, ChildNumber.HARDENED_BIT);

        accounts = new ArrayList<>();
        for (int i = 0; i < nbAccounts; i++) {
            accounts.add(new HDAccount(params, dkRoot, i));
        }

        if (purpose == Derivation.LEGACY_PURPOSE) {
            stxAccount = new STXAccount(params, dKey);
        }
        strPath = dKey.getPathAsString();
    }

    /**
     * Constructor for watch-only wallet initialized from submitted XPUB(s).
     *
     * @param xpubs arrayList of XPUB strings
     */
    public HDWallet(
        NetworkParameters params,
        List<String> xpubs
    ) throws AddressFormatException {

        this.params = params;
        accounts = new ArrayList<>();

        int i = 0;
        for (String xpub : xpubs) {
            accounts.add(new HDAccount(params, xpub, i));
            i++;
        }

        hd_seed = null;
    }

    /**
     * Return wallet seed as byte array.
     *
     * @return byte[]
     */
    public byte[] getSeed() {
        return seed;
    }

    /**
     * Return wallet seed as hex string.
     *
     * @return String
     */
    public String getSeedHex() {
        return HexUtils.encodeHexString(seed);
    }

    public List<String> getMnemonic() {
        return wordList;
    }

    /**
     * Return wallet BIP39 passphrase.
     *
     * @return String
     */
    public String getPassphrase() {
        return strPassphrase;
    }

    /**
     * Return accounts for this wallet.
     *
     * @return List<HDAccount>
     */
    public List<HDAccount> getAccounts() {
        return accounts;
    }

    /**
     * Return account for submitted account id.
     *
     * @return HDAccount
     */
    public HDAccount getAccount(int accountId) {
        return accounts.get(accountId);
    }

    /**
     * Add new account.
     */
    public HDAccount addAccount() {
        HDAccount account = new HDAccount(params, dkRoot, accounts.size());
        accounts.add(account);

        return account;
    }

    /**
     * Return BIP44 path for this wallet (m / purpose').
     *
     * @return String
     */
    public String getPath() {
        return strPath;
    }

    public STXAccount getSTXAccount() {
        return stxAccount;
    }

    public MasterKey getMasterKey() {
        return new MasterKeyImpl(dkKey);
    }

    public byte[] getHdSeed() {
        return hd_seed;
    }
}
