package info.blockchain.wallet.payment;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.ProtocolException;
import org.bitcoinj.core.SegwitAddress;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutPoint;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptException;
import org.bitcoinj.signers.LocalTransactionSigner;
import org.bitcoinj.signers.TransactionSigner;
import org.bitcoinj.wallet.DecryptingKeyBag;
import org.bitcoinj.wallet.KeyBag;
import org.bitcoinj.wallet.KeyChainGroup;
import org.bitcoinj.wallet.RedeemData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import info.blockchain.wallet.api.dust.data.DustInput;
import info.blockchain.wallet.bch.BchLocalTransactionSigner;
import info.blockchain.wallet.bch.CashAddress;
import info.blockchain.wallet.payload.model.Utxo;
import info.blockchain.wallet.util.FormatsUtil;
import info.blockchain.wallet.util.Hash;
import info.blockchain.wallet.util.Tools;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

class PaymentTx {

    private static final Logger log = LoggerFactory.getLogger(PaymentTx.class);

    public static synchronized Transaction makeSimpleTransaction(
            NetworkParameters networkParameters,
            List<Utxo> unspentCoins,
            HashMap<String, BigInteger> receivingAddresses,
            BigInteger fee,
            @Nullable String changeAddress
    ) throws InsufficientMoneyException, AddressFormatException {
        Transaction transaction = Tools.makeTxObject(networkParameters);

        // Outputs
        BigInteger outputValueSum = addTransactionOutputs(
            networkParameters,
            transaction,
            receivingAddresses
        );
        BigInteger valueNeeded = outputValueSum.add(fee);

        // Inputs
        BigInteger inputValueSum = addTransactionInputList(
            networkParameters,
            transaction,
            unspentCoins,
            valueNeeded
        );

        // Add Change
        if (changeAddress != null) {
            addChange(
                networkParameters,
                transaction,
                fee,
                changeAddress,
                outputValueSum,
                inputValueSum
            );
        }

        return Tools.applyBip69(transaction);
    }

    private static BigInteger addTransactionOutputs(
        NetworkParameters networkParameters,
        Transaction transaction,
        Map<String, BigInteger> receivingAddresses
    ) throws AddressFormatException {

        BigInteger outputValueSum = BigInteger.ZERO;

        Set<Entry<String, BigInteger>> set = receivingAddresses.entrySet();
        for (Entry<String, BigInteger> mapEntry : set) {

            String toAddress = mapEntry.getKey();
            BigInteger amount = mapEntry.getValue();

            //Don't allow less than dust value
            if (amount == null
                    || amount.compareTo(BigInteger.ZERO) <= 0
                    || amount.compareTo(Payment.Companion.getDUST()) < 0) {
                continue;
            }

            Coin coin = Coin.valueOf(amount.longValue());

            Address address = toInternalAddress(networkParameters, toAddress);
            transaction.addOutput(coin, address);

            outputValueSum = outputValueSum.add(amount);
        }

        return outputValueSum;
    }

    private static Address toInternalAddress(NetworkParameters networkParams, String toAddress) {
        if (FormatsUtil.isValidBCHAddress(toAddress)) {
            toAddress = CashAddress.toLegacy(networkParams, toAddress);
            return LegacyAddress.fromBase58(networkParams, toAddress);
        }

        try {
            return SegwitAddress.fromBech32(networkParams, toAddress);
        } catch(Exception e) {
            return LegacyAddress.fromBase58(networkParams, toAddress);
        }
    }

    private static BigInteger addTransactionInputList(
        NetworkParameters networkParameters,
        Transaction transaction,
        List<Utxo> unspentCoins,
        BigInteger valueNeeded
    ) throws InsufficientMoneyException {

        BigInteger inputValueSum = BigInteger.ZERO;
        BigInteger minFreeOutputSize = BigInteger.valueOf(1000000);

        for (Utxo unspentCoin : unspentCoins) {

            Hash hash = new Hash(Hex.decode(unspentCoin.getTxHash()));
            hash.reverse();
            Sha256Hash txHash = Sha256Hash.wrap(hash.getBytes());

            TransactionOutPointConnected outPoint = new TransactionOutPointConnected(
                networkParameters,
                unspentCoin.getTxOutputCount(),
                txHash
            );

            Coin inputVal = Coin.valueOf(unspentCoin.getValue().longValue());
            //outPoint needs connected output here
            TransactionOutput output = new TransactionOutput(
                networkParameters,
                null,
                inputVal,
                Hex.decode(unspentCoin.getScript())
            );
            outPoint.setConnectedOutput(output);

            TransactionInput input = new TransactionInput(
                networkParameters,
                null,
                new byte[0],
                outPoint,
                inputVal
            );

            transaction.addInput(input);
            inputValueSum = inputValueSum.add(unspentCoin.getValue());

            if (inputValueSum.compareTo(valueNeeded) == 0
                    || inputValueSum.compareTo(valueNeeded.add(minFreeOutputSize)) >= 0) {
                break;
            }
        }

        if (inputValueSum.compareTo(BigInteger.valueOf(2100000000000000L)) > 0) {
            throw new ProtocolException("21m limit exceeded");
        }

        if (inputValueSum.compareTo(valueNeeded) < 0) {
            throw new InsufficientMoneyException(valueNeeded.subtract(inputValueSum));
        }

        return inputValueSum;
    }

