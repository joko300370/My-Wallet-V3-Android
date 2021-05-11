package info.blockchain.wallet.bip44;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;

/**
 * HDChain.java : a chain in a BIP44 wallet account
 */
public class HDChain {

    private final DeterministicKey cKey;
    private final boolean isReceive;

    private final String strPath;

    private final NetworkParameters params;
    private final String xPub;

    public static final int RECEIVE_CHAIN = 0;
    public static final int CHANGE_CHAIN = 1;

    /**
     * Constructor for a chain.
     *
     * @param params    NetworkParameters
     * @param aKey      deterministic key for this chain
     * @param isReceive this is the receive chain
     */
    public HDChain(NetworkParameters params, DeterministicKey aKey, boolean isReceive) {
        this.params = params;
        this.isReceive = isReceive;
        int chain = isReceive ? RECEIVE_CHAIN : CHANGE_CHAIN;
        cKey = HDKeyDerivation.deriveChildKey(aKey, chain);

        strPath = cKey.getPathAsString();

        xPub = cKey.serializePubB58(params);
    }

    /**
     * Test if this is the receive chain.
     *
     * @return boolean
     */
    public boolean isReceive() {
        return isReceive;
    }

    /**
     * Return HDAddress at provided index into chain.
     *
     * @return HDAddress
     */
    public HDAddress getAddressAt(int addrIdx, int purpose) {
        return new HDAddress(params, cKey, addrIdx, purpose);
    }

    /**
     * Return BIP44 path for this chain (m / purpose' / coin_type' / account' / chain).
     *
     * @return String
     */
    public String getPath() {
        return strPath;
    }

    public String getXpub() {
        return xPub;
    }
}