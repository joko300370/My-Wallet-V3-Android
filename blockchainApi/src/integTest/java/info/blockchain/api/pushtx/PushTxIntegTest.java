package info.blockchain.api.pushtx;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by riaanvos on 26/01/2017.
 */
public class PushTxIntegTest {

    private PushTx client;

    @Before
    public void setup() {

        client = new PushTx();
    }

    @Test
    public void testPushTx_legacy() {

        String txHash = "0100000001ba00dc25caab5a3806a5a8d84a07293b9d2fddcbbe75cb2e8c3be5fb9a8f7f3a010000006a473044022062533def9654e0fe521750a5334172644d182714792c9d801739ea24bada26eb02202d10f6cfa0a890a6ee0fb0a03a1cd7d7e30b8fc0f8c3a35ed5e2bd70fbcceffa0121028cd0b0633451ea95100c6268650365e829315c941ae82cf0424091a1cf7aa355ffffffff018e850100000000001976a914a3a7c7be8e2b0b209c6347c73a175cfb381ffeb788ac00000000";
        Call<ResponseBody> call = client.pushTx(txHash);

        try {
            Response<ResponseBody> exe = call.execute();
            if (exe.isSuccessful()) {
                Assert.assertTrue(true);
            } else {
                if (exe.errorBody().string()
                    .contains("Transaction already exists")) {
                    Assert.assertTrue(true);
                } else {
                    Assert.fail();
                }
            }
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void testPushTx_BTC() {

        String txHash = "0100000001ba00dc25caab5a3806a5a8d84a07293b9d2fddcbbe75cb2e8c3be5fb9a8f7f3a010000006a473044022062533def9654e0fe521750a5334172644d182714792c9d801739ea24bada26eb02202d10f6cfa0a890a6ee0fb0a03a1cd7d7e30b8fc0f8c3a35ed5e2bd70fbcceffa0121028cd0b0633451ea95100c6268650365e829315c941ae82cf0424091a1cf7aa355ffffffff018e850100000000001976a914a3a7c7be8e2b0b209c6347c73a175cfb381ffeb788ac00000000";
        Call<ResponseBody> call = client.pushTx("btc", txHash);

        try {
            Response<ResponseBody> exe = call.execute();
            if (exe.isSuccessful()) {
                Assert.assertTrue(true);
            } else {
                if (exe.errorBody().string()
                        .contains("Transaction already exists")) {
                    Assert.assertTrue(true);
                } else {
                    Assert.fail();
                }
            }
        } catch (IOException e) {
            Assert.fail();
        }
    }

    @Test
    public void testPushTx_BCH() {

        String txHash = "0100000001ba00dc25caab5a3806a5a8d84a07293b9d2fddcbbe75cb2e8c3be5fb9a8f7f3a010000006a473044022062533def9654e0fe521750a5334172644d182714792c9d801739ea24bada26eb02202d10f6cfa0a890a6ee0fb0a03a1cd7d7e30b8fc0f8c3a35ed5e2bd70fbcceffa0121028cd0b0633451ea95100c6268650365e829315c941ae82cf0424091a1cf7aa355ffffffff018e850100000000001976a914a3a7c7be8e2b0b209c6347c73a175cfb381ffeb788ac00000000";
        Call<ResponseBody> call = client.pushTx("bch", txHash);

        try {
            Response<ResponseBody> exe = call.execute();
            if (exe.isSuccessful()) {
                Assert.assertTrue(true);
            } else {
                if (exe.errorBody().string()
                        .contains("Transaction already exists")) {
                    Assert.assertTrue(true);
                } else {
                    Assert.fail();
                }
            }
        } catch (IOException e) {
            Assert.fail();
        }
    }

}
