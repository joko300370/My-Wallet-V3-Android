package info.blockchain.api.statistics;

import info.blockchain.api.data.Chart;
import info.blockchain.api.data.Stats;
import java.io.IOException;
import java.util.HashMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import retrofit2.Call;

/**
 * Created by riaanvos on 26/01/2017.
 */
public class StatisticsIntegTest {

    Statistics client;

    @Before
    public void setup() throws Exception {

        client = new Statistics();
    }

    @Test
    public void testGetChart() throws IOException {

        Call<Chart> call = client.getChart("total-bitcoins", "5weeks", "8hours");

        Chart body = call.execute().body();

        Assert.assertEquals(body.getStatus(), "ok");
        Assert.assertNotNull(body.getName());
        Assert.assertNotNull(body.getUnit());
        Assert.assertNotNull(body.getPeriod());
        Assert.assertNotNull(body.getDescription());
    }

    @Test
    public void testGetStats() throws IOException {

        Call<Stats> stats = client.getStats();

        Stats body = stats.execute().body();

        Assert.assertNotNull(body);
    }

    @Test
    public void testGetPools() throws IOException {

        Call<HashMap<String, Integer>> call = client.getPools("5days");
        HashMap<String, Integer> stats = call.execute().body();

        Assert.assertNotNull(stats);
        Assert.assertTrue(stats.size() > 0);
    }

}
