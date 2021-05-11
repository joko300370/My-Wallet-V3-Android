package info.blockchain.wallet.util;

import info.blockchain.wallet.bch.BchTransaction;
import info.blockchain.wallet.bch.BchMainNetParams;
import info.blockchain.wallet.bip44.HDAccount;
import info.blockchain.wallet.payload.data.ImportedAddress;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.ArrayUtils;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.params.MainNetParams;

public class Tools {

    public static Transaction applyBip69(Transaction transaction) {
        //This will render an already signed transaction invalid, as the signature covers the ordering of the in/outputs.

        List<TransactionInput> inputList = new ArrayList<>(transaction.getInputs());
        List<TransactionOutput> outputList = new ArrayList<>(transaction.getOutputs());

        Collections.sort(inputList, (o1, o2) -> {
            byte[] hash1 = o1.getOutpoint().getHash().getBytes();
            byte[] hash2 = o2.getOutpoint().getHash().getBytes();
            int hashCompare = LexicographicalComparator.getComparator().compare(hash1, hash2);
            if (hashCompare != 0) {
                return hashCompare;
            } else {
                return (int) (o1.getOutpoint().getIndex() - o2.getOutpoint().getIndex());
            }
        });

        Collections.sort(outputList, (o1, o2) -> {
            long amountDiff = o1.getValue().getValue() - o2.getValue().value;
            if (amountDiff != 0) {
                return (int) amountDiff;
            } else {
                byte[] hash1 = o1.getScriptBytes();
                byte[] hash2 = o2.getScriptBytes();
                return LexicographicalComparator.getComparator().compare(hash1, hash2);
            }
        });

        Transaction sortedTransaction = makeTxObject(transaction.getParams());
        for (TransactionInput input : inputList) {
            sortedTransaction.addInput(input);
        }
        for (TransactionOutput output : outputList) {
            sortedTransaction.addOutput(output);
        }
        return sortedTransaction;
    }

    public static byte[] hexStringToByteArray(String s) {
        s = s.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        int len = s.length();

        if (len % 2 != 0) {
            throw new IllegalArgumentException("Uneven hexadecimal string");
        }

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static ECKey getECKeyFromKeyAndAddress(
        @Nonnull String decryptedKey,
        @Nonnull String address
    ) throws  AddressFormatException {

        byte[] privBytes = Base58.decode(decryptedKey);
        ECKey ecKey;

        ECKey keyCompressed;
        ECKey keyUnCompressed;
        BigInteger priv = new BigInteger(privBytes);
        if (priv.compareTo(BigInteger.ZERO) >= 0) {
            keyCompressed = ECKey.fromPrivate(priv, true);
            keyUnCompressed = ECKey.fromPrivate(priv, false);
        } else {
            byte[] appendZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
            BigInteger priv2 = new BigInteger(appendZeroByte);
            keyCompressed = ECKey.fromPrivate(priv2, true);
            keyUnCompressed = ECKey.fromPrivate(priv2, false);
        }

        if (LegacyAddress.fromKey(MainNetParams.get(), keyCompressed)
                .toString().equals(address)) {
            ecKey = keyCompressed;
        } else if (LegacyAddress.fromKey(MainNetParams.get(), keyUnCompressed)
                .toString().equals(address)) {
            ecKey = keyUnCompressed;
        } else {
            ecKey = null;
        }

        return ecKey;
    }

    public static List<String> filterImportedAddress(int filter, @Nonnull List<ImportedAddress> keys) {

        List<String> addressList = new ArrayList<>();

        for (ImportedAddress key : keys) {
            if (key.getTag() == filter) {
                addressList.add(key.getAddress());
            }
        }

        return addressList;
    }


    /**
     * Returns a list of receive addresses between two points on the chain.
     *
     * @param account    The {@link HDAccount} that you wish to derive addresses from
     * @param startIndex The starting index, probably the next available index
     * @param endIndex   The finishing index, an arbitrary number away from the starting point
     * @return A non-null List of addresses as Strings
     */
    public static List<String> getReceiveAddressList(HDAccount account, int startIndex, int endIndex, int derivationType) {
        List<String> list = new ArrayList<>();

        for (int i = startIndex; i < endIndex; i++) {
            list.add(account.getReceive().getAddressAt(i, derivationType).getFormattedAddress());
        }

        return list;
    }

    public static Transaction makeTxObject(NetworkParameters params) {
        if (params instanceof BchMainNetParams) {
            return new BchTransaction(params);
        } else {
            return new Transaction(params);
        }
    }
}
