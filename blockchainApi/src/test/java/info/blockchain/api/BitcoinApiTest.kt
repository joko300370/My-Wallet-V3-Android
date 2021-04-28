package info.blockchain.api

import info.blockchain.api.bitcoin.data.UnspentOutputDto
import info.blockchain.api.bitcoin.data.XpubDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import kotlin.test.assertNotNull

class BitcoinApiTest : BaseApiClientTester() {
    private val client: BitcoinApi = BitcoinApi(makeRetrofitApi(), API_CODE)

    @Test
    fun testGetBalance() {
        val address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW"
        val address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7" +
            "W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"
        val listLegacy = listOf(address1, address2)
        val listBech32 = emptyList<String>()
        mockInterceptor.responseString =
            "{    \"xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6bu" +
                "VkQk73ftKE1z4tt9cPHWRn\": {        \"final_balance\": 20000,        \"n_tx\": 1,        " +
                "\"total_received\": 20000    },    \"1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW\": {        " +
                "\"final_balance\": 0,        \"n_tx\": 2,        \"total_received\": 20000    }}"

        val call = client
            .getBalance(
                BitcoinApi.BITCOIN,
                listLegacy,
                listBech32,
                BitcoinApi.BalanceFilter.All
            )
        val response = call.execute().body()
        assertNotNull(response)

        val balance1 = response[address1]
        assertEquals(balance1?.finalBalance, "0")
        assertEquals(balance1?.txCount, 2L)
        assertEquals(balance1?.totalReceived, "20000")
        val balance2 = response[address2]
        assertEquals(balance2?.finalBalance, "20000")
        assertEquals(balance2?.txCount, 1L)
        assertEquals(balance2?.totalReceived, "20000")
    }

    @Test
    fun testGetBalance_BTC() {
        val address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW"
        val address2 =
            "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRB" +
                "mZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"
        val listLegacy = listOf(address1, address2)
        val listBech32 = emptyList<String>()
        mockInterceptor.responseString =
            "{    \"xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQo" +
                "cB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn\": {        \"final_balance\": 20000," +
                "        \"n_tx\": 1,        \"total_received\": 20000    }" +
                ",    \"1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW\": {        \"final_balance\": 0" +
                ",        \"n_tx\": 2,        \"total_received\": 20000    }}"

        val call = client.getBalance(
            BitcoinApi.BITCOIN,
            listLegacy,
            listBech32,
            BitcoinApi.BalanceFilter.All
        )
        val response = call.execute().body()
        assertNotNull(response)

        val balance1 = response[address1]
        assertEquals(balance1?.finalBalance, "0")
        assertEquals(balance1?.txCount, 2L)
        assertEquals(balance1?.totalReceived, "20000")

        val balance2 = response[address2]
        assertEquals(balance2?.finalBalance, "20000")
        assertEquals(balance2?.txCount, 1L)
        assertEquals(balance2?.totalReceived, "20000")
    }

