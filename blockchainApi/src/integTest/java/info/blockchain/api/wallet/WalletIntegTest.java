package info.blockchain.api.wallet;

import info.blockchain.api.data.*;

import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by riaanvos on 26/01/2017.
 */
@Ignore("Only run this manually when a local wallet service is available")
public class WalletIntegTest {

    /*
    Integration tests will only work if local instance of service-my-wallet is running.
     */

    Wallet wallet;

    @Before
    public void setup() throws Exception {

        wallet = new Wallet(
            "http://localhost:3000/",
            null,
            "0dc7d131-be7b-42e3-8336-a08769eacb8a",
            "SecretPassword");
    }

    @Test
    public void testGetBalance() throws IOException {
        Call<WalletBalance> call = wallet.getBalance();
        Response<WalletBalance> execute = call.execute();

        Assert.assertEquals(BigInteger.ZERO, execute.body().getBalance());
    }

    @Test
    public void send() throws IOException {
        Call<WalletPaymentResponse> call = wallet.send("19RGq6CrJ5wnMNestsGqBdgGfMrpWzADn2", 40000L, null, 1000L, "Amazon payment");
        Response<WalletPaymentResponse> execute = call.execute();

        Assert.assertEquals("{\"error\":\"Insufficient funds\",\"needed\":0.00041,\"available\":0}", execute.errorBody().string());
    }

    @Test
    public void sendMany() throws IOException {

        Map<String, Long> map = new HashMap<>();
        map.put("19RGq6CrJ5wnMNestsGqBdgGfMrpWzADn2",20000L);
        map.put("1D92NqjyvovQVTpQJGaeFUUYxbcQjcSFdm",20000L);

        Call<WalletPaymentResponse> call = wallet.sendMany(map,null, 1000L, "Amazon payment");
        Response<WalletPaymentResponse> execute = call.execute();

        Assert.assertEquals("{\"error\":\"Insufficient funds\",\"needed\":0.00041,\"available\":0}", execute.errorBody().string());
    }

    @Test
    public void testListAddresses() throws IOException {
        Call<WalletAddressList> call = wallet.listAddresses(3);
        Response<WalletAddressList> execute = call.execute();

        WalletAddressList response = execute.body();
        Assert.assertEquals("1EZMrueXZubzKGMDGmCMWELvuNN9MZSUYD", response.getAddressList().get(0).getAddress());
        Assert.assertEquals("", response.getAddressList().get(0).getLabel());
        Assert.assertEquals(BigInteger.ZERO, response.getAddressList().get(0).getBalance());
        Assert.assertEquals(BigInteger.ZERO, response.getAddressList().get(0).getTotalReceived());
    }

    @Test
    public void testListAddressesNew() throws IOException {
        Call<WalletAddressList> call = wallet.listAddresses();
        Response<WalletAddressList> execute = call.execute();

        WalletAddressList response = execute.body();
        Assert.assertEquals("1EZMrueXZubzKGMDGmCMWELvuNN9MZSUYD", response.getAddressList().get(0).getAddress());
        Assert.assertEquals("", response.getAddressList().get(0).getLabel());
        Assert.assertEquals(BigInteger.ZERO, response.getAddressList().get(0).getBalance());
        Assert.assertEquals(BigInteger.ZERO, response.getAddressList().get(0).getTotalReceived());
    }

    @Test
    public void testGetAddress() throws IOException {
        Call<WalletAddress> call = wallet.getAddress("1EZMrueXZubzKGMDGmCMWELvuNN9MZSUYD", 0);
        Response<WalletAddress> execute = call.execute();

        WalletAddress response = execute.body();
        Assert.assertEquals("1EZMrueXZubzKGMDGmCMWELvuNN9MZSUYD", response.getAddress());
        Assert.assertEquals(BigInteger.ZERO, response.getBalance());
        Assert.assertEquals(BigInteger.ZERO, response.getTotalReceived());
    }

    @Test
    public void testGetAddressNew() throws IOException {
        Call<WalletAddress> call = wallet.getAddress("1EZMrueXZubzKGMDGmCMWELvuNN9MZSUYD");
        Response<WalletAddress> execute = call.execute();

        WalletAddress response = execute.body();
        Assert.assertEquals("1EZMrueXZubzKGMDGmCMWELvuNN9MZSUYD", response.getAddress());
        Assert.assertEquals(BigInteger.ZERO, response.getBalance());
        Assert.assertEquals(BigInteger.ZERO, response.getTotalReceived());
    }

    @Test
    public void testNewAddress() throws IOException {
        Call<WalletAddress> call = wallet.newAddress("My Label");
        Response<WalletAddress> execute = call.execute();

        WalletAddress response = execute.body();
        Assert.assertEquals("My Label", response.getLabel());
        Assert.assertNull(response.getBalance());
        Assert.assertNull(response.getTotalReceived());
    }

    @Test
    public void testArchiveAddress() throws IOException {
        Call<WalletAddressArchiveResponse> call = wallet.archiveAddress("1AtUdy94b3k5xWXuU3WeQ4oEX5pYP2jZFk");
        Response<WalletAddressArchiveResponse> execute = call.execute();

        Assert.assertEquals("1AtUdy94b3k5xWXuU3WeQ4oEX5pYP2jZFk", execute.body().getArchived());
    }

    @Test
    public void testUnArchiveAddress() throws IOException {
        Call<WalletAddressUnarchiveResponse> call = wallet.unarchiveAddress("1AtUdy94b3k5xWXuU3WeQ4oEX5pYP2jZFk");
        Response<WalletAddressUnarchiveResponse> execute = call.execute();

        Assert.assertEquals("1AtUdy94b3k5xWXuU3WeQ4oEX5pYP2jZFk", execute.body().getActive());
    }

    @Test
    public void testCreateWallet() throws IOException {

        Call<CreateWalletResponse> call = wallet.createWallet("http://localhost:3000/", "A_SECURE_PASSWORD", "YOUR_API_CODE", null, null, null);
        Response<CreateWalletResponse> exe = call.execute();

        Assert.assertEquals("{\"error\":\"Unknown API key\"}", exe.errorBody().string());
    }
}
