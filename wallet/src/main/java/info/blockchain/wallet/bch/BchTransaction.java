package info.blockchain.wallet.bch;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Message;
import org.bitcoinj.core.MessageSerializer;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.script.Script;
import org.bouncycastle.crypto.params.KeyParameter;

import javax.annotation.Nullable;

public class BchTransaction extends Transaction {

    public BchTransaction(NetworkParameters params) {
        super(params);
    }

    public BchTransaction(NetworkParameters params, byte[] payload, int offset, @Nullable Message parent,
                          MessageSerializer setSerializer, int length, @Nullable byte[] hashFromHeader) throws ProtocolException {
        super(params, payload, offset, parent, setSerializer, length, hashFromHeader);
    }

    public Sha256Hash hashForSignature(int inputIndex, byte[] redeemScript,
                                       SigHash type, boolean anyoneCanPay) {
        byte sigHashType = (byte) BchTransactionSignature.calcSigHashValue(type, anyoneCanPay);
        return hashForSignature(inputIndex, redeemScript, sigHashType);
    }

    public Sha256Hash hashForSignature(int inputIndex, Script redeemScript,
                                       Transaction.SigHash type, boolean anyoneCanPay) {
        int sigHash = BchTransactionSignature.calcSigHashValue(type, anyoneCanPay);
        return hashForSignature(inputIndex, redeemScript.getProgram(), (byte) sigHash);
    }

    public TransactionSignature calculateWitnessSignature(
        int inputIndex,
        ECKey key,
        byte[] scriptCode,
        Coin value,
        Transaction.SigHash hashType,
        boolean anyoneCanPay) {
        Sha256Hash hash = hashForWitnessSignature(inputIndex, scriptCode, value, hashType, anyoneCanPay);
        return new BchTransactionSignature(key.sign(hash), hashType, anyoneCanPay);
    }

    public TransactionSignature calculateWitnessSignature(
        int inputIndex,
        ECKey key,
        @Nullable KeyParameter aesKey,
        byte[] scriptCode,
        Coin value,
        Transaction.SigHash hashType,
        boolean anyoneCanPay) {
        Sha256Hash hash = hashForWitnessSignature(inputIndex, scriptCode, value, hashType, anyoneCanPay);
        return new BchTransactionSignature(key.sign(hash, aesKey), hashType, anyoneCanPay);
    }

    public synchronized Sha256Hash hashForWitnessSignature(
        int inputIndex,
        byte[] scriptCode,
        Coin prevValue,
        Transaction.SigHash type,
        boolean anyoneCanPay) {
        int sigHash = BchTransactionSignature.calcSigHashValue(type, anyoneCanPay);
        return hashForWitnessSignature(inputIndex, scriptCode, prevValue, (byte) sigHash);
    }
}

