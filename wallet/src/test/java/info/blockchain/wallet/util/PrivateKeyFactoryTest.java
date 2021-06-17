package info.blockchain.wallet.util;

import com.blockchain.api.NonCustodialBitcoinService;
import com.blockchain.api.bitcoin.data.BalanceDto;
import info.blockchain.wallet.WalletApiMockedResponseTest;
import info.blockchain.wallet.keys.SigningKey;
import retrofit2.Call;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.LegacyAddress;
import org.bitcoinj.params.MainNetParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PrivateKeyFactoryTest extends WalletApiMockedResponseTest {

    private PrivateKeyFactory privateKeyFactory;
    private final NonCustodialBitcoinService bitcoinApi = mock(NonCustodialBitcoinService.class);

    //Mini key
    private final String miniKey = "SxuRMDrSNbwozww4twnedUPouUmGST";
    private final String miniUncompressedAddress = "16FFsrfKxeKt7JWhtpB4VrGBjQ1kKv5o3p";
    private final String miniCompressedAddress = "1H2E6b2Ny6UeQ6bM5V2pSxAwZaVYAaaYUH";

    //Hex key
    private final String hexKey = "C7C4AEE098C6EF6C8A9363E4D760F515FA27D67C219E7238510F458235B9870D";
    private final String hexUncompressedAddress = "1NZUGwdmQJ7AA6QrEuBz4jeT6z7yjty5dM";
    private final String hexCompressedAddress = "1NLLkARpefxpXaMb7ZhHmc2DYNoVUnzBAz";

    private final String balanceApiResponse = "{\n" +
                                              "    \"%s\": {\n" +
                                              "        \"final_balance\": %d,\n" +
                                              "        \"n_tx\": 22,\n" +
                                              "        \"total_received\": 259526\n" +
                                              "    },\n" +
                                              "    \"%s\": {\n" +
                                              "        \"final_balance\": %d,\n" +
                                              "        \"n_tx\": 51,\n" +
                                              "        \"total_received\": 622078\n" +
                                              "    }\n" +
                                              "}";

    @Before
    public void setup() {
        privateKeyFactory = new PrivateKeyFactory();
    }

    @Test
    public void test_Mini_KeyFormat(){

        String miniKey = "SmZxHc2PURmBHgKKXo97rEYWfnQKYu";
        String format = privateKeyFactory.getFormat(miniKey);
        Assert.assertEquals(PrivateKeyFactory.MINI, format);

        String miniKey2 = "SxuRMDrSNbwozww4twnedUPouUmGST";
        String format2 = privateKeyFactory.getFormat(miniKey2);
        Assert.assertEquals(PrivateKeyFactory.MINI, format2);
    }

    @Test
    public void test_BASE58_KeyFormat(){

        String key = "22mPQQDMarsk4UcUuNH34PhebdftEtrQuftXDg5kA4QG";
        String format = privateKeyFactory.getFormat(key);
        Assert.assertEquals(PrivateKeyFactory.BASE58, format);
    }

    @Test
    public void test_BASE64_KeyFormat() {

        String key = "vICceVGqzvxqnB7haMDSB1q+XtBJ2kYraP45sjPd3CA=";
        String format = privateKeyFactory.getFormat(key);
        Assert.assertEquals(PrivateKeyFactory.BASE64, format);
    }

    @Test
    public void test_HEX_KeyFormat() {

        String format = privateKeyFactory.getFormat(hexKey);
        Assert.assertEquals(PrivateKeyFactory.HEX, format);
    }

    @Test
    public void test_WIF_COMPRESSED_KeyFormat() {
        String key = "KyCHxZe68e5PNfqh8Ls8DrihMuweHKxvjtm3PGTrj43MyWuvN2aE";

        String format = privateKeyFactory.getFormat(key);
        Assert.assertEquals(PrivateKeyFactory.WIF_COMPRESSED, format);
    }

    @Test
    public void test_WIF_UNCOMPRESSED_KeyFormat() {
        String key = "5JKxWHiBf1GX2A83BRVxYG4xpqsbfR3w9kQtppAUUJ6jnafURkm";

        String format = privateKeyFactory.getFormat(key);
        Assert.assertEquals(PrivateKeyFactory.WIF_UNCOMPRESSED, format);
    }

    @Test
    public void test_BIP38_KeyFormat() {

        String key = "6PfY1oK1kJX7jYDPMGBkcECCYwzH2qTCHfMdz67cBJrL7oZvpH8H8jfH2j";
        String format = privateKeyFactory.getFormat(key);
        Assert.assertEquals(PrivateKeyFactory.BIP38, format);
    }

    @Test
    public void test_Mini_KeyFormat_shouldReturnCompressed_byDefault() throws Exception {
        //Act
        String format = privateKeyFactory.getFormat(miniKey);

        SigningKey key = privateKeyFactory.getKeyFromImportedData(format, miniKey, bitcoinApi);
        Address address = LegacyAddress.fromKey(MainNetParams.get(), key.toECKey());

        //Assert
        Assert.assertEquals(miniCompressedAddress, address.toString());
        Assert.assertTrue(key.toECKey().isCompressed());
    }

    @Test
    public void test_Mini_KeyFormat_shouldReturnUncompressed_ifHasBalance() throws Exception {

        //Arrange
        String uncompressedWithBalance = String.format(
            balanceApiResponse,
            miniUncompressedAddress,
            1000,
            miniCompressedAddress,
            0
        );

        Call<Map<String, BalanceDto>> bchBalanceResponse = makeBalanceResponse(uncompressedWithBalance);
        when(bitcoinApi.getBalance(any(), any(), any(), any()))
            .thenReturn(bchBalanceResponse);

        //Act
        String format = privateKeyFactory.getFormat(miniKey);
        SigningKey key = privateKeyFactory.getKeyFromImportedData(format, miniKey, bitcoinApi);
        Address address = LegacyAddress.fromKey(MainNetParams.get(), key.toECKey());

        //Assert
        Assert.assertEquals(miniUncompressedAddress, address.toString());
        Assert.assertFalse(key.toECKey().isCompressed());
    }

    @Test
    public void test_Mini_KeyFormat_shouldReturnCompressed_ifBothHaveFunds() throws Exception {
        //Act
        String format = privateKeyFactory.getFormat(miniKey);

        SigningKey key = privateKeyFactory.getKeyFromImportedData(format, miniKey, bitcoinApi);
        Address address = LegacyAddress.fromKey(MainNetParams.get(), key.toECKey());

        //Assert
        Assert.assertEquals(miniCompressedAddress, address.toString());
        Assert.assertTrue(key.toECKey().isCompressed());
    }

    @Test
    public void test_HEX_KeyFormat_shouldReturnCompressed_byDefault() throws Exception {
        //Act
        String format = privateKeyFactory.getFormat(hexKey);

        SigningKey key = privateKeyFactory.getKeyFromImportedData(format, hexKey, bitcoinApi);
        Address address = LegacyAddress.fromKey(MainNetParams.get(), key.toECKey());

        //Assert
        Assert.assertEquals(hexCompressedAddress, address.toString());
        Assert.assertTrue(key.toECKey().isCompressed());
        Assert.assertEquals(PrivateKeyFactory.HEX, format);
    }

    @Test
    public void test_HEX_KeyFormat_shouldReturnCompressed_ifBothHaveFunds() throws Exception {

        //Act
        String format = privateKeyFactory.getFormat(hexKey);
        SigningKey key = privateKeyFactory.getKeyFromImportedData(format, hexKey, bitcoinApi);
        Address address = LegacyAddress.fromKey(MainNetParams.get(), key.toECKey());

        //Assert
        Assert.assertEquals(hexCompressedAddress, address.toString());
        Assert.assertTrue(key.toECKey().isCompressed());
        Assert.assertEquals(PrivateKeyFactory.HEX, format);
    }

    @Test
    public void test_HEX_KeyFormat_shouldReturnUncompressed_ifHasBalance() throws Exception {

        //Arrange
        String uncompressedWithBalance = String.format(balanceApiResponse, hexUncompressedAddress, 1000 ,hexCompressedAddress, 0);
        Call<Map<String, BalanceDto>> bchBalanceResponse = makeBalanceResponse(uncompressedWithBalance);
        when(bitcoinApi.getBalance(any(), any(), any(), any()))
            .thenReturn(bchBalanceResponse);

        //Act
        String format = privateKeyFactory.getFormat(hexKey);

        SigningKey key = privateKeyFactory.getKeyFromImportedData(format, hexKey, bitcoinApi);
        Address address = LegacyAddress.fromKey(MainNetParams.get(), key.toECKey());

        //Assert
        Assert.assertEquals(PrivateKeyFactory.HEX, format);
        Assert.assertEquals(hexUncompressedAddress, address.toString());
        Assert.assertFalse(key.toECKey().isCompressed());
    }
}
