package info.blockchain.wallet.payload

import info.blockchain.api.BitcoinApi
import info.blockchain.api.bitcoin.data.BalanceResponseDto
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import retrofit2.Call

class BalanceManagerBch(
    bitcoinApi: BitcoinApi
) : BalanceManager(
    bitcoinApi,
    CryptoCurrency.BCH
) {
    override fun getBalanceOfAddresses(xpubs: List<XPubs>): Call<BalanceResponseDto> {
        return bitcoinApi.getBalance(
            BitcoinApi.BITCOIN_CASH,
            xpubs.legacyXpubAddresses(),
            emptyList(),
            BitcoinApi.BalanceFilter.RemoveUnspendable
        )
    }
}
