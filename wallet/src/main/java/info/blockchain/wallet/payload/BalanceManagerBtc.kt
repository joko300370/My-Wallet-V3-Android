package info.blockchain.wallet.payload

import info.blockchain.api.BitcoinApi
import info.blockchain.api.bitcoin.data.BalanceResponseDto
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import info.blockchain.wallet.payload.data.segwitXpubAddresses
import retrofit2.Call

class BalanceManagerBtc(
    bitcoinApi: BitcoinApi
) : BalanceManager(
    bitcoinApi,
    CryptoCurrency.BTC
) {
    @Deprecated("Use getBalanceQuery")
    override fun getBalanceOfAddresses(xpubs: List<XPubs>): Call<BalanceResponseDto> {
        return bitcoinApi.getBalance(
            BitcoinApi.BITCOIN,
            xpubs.legacyXpubAddresses(),
            xpubs.segwitXpubAddresses(),
            BitcoinApi.BalanceFilter.RemoveUnspendable
        )
    }
}