    @Test
    fun testGetMultiAddress_BTC() {
        val address1 = "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW"
        val address2 = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W" +
            "7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"

        val list = listOf(address1, address2)
        val listP2SH = emptyList<String>()
        mockInterceptor.responseString =
            "{\"recommend_include_fee\":true," +
                "\"sharedcoin_endpoint\":\"https://api.sharedcoin.com\"," +
                "\"info\":{\"nconnected\":188,\"conversion\":108411.66075823," +
                "\"symbol_local\":{\"code\":\"USD\",\"symbol\":\"$\",\"name\":\"U.S. dollar\"" +
                ",\"conversion\":108411.66075823,\"symbolAppearsAfter\":false,\"local\":true}," +
                "\"symbol_btc\":{\"code\":\"BTC\",\"symbol\":\"BTC\",\"name\":\"Bitcoin\"," +
                "\"conversion\":100000000.00000000,\"symbolAppearsAfter\":true,\"local\":false}" +
                ",\"latest_block\":{\"block_index\":1455666," +
                "\"hash\":\"0000000000000000027356741342842027a7f58bff9dda3e422fc57f5560a5b1\"," +
                "\"height\":449387,\"time\":1485037165}},\"wallet\":{\"n_tx\":3," +
                "\"n_tx_filtered\":3,\"total_received\":40000,\"total_sent\":20000," +
                "\"final_balance\":20000},\"addresses\":[{\"address\":\"1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW\"," +
                "\"n_tx\":2,\"total_received\":20000,\"total_sent\":20000,\"final_balance\":0," +
                "\"change_index\":0,\"account_index\":0}," +
                "{\"address\":\"xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W" +
                "7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn\"," +
                "\"n_tx\":1,\"total_received\":20000,\"total_sent\":0,\"final_balance\":20000," +
                "\"gap_limit\":20,\"change_index\":0,\"account_index\":1}]," +
                "\"txs\":[{\"hash\":\"72743ce381c5eab3a23535ef158c6e6b435ebfc8d493d387b90aee1818b47a2e\"," +
                "\"ver\":1,\"vin_sz\":1,\"vout_sz\":2,\"size\":226,\"relayed_by\":\"0.0.0.0\"," +
                "\"lock_time\":0,\"tx_index\":145849898,\"double_spend\":false,\"result\":20000," +
                "\"balance\":20000,\"time\":1462466670,\"block_height\":410371," +
                "\"inputs\":[{\"prev_out\":{\"value\":240240,\"tx_index\":145808878,\"n\":1," +
                "\"spent\":true,\"script\":\"76a914ade8ea8fa072aafc8caf66af4ea815dd1e3dfe6f88ac\"," +
                "\"type\":0,\"addr\":\"1GrYvVX76JMMeU32PCoyndaeYU5odDGAu3\"},\"sequence\":4294967295," +
                "\"script\":\"483045022100e766eda1bcccae4d0076dc09242a42492b39e31f7e83a11263a93d75f3cd86f602206" +
                "9665bb861898ab198392698c5b21caead19ff535df223c0bb63a978b1221ac2012102a4cc88b940db6a00487b2638c" +
                "ae13dd3c7853ced968c99b1187eeceea0f91ceb\"}],\"out\":[{\"value\":20000," +
                "\"tx_index\":145849898,\"n\":0,\"spent\":false," +
                "\"script\":\"76a91461718f0b60dc85dc09c8e59d0ddd6901bab900da88ac\"," +
                "\"type\":0,\"addr\":\"19tEaovasXx75vjuwYqziZSJg7b3u1MTQt\"," +
                "\"xpub\":{\"m\":\"xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1bRRBmZPzQoc" +
                "B6qar9ay6buVkQk73ftKE1z4tt9cPHWRn\",\"path\":\"M/0/0\"}}]},{\"hash\":\"04734caac4e2ae7feba9b74fb" +
                "8d2c145db9ea9651487371c4d741428f8f5a24b\",\"ver\":1,\"vin_sz\":1,\"vout_sz\":1,\"size\":224," +
                "\"relayed_by\":\"127.0.0.1\",\"lock_time\":0,\"tx_index\":93553551,\"double_spend\":false," +
                "\"result\":-20000,\"balance\":0,\"time\":1436437493,\"block_height\":364542," +
                "\"inputs\":[{\"prev_out\":{\"value\":20000,\"tx_index\":93361644,\"n\":0," +
                "\"spent\":true,\"script\":\"76a91407feead7f9fb7d16a0251421ac9fa090169cc16988ac\"," +
                "\"type\":0,\"addr\":\"1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW\"},\"sequence\":4294967295," +
                "\"script\":\"483045022100c7eb789c70a3732b44a59ad94fecb3da754b53d0fe9372c0fd333f7074965ed702202b" +
                "7973a2c9b249921b9020c84c0bc0fdf35ad84b8c2504e188a0210ca8b6d3a501410454bf54158ef3a0dafe6dc92958b" +
                "3be4d7decb88b53daf9d783c99ccb1f52b9a20238894c243dd74f44fa9e38f6640eca2f63ed918877dc41abb6fbbf7" +
                "94f4575\"}],\"out\":[{\"value\":10000,\"tx_index\":93553551,\"n\":0,\"spent\":true," +
                "\"script\":\"76a914f58dc062e22da323169ad3549ed229f74c7dca6788ac\",\"type\":0," +
                "\"addr\":\"1PPNN4psDFyAgdjQcKBJ8GSgE4ES4GHP9c\"}]}," +
                "{\"hash\":\"3775a2cd2e20c8be6bafd003270b5323f11024385ea9e72045221325a00f1d15\"," +
                "\"ver\":1,\"vin_sz\":1,\"vout_sz\":2,\"size\":226,\"relayed_by\":\"127.0.0.1\"," +
                "\"lock_time\":0,\"tx_index\":93361644,\"double_spend\":false,\"result\":20000," +
                "\"balance\":20000,\"time\":1436345752,\"block_height\":364382," +
                "\"inputs\":[{\"prev_out\":{\"value\":291978,\"tx_index\":93179290,\"n\":1," +
                "\"spent\":true,\"script\":\"76a9140cb08ca600b9df701f3f8144cd2727e821a9a52988ac\"," +
                "\"type\":0,\"addr\":\"12A6cArVjYTb6s34q24EkvXpZaDiEb4j6v\"},\"sequence\":4294967295," +
                "\"script\":\"483045022100ad7897b5a5ac6cf79cd38a8bf1a60fce7bd30a5ff0d8fd7c0c9d764ac55507a10220714" +
                "200f56511763bd5c90e1d1114fb4ccaa75c42d9481e5d1809e3df216342cc012102a5e70759b8034ffe8f13d146f73bf" +
                "0a9a83c9964113c70c9b7b00b0d1a188646\"}],\"out\":[{\"value\":20000,\"tx_index\":93361644," +
                "\"n\":0,\"spent\":true,\"script\":\"76a91407feead7f9fb7d16a0251421ac9fa090169cc16988ac\"," +
                "\"type\":0,\"addr\":\"1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW\"}]}]}"

        val call = client.getMultiAddress(
            BitcoinApi.BITCOIN,
            list,
            listP2SH,
            null,
            BitcoinApi.BalanceFilter.All,
            20,
            0
        )
        val multiAddress = call.execute().body()

        val balance = multiAddress!!.multiAddressBalance
        assertEquals(balance.txCount, 3)
        assertEquals(balance.txCountFiltered, 3)
        assertEquals(balance.totalReceived.toLong(), 40000)
        assertEquals(balance.totalSent.toLong(), 20000)
        assertEquals(balance.finalBalance.toLong(), 20000)

        // Addresses
        val firstSummary = multiAddress.addresses[0]
        assertEquals(
            firstSummary.address,
            "1jH7K4RJrQBXijtLj1JpzqPRhR7MdFtaW"
        )
        assertEquals(firstSummary.txCount, 2)
        assertEquals(firstSummary.totalReceived.toLong(), 20000)
        assertEquals(firstSummary.totalSent.toLong(), 20000)
        assertEquals(firstSummary.finalBalance.toLong(), 0)
        val secondSummary = multiAddress.addresses[1]
        assertEquals(
            secondSummary.address,
            "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48K" +
                "xuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"
        )
        assertEquals(secondSummary.txCount, 1)
        assertEquals(secondSummary.totalReceived.toLong(), 20000)
        assertEquals(secondSummary.totalSent.toLong(), 0)
        assertEquals(secondSummary.finalBalance.toLong(), 20000)

        // Txs
        val firstTx = multiAddress.txs[0]
        assertEquals(
            firstTx.hash,
            "72743ce381c5eab3a23535ef158c6e6b435ebfc8d493d387b90aee1818b47a2e"
        )
        assertEquals(firstTx.ver, 1)
        assertEquals(firstTx.lockTime, 0)
        assertEquals(firstTx.blockHeight, 410371)
        assertEquals(firstTx.relayedBy, "0.0.0.0")
        assertEquals(firstTx.result.toLong(), 20000)
        assertEquals(firstTx.size, 226)
        assertEquals(firstTx.time, 1462466670)
        assertEquals(firstTx.txIndex, 145849898)
        assertEquals(firstTx.vinSz, 1)
        assertEquals(firstTx.voutSz, 2)
        assertFalse(firstTx.isDoubleSpend)
        val firstInput = firstTx.inputs[0]
        assertEquals(
            firstInput.sequence,
            4294967295L
        )
        assertEquals(
            firstInput.script,
            "483045022100e766eda1bcccae4d0076dc09242a42492b39e31f7e83a11263a93d75f3cd86f6022" +
                "069665bb861898ab198392698c5b21caead19ff535df223c0bb63a978b1221ac2012102a4cc88b940" +
                "db6a00487b2638cae13dd3c7853ced968c99b1187eeceea0f91ceb"
        )
        assertEquals(firstInput.prevOut?.isSpent, true)
        assertEquals(firstInput.prevOut?.txIndex, 145808878L)
        assertEquals(firstInput.prevOut?.type, 0)
        assertEquals(firstInput.prevOut?.addr, "1GrYvVX76JMMeU32PCoyndaeYU5odDGAu3")
        assertEquals(firstInput.prevOut?.value?.toLong(), 240240L)
        assertEquals(firstInput.prevOut?.count, 1L)
        assertEquals(firstInput.prevOut?.script, "76a914ade8ea8fa072aafc8caf66af4ea815dd1e3dfe6f88ac")

        val firstOutput = firstTx.out[0]
        assertFalse(firstOutput.isSpent)
        assertEquals(firstOutput.txIndex, 145849898L)
        assertEquals(firstOutput.type.toLong(), 0)
        assertEquals(firstOutput.addr, "19tEaovasXx75vjuwYqziZSJg7b3u1MTQt")
        assertEquals(firstOutput.value.toLong(), 20000L)
        assertEquals(firstOutput.count, 0L)
        assertEquals(firstOutput.script, "76a91461718f0b60dc85dc09c8e59d0ddd6901bab900da88ac")
        assertEquals(firstOutput.xpub?.address,
            "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf48Kxuj1b" +
                "RRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"
        )
        assertEquals(firstOutput.xpub?.derivationPath, "M/0/0")
    }

    @Test
    fun testUnspentOutputs_BTC() {
        val address1 = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T"
        val address2 =
            "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W" +
                "7CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn"
        val listLegacy = listOf(address1, address2)
        val listBech32 = emptyList<String>()
        mockInterceptor.responseString =
            "{\"notice\":\"Someinputsareunconfirmedandarenotspendable\",\"unspent_outputs\":" +
                "[{\"tx_hash\":\"2e7ab41818ee0ab987d393d4c8bf5e436b6e8c15ef3535a2b3eac581e33c7472\"," +
                "\"tx_hash_big_endian\":\"72743ce381c5eab3a23535ef158c6e6b435ebfc8d493d387b90aee1818b47a2e\"," +
                "\"tx_index\":145849898,\"tx_output_n\":0,\"script\":\"76a91461718f0b60dc85dc09c8e59d0ddd69" +
                "01bab900da88ac\",\"xpub\":{\"m\":\"xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7" +
                "CNbf48Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn\",\"path\":\"M/0/0\"},\"value\":20000," +
                "\"value_hex\":\"4e20\",\"confirmations\":39303},{\"tx_hash\":\"c76de9a5840a8bd00e34be2d02959558" +
                "71a6e4dc8621c576534ab26303720201\",\"tx_hash_big_endian\":\"0102720363b24a5376c52186dce4a6715895" +
                "95022dbe340ed08b0a84a5e96dc7\",\"tx_index\":213199563,\"tx_output_n\":0,\"script\":\"76a914a2eec" +
                "5a24d631dd8d07cb99670602660acf8d12a88ac\",\"value\":99460,\"value_hex\":\"018484\"," +
                "\"confirmations\":2}]}"

        val expected = UnspentOutputDto(
            txHash = "2e7ab41818ee0ab987d393d4c8bf5e436b6e8c15ef3535a2b3eac581e33c7472",
            txHashBigEndian = "72743ce381c5eab3a23535ef158c6e6b435ebfc8d493d387b90aee1818b47a2e",
            txIndex = 145849898L,
            txOutputCount = 0,
            script = "76a91461718f0b60dc85dc09c8e59d0ddd6901bab900da88ac",
            value = "20000",
            valueHex = "4e20",
            confirmations = 39303L,
            xpub = XpubDto(
                address = "xpub6CmZamQcHw2TPtbGmJNEvRgfhLwitarvzFn3fBYEEkFTqztus7W7CNbf4" +
                    "8Kxuj1bRRBmZPzQocB6qar9ay6buVkQk73ftKE1z4tt9cPHWRn",
                derivationPath = "M/0/0"
            ),
            replayable = true
        )

        client.getUnspentOutputs(
            BitcoinApi.BITCOIN,
            listLegacy,
            listBech32,
            10,
            50
        ).test()
            .assertComplete()
            .assertValue { (notice, unspentOutputs) ->
                notice != null && unspentOutputs[0] == expected
            }
    }

