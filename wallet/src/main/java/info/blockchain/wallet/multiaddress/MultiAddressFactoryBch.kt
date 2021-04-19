package info.blockchain.wallet.multiaddress

import info.blockchain.api.BitcoinApi
import info.blockchain.api.bitcoin.data.MultiAddress
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses

import retrofit2.Call

class MultiAddressFactoryBch(bitcoinApi: BitcoinApi) : MultiAddressFactory(bitcoinApi) {

    override fun getMultiAddress(
        xpubs: List<XPubs>,
        limit: Int,
        offset: Int,
        context: List<String>?
    ): Call<MultiAddress> {
        return bitcoinApi.getMultiAddress(
            BitcoinApi.BITCOIN_CASH,
            xpubs.legacyXpubAddresses(),
            emptyList(),
            context?.joinToString("|"),
            BitcoinApi.BalanceFilter.RemoveUnspendable,
            limit,
            offset)
    }
}
