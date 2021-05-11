package info.blockchain.wallet.payment;

import info.blockchain.api.BitcoinApi;
import info.blockchain.api.bitcoin.data.UnspentOutputsDto;
import info.blockchain.wallet.MockedResponseTest;
import info.blockchain.wallet.keys.SigningKey;
import info.blockchain.wallet.payload.data.XPub;
import info.blockchain.wallet.payload.data.XPubs;
import info.blockchain.wallet.payload.model.Utxo;
import info.blockchain.wallet.test_data.UnspentTestData;
import info.blockchain.wallet.util.LoaderUtilKt;
import info.blockchain.wallet.util.PrivateKeyFactory;
import io.reactivex.Single;
import kotlin.Pair;
import okhttp3.ResponseBody;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PaymentTest extends MockedResponseTest {

    final BitcoinApi mockApi = mock(BitcoinApi.class);
    final private Payment subject = new Payment(mockApi);
    final private OutputType targetOutputType = OutputType.P2PKH;
    final private OutputType changeOutputType = OutputType.P2PKH;

    final List<Utxo> singleInput = LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.singleInput);
    final List<Utxo> doubleInput = LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.doubleInput);
    final List<OutputType> singleOutput = Stream.of(OutputType.P2PKH).collect(Collectors.toList());
    final List<OutputType> doubleOutput = Stream.of(OutputType.P2PKH, OutputType.P2PKH).collect(Collectors.toList());

    @Test
    public void estimatedFee() {
        ArrayList<int[]> cases = new ArrayList<>();
        //new int[]{[--inputs--],[--outputs--],[--feePrKb--],[--absoluteFee--]}
        cases.add(new int[]{1, 1, 0, 0});
        cases.add(new int[]{1, 2, 0, 0});
        cases.add(new int[]{2, 1, 0, 0});
        cases.add(new int[]{1, 1, 30000, 5760});
        cases.add(new int[]{1, 2, 30000, 6780});
        cases.add(new int[]{2, 1, 30000, 10200});
        cases.add(new int[]{3, 3, 30000, 16680});
        cases.add(new int[]{5, 10, 30000, 32701});

        Utxo utxo = new Utxo(BigInteger.ZERO, "", "", 0, true, null, false, false);

        for (int[] aCase : cases) {
            ArrayList<Utxo> inputs = new ArrayList<>(Collections.nCopies(aCase[0], utxo));
            ArrayList<OutputType> outputs = new ArrayList<>(Collections.nCopies(aCase[1], OutputType.P2PKH));

            BigInteger absoluteFee = subject.estimatedFee(inputs, outputs, BigInteger.valueOf(aCase[2]));
            assert (aCase[3] == absoluteFee.longValue());
        }
    }

    @Test
    public void estimatedSize() {
        assertEquals(192, subject.estimatedSize(singleInput, singleOutput), 0);
        assertEquals(226, subject.estimatedSize(singleInput, doubleOutput), 0);
        assertEquals(340, subject.estimatedSize(doubleInput, singleOutput), 0);
        assertEquals(374, subject.estimatedSize(doubleInput, doubleOutput), 0);
    }

    @Test
    public void isAdequateFee() {
        assertTrue(subject.isAdequateFee(singleInput, singleOutput, BigInteger.valueOf(193)));
        assertFalse(subject.isAdequateFee(singleInput, singleOutput, BigInteger.valueOf(192)));
    }

    private long calculateFee(int outputs, int inputs, BigInteger feePerKb) {
        // Manually calculated fee
        long size = (outputs * 34) + (inputs * 149) + 10; //36840L
        double txBytes = ((double) size / 1000.0);
        return (long) Math.ceil(feePerKb.doubleValue() * txBytes);
    }

    @Test
    public void getUnspentCallsTheApiForOnlyLegacy()  {
        String dummyAddress = "Dummy Address";
        XPubs dummyArg = new XPubs(
            new XPub(dummyAddress, XPub.Format.LEGACY)
        );
        List<String> expectedLegacyAddresses = new ArrayList<>();
        expectedLegacyAddresses.add(dummyAddress);
        List<String> expectedSegwitAddresses = new ArrayList<>();

        final UnspentOutputsDto result = new UnspentOutputsDto(
            null,
            new ArrayList<>()
        );

        when(mockApi.getUnspentOutputs(
            BitcoinApi.BITCOIN,
            expectedLegacyAddresses,
            expectedSegwitAddresses,
            null,
            null)
        ).thenReturn(Single.just(result));

       subject.getUnspentBtcCoins(dummyArg)
           .test()
           .assertComplete();

        verify(mockApi).getUnspentOutputs(
            BitcoinApi.BITCOIN,
            expectedLegacyAddresses,
            expectedSegwitAddresses,
            null,
            null
        );
        verifyNoMoreInteractions(mockApi);
    }

    @Test
    public void getUnspentCallsTheApiForLegacyAndSegwit()  {
        String legacyAddress = "Legacy Address";
        String segwitAddress = "Segwit Address";
        ArrayList<XPub> xpubs = new ArrayList<>();
        xpubs.add(new XPub(legacyAddress, XPub.Format.LEGACY));
        xpubs.add(new XPub(segwitAddress, XPub.Format.SEGWIT));
        XPubs dummyArg = new XPubs(xpubs);

        List<String> expectedLegacyAddresses = new ArrayList<>();
        expectedLegacyAddresses.add(legacyAddress);
        List<String> expectedSegwitAddresses = new ArrayList<>();
        expectedSegwitAddresses.add(segwitAddress);

        final UnspentOutputsDto result = new UnspentOutputsDto(
            null,
            new ArrayList<>()
        );

        when(mockApi.getUnspentOutputs(
            BitcoinApi.BITCOIN,
            expectedLegacyAddresses,
            expectedSegwitAddresses,
            null,
            null)
        ).thenReturn(Single.just(result));

        subject.getUnspentBtcCoins(dummyArg)
            .test()
            .assertComplete();

        verify(mockApi).getUnspentOutputs(
            BitcoinApi.BITCOIN,
            expectedLegacyAddresses,
            expectedSegwitAddresses,
            null,
            null
        );
        verifyNoMoreInteractions(mockApi);
    }

    @Test
    public void getMaximumAvailable() {
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        Pair<BigInteger, BigInteger> sweepBundle = subject
                .getMaximumAvailable(
                    unspentOutputs,
                    targetOutputType,
                    BigInteger.valueOf(30000L),
                    false
                );

        long feeManual = calculateFee(1, UnspentTestData.UNSPENT_OUTPUTS_COUNT, BigInteger.valueOf(30000L));

        assertEquals(feeManual, sweepBundle.getSecond().longValue());
        assertEquals(UnspentTestData.BALANCE - feeManual, sweepBundle.getFirst().longValue());
    }

    @Test
    public void spendFirstCoin_minusFee_shouldNotExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long spendAmount = 80200L;
        int inputs = 1;
        int outputs = 1; // No change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));
        BigInteger spendAmountMinusFee = BigInteger.valueOf(spendAmount - feeManual);
        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                spendAmountMinusFee,
                BigInteger.valueOf(30000L),
                false
            );

        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue());
        assertEquals(0L, Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void spendFirstCoin_minusDust_minusFee_shouldNotExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long consumedAmount = Payment.Companion.getDUST().longValue();
        long spendAmount = 80200L - consumedAmount;
        int inputs = 1;
        int outputs = 1;//no change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));
        BigInteger spendAmountMinusFee = BigInteger.valueOf(spendAmount - feeManual);
        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                spendAmountMinusFee,
                BigInteger.valueOf(30000L),
                false
            );

        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue() - consumedAmount);
        assertEquals(consumedAmount, Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void spendFirstCoin_minusLessThanDust_minusFee_shouldNotExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long consumedAmount = 300L;
        long spendAmount = 80200L - consumedAmount;
        int inputs = 1;
        int outputs = 1;//no change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));
        BigInteger spendAmountMinusFee = BigInteger.valueOf(spendAmount - feeManual);
        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                spendAmountMinusFee,
                BigInteger.valueOf(30000L),
                false
            );

        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue() - consumedAmount);
        assertEquals(consumedAmount, Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void spendFirstTwoCoins_minusFee_shouldNotExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long spendAmount = 80200L + 70000L;
        int inputs = 2; // Coins
        int outputs = 1; // No change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));
        BigInteger spendAmountMinusFee = BigInteger.valueOf(spendAmount - feeManual);
        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                spendAmountMinusFee,
                BigInteger.valueOf(30000L),
                false
            );

        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue());
        assertEquals(0L, Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void spendFirstTwoCoins_minusDust_minusFee_shouldNotExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long consumedAmount = Payment.Companion.getDUST().longValue();
        long spendAmount = 80200L + 70000L - consumedAmount;
        int inputs = 2; // Coins
        int outputs = 1; // No change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));
        BigInteger spendAmountMinusFee = BigInteger.valueOf(spendAmount - feeManual);
        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                spendAmountMinusFee,
                BigInteger.valueOf(30000L),
                false
            );

        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue() - consumedAmount);
        assertEquals(consumedAmount, Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void spendFirstThreeCoins_minusDust_minusFee_shouldNotExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long consumedAmount = Payment.Companion.getDUST().longValue();
        long spendAmount = 80200L + 70000L + 60000L - consumedAmount;
        int inputs = 3; // Coins
        int outputs = 1; // No change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));
        BigInteger spendAmountMinusFee = BigInteger.valueOf(spendAmount - feeManual);
        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                spendAmountMinusFee,
                BigInteger.valueOf(30000L),
                false
            );

        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue() - consumedAmount);
        assertEquals(consumedAmount, Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void spendFirstThreeCoins_plusSome_minusFee_shouldExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long spendAmount = 80200L + 70000L + 60000L + 30000L;
        int inputs = 4; // Coins
        int outputs = 2; // Change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));
        BigInteger spendAmountMinusFee = BigInteger.valueOf(spendAmount - feeManual);
        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                spendAmountMinusFee,
                BigInteger.valueOf(30000L),
                false
            );

        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue());
        assertEquals(0L, Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void spendFirstThreeCoins_plusFee_shouldUse4Inputs_AndExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long spendAmount = 80200L + 70000L + 60000L;
        int inputs = 4; // Coins
        int outputs = 2; // Change
        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(30000L),
                false
            );

        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));
        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue());
        assertEquals(0L, Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void spendAllCoins_minusFee_shouldUse8Inputs_AndNotExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long spendAmount = 80200L + 70000L + 60000L + 50000L + 40000L + 30000L + 20000L + 10000L;
        int inputs = 8; // Coins
        int outputs = 1; // No change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));

        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                BigInteger.valueOf(spendAmount - feeManual),
                BigInteger.valueOf(30000L),
                false
            );

        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue());
        assertEquals(0L, Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void spendAllCoins_minusFee_minusDust_shouldUse8Inputs_AndNotExpectChange() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long consumedAmount = Payment.Companion.getDUST().longValue();
        long spendAmount = 80200L + 70000L + 60000L + 50000L + 40000L + 30000L + 20000L + 10000L - consumedAmount;
        int inputs = 8; // Coins
        int outputs = 1; // No change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));

        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                BigInteger.valueOf(spendAmount - feeManual),
                BigInteger.valueOf(30000L),
                false
            );

        assertEquals(inputs, paymentBundle.getSpendableOutputs().size());
        assertEquals(feeManual, paymentBundle.getAbsoluteFee().longValue() - consumedAmount);
        assertEquals(Payment.Companion.getDUST().longValue(), Math.abs(paymentBundle.getConsumedAmount().longValue()));
    }

    @Test
    public void makeTransaction() {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long spendAmount = 80200L + 70000L + 60000L + 50000L + 40000L + 30000L + 20000L + 10000L - Payment.Companion.getDUST().longValue();
        int inputs = 8; // Coins
        int outputs = 1; // No change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));

        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                BigInteger.valueOf(spendAmount - feeManual),
                BigInteger.valueOf(30000L),
                false
            );

        String toAddress = "1GYkgRtJmEp355xUtVFfHSFjFdbqjiwKmb";
        String changeAddress = "1GiEQZt9aX2XfDcj14tCC4xAWEJtq9EXW7";
        BigInteger bigIntFee = BigInteger.ZERO;
        BigInteger bigIntAmount = BigInteger.valueOf(Coin.parseCoin("0.0001").longValue());

        final HashMap<String, BigInteger> receivers = new HashMap<>();
        receivers.put(toAddress, bigIntAmount);

        Transaction tx = subject.makeBtcSimpleTransaction(
            paymentBundle.getSpendableOutputs(),
            receivers,
            bigIntFee,
            changeAddress
        );

        assertEquals("5ee5cb75b364c13fc3c6457be1fd90f58f0b7b2e4f37fcfbd652b90669686420", tx.getTxId().toString());
    }

    @Test
    public void signTransaction() throws Exception {
        // 8 available Payment. [80200,70000,60000,50000,40000,30000,20000,10000]
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList("{\"unspent_outputs\":[{\"tx_hash\":\"e4fb18c8c8279b3433001b5eed2e1a83588196095512fd1d01f236cda223b9e3\",\"tx_hash_big_endian\":\"e3b923a2cd36f2011dfd125509968158831a2eed5e1b0033349b27c8c818fbe4\",\"tx_index\":218376099,\"tx_output_n\":0,\"script\":\"76a914c27982b0008a2fdb1edd3f663ec554019204ad2e88ac\",\"value\":48916,\"value_hex\":\"00bf14\",\"confirmations\":0}]}");

        long spendAmount = Payment.Companion.getDUST().longValue();
        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                BigInteger.valueOf(spendAmount),
                BigInteger.valueOf(30000L),
                false
            );

        String toAddress = "1NNDb5uQU32CtQnBxnrfvJSjkWcREoFWe7";
        String changeAddress = "1JjHeuviHUxCRGcVXYjt3XTbX8H1qifUt2";
        BigInteger bigIntFee = BigInteger.ZERO;
        BigInteger bigIntAmount = Payment.Companion.getPUSHTX_MIN();

        final HashMap<String, BigInteger> receivers = new HashMap<>();
        receivers.put(toAddress, bigIntAmount);

        Transaction tx = subject.makeBtcSimpleTransaction(
            paymentBundle.getSpendableOutputs(),
            receivers,
            bigIntFee,
            changeAddress
        );

        List<SigningKey> keys = new ArrayList<>();
        keys.add(
            new PrivateKeyFactory().getSigningKey(
                PrivateKeyFactory.WIF_UNCOMPRESSED,
                "L3wP9Q3gTZ9YwuTuB8nuczhWG9uEXQEE94PTWDZgpVttFzJbKSHL"
            )
        );

        assertEquals("393988f87ba7a6be24705d0821d4f61c54b341eda3776cc4f3c4eb7af5f7fa7c", tx.getTxId().toString());
        subject.signBtcTransaction(tx, keys);
        assertEquals("efe67d55f73c187447f7fbae66e6daf126efce20ca9b13897b5e81f8cabee639", tx.getTxId().toString());

        mockPushTxError("An outpoint is already spent in [DBBitcoinTx{txIndex=218401014, ip=127.0.0.1, time=1486398252, size=191, distinctIn=null, distinctOut=null, note='null', blockIndexes=[], nTxInput=1, nTxOutput=1}] [OutpointImpl{txIndex=218376099, txOutputN=0}]");
        Call<ResponseBody> call = subject.publishBtcSimpleTransaction(tx);

        assertTrue(call.execute().errorBody().string().contains("An outpoint is already spent in"));
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "SameParameterValue" })
    private void mockPushTxError(String msg) throws IOException {
        ResponseBody errorBody = mock(ResponseBody.class);
        when(errorBody.string()).thenReturn(msg);

        Response response = mock(Response.class);
        when(response.errorBody()).thenReturn(errorBody);

        Call<ResponseBody> call = mock(Call.class);
        when(call.execute()).thenReturn(response);

        when(mockApi.pushTx(anyString(), anyString())).thenReturn(call);
    }

    @Test(expected = InsufficientMoneyException.class)
    public void InsufficientMoneyException() {
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(UnspentTestData.apiResponseString);

        long spendAmount = 80200L + 70000L + 60000L + 50000L + 40000L + 30000L + 20000L + 10000L - Payment.Companion.getDUST().longValue();
        int inputs = 8;//coins
        int outputs = 1;//no change
        long feeManual = calculateFee(outputs, inputs, BigInteger.valueOf(30000L));

        SpendableUnspentOutputs paymentBundle = subject
            .getSpendableCoins(
                unspentOutputs,
                targetOutputType,
                changeOutputType,
                BigInteger.valueOf(spendAmount - feeManual),
                BigInteger.valueOf(30000L),
                false
            );

        String toAddress = "1GYkgRtJmEp355xUtVFfHSFjFdbqjiwKmb";
        String changeAddress = "1GiEQZt9aX2XfDcj14tCC4xAWEJtq9EXW7";
        BigInteger bigIntFee = BigInteger.ZERO;
        BigInteger bigIntAmount = BigInteger.valueOf(6000000L);

        final HashMap<String, BigInteger> receivers = new HashMap<>();
        receivers.put(toAddress, bigIntAmount);

        subject.makeBtcSimpleTransaction(
            paymentBundle.getSpendableOutputs(),
            receivers,
            bigIntFee,
            changeAddress
        );
    }

    @Test
    public void signBCHTransaction() throws Exception {
        String unspentApiResponse = loadResourceContent("transaction/bch_unspent_output.txt");
        List<Utxo> unspentOutputs =
            LoaderUtilKt.parseUnspentOutputsAsUtxoList(unspentApiResponse);

        BigInteger sweepable = BigInteger.valueOf(325000L);
        BigInteger absoluteFee = BigInteger.valueOf(1000);

        SpendableUnspentOutputs paymentBundle = subject.getSpendableCoins(
            unspentOutputs,
            targetOutputType,
            changeOutputType,
            sweepable,
            absoluteFee,
            false
        );

        String receiveAddress = "1JEggWq9VVaVnDmdTbYuHmJXN4icdF89Kq";

        final HashMap<String, BigInteger> receivers = new HashMap<>();
        receivers.put(receiveAddress, sweepable);

        Transaction tx = subject.makeBtcSimpleTransaction(
            paymentBundle.getSpendableOutputs(),
            receivers,
            absoluteFee,
            null
        );

        assertEquals(
            "0100000003cf759867fb887f188d4207f2f79582ed25ee00b9244f37a3e7555c13e1577a550000000000ffffffff5f2061d611a866145b99d75ac4d0885d399c7904b2851e443d0a19fddab86b8e0000000000ffffffff15261589a6a5d95842306db374b3a3edf77c3acfc46f77c023187b1830d5f7920100000000ffffffff0188f50400000000001976a914bd10ab8b35f4343aa9c083d2b6217f2f33f1321288ac00000000",
                Hex.toHexString(tx.bitcoinSerialize())
        );
        assertEquals(
            "6e964bc899795579f0f30c2f1834d87168a625133cb9ea13e66d35db781b13ad",
            tx.getTxId().toString()
        );

        List<SigningKey> keys = new ArrayList<>();
        keys.add(new PrivateKeyFactory().getSigningKey(PrivateKeyFactory.WIF_COMPRESSED, "Kyf1r2iNDikTTGEemDtsGP4jgcSqyYupsc4Rs2jQqHgwUZJNoyHK"));
        keys.add(new PrivateKeyFactory().getSigningKey(PrivateKeyFactory.WIF_COMPRESSED, "L57gn4CnJMdJAiaKXwRL9zFGNqcrvJT4mSJLXPdkFwPBYdvfYLAG"));
        keys.add(new PrivateKeyFactory().getSigningKey(PrivateKeyFactory.WIF_COMPRESSED, "KyGNbFSewezfpHfghxFfyoj3uscpFjLKih75PyU1ZoRkMwPuSWjb"));

        //Act
        subject.signBchTransaction(tx, keys);

        //Assert
        assertEquals("0100000003cf759867fb887f188d4207f2f79582ed25ee00b9244f37a3e7555c13e1577a55000000006a473044022004b6a56b92e9889a3802f9221b8d918b6a5666df541bdf94cbaddc091c73110d0220352f9ff024cf629e97d0d9144700d25ac66daaa55cb5eaac581737c6e818b0210121034eeeae0afd407733476ec3b6d729ffaae408ffd678eeba8c4b8fd2fb4b716f87ffffffff5f2061d611a866145b99d75ac4d0885d399c7904b2851e443d0a19fddab86b8e000000006b483045022100ae7082c2ad557507cbe216e3454e8db9815c2ddd6bae0bbafc6924456e6470120220396921b7bf1dc98c39e6e7ffb88f05225bd8058034d7de8b3b969d73845eaa3401210248eb68f88e4a90df7159887c0acdb888c643d13fd2df45c3b4c45414f11d7635ffffffff15261589a6a5d95842306db374b3a3edf77c3acfc46f77c023187b1830d5f792010000006a473044022049a5d9b2eb50811caf38185c5b7854840cb2dc8b0aea694dbb636aa1bb5bb0310220433c4afa7a5a530693f896d07eade034fd16c8f3e0b62827519393d16d630848012103183db6bf9edfa63716905fa267546ffd59647cc00448a7a6aa6c0c44bf4d0a87ffffffff0188f50400000000001976a914bd10ab8b35f4343aa9c083d2b6217f2f33f1321288ac00000000",
                Hex.toHexString(tx.bitcoinSerialize()));
        assertEquals("4518b1364f7f1b9d24109445f2c266016bf420d0ac4d52507e7b2eb980386c18",
                tx.getTxId().toString());
    }
}

