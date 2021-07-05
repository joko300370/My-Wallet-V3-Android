package info.blockchain.wallet.payload;

import com.blockchain.api.NonCustodialBitcoinService;
import com.blockchain.api.bitcoin.data.BalanceDto;
import com.blockchain.api.bitcoin.data.MultiAddress;
import info.blockchain.wallet.ImportedAddressHelper;
import info.blockchain.wallet.WalletApiMockedResponseTest;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.keys.SigningKey;
import info.blockchain.wallet.keys.SigningKeyImpl;
import info.blockchain.wallet.multiaddress.MultiAddressFactory;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.multiaddress.TransactionSummary.TransactionType;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.ImportedAddress;
import info.blockchain.wallet.payload.data.Wallet;
import info.blockchain.wallet.payload.data.XPub;
import info.blockchain.wallet.payload.data.XPubs;
import retrofit2.Call;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.crypto.DeterministicKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class PayloadManagerTest extends WalletApiMockedResponseTest {

    private final NonCustodialBitcoinService bitcoinApi = mock(NonCustodialBitcoinService.class);

    private PayloadManager payloadManager;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        payloadManager = new PayloadManager(
            walletApi,
            bitcoinApi,
            new MultiAddressFactory(bitcoinApi),
            new BalanceManagerBtc(bitcoinApi),
            new BalanceManagerBch(bitcoinApi)
        );
    }

    @Test
    public void getInstance() {
        assertNotNull(payloadManager);
    }

    @Test
    public void create_v3() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        mockInterceptor.setResponseStringList(responseList);

        mockEmptyBalance(bitcoinApi);

        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "SomePassword",
            false
        );

        Wallet walletBody = payloadManager
                .getPayload();

        assertEquals(36, walletBody.getGuid().length());//GUIDs are 36 in length
        assertEquals("My HDWallet", walletBody.getWalletBody().getAccounts().get(0).getLabel());

        assertEquals(1, walletBody.getWalletBody().getAccounts().size());

        assertEquals(5000, walletBody.getOptions().getPbkdf2Iterations());
        assertEquals(600000, walletBody.getOptions().getLogoutTime());
        assertEquals(10000, walletBody.getOptions().getFeePerKb());
    }

    @Test(expected = ServerConnectionException.class)
    public void create_ServerConnectionException() throws Exception {

        mockInterceptor.setResponseString("Save failed.");
        mockInterceptor.setResponseCode(500);
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "SomePassword",
            true
        );
    }

    @Test
    public void recoverFromMnemonic_v3() throws Exception {

        String mnemonic = "all all all all all all all all all all all all";

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("HDWallet successfully synced with server");
        mockInterceptor.setResponseStringList(responseList);

        //Responses for checking how many accounts to recover
        String balance1 = loadResourceContent("balance/wallet_all_balance_1.txt");
        Call<Map<String, BalanceDto>> balanceResponse1 = makeBalanceResponse(balance1);

        String balance2 = loadResourceContent("balance/wallet_all_balance_2.txt");
        Call<Map<String, BalanceDto>> balanceResponse2 = makeBalanceResponse(balance2);

        String balance3 = loadResourceContent("balance/wallet_all_balance_3.txt");
        Call<Map<String, BalanceDto>> balanceResponse3 = makeBalanceResponse(balance3);

        when(bitcoinApi.getBalance(any(String.class), any(), any(), any()))
            .thenReturn(balanceResponse1)
            .thenReturn(balanceResponse2)
            .thenReturn(balanceResponse3);

        payloadManager.recoverFromMnemonic(
            mnemonic,
            "My HDWallet",
            "name@email.com",
            "SomePassword",
            false
        );

        Wallet walletBody = payloadManager
                .getPayload();

        assertEquals(36, walletBody.getGuid().length());//GUIDs are 36 in length
        assertEquals("My HDWallet", walletBody.getWalletBody().getAccounts().get(0).getLabel());
        assertEquals("0660cc198330660cc198330660cc1983", walletBody.getWalletBody().getSeedHex());

        assertEquals(10, walletBody.getWalletBody().getAccounts().size());

        assertEquals(5000, walletBody.getOptions().getPbkdf2Iterations());
        assertEquals(600000, walletBody.getOptions().getLogoutTime());
        assertEquals(10000, walletBody.getOptions().getFeePerKb());
    }

    @Test(expected = ServerConnectionException.class)
    public void recoverFromMnemonic_ServerConnectionException_v3() throws Exception {

        String mnemonic = "all all all all all all all all all all all all";

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("Save failed");
        mockInterceptor.setResponseStringList(responseList);

        //checking if xpubs has txs succeeds but then savinf fails
        LinkedList<Integer> codes = new LinkedList<>();
        codes.add(500);
        mockInterceptor.setResponseCodeList(codes);

        //Responses for checking how many accounts to recover
        String balance1 = loadResourceContent("balance/wallet_all_balance_1.txt");
        Call<Map<String, BalanceDto>> balanceResponse1 = makeBalanceResponse(balance1);

        String balance2 = loadResourceContent("balance/wallet_all_balance_2.txt");
        Call<Map<String, BalanceDto>> balanceResponse2 = makeBalanceResponse(balance2);

        String balance3 = loadResourceContent("balance/wallet_all_balance_3.txt");
        Call<Map<String, BalanceDto>> balanceResponse3 = makeBalanceResponse(balance3);

        when(bitcoinApi.getBalance(any(String.class), any(), any(), any()))
            .thenReturn(balanceResponse1)
            .thenReturn(balanceResponse2)
            .thenReturn(balanceResponse3);

        payloadManager.recoverFromMnemonic(
            mnemonic,
            "My HDWallet",
            "name@email.com",
            "SomePassword",
            false
        );

        Wallet walletBody = payloadManager
                .getPayload();

        assertEquals(36, walletBody.getGuid().length());//GUIDs are 36 in length
        assertEquals("My HDWallet", walletBody.getWalletBody().getAccounts().get(0).getLabel());
        assertEquals("0660cc198330660cc198330660cc1983", walletBody.getWalletBody().getSeedHex());

        assertEquals(10, walletBody.getWalletBody().getAccounts().size());

        assertEquals(5000, walletBody.getOptions().getPbkdf2Iterations());
        assertEquals(600000, walletBody.getOptions().getLogoutTime());
        assertEquals(10000, walletBody.getOptions().getFeePerKb());
    }

    @Test(expected = UnsupportedVersionException.class)
    public void initializeAndDecrypt_unsupported_version_v4() throws Exception {
        String walletBase = loadResourceContent("wallet_v5_unsupported.txt");

        mockInterceptor.setResponseString(walletBase);
        payloadManager.initializeAndDecrypt(
            "any_shared_key",
            "any_guid",
            "SomeTestPassword",
            true
        );
    }

    @Test
    public void initializeAndDecrypt_v3() throws Exception {

        String walletBase = loadResourceContent("wallet_v3_3.txt");

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(walletBase);
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.initializeAndDecrypt(
            "any",
            "any",
            "SomeTestPassword",
            false
        );
    }

    @Test
    public void initializeAndDecrypt_v4() throws Exception {
        String walletBase = loadResourceContent("wallet_v4_encrypted.txt");

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(walletBase);
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.initializeAndDecrypt(
            "any",
            "any",
            "blockchain",
            true
        );
    }

    @Test(expected = InvalidCredentialsException.class)
    public void initializeAndDecrypt_invalidGuid() throws Exception {

        String walletBase = loadResourceContent("invalid_guid.txt");

        mockInterceptor.setResponseString(walletBase);
        mockInterceptor.setResponseCode(500);
        payloadManager.initializeAndDecrypt(
            "any",
            "any",
            "SomeTestPassword",
            false
        );
    }

    @Test(expected = HDWalletException.class)
    public void save_HDWalletException() throws Exception {
        //Nothing to save
        payloadManager.save();
    }

    @Test
    public void save_v3() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "SomePassword",
            false
        );

        mockInterceptor.setResponseString("MyWallet save successful.");
        payloadManager.save();
    }

    @Test
    public void save_v4() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "SomePassword",
            true
        );

        mockInterceptor.setResponseString("MyWallet save successful.");
        payloadManager.save();
    }

    @Test
    public void upgradeV2PayloadToV3() {
        // Tested in integration tests
    }

    @Test
    public void addAccount_v3() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "MyTestWallet",
            false
        );

        assertEquals(1, payloadManager.getPayload().getWalletBody().getAccounts().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addAccount("Some Label", null);
        assertEquals(2, payloadManager.getPayload().getWalletBody().getAccounts().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addAccount("Some Label", null);
        assertEquals(3, payloadManager.getPayload().getWalletBody().getAccounts().size());
    }

    @Test
    public void addLegacyAddress_v3() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "MyTestWallet",
            false
        );

        assertEquals(0, payloadManager.getPayload().getImportedAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addImportedAddress(ImportedAddressHelper.getImportedAddress());
        assertEquals(1, payloadManager.getPayload().getImportedAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addImportedAddress(ImportedAddressHelper.getImportedAddress());
        assertEquals(2, payloadManager.getPayload().getImportedAddressList().size());
    }

    @Test
    public void setKeyForLegacyAddress_v3() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);

        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "MyTestWallet",
            false
        );

        assertEquals(0, payloadManager.getPayload().getImportedAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");

        mockInterceptor.setResponseStringList(responseList);

        payloadManager.addImportedAddress(ImportedAddressHelper.getImportedAddress());
        assertEquals(1, payloadManager.getPayload().getImportedAddressList().size());

        ImportedAddress importedAddressBody = payloadManager.getPayload()
                .getImportedAddressList().get(0);

        SigningKey key = new SigningKeyImpl(
            DeterministicKey.fromPrivate(Base58.decode(importedAddressBody.getPrivateKey()))
        );

        importedAddressBody.setPrivateKey(null);
        mockInterceptor.setResponseString("MyWallet save successful.");
        payloadManager.setKeyForImportedAddress(key, null);
    }

    @Test
    public void setKeyForLegacyAddress_NoSuchAddressException() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "MyTestWallet",
            false
        );

        assertEquals(0, payloadManager.getPayload().getImportedAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addImportedAddress(ImportedAddressHelper.getImportedAddress());
        assertEquals(1, payloadManager.getPayload().getImportedAddressList().size());

        ImportedAddress existingImportedAddressBody = payloadManager.getPayload()
                .getImportedAddressList().get(0);

        //Try non matching ECKey
        SigningKey key = new SigningKeyImpl(new ECKey());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");

        mockInterceptor.setResponseStringList(responseList);

        ImportedAddress newlyAdded = payloadManager
                .setKeyForImportedAddress(key, null);

        //Ensure new address is created if no match found
        assertNotNull(newlyAdded);
        assertNotNull(newlyAdded.getPrivateKey());
        assertNotNull(newlyAdded.getAddress());
        assertNotEquals(existingImportedAddressBody.getPrivateKey(), newlyAdded.getPrivateKey());
        assertNotEquals(existingImportedAddressBody.getAddress(), newlyAdded.getAddress());
    }

    @Test
    public void setKeyForLegacyAddress_saveFail_revert() throws Exception {

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add("MyWallet save successful.");
        mockEmptyBalance(bitcoinApi);

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.create(
            "My HDWallet",
            "name@email.com",
            "MyTestWallet",
            false
        );

        assertEquals(0, payloadManager.getPayload().getImportedAddressList().size());

        responseList = new LinkedList<>();
        responseList.add("MyWallet save successful");

        mockInterceptor.setResponseStringList(responseList);
        payloadManager.addImportedAddress(ImportedAddressHelper.getImportedAddress());
        assertEquals(1, payloadManager.getPayload().getImportedAddressList().size());

        ImportedAddress importedAddressBody = payloadManager.getPayload()
                .getImportedAddressList().get(0);

        SigningKey key = new SigningKeyImpl(
            DeterministicKey.fromPrivate(Base58.decode(importedAddressBody.getPrivateKey()))
        );

        importedAddressBody.setPrivateKey(null);
        mockInterceptor.setResponseCode(500);
        mockInterceptor.setResponseString("Oops something went wrong");
        payloadManager.setKeyForImportedAddress(key, null);

        // Ensure private key reverted on save fail
        assertNull(importedAddressBody.getPrivateKey());
    }

    @Test
    public void getNextAddress_v3() throws Exception {

        String walletBase = loadResourceContent("wallet_v3_5.txt");

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(walletBase);
        mockInterceptor.setResponseStringList(responseList);
        mockEmptyBalance(bitcoinApi);

        String multi1 = loadResourceContent("multiaddress/wallet_v3_5_m1.txt");
        Call<MultiAddress> multiResponse1 = makeMultiAddressResponse(multi1);

        String multi2 = loadResourceContent("multiaddress/wallet_v3_5_m2.txt");
        Call<MultiAddress> multiResponse2 = makeMultiAddressResponse(multi2);

        String multi3 = loadResourceContent("multiaddress/wallet_v3_5_m3.txt");
        Call<MultiAddress> multiResponse3 = makeMultiAddressResponse(multi3);

        String multi4 = loadResourceContent("multiaddress/wallet_v3_5_m4.txt");
        Call<MultiAddress> multiResponse4 = makeMultiAddressResponse(multi4);

        when(bitcoinApi.getMultiAddress(
            any(String.class), any(), any(), any(String.class), any(), any(Integer.class), any(Integer.class)
        )).thenReturn(multiResponse1)
            .thenReturn(multiResponse2)
            .thenReturn(multiResponse3)
            .thenReturn(multiResponse4);

        payloadManager.initializeAndDecrypt(
            "06f6fa9c-d0fe-403d-815a-111ee26888e2",
            "4750d125-5344-4b79-9cf9-6e3c97bc9523",
            "MyTestWallet",
            false
        );

        Wallet wallet = payloadManager.getPayload();

        // Reserve an address to ensure it gets skipped
        Account account = wallet.getWalletBody().getAccounts().get(0);
        account.addAddressLabel(1, "Reserved");

        // set up indexes first
        payloadManager.getAccountTransactions(
            account.getXpubs().getDefault().getAddress(), 50, 0
        );

        // Next Receive
        String nextReceiveAddress = payloadManager.getNextReceiveAddress(account);
        assertEquals("1H9FdkaryqzB9xacDbJrcjXsJ9By4UVbQw", nextReceiveAddress);

        // Increment receive and check
        payloadManager.incrementNextReceiveAddress(account);
        nextReceiveAddress = payloadManager.getNextReceiveAddress(account);
        assertEquals("18DU2RjyadUmRK7sHTBHtbJx5VcwthHyF7", nextReceiveAddress);

        // Next Change
        String nextChangeAddress = payloadManager.getNextChangeAddress(account);
        assertEquals("1GEXfMa4SMh3iUZxP8HHQy7Wo3aqce72Nm", nextChangeAddress);

        // Increment Change and check
        payloadManager.incrementNextChangeAddress(account);
        nextChangeAddress = payloadManager.getNextChangeAddress(account);
        assertEquals("1NzpLHV6LLVFCYdYA5woYL9pHJ48KQJc9K", nextChangeAddress);
    }

     @Test
     public void balance() throws Exception {
        String walletBase = loadResourceContent("wallet_v3_6.txt");

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(walletBase);
        mockInterceptor.setResponseStringList(responseList);

        // Bitcoin
        String btcBalance = loadResourceContent("balance/wallet_v3_6_balance.txt");
        Call<Map<String, BalanceDto>> btcResponse = makeBalanceResponse(btcBalance);
        when(bitcoinApi.getBalance(
            eq("btc"),
            any(),
            any(),
            any())
        ).thenReturn(btcResponse);

        // Bitcoin Cash
        String bchBalance = loadResourceContent("balance/wallet_v3_6_balance.txt");
        Call<Map<String, BalanceDto>> bchResponse = makeBalanceResponse(bchBalance);
        when(bitcoinApi.getBalance(
            eq("bch"),
            any(),
            any(),
            any())
        ).thenReturn(bchResponse);

        payloadManager.initializeAndDecrypt(
            "any",
            "any",
            "MyTestWallet",
            false
        );

        // 'All' wallet balance and transactions
        assertEquals(743071, payloadManager.getWalletBalance().longValue());

        BigInteger balance = payloadManager.getImportedAddressesBalance();
        // Imported addresses consolidated
        assertEquals(137505, balance.longValue());

        // Account and address balances
        XPubs first = new XPubs(
            new XPub(
                "xpub6CdH6yzYXhTtR7UHJHtoTeWm3nbuyg9msj3rJvFnfMew9CBff6Rp62zdTrC57Spz4TpeRPL8m9xLiVaddpjEx4Dzidtk44rd4N2xu9XTrSV",
                XPub.Format.LEGACY
            )
        );
        assertEquals(
            BigInteger.valueOf(566349),
            payloadManager.getAddressBalance(first).toBigInteger()
        );

        XPubs second = new XPubs(
             new XPub(
                 "xpub6CdH6yzYXhTtTGPPL4Djjp1HqFmAPx4uyqoG6Ffz9nPysv8vR8t8PEJ3RGaSRwMm7kRZ3MAcKgB6u4g1znFo82j4q2hdShmDyw3zuMxhDSL",
                 XPub.Format.LEGACY
             )
        );
        assertEquals(
            BigInteger.valueOf(39217),
            payloadManager.getAddressBalance(second).toBigInteger()
        );

        XPubs third = new XPubs(
            new XPub(
                "189iKJLruPtUorasDuxmc6fMRVxz6zxpPS",
                XPub.Format.LEGACY
            )
        );
        assertEquals(
            BigInteger.valueOf(137505),
            payloadManager.getAddressBalance(third).toBigInteger()
        );
    }

    @Test
    public void getAccountTransactions() throws Exception {
        //guid 5350e5d5-bd65-456f-b150-e6cc089f0b26
        String walletBase = loadResourceContent("wallet_v3_6.txt");

        LinkedList<String> responseList = new LinkedList<>();
        responseList.add(walletBase);
        mockInterceptor.setResponseStringList(responseList);

        // Bitcoin
        String btcBalance = loadResourceContent("balance/wallet_v3_6_balance.txt");
        Call<Map<String, BalanceDto>> btcBalanceResponse = makeBalanceResponse(btcBalance);
        when(bitcoinApi.getBalance(eq("btc"), any(), any(), any()))
            .thenReturn(btcBalanceResponse);

        // Bitcoin Cash
        String bchBalance = loadResourceContent("balance/wallet_v3_6_balance.txt");
        Call<Map<String, BalanceDto>> bchBalanceResponse = makeBalanceResponse(bchBalance);
        when(bitcoinApi.getBalance(eq("bch"), any(), any(), any()))
            .thenReturn(bchBalanceResponse);

        // Bitcoin
        mockMultiAddress(bitcoinApi, "btc", "multiaddress/wallet_v3_6_m1.txt");
        // Bitcoin cash
        mockMultiAddress(bitcoinApi, "bch", "multiaddress/wallet_v3_6_m1.txt");

        payloadManager.initializeAndDecrypt(
            "0f28735d-0b89-405d-a40f-ee3e85c3c78c",
            "5350e5d5-bd65-456f-b150-e6cc089f0b26",
            "MyTestWallet",
            false
        );

        //Account 1
        String first = "xpub6CdH6yzYXhTtR7UHJHtoTeWm3nbuyg9msj3rJvFnfMew9CBff6Rp62zdTrC57Spz4TpeRPL8m9xLiVaddpjEx4Dzidtk44rd4N2xu9XTrSV";
        mockMultiAddress(bitcoinApi, "multiaddress/wallet_v3_6_m2.txt");

        List<TransactionSummary> transactionSummaries = payloadManager
            .getAccountTransactions(first, 50, 0);
        Assert.assertEquals(8, transactionSummaries.size());
        TransactionSummary summary = transactionSummaries.get(0);
        Assert.assertEquals(68563, summary.getTotal().longValue());
        Assert.assertEquals(TransactionType.TRANSFERRED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("125QEfWq3eKzAQQHeqcMcDMeZGm13hVRvU"));//My Bitcoin Account
        Assert.assertEquals(2, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("1Nm1yxXCTodAkQ9RAEquVdSneJGeubqeTw"));//Savings account
        Assert.assertTrue(summary.getOutputsMap().containsKey("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));

        summary = transactionSummaries.get(1);
        Assert.assertEquals(138068, summary.getTotal().longValue());
        Assert.assertEquals(TransactionType.SENT, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("1CQpuTQrJQLW6PEar17zsd9EV14cZknqWJ"));//My Bitcoin Wallet
        Assert.assertEquals(2, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("1LQwNvEMnYjNCNxeUJzDfD8mcSqhm2ouPp"));
        Assert.assertTrue(summary.getOutputsMap().containsKey("1AdTcerDBY735kDhQWit5Scroae6piQ2yw"));

        summary = transactionSummaries.get(2);
        Assert.assertEquals(800100, summary.getTotal().longValue());
        Assert.assertEquals(TransactionSummary.TransactionType.RECEIVED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("19CMnkUgBnTBNiTWXwoZr6Gb3aeXKHvuGG"));
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("1CQpuTQrJQLW6PEar17zsd9EV14cZknqWJ"));//My Bitcoin Wallet

        summary = transactionSummaries.get(3);
        Assert.assertEquals(35194, summary.getTotal().longValue());
        Assert.assertEquals(TransactionSummary.TransactionType.SENT, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("15HjFY96ZANBkN5kvPRgrXH93jnntqs32n"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("1PQ9ZYhv9PwbWQQN74XRqUCjC32JrkyzB9"));

        summary = transactionSummaries.get(4);
        Assert.assertEquals(98326, summary.getTotal().longValue());
        Assert.assertEquals(TransactionType.TRANSFERRED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));

        summary = transactionSummaries.get(5);
        Assert.assertEquals(160640, summary.getTotal().longValue());
        Assert.assertEquals(TransactionType.RECEIVED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("1BZe6YLaf2HiwJdnBbLyKWAqNia7foVe1w"));
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ"));//My Bitcoin Wallet

        summary = transactionSummaries.get(6);
        Assert.assertEquals(9833, summary.getTotal().longValue());
        Assert.assertEquals(TransactionSummary.TransactionType.TRANSFERRED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("1AtunWT3F6WvQc3aaPuPbNGeBpVF3ZPM5r"));//Savings account

        summary = transactionSummaries.get(7);
        Assert.assertEquals(40160, summary.getTotal().longValue());
        Assert.assertEquals(TransactionType.RECEIVED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("1Baa1cjB1CyBVSjw8SkFZ2YBuiwKnKLXhe"));
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur"));//My Bitcoin Wallet

        //Account 2
        String second = "xpub6CdH6yzYXhTtTGPPL4Djjp1HqFmAPx4uyqoG6Ffz9nPysv8vR8t8PEJ3RGaSRwMm7kRZ3MAcKgB6u4g1znFo82j4q2hdShmDyw3zuMxhDSL";
        mockMultiAddress(bitcoinApi, "multiaddress/wallet_v3_6_m3.txt");

        transactionSummaries = payloadManager.getAccountTransactions(second, 50, 0);
        Assert.assertEquals(2, transactionSummaries.size());
        summary = transactionSummaries.get(0);
        Assert.assertEquals(68563, summary.getTotal().longValue());
        Assert.assertEquals(TransactionType.TRANSFERRED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("125QEfWq3eKzAQQHeqcMcDMeZGm13hVRvU"));//My Bitcoin Wallet
        Assert.assertEquals(2, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("1Nm1yxXCTodAkQ9RAEquVdSneJGeubqeTw"));//Savings account
        Assert.assertTrue(summary.getOutputsMap().containsKey("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));

        summary = transactionSummaries.get(1);
        Assert.assertEquals(9833, summary.getTotal().longValue());
        Assert.assertEquals(TransactionSummary.TransactionType.TRANSFERRED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("17ijgwpGsVQRzMjsdAfdmeP53kpw9yvXur"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("1AtunWT3F6WvQc3aaPuPbNGeBpVF3ZPM5r"));//Savings account

        //Imported addresses (consolidated)
        mockMultiAddress(bitcoinApi, "multiaddress/wallet_v3_6_m1.txt");
        transactionSummaries = payloadManager.getImportedAddressesTransactions(50, 0);

        Assert.assertEquals(2, transactionSummaries.size());

        summary = transactionSummaries.get(0);
        Assert.assertEquals(2, transactionSummaries.size());
        Assert.assertEquals(68563, summary.getTotal().longValue());
        Assert.assertEquals(TransactionType.TRANSFERRED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("125QEfWq3eKzAQQHeqcMcDMeZGm13hVRvU"));//My Bitcoin Wallet
        Assert.assertEquals(2, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("1Nm1yxXCTodAkQ9RAEquVdSneJGeubqeTw"));//Savings account
        Assert.assertTrue(summary.getOutputsMap().containsKey("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));

        summary = transactionSummaries.get(1);
        Assert.assertEquals(98326, summary.getTotal().longValue());
        Assert.assertEquals(TransactionSummary.TransactionType.TRANSFERRED, summary.getTransactionType());
        Assert.assertEquals(1, summary.getInputsMap().size());
        Assert.assertTrue(summary.getInputsMap().containsKey("1Peysd3qYDe35yNp6KB1ZkbVYHr42JT9zZ"));//My Bitcoin Wallet
        Assert.assertEquals(1, summary.getOutputsMap().size());
        Assert.assertTrue(summary.getOutputsMap().containsKey("189iKJLruPtUorasDuxmc6fMRVxz6zxpPS"));
    }
}