package info.blockchain.wallet.bch;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.params.AbstractBitcoinNetParams;
import org.bitcoinj.store.BlockStore;

public class BchMainNetParams extends AbstractBitcoinNetParams {
    public static final int MAINNET_MAJORITY_WINDOW = 1000;
    public static final int MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED = 950;
    public static final int MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE = 750;
    public static final String URI_SCHEME = "bitcoincash";

    public BchMainNetParams() {
        super();
        maxTarget = Utils.decodeCompactBits(0x1d00ffffL);
        dumpedPrivateKeyHeader = 128;
        addressHeader = 0;
        p2shHeader = 5;
        packetMagic = 0xe3e1f3e8L;

        bip32HeaderP2PKHpub = 0x0488B21E; //The 4 byte header that serializes in base58 to "xpub".
        bip32HeaderP2PKHpriv = 0x0488ADE4; //The 4 byte header that serializes in base58 to "xprv"

        majorityEnforceBlockUpgrade = MAINNET_MAJORITY_ENFORCE_BLOCK_UPGRADE;
        majorityRejectBlockOutdated = MAINNET_MAJORITY_REJECT_BLOCK_OUTDATED;
        majorityWindow = MAINNET_MAJORITY_WINDOW;

        spendableCoinbaseDepth = 100;
    }

    public static char BECH32_SEPARATOR = 0x3a;

    private static BchMainNetParams instance;
    public static synchronized BchMainNetParams get() {
        if (instance == null) {
            instance = new BchMainNetParams();
        }
        return instance;
    }

    /** Human readable part of bech32 encoded segwit address. */
    @Override
    public String getSegwitAddressHrp() {
        return URI_SCHEME;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_MAINNET;
    }

    @Override
    public void checkDifficultyTransitions(
        StoredBlock storedPrev,
        Block next,
        BlockStore blockStore
    ) throws VerificationException { }

    @Override
    public String getUriScheme() {
        return URI_SCHEME;
    }

    public BitcoinSerializer getSerializer(boolean parseRetain) {
        return new BchSerializer(this, parseRetain);
    }
}
