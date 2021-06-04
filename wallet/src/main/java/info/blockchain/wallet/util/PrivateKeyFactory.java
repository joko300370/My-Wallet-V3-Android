package info.blockchain.wallet.util;

import info.blockchain.api.ApiException;
import info.blockchain.api.BitcoinApi;
import info.blockchain.api.bitcoin.data.BalanceDto;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.ArrayUtils;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;
import org.jetbrains.annotations.NotNull;
import org.spongycastle.util.encoders.Hex;

import info.blockchain.wallet.keys.SigningKey;
import info.blockchain.wallet.keys.SigningKeyImpl;
import info.blockchain.wallet.payload.model.Balance;
import info.blockchain.wallet.payload.model.BalanceKt;
import retrofit2.Response;

@SuppressWarnings("WeakerAccess")
public class PrivateKeyFactory {

    public final static String BASE58 = "base58";
    public final static String BASE64 = "base64";
    public final static String BIP38 = "bip38";
    public final static String HEX = "hex";
    public final static String MINI = "mini";
    public final static String WIF_COMPRESSED = "wif_c";
    public final static String WIF_UNCOMPRESSED = "wif_u";

    public String getFormat(String key) {
        try {
            // 51 characters base58, always starts with a '5'
            if (key.matches("^5[1-9A-HJ-NP-Za-km-z]{50}$")) {
                return WIF_UNCOMPRESSED;
            }
            // 52 characters, always starts with 'K' or 'L' (or 'c' for testnet)
            else if (key.matches("^[LK][1-9A-HJ-NP-Za-km-z]{51}$")) {
                return WIF_COMPRESSED;

            }
            else if (key.matches("^[1-9A-HJ-NP-Za-km-z]{44}$") || key
                    .matches("^[1-9A-HJ-NP-Za-km-z]{43}$")) {
                return BASE58;
            }
            else if (key.matches("^[A-Fa-f0-9]{64}$")) {
                return HEX;
            }
            else if (key.matches("^[A-Za-z0-9/=+]{44}$")) {
                return BASE64;
            }
            else if (key.matches("^6P[1-9A-HJ-NP-Za-km-z]{56}$")) {
                return BIP38;
            }
            else if (key.matches("^S[1-9A-HJ-NP-Za-km-z]{21}$") ||
                     key.matches("^S[1-9A-HJ-NP-Za-km-z]{25}$") ||
                     key.matches("^S[1-9A-HJ-NP-Za-km-z]{29}$") ||
                     key.matches("^S[1-9A-HJ-NP-Za-km-z]{30}$")) {

                String data = key + "?";
                Hash hash = new Hash(MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8)));
                byte[] testBytes = hash.getBytes();

                if ((testBytes[0] == 0x00)) {
                    return MINI;
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    public SigningKey getKeyFromImportedData(String format, String data, BitcoinApi bitcoinApi) throws Exception {
        // If we're importing a key, we need bitcoinApi, if we're signing then we don't, so use the other
        // 'getSigningKey()' method
        MainNetParams netParams = MainNetParams.get();
        switch (format) {
            case WIF_UNCOMPRESSED:
            case WIF_COMPRESSED:
                DumpedPrivateKey pk = DumpedPrivateKey.fromBase58(netParams, data);
                return new SigningKeyImpl(pk.getKey());
            case BASE58:
                return new SigningKeyImpl(decodeBase58PK(data));
            case BASE64:
                return new SigningKeyImpl(decodeBase64PK(data));
            case HEX:
                return new SigningKeyImpl(determineImportedKey(data, bitcoinApi, netParams));
            case MINI:
                return new SigningKeyImpl(decodeMiniKey(data, bitcoinApi, netParams));
            default:
                throw new Exception("Unknown key format: " + format);
        }
    }

    public SigningKey getSigningKey(String format, String data) throws Exception {
        // If we're importing a key, we need bitcoinApi, if we're signing then we don't
        MainNetParams netParams = MainNetParams.get();
        switch (format) {
            case WIF_UNCOMPRESSED:
            case WIF_COMPRESSED:
                DumpedPrivateKey pk = DumpedPrivateKey.fromBase58(netParams, data);
                return new SigningKeyImpl(pk.getKey());
            default:
                throw new Exception("Unknown signing key format: " + format);
        }
    }

    public SigningKey getBip38Key(
        @NotNull String keyData,
        @NotNull String keyPassword
    ) throws BIP38PrivateKey.BadPassphraseException {
        MainNetParams netParams = MainNetParams.get();
        return new SigningKeyImpl(BIP38PrivateKey.fromBase58(netParams, keyData).decrypt(keyPassword));
    }

    private ECKey decodeMiniKey(
        String mini,
        BitcoinApi bitcoinApi,
        NetworkParameters btcParameters
    ) throws Exception {
        Hash hash = new Hash(
            MessageDigest.getInstance("SHA-256")
                .digest(mini.getBytes(StandardCharsets.UTF_8))
        );
        return determineImportedKey(hash.toString(), bitcoinApi, btcParameters);
    }

    private ECKey determineImportedKey(String hash, BitcoinApi bitcoinApi, NetworkParameters btcParameters) {

        ECKey uncompressedKey = decodeHexPK(hash, false);
        ECKey compressedKey = decodeHexPK(hash, true);

        try {
            String uncompressedAddress = LegacyAddress.fromKey(btcParameters, uncompressedKey).toString();
            String compressedAddress = LegacyAddress.fromKey(btcParameters, compressedKey).toString();

            ArrayList<String> list = new ArrayList<>();
            list.add(uncompressedAddress);
            list.add(compressedAddress);

            // No need to use segwit xpubs/addresses here, this is only called for imported
            // addresses, which don't support segwit at this time.
            Response<Map<String, BalanceDto>> exe = bitcoinApi.getBalance(
                BitcoinApi.BITCOIN,
                list,
                Collections.emptyList(),
                BitcoinApi.BalanceFilter.RemoveUnspendable
            ).execute();

            if (!exe.isSuccessful()) {
                throw new ApiException("Failed to connect to server.");
            }

            Map<String, Balance> body = BalanceKt.toBalanceMap(exe.body());

            BigInteger uncompressedBalance = body.get(uncompressedAddress).getFinalBalance();
            BigInteger compressedBalance = body.get(compressedAddress).getFinalBalance();

            if (compressedBalance != null && compressedBalance.compareTo(BigInteger.ZERO) == 0
                && uncompressedBalance != null && uncompressedBalance.compareTo(BigInteger.ZERO) > 0) {
                return uncompressedKey;
            } else {
                return compressedKey;
            }
        } catch (Exception e) {
            // TODO: 08/03/2017 Is this safe? Could this not return an uninitialized ECKey?
            e.printStackTrace();
            return compressedKey;
        }
    }

    private ECKey decodeBase58PK(String base58Priv) {
        byte[] privBytes = Base58.decode(base58Priv);
        // Prepend a zero byte to make the biginteger unsigned
        byte[] appendZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
        return ECKey.fromPrivate(new BigInteger(appendZeroByte), true);
    }

    private ECKey decodeBase64PK(String base64Priv) {
        byte[] privBytes = Base64.decodeBase64(base64Priv.getBytes());
        // Prepend a zero byte to make the biginteger unsigned
        byte[] appendZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
        return ECKey.fromPrivate(new BigInteger(appendZeroByte), true);
    }

    private ECKey decodeHexPK(String hex, boolean compressed) {
        byte[] privBytes = Hex.decode(hex);
        // Prepend a zero byte to make the biginteger unsigned
        byte[] appendZeroByte = ArrayUtils.addAll(new byte[1], privBytes);
        return ECKey.fromPrivate(new BigInteger(appendZeroByte), compressed);
    }

    private ECKey decodePK(String base58Priv) {
        return decodeBase58PK(base58Priv);
    }

    private byte[] hash(byte[] data, int offset, int len) {
        try {
            MessageDigest a = MessageDigest.getInstance("SHA-256");
            a.update(data, offset, len);
            return a.digest(a.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] hash(byte[] data) {
        return hash(data, 0, data.length);
    }

}