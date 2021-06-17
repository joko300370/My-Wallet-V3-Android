package info.blockchain.wallet.payload

import com.blockchain.api.NonCustodialBitcoinService
import com.blockchain.api.bitcoin.data.BalanceResponseDto
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import retrofit2.Call

class BalanceManagerBch(
    bitcoinApi: NonCustodialBitcoinService
) : BalanceManager(
    bitcoinApi,
    CryptoCurrency.BCH
) {
    override fun getBalanceOfAddresses(xpubs: List<XPubs>): Call<BalanceResponseDto> {
        return bitcoinApi.getBalance(
            NonCustodialBitcoinService.BITCOIN_CASH,
            xpubs.legacyXpubAddresses(),
            emptyList(),
            NonCustodialBitcoinService.BalanceFilter.RemoveUnspendable
        )
    }
}
