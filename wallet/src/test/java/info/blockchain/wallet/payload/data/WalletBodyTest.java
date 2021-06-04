package info.blockchain.wallet.payload.data;

import com.google.common.collect.BiMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import info.blockchain.api.BitcoinApi;
import info.blockchain.api.bitcoin.data.BalanceDto;
import info.blockchain.wallet.WalletApiMockedResponseTest;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.keys.SigningKey;
import info.blockchain.wallet.payload.model.Utxo;
import info.blockchain.wallet.payment.OutputType;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;
import info.blockchain.wallet.util.LoaderUtilKt;
import retrofit2.Call;

import org.bitcoinj.core.Base58;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WalletBodyTest extends WalletApiMockedResponseTest {

    private final ObjectMapper mapperV3 = WalletWrapper.getMapperForVersion(WalletWrapper.V3);
    private final BitcoinApi bitcoinApi = mock(BitcoinApi.class);

    @Test
    public void fromJson_1() throws Exception {
        String body = loadResourceContent("wallet_body_1.txt");

        Wallet wallet = Wallet.fromJson(body);
        WalletBody walletBody = wallet.getWalletBody();

        assertEquals(68, walletBody.getAccounts().size());
        assertEquals("i3gtswW35zfbS/23fnh3IzKzcrpD04Tp+zeKbj++rODMOGRMO1aMQukwE3Q+63ds8pUMzBFnzomkjntprhisrQ==", walletBody.getSeedHex());
        assertEquals("", walletBody.getPassphrase());
        assertTrue(walletBody.isMnemonicVerified());
        assertEquals(0, walletBody.getDefaultAccountIdx());
    }

    @Test
    public void fromJson_2() throws Exception {
        String body = loadResourceContent("wallet_body_2.txt");

        Wallet wallet = Wallet.fromJson(body);
        Assert.assertEquals(0, wallet.getWalletBodies().size());
    }

    @Test
    public void fromJson_6() throws Exception {
        String body = loadResourceContent("wallet_body_6.txt");

        Wallet wallet = Wallet.fromJson(body);
        WalletBody walletBody = wallet.getWalletBody();

        assertEquals(1, walletBody.getAccounts().size());
        assertEquals("bfb70136ef9f973e866dff00817b8070", walletBody.getSeedHex());
        assertEquals("somePassPhrase", walletBody.getPassphrase());
        assertFalse(walletBody.isMnemonicVerified());
        assertEquals(2, walletBody.getDefaultAccountIdx());
    }

    @Test
    public void testToJSON() throws Exception {

        //Ensure toJson doesn't write any unintended fields
        String body = loadResourceContent("wallet_body_1.txt");

        Wallet wallet = Wallet.fromJson(body);
        WalletBody walletBody = wallet.getWalletBody();
        String jsonString = walletBody.toJson(mapperV3);

        JSONObject jsonObject = new JSONObject(jsonString);
        assertEquals(5, jsonObject.keySet().size());
    }

    @Test
    public void recoverFromMnemonic() throws Exception {

        String mnemonic = "all all all all all all all all all all all all";

        LinkedList<String> xpubs = new LinkedList<>();
        xpubs.add("HDWallet successfully synced with server");
        mockInterceptor.setResponseStringList(xpubs);

        Call<Map<String, BalanceDto>> balanceResponse1 = makeBalanceResponse(recoverBalance_3);
        Call<Map<String, BalanceDto>> balanceResponse2 = makeBalanceResponse(recoverBalance_4);
        Call<Map<String, BalanceDto>> balanceResponse3 = makeBalanceResponse(recoverBalance_5);
        when(bitcoinApi.getBalance(any(), any(), any(), any()))
            .thenReturn(balanceResponse1)
            .thenReturn(balanceResponse2)
            .thenReturn(balanceResponse3);

        String label = "HDAccount 1";
        WalletBody walletBody = WalletBody.recoverFromMnemonic(mnemonic, label, bitcoinApi, false);

        assertEquals(walletBody.getAccounts().get(0).getLabel(), label);
        assertEquals(10, walletBody.getAccounts().size());
    }

    @Test
    public void recoverFromMnemonic_passphrase() throws Exception {

        String mnemonic = "all all all all all all all all all all all all";

        Call<Map<String, BalanceDto>> balanceResponse1 = makeBalanceResponse(recoverBalance_1);
        Call<Map<String, BalanceDto>> balanceResponse2 = makeBalanceResponse(recoverBalance_2);
        when(bitcoinApi.getBalance(any(), any(), any(), any()))
            .thenReturn(balanceResponse1)
            .thenReturn(balanceResponse2);

        LinkedList<String> xpubs = new LinkedList<>();
        xpubs.add("HDWallet successfully synced with server");
        mockInterceptor.setResponseStringList(xpubs);

        String label = "HDAccount 1";
        WalletBody walletBody = WalletBody.recoverFromMnemonic(
            mnemonic,
            "somePassphrase",
            label,
            bitcoinApi,
            false
        );

        assertEquals(walletBody.getAccounts().get(0).getLabel(), label);
        assertEquals(1, walletBody.getAccounts().size());
    }

    @Test
    public void getHDKeysForSigning() throws Exception {
        String body = loadResourceContent("hd_wallet_body_1.txt");
        WalletBody walletBody = WalletBody.fromJson(body, mapperV3);

        walletBody.decryptHDWallet(
            "hello",
            "d14f3d2c-f883-40da-87e2-c8448521ee64",
            5000
        );

        // Available unspents: [8290, 4616, 5860, 3784, 2290, 13990, 8141]
        body = loadResourceContent("wallet_body_1_account1_unspent.txt");
        final List<Utxo> unspentOutputs = LoaderUtilKt.parseUnspentOutputsAsUtxoList(body);

        final BitcoinApi mockApi = mock(BitcoinApi.class);
        Payment payment = new Payment(mockApi);

        long spendAmount = 40108;

        SpendableUnspentOutputs paymentBundle = payment
            .getSpendableCoins(
                unspentOutputs,
                OutputType.P2PKH,
                OutputType.P2PKH,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(1000L),
                false
            );

        assertEquals(789, paymentBundle.getAbsoluteFee().longValue());

        List<SigningKey> keyList = walletBody
                .getHDKeysForSigning(walletBody.getAccount(0), paymentBundle);

        //Contains 5 matching keys for signing
        assertEquals(5, keyList.size());
    }

    @Test
    public void getMasterKey() throws Exception {
        String body = loadResourceContent("hd_wallet_body_2.txt");

        //HD seed is encrypted, only xpubs available
        WalletBody walletBody = WalletBody.fromJson(body, mapperV3);

        assertEquals("5F8YjqPVSq9HnXBrDxUmUoDKXsya8q5LGHnAopadTRYE",
                Base58.encode(walletBody.getMasterKey().toDeterministicKey().getPrivKeyBytes()));
    }

    @Test(expected = HDWalletException.class)
    public void getMasterKey_DecryptionException() throws Exception {
        String body = loadResourceContent("hd_wallet_body_1.txt");
        WalletBody walletBody = WalletBody.fromJson(body, mapperV3);

        walletBody.getMasterKey();
    }

    @Test
    public void getMnemonic() throws Exception {
        String body = loadResourceContent("hd_wallet_body_2.txt");
        WalletBody walletBody = WalletBody.fromJson(body, mapperV3);

        assertEquals("[car, region, outdoor, punch, poverty, shadow, insane, claim, one, whisper, learn, alert]",
                     walletBody.getMnemonic().toString());
    }

    @Test(expected = HDWalletException.class)
    public void getMnemonic_DecryptionException() throws Exception {
        String body = loadResourceContent("hd_wallet_body_1.txt");
        WalletBody walletBody = WalletBody.fromJson(body, mapperV3);

        walletBody.getMnemonic();
    }

    @Test
    public void getXpubToAccountIndexMap() throws Exception {
        String body = loadResourceContent("hd_wallet_body_1.txt");
        WalletBody walletBody = WalletBody.fromJson(body, mapperV3);

        BiMap<String, Integer> map = walletBody.getXpubToAccountIndexMap();

        assertEquals(0, map.get("xpub6DEe2bJAU7GbP12FBdsBckUkGPzQKMnZXaF2ajz2NCFfYJMEzb5G3oGwYrE6WQjnjhLeB6TgVudV3B9kKtpQmYeBJZLRNyXCobPht2jPUBm").intValue());
        assertEquals(1, map.get("xpub6DEe2bJAU7GbQcGHvqgJ4T6pzZUU8j1WqLPyVtaWJFewfjChAKtUX5uRza9rabc6rAgFhXptveBmaoy7ptVGgbYT8KKaJ9E7wmyj5o4aqvr").intValue());
        assertEquals(2, map.get("xpub6DEe2bJAU7GbUw3HDGPUY9c77mUcP9xvAWEhx9GReuJM9gppeGxHqBcaYAfrsyY8R6cfVRsuFhi2PokQFYLEQBVpM8p4MTLzEHpVu4SWq9a").intValue());
        assertEquals(3, map.get("xpub6DEe2bJAU7GbW4d8d8Cfckg8kbHinDUQYHvXk3AobXNDYwGhaKZ1wZxGCBq67RiYzT3UuQjS3Jy3SGM3b9wz7aHVipE3Bg1HXhLguCgoALJ").intValue());
        assertEquals(4, map.get("xpub6DEe2bJAU7GbYjCHygUwVDJYv5fjCUyQ1AHvkM1ecRL2PZ7vYv9a5iRiHjxmRgi3auyaA9NSAw88VwHm4hvw4C8zLbuFjNBcw2Cx7Ymq5zk").intValue());
    }

    private final String recoverBalance_1 ="{\n"
          + "    \"xpub6BvvF1nwmp51CapAefmDYrKWeGC2Y96TcGtB6BTfiTJezHLjBxgsWdKRvWWChGAhWPjdRjSUsDeEgnSar2xjenixNArkytRU2heAWr3HmQ5\": {\n"
          + "        \"final_balance\": 0,\n"
          + "        \"n_tx\": 0,\n"
          + "        \"total_received\": 0\n"
          + "    },\n"
          + "    \"xpub6BvvF1nwmp51MoY8LZ6RqZ6xc9PE5mASd2jpTGARe61HwscsK1tVLF5xJFf1QKnNP2T5YAKDyrK2WGAZS1p5aD9EuYhqC53EFYC7UpnYnz5\": {\n"
          + "        \"final_balance\": 0,\n"
          + "        \"n_tx\": 0,\n"
          + "        \"total_received\": 0\n"
          + "    },\n"
          + "    \"xpub6BvvF1nwmp51G75mNUQLmQZm8r3CXBFJChJt6fvoURVS1Nz1jCVN6Nf5nMUfDuT53X8uAXjAX3eHJRPWcpDYMVPwzv1hpMAJKvKQVMefiRJ\": {\n"
          + "        \"final_balance\": 0,\n"
          + "        \"n_tx\": 0,\n"
          + "        \"total_received\": 0\n"
          + "    },\n"
          + "    \"xpub6BvvF1nwmp51Gpj1eAidXtRpoq6AUwzZ3L2uv49oWnMQiW9KZ42UYrrM3fHoCyidHzAY14GRrZ8fSS2JZroAEXD5bqiLvjGDNGYbuMCa6vi\": {\n"
          + "        \"final_balance\": 0,\n"
          + "        \"n_tx\": 0,\n"
          + "        \"total_received\": 0\n"
          + "    },\n"
          + "    \"xpub6BvvF1nwmp51A6sqV9YTcTGKmWorM48PmZdSXEYgG9pQffDsUavLdPz14RX5tTghiGfApJLqYdv9ramj9agke9o1uKYLesYp6rPKExDmCFX\": {\n"
          + "        \"final_balance\": 0,\n"
          + "        \"n_tx\": 0,\n"
          + "        \"total_received\": 0\n"
          + "    }\n"
          + "}";

        private final String recoverBalance_2 = "{\n"
            + "    \"xpub6BvvF1nwmp51N9UVeokUscF6vwT8TN35TSxQmW8GSJPgj7NQwUKrR9rZvug2KLeZf4SnviBmmqgtaWJstuMT18bcNpPttrhrBEWptdYHGcF\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BvvF1nwmp51XdWV7XBeBgcErsvkJ6f79vzppG278gJ4MPfJ9G5mPqaS8w1zWVyhVrXj3nnr2BSaLcNxHVM548go7UvS3MV1uynsi813YrY\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BvvF1nwmp51fx7JnC4c4546HgVt5PJCRf1X2VWmVCJXPztdRxuhUpJYyjzkJQmifMrPAZXBze28og6myNAZSa29PmUXEiTrTrRDUWJhBkm\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BvvF1nwmp51iXFfchx7ov6daSQtm5VafaN47KDvEirVgM7HoYayxndrmpstWt6pWRNKuVUFPjwFxCVPsM3EsXwn5GDosH7BeeMCzhv4tP6\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BvvF1nwmp51e2qGQXDVUX6VMivMHTCFkj9PmDbNQTyedyeansUT6LVrCUxZ2XGtS9e8KybZZ91mDxdPY3FpWE1HRh5vL2RKXc68swdN7MG\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BvvF1nwmp51V9WWEKjQRhqKYmuHj5gkjCr45c4BUAiLkS5y33zcQT39ZnXztG4NSwF98mo4DP1rTyugJsLbFKxDNQCXJegHoULicosyjMG\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BvvF1nwmp51YapEhZtHpMfd1tfjMS2RSd8twpbE8S2aDTxkusxbuwYsJwS1FXPirT3onMXAZvRdMaKjbxSAKDwW6gYc6DgbsWvMcwmC96z\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BvvF1nwmp51nq8joLkkUMDg8x3rFxnSexm5JhXA8Tev9gNW6mk9hp1Lky2aA1HoNVwjyJTubwtYXt7kgehoyzdPitB3osGzKupHhfR568E\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BvvF1nwmp51RG4LdnpS4wW4q7hyjPfujhQ6iWQDKdQPBvjaYQz9CbJD6zYae1M9FfEFCCb2CyjcwPKj2qzQAyYNq3XM5rn1XNanTB8Mc3p\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BvvF1nwmp51akH4ab1pd87W7yxes74P66SSCJhYQAZFKdT1qtSN5dsPfyhnYphP3Bu6EXfz6waPAWkSfzPVsHeSow3yzH9B5SSVbBivLRT\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    }\n"
            + "}";

    private final String recoverBalance_3 = "{\n"
                    + "    \"xpub6BiVtCpG9fQQ77Qr7WArXSG3yWYm2bkRYpoSYtRkVEAk5nrcULBG8AeRYMMKVUXAsNeXdR7TGuL6SkUc4RF2YC7X4afLyZrT9NrrUFyotkH\": {\n"
                    + "        \"final_balance\": 0,\n"
                    + "        \"n_tx\": 23,\n"
                    + "        \"total_received\": 22154257\n"
                    + "    },\n"
                    + "    \"xpub6BiVtCpG9fQQ4xJHzNkdmqspAeMdBTDFZ2kYM39RzDYMAcb4wtkWZNSu7k3BbJgoPgTzx62G69mBiUjDnD3EJrTA5ZYZg4vfz1YWcGBnX2x\": {\n"
                    + "        \"final_balance\": 0,\n"
                    + "        \"n_tx\": 20,\n"
                    + "        \"total_received\": 18192818\n"
                    + "    },\n"
                    + "    \"xpub6BiVtCpG9fQQ1EW99bMSYwySbPWvzTFRQZCFgTmV3samLSZAYU7C3f4Je9vkNh7h1GAWi5Fn93BwoGBy9EAXbWTTgTnVKAbthHpxM1fXVRL\": {\n"
                    + "        \"final_balance\": 0,\n"
                    + "        \"n_tx\": 36,\n"
                    + "        \"total_received\": 12099702\n"
                    + "    },\n"
                    + "    \"xpub6BiVtCpG9fQQ8pVjVF7jm3kLahkNbQRkWGUvzsKQpXWYvhYD4d4UDADxZUL4xp9UwsDT5YgwNKofTWRtwJgnHkbNxuzLDho4mxfS9KLesGP\": {\n"
                    + "        \"final_balance\": 0,\n"
                    + "        \"n_tx\": 20,\n"
                    + "        \"total_received\": 11963629\n"
                    + "    },\n"
                    + "    \"xpub6BiVtCpG9fQQCgxA541qm9qZ9VrGLScde4zsAMj2d15ewiMysCAnbgvSDSZXhFUdsyA2BfzzMrMFJbC4VSkXbzrXLZRitAmUVURmivxxqMJ\": {\n"
                    + "        \"final_balance\": 0,\n"
                    + "        \"n_tx\": 20,\n"
                    + "        \"total_received\": 15137242\n"
                    + "    }\n"
                    + "}";

    private final String recoverBalance_4 = "{\n"
            + "    \"xpub6BiVtCpG9fQQGq7bXBjjf5zyguEXHrmxDu4t7pdTFUtDWD5epi4ecKmWBTMHvPQtRmQnby8gET7ArTzxjL4SNYdD2RYSdjk7fwYeEDMzkce\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 2,\n"
            + "        \"total_received\": 4242108\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQUGTtsZvQdWaXHNmNd1Rzo8C8kfhzJQsLw1nijQ3HNSGMrLyMygHMvRTv9SL7o29hMPrtC32vfoW3NkGjCETYZpH4s6isLX3\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQNBuKZoKzhzmENDKdCeXQsNVPF2Ynt8rhyYznmPURQNDmnNnX9SYahZ1DVTaNtsh3pJ4b2jKvsZhpv2oVj76YETCGztKJ3LM\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 9,\n"
            + "        \"total_received\": 4346308\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQXPkGarFwhcPbhRN5TEfpCfHPe37cdG7iYgYMjt85hZ1HHPAbqYneHs4bZtJ47dGRncD2z5q1aix83zgjEwQ3KkNuyyK8eFx\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQdziwDT8EyYPLnuXs14FwNZqGHhMzPDMdLKc97agwFKMb3FfiweRsnqkeHYymF31RJc9EozZxHUSHzkjQ2H9SKGe7GmRDGPM\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQV7PkQJFKHKs2BQVYJ2k7bF8E2dTtqb61viou61EaAm2McoArGW2pjfe8wxLmESVEcDo4pHLLe2KZkLthXBXBR8rvem35ZnN\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQDvwDNekCEzAr3gYcoGXEF27bMwSBsCVP3bJYdUZ6m3jhv9vSG7hVxff3VEfnfK4fcMr2YRwfTfHcJwM4ioS6Eiwnrm1wcuf\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 9,\n"
            + "        \"total_received\": 4785453\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQamLupKW3xzULucDGpsp3KgKfVdfmP65MJPJ6bU7UuaKBZeUYQW58hU5iAKEdMJHeQNsMEquLMf8he4M6wZ3fA6P1vAHGdhH\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQJXDcLwQU1cXECNqaGYb3nNSu1ZEuwFKMXjDbCni6eMhN6rFkdxQsgF1amKAqeLSN63zrYPKJ3GU2ppowBWZSdGBk7QUxgLV\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 4,\n"
            + "        \"total_received\": 4285772\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQR6cSuFeDaSvCDgNvNme499JUGX4RHDiZVWwZy9NwNieWKXHLe8XRbdrEmY87aqztBCbRJkXWV7VJB96XBT5cpkqYMHwvLWB\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    }\n"
            + "}";

    private final String recoverBalance_5 = "{\n"
            + "    \"xpub6BiVtCpG9fQR4Bp1D4k4P1a48uHPJPtHmnHjrvwpZgg47sJfg9e5wqjEVZs1YdhR3EsfWo16qPcA7fsk6Hzr5e8VAjNbgmVy67DGkoGJfv4\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQmHu21ccttmBpbz5uT8zUQ5nXoTBkMbJBAZ35KTZ9bCi6ChqHZFUc6D2UnrZwLWZZqye9GDtRDw8T9kxEt13fN2UVFBgBEzJ\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQRBDv37eyBUDVV4Wpp5w5G1ZdCBwv3cBEUor71SXG48SqYtKccateyEfjoRwDYSojk8XDkBaK6HrGt4A68oJzb536gPQG5c36\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQRCXQmCHhnL9AqkNyVEesEsP7xunYZrtboZpqUEne9MQqGc9dZDryV27179yfD9rRQsxErUwwEgDKz1EJLS9i1sh6XPG8yoH6\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQRSxhKnoTeamr7c6LnWkFqUASymUyfga1r2sjttqqyjXk5N5ec36HfD1XL2475EwMsN3pSyvDhuqU7v4Rv6mryVNGjSrhzFc1\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQgFPkux7cvyVgyAwWWYRG935BDBYTcXEJGnr1H3vfTfaA8Zg2pRPKxLPKRSY9ztrirhhD2Ud4KKeR11oWpomvNUY8jgXcSWN\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQrFR7q8AQCrDwH5ZiPv9ozL6xg4eiXCGCTDYmw4uZkZgYDfaS42jeA2tjMWQ2vzaHtRjFTTMYCYpGEsgFku324rRdMDckp1i\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQRPvqDD7BCc9fCy1WhR7h7N3QnVPHW3QrhmXXrzpUnWogvr2x6ekxX1jCCvyDo11jz75zzC6AS2TU5DE7CcV6xaLt1X8h3iPe\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQwh4qKjcJnN2NdZWDqKjKsAMFMbmrsgAKSQBhZDc5cubR5ZhoBt51jjZY79NYpVpRJBWJAHsnaXtT6uqQ6Ps9d1YkDpY7QH2\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQhRocxvUvFdoG8FJqm8PYPjgGKrryMpRDCcHjzXkYbjuEDFdPRKUv7jJ5H2FUuCFLY2FBNA7gosDpf36coCvBtc48DkoqX5M\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQRVMuTAkEfkJmL7vsFbw8cRtexWLnG98KSnHy4akwmQrLUnszJL4TTvuvKgtCGr6s7fS8py5ZaGfBupSxy8qynUUg9ynP66L8\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQR6krjJPX1Gi9R5aPzGHCgjL6CEEeYRemjYPuTocawpXgJLMNbJvrToG94hVpT4RxakNde8UEnePB2rJKheeUd89dvaHK7cnT\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQRLT8VREWcckFJD7syF6hF6W7PKcbyjKpQdh2Aj46zm2nXaLRJmTak6E9VBq2c5ZDMuJNU4dvkEsRfZXXQL7Agxwy2pURfWBA\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQxjv3ciBgJguCKHLN25rUzZDnDPRLssQVqtZJTgRFPNMXpkE5RUSBxiBx4twZ9ecfHtNUnxvsURC9whqRazKa6ziYSC9rvEj\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQR2aaiVePK2BVjSwVa8uNe3uKTKZEtagRqarGM4BHcFV7K6KS5cUU5DkFJLDvwH289DskNRysWZZtmBKZb4fTCXQgNjtrwsun\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQRGMmvS6eCvosDg67t6hwX5pp3cLLNUvsoTHHpK2Yf6cYRJgK6XYVzrHHV5YL42kdUaz1oQdbyTdJCfpiehde5r6SVJHUHs8A\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQprM8465d1xUgyrh1KY7UrkPa9f5pt58znTb4rf1bgvvGgxj3ASXvcn9yyGqinFcV7n5LpW5vAg95k99zYZ5BXFQS4J7yZw9\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQRJVkYhiHDzuZyDeCmch3agVDwV9ryfx87gPBXa7LbSHWM9sn9aFmSgouwDLbH2hesXt8VZUrzKdMdMbJ3ayxbfgygqZqRrsV\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQRRqpQafDuYL8X1N2ffjW1UhUZ2TU1H8dJhprANMLAuF2bqrK3iPWxHzEvFWQaEga3wkq956WVfZBL1fgNWMyK1YMkQbeTnBt\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    },\n"
            + "    \"xpub6BiVtCpG9fQQuhhzr1YNBXEPPCLBYVu6FpSYYpeBHaSVBDhavkcTZZr7ZGPULBUzAQ8QaxYtG5U1KyBq61cAP6VedtPXYvXu1ch4rNLDZYd\": {\n"
            + "        \"final_balance\": 0,\n"
            + "        \"n_tx\": 0,\n"
            + "        \"total_received\": 0\n"
            + "    }\n"
            + "}";
}

