package info.blockchain.wallet;

import com.blockchain.api.NonCustodialBitcoinService;
import com.blockchain.api.bitcoin.data.BalanceDto;
import com.blockchain.api.bitcoin.data.MultiAddress;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.WalletExplorerEndpoints;
import info.blockchain.wallet.util.LoaderUtilKt;
import retrofit2.Call;
import retrofit2.Response;

import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class WalletApiMockedResponseTest extends MockedResponseTest {

    protected WalletApi walletApi;

    // TODO: replace with real key once required for tests
    private final String siteKey = "site-key";

    @Before
    public void setWalletApiAccess() {
        walletApi = new WalletApi(
            getRetrofit(
                "https://explorer.staging.blockchain.info/",
                newOkHttpClient()
            ).create(WalletExplorerEndpoints.class),
            BlockchainFramework::getApiCode,
            siteKey
        );
    }

    @After
    public void clearWalletApiAccess() {
        walletApi = null;
    }

    @SuppressWarnings("unchecked")
    protected Call<Map<String, BalanceDto>> makeBalanceResponse(String body) throws IOException {
        Response<Map<String, BalanceDto>> response = mock(Response.class);
        Map<String, BalanceDto> data = LoaderUtilKt.parseBalanceResponseDto(body);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(data);

        Call<Map<String, BalanceDto>> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        return call;
    }

    @SuppressWarnings("unchecked")
    protected Call<Map<String, BalanceDto>> makeEmptyBalanceResponse() throws IOException {
        Response<Map<String, BalanceDto>> response = mock(Response.class);
        Map<String, BalanceDto> data = new HashMap<>();
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(data);

        Call<Map<String, BalanceDto>> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        return call;
    }

    @SuppressWarnings("unchecked")
    protected Call<MultiAddress> makeMultiAddressResponse(String body) throws IOException {
        Response<MultiAddress> response = mock(Response.class);
        MultiAddress data = LoaderUtilKt.parseMultiAddressResponse(body);
        when(response.isSuccessful()).thenReturn(true);
        when(response.body()).thenReturn(data);

        Call<MultiAddress> call = mock(Call.class);
        when(call.execute()).thenReturn(response);
        return call;
    }

    protected void mockMultiAddress(NonCustodialBitcoinService bitcoinApi, String coin, String resourceFile) throws IOException {
        String multi = loadResourceContent(resourceFile);
        Call<MultiAddress> bchMultiResponse = makeMultiAddressResponse(multi);
        when(bitcoinApi.getMultiAddress(
            eq(coin), any(), any(), any(String.class), any(), any(Integer.class), any(Integer.class)
        )).thenReturn(bchMultiResponse);
    }

    protected void mockMultiAddress(NonCustodialBitcoinService bitcoinApi, String resourceFile) throws IOException {
        String multi = loadResourceContent(resourceFile);
        Call<MultiAddress> bchMultiResponse = makeMultiAddressResponse(multi);
        when(bitcoinApi.getMultiAddress(
            any(String.class), any(), any(), nullable(String.class), any(), any(Integer.class), any(Integer.class)
        )).thenReturn(bchMultiResponse);
    }

    protected void mockEmptyBalance(NonCustodialBitcoinService bitcoinApi) throws IOException {
        Call<Map<String, BalanceDto>> bchBalanceResponse = makeEmptyBalanceResponse();
        when(bitcoinApi.getBalance(
            any(String.class), any(), any(), any()
        )).thenReturn(bchBalanceResponse);
    }
}
