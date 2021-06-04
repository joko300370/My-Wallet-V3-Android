package info.blockchain.wallet.bch;

import org.bitcoinj.core.BitcoinSerializer;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Transaction;

public class BchSerializer extends BitcoinSerializer {

    public BchSerializer(NetworkParameters params, boolean parseRetain) {
        super(params, parseRetain);
    }

    @Override
    public Transaction makeTransaction(byte[] payloadBytes, int offset, int length, byte[] hashFromHeader)
        throws ProtocolException {
        return new BchTransaction(getParameters(), payloadBytes, offset, null, this, length, hashFromHeader);
    }
}
