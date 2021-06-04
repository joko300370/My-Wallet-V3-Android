package info.blockchain.api.receive;

import info.blockchain.api.data.ReceivePayment;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by riaanvos on 26/01/2017.
 */
public class ReceiveIntegTest {

    Receive client;

    @Before
    public void setup() throws Exception {

        client = new Receive();
    }

    @Test
    @Ignore("The http callback validation does not pass, need to fix later")
    public void testGetReceivePaymentsAddress() throws IOException {

        Call<ReceivePayment> call = client.receive("xpub6CWiJoiwxPQni3DFbrQNHWq8kwrL2J1HuBN7zm4xKPCZRmEshc7Dojz4zMah7E4o2GEEbD6HgfG7sQid186Fw9x9akMNKw2mu1PjqacTJB2",
            null, "c327e75c-5a03-4480-95f5-65c8c59adb2c");
        Response<ReceivePayment> exe = call.execute();

        Assert.assertEquals(exe.errorBody().string(), "{\"message\":\"Missing 'key' parameter for the API Key\"}");
    }

}
