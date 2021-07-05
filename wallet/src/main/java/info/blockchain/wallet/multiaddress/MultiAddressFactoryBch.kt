package info.blockchain.wallet.multiaddress

import com.blockchain.api.NonCustodialBitcoinService
import com.blockchain.api.bitcoin.data.MultiAddress
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses

import retrofit2.Call

class MultiAddressFactoryBch(bitcoinApi: NonCustodialBitcoinService) : MultiAddressFactory(bitcoinApi) {

    override fun getMultiAddress(
        xpubs: List<XPubs>,
        limit: Int,
        offset: Int,
        context: List<String>?
    ): Call<MultiAddress> {
        return bitcoinApi.getMultiAddress(
            NonCustodialBitcoinService.BITCOIN_CASH,
            xpubs.legacyXpubAddresses(),
            emptyList(),
            context?.joinToString("|"),
            NonCustodialBitcoinService.BalanceFilter.RemoveUnspendable,
            limit,
            offset)
    }
}
