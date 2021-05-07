package info.blockchain.api.blockexplorer;

import static org.junit.Assert.*;

import info.blockchain.api.data.AddressFull;
import info.blockchain.api.data.Balance;
import info.blockchain.api.data.Block;
import info.blockchain.api.data.ExportHistory;
import info.blockchain.api.data.MultiAddress;
import info.blockchain.api.data.RawBlock;
import info.blockchain.api.data.RawBlocks;
import info.blockchain.api.data.Transaction;
import info.blockchain.api.data.Transactions;
import info.blockchain.api.data.UnspentOutputs;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Created by riaanvos on 26/01/2017.
 */
public class BlockExplorerIntegTest {

    BlockExplorer client;

    @Before
    public void setup() throws Exception {

        client = new BlockExplorer();
    }

    @Test
    public void testCertificatePinner() {

        try {
            BlockExplorer secureClient = new BlockExplorer();
            Response<Transaction> execute = secureClient.getTransactionDetails(1455666).execute();
            assertTrue(execute.isSuccessful());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testGetAddressInfo() throws Exception {

        Call<AddressFull> call = client
                .getAddress("1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW", 100, 0);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetAddressInfoNew() throws Exception {

        Call<AddressFull> call = client.getAddress("1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW", null, null, null);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetBalance() throws Exception {

        String address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW";
        String address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn";

        List<String> list = Arrays.asList(address1, address2);
        List<String> listP2SH = Collections.emptyList();

        Call<HashMap<String, Balance>> call = client
            .getBalance(list, listP2SH, FilterType.All);

        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetBalance_BCH() throws Exception {

        String address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW";
        String address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn";

        List<String> list = Arrays.asList(address1, address2);
        List<String> listP2SH = Collections.emptyList();

        Call<HashMap<String, Balance>> call = client
                .getBalance("bch", list, listP2SH, FilterType.All);

        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetMultiAddress() throws Exception {

        String address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW";
        String address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn";

        List<String> list = Arrays.asList(address1, address2);
        List<String> listP2SH = Collections.emptyList();

        Call<MultiAddress> call = client
            .getMultiAddress(list, listP2SH, null, FilterType.All.getFilterInt(), 20, 0);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetMultiAddressNew() throws Exception {

        String address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW";
        String address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn";
        List<String> list = Arrays.asList(address1, address2);
        List<String> listP2SH = Collections.emptyList();
        Call<MultiAddress> call = client.getMultiAddress(list, listP2SH, FilterType.All, null, null);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetMultiAddress_BTC() throws Exception {

        String address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW";
        String address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn";
        List<String> list = Arrays.asList(address1, address2);
        List<String> listP2SH = Collections.emptyList();
        Call<MultiAddress> call = client.getMultiAddress("btc", list, listP2SH, null, FilterType.All, null, null);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetTransactionDetails() throws IOException {

        Call<Transaction> call = client.getTransaction(
            "0102720363b24a5376c52186dce4a671589595022dbe340ed08b0a84a5e96dc7");
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testUnspentOutputs() throws IOException {

        String address1 = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T";
        String address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn";

        List<String> list = Arrays.asList(address1, address2);
        List<String> listP2SH = Collections.emptyList();

        Call<UnspentOutputs> call = client.getUnspentOutputs(list, listP2SH);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testUnspentOutputsNew() throws IOException {

        String address1 = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T";
        String address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn";

        List<String> list = Arrays.asList(address1, address2);
        List<String> listP2SH = Collections.emptyList();

        Call<UnspentOutputs> call = client.getUnspentOutputs(list, listP2SH, 6, 10);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testUnspentOutputs_BCH() throws IOException {

        String address1 = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T";
        String address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn";

        List<String> list = Arrays.asList(address1, address2);
        List<String> listP2SH = Collections.emptyList();

        Call<UnspentOutputs> call = client.getUnspentOutputs("bch", list, listP2SH, 6, 10);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testUnspentOutputs_noFree() throws IOException {

        String address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW";
        List<String> list = Arrays.asList(address1);
        List<String> listP2SH = Collections.emptyList();

        Call<UnspentOutputs> call = client.getUnspentOutputs(list, listP2SH);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void getRawBlockFromIndex() throws IOException {

        Call<RawBlock> call = client.getBlockDetails(1455666);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    @Ignore ("Fails too often because of SocketTimeoutException")
    public void getRawBlockFromHash() throws IOException {

        Call<RawBlock> call = client
            .getBlockDetails("0000000000000000027356741342842027a7f58bff9dda3e422fc57f5560a5b1");
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void getRawTxFromHash() throws IOException {
        Call<Transaction> call = client
            .getTransactionDetails("b6f6991d03df0e2e04dafffcd6bc418aac66049e2cd74b80f14ac86db1e3f0da");
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void getRawTxFromIndex() throws IOException {
        Call<Transaction> call = client.getTransactionDetails(1939312);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetBlockAtHeight() throws Exception {
        Call<RawBlocks> call = client.getBlocksAtHeight(123456);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testLatestBlock() throws Exception {
        Call<Block> call = client.getLatestBlock();
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetUnconfirmedTransactions() throws Exception {
        Call<Transactions> call = client.getUnconfirmedTransactions();
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetBlocksAtTime() throws IOException {
        Call<RawBlocks> call = client.getBlocksAtTime(1485257472000L);
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetBlocksFromPool() throws IOException {
        Call<RawBlocks> call = client.getBlocksFromPool("AntPool");
        assertTrue(call.execute().isSuccessful());
    }

    @Test
    public void testGetExportHistory() throws IOException {
        String address1 = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T";
        String address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn";

        List<String> list = Arrays.asList(address1, address2);
        List<String> listP2SH = Collections.emptyList();

        Call<List<ExportHistory>> call = client
            .getExportHistory(list, listP2SH, "05/05/2016", "23/01/2017", "USD");

        assertTrue(call.execute().isSuccessful());
    }

}