    private static void addChange(
        NetworkParameters networkParameters,
        Transaction transaction,
        BigInteger fee,
        String changeAddress,
        BigInteger outputValueSum,
        BigInteger inputValueSum
    ) throws AddressFormatException {

        BigInteger change = inputValueSum.subtract(outputValueSum).subtract(fee);

        Address internalAddress = toInternalAddress(networkParameters, changeAddress);
        Coin val = Coin.valueOf(change.longValue());

        // Consume dust if needed
        if (change.compareTo(BigInteger.ZERO) > 0 && (change.compareTo(Payment.Companion.getDUST()) > 0)) {

            Script changeScript = ScriptBuilder.createOutputScript(internalAddress);

            TransactionOutput change_output = new TransactionOutput(
                networkParameters,
                null,
                val,
                changeScript.getProgram()
            );
            transaction.addOutput(change_output);
        }
    }

    public static synchronized void signSimpleTransaction(
        NetworkParameters networkParameters,
        Transaction tx,
        List<ECKey> keys,
        boolean useForkId
    ) {

        KeyChainGroup keybag = KeyChainGroup.createBasic(networkParameters);
        keybag.importKeys(keys);

        KeyBag maybeDecryptingKeyBag = new DecryptingKeyBag(keybag, null);

        List<TransactionInput> inputs = tx.getInputs();
        List<TransactionOutput> outputs = tx.getOutputs();
        checkState(inputs.size() > 0);
        checkState(outputs.size() > 0);

        int numInputs = tx.getInputs().size();

        for (int i = 0; i < numInputs; i++) {
            TransactionInput txIn = tx.getInput(i);
            if (txIn.getConnectedOutput() == null) {
                // Missing connected output, assuming already signed.
                continue;
            }

            try {
                // We assume if its already signed, its hopefully got a SIGHASH type that will not invalidate when
                // we sign missing pieces (to check this would require either assuming any signatures are signing
                // standard output types or a way to get processed signatures out of script execution)
                if (useForkId) {
                    txIn.getScriptSig()
                            .correctlySpends(
                                    tx,
                                    i,
                                    null,
                                    txIn.getConnectedOutput().getValue(),
                                    txIn.getConnectedOutput().getScriptPubKey(),
                                    Script.ALL_VERIFY_FLAGS
                            );
                } else {
                    txIn.getScriptSig()
                            .correctlySpends(
                                    tx,
                                    i,
                                    txIn.getWitness(),
                                    txIn.getValue(),
                                    txIn.getConnectedOutput().getScriptPubKey(),
                                    Script.ALL_VERIFY_FLAGS);
                }

                log.warn(
                        "Input {} already correctly spends output, assuming SIGHASH type used will be safe and skipping signing.",
                        i);
                continue;
            } catch (ScriptException e) {
                log.debug("Input contained an incorrect signature", e);
                // Expected.
            }

            Script scriptPubKey = txIn.getConnectedOutput().getScriptPubKey();
            RedeemData redeemData = txIn.getConnectedRedeemData(maybeDecryptingKeyBag);
            checkNotNull(
                redeemData,
                "Transaction exists in wallet that we cannot redeem: %s",
                txIn.getOutpoint().getHash()
            );
            txIn.setScriptSig(
                scriptPubKey.createEmptyInputScript(
                    redeemData.keys.get(0),
                    redeemData.redeemScript)
            );
        }

        TransactionSigner.ProposedTransaction proposal = new TransactionSigner.ProposedTransaction(tx);
        LocalTransactionSigner signer;
        if (useForkId) {
            signer = new BchLocalTransactionSigner();
        } else {
            signer = new LocalTransactionSigner();
        }

        if (!signer.signInputs(proposal, maybeDecryptingKeyBag)) {
            log.info("{} returned false for the tx", signer.getClass().getName());
        }
    }

    static Transaction makeNonReplayableTransaction(
        NetworkParameters networkParameters,
        List<Utxo> unspentCoins,
        HashMap<String, BigInteger> receivingAddresses,
        BigInteger fee,
        @Nullable String changeAddress,
        DustInput dustServiceInput
    ) throws InsufficientMoneyException, AddressFormatException {
        log.info("Making transaction");
        Transaction transaction = Tools.makeTxObject(networkParameters);

        // Outputs
        BigInteger outputValueSum = addTransactionOutputs(networkParameters, transaction, receivingAddresses);
        BigInteger valueNeeded = outputValueSum.add(fee);

        if (unspentCoins.get(0).getValue().compareTo(Payment.Companion.getDUST()) == 0
                && unspentCoins.get(0).isForceInclude()) {
            log.info("Remove forced dust input");
            unspentCoins.remove(0);
            valueNeeded = valueNeeded.subtract(Payment.Companion.getDUST());
        }

        // Inputs
        BigInteger inputValueSum = addTransactionInputList(networkParameters, transaction, unspentCoins, valueNeeded);

        // Add Change
        if (changeAddress != null) {
            addChange(networkParameters, transaction, fee, changeAddress, outputValueSum, inputValueSum);
        }

        // Add dust input/output
        Script dustOutput = new Script(Hex.decode(dustServiceInput.getOutputScript()));
        Coin dustCoin = Coin.valueOf(dustServiceInput.getValue().longValue());

        TransactionOutPoint dustOutpoint = dustServiceInput.getTransactionOutPoint(networkParameters);
        transaction.addInput(dustOutpoint.getHash(), dustOutpoint.getIndex(), new Script(new byte[0]));
        transaction.addOutput(dustCoin, dustOutput);

        return Tools.applyBip69(transaction);
    }
}
