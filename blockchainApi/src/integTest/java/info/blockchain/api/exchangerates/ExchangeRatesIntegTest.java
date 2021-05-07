package info.blockchain.api.exchangerates;

import info.blockchain.api.data.Ticker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import retrofit2.Call;

/**
 * Created by riaanvos on 26/01/2017.
 */
public class ExchangeRatesIntegTest {

    private ExchangeRates client;

    @Before
    public void setup() throws Exception {

        client = new ExchangeRates();
    }

    @Test
    public void testGetExchangeRate() throws Exception {

        Call<Ticker> call = client.getBtcTicker();

        Ticker ticker = call.execute().body();

        Assert.assertEquals(ticker.getUSD().getSymbol(), "$");
        Assert.assertEquals(ticker.getGBP().getSymbol(), "£");
    }

    @Test
    public void testGetBtcToCurrency() throws IOException {

        Call<Double> call = client.toBTC("USD", 500000.50);
        Double btc = call.execute().body();

        Assert.assertNotNull(btc);
    }

    @Test
    public void testGetCurrencyToBtc() throws IOException {

        Call<Double> call = client.toBTC("USD", 50.50);
        Double btc = call.execute().body();

        Assert.assertNotNull(btc);
    }

}
