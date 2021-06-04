package info.blockchain.wallet.bch;

import com.google.common.base.Preconditions;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A TransactionSignature wraps an {@link ECKey.ECDSASignature} and adds methods for handling
 * the additional SIGHASH mode byte that is used.
 */
public class BchTransactionSignature extends TransactionSignature {
    /**
     * A byte that controls which parts of a transaction are signed. This is exposed because signatures
     * parsed off the wire may have sighash flags that aren't "normal" serializations of the enum values.
     * Because Bitcoin Core works via bit testing, we must not lose the exact value when round-tripping
     * otherwise we'll fail to verify signature hashes.
     */
    public final int sighashFlags;

    /** Constructs a transaction signature based on the ECDSA signature. */
    public BchTransactionSignature(ECKey.ECDSASignature signature, Transaction.SigHash mode, boolean anyoneCanPay) {
        super(signature.r, signature.s);
        sighashFlags = calcSigHashValue(mode, anyoneCanPay);
    }

    private static final int SIGHASH_FORKID = 0x40;
    /** Calculates the byte used in the protocol to represent the combination of mode and anyoneCanPay. */
    public static int calcSigHashValue(Transaction.SigHash mode, boolean anyoneCanPay) {
        Preconditions.checkArgument(Transaction.SigHash.ALL == mode || Transaction.SigHash.NONE == mode || Transaction.SigHash.SINGLE == mode); // enforce compatibility since this code was made before the SigHash enum was updated
        int sighashFlags = mode.value | SIGHASH_FORKID;
        if (anyoneCanPay)
            sighashFlags |= Transaction.SigHash.ANYONECANPAY.value;
        return sighashFlags;
    }

    @Override
    public boolean anyoneCanPay() {
        return (sighashFlags & Transaction.SigHash.ANYONECANPAY.value) != 0;
    }

    @Override
    public Transaction.SigHash sigHashMode() {
        final int mode = sighashFlags & 0x1f;
        if (mode == Transaction.SigHash.NONE.value)
            return Transaction.SigHash.NONE;
        else if (mode == Transaction.SigHash.SINGLE.value)
            return Transaction.SigHash.SINGLE;
        else
            return Transaction.SigHash.ALL;
    }

    /**
     * What we get back from the signer are the two components of a signature, r and s. To get a flat byte stream
     * of the type used by Bitcoin we have to encode them using DER encoding, which is just a way to pack the two
     * components into a structure, and then we append a byte to the end for the sighash flags.
     */
    @Override
    public byte[] encodeToBitcoin() {
        try {
            ByteArrayOutputStream bos = derByteStream();
            bos.write(sighashFlags);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);  // Cannot happen.
        }
    }

    @Override
    public ECKey.ECDSASignature toCanonicalised() {
        return new TransactionSignature(super.toCanonicalised(), sigHashMode(), anyoneCanPay());
    }
}