    @Test
    fun testUnspentOutputs_BTC_no_UTXOs() {
        val address = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T"

        val listLegacy = listOf(address)
        val listBech32 = emptyList<String>()

        mockInterceptor.responseString = ""
        mockInterceptor.responseCode = 500

        client.getUnspentOutputs(
            BitcoinApi.BITCOIN,
            listLegacy,
            listBech32,
            10,
            50
        ).test()
            .assertComplete()
            .assertValue { (notice, unspentOutputs) ->
                notice == null && unspentOutputs.isEmpty()
            }
    }

    @Test
    fun testUnspentOutputs_BTC_server_error() {
        val address = "1FrWWFJ95Jq7EDgpkeBwVLAtoJMPwmYS7T"

        val listLegacy = listOf(address)
        val listBech32 = emptyList<String>()

        mockInterceptor.responseString = ""
        mockInterceptor.responseCode = 501

        client.getUnspentOutputs(
            BitcoinApi.BITCOIN,
            listLegacy,
            listBech32,
            10,
            50
        ).test()
            .assertError { e -> e is HttpException }
    }

    @Test
    fun testPushTx_BTC() {
        val txHash = "0100000001ba00dc25caab5a3806a5a8d84a07293b9d2fddcbbe75cb2e8c3be5fb9a8f7f3a010000006a47304" +
            "4022062533def9654e0fe521750a5334172644d182714792c9d801739ea24bada26eb02202d10f6cfa0a890a6ee0fb0a03" +
            "a1cd7d7e30b8fc0f8c3a35ed5e2bd70fbcceffa0121028cd0b0633451ea95100c6268650365e829315c941ae82cf042409" +
            "1a1cf7aa355ffffffff018e850100000000001976a914a3a7c7be8e2b0b209c6347c73a175cfb381ffeb788ac00000000"
        mockInterceptor.responseCode = 200
        val call = client.pushTx("btc", txHash)
        val exe = call.execute()
        assertTrue(exe.isSuccessful)
    }

    @Test
    fun testPushTx_BCH() {
        val txHash = "0100000001ba00dc25caab5a3806a5a8d84a07293b9d2fddcbbe75cb2e8c3be5fb9a8f7f3a010000006" +
            "a473044022062533def9654e0fe521750a5334172644d182714792c9d801739ea24bada26eb02202d10f6cfa0a8" +
            "90a6ee0fb0a03a1cd7d7e30b8fc0f8c3a35ed5e2bd70fbcceffa0121028cd0b0633451ea95100c6268650365e82" +
            "9315c941ae82cf0424091a1cf7aa355ffffffff018e850100000000001976a914a3a7c7be8e2b0b209c6347c73a" +
            "175cfb381ffeb788ac00000000"
        mockInterceptor.responseCode = 200
        val call = client.pushTx("bch", txHash)
        val exe = call.execute()
        assertTrue(exe.isSuccessful)
    }
}