package info.blockchain.wallet.payload

import com.blockchain.api.NonCustodialBitcoinService
import com.blockchain.api.bitcoin.data.BalanceResponseDto
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.data.legacyXpubAddresses
import info.blockchain.wallet.payload.data.segwitXpubAddresses
import retrofit2.Call

class BalanceManagerBtc(
    bitcoinApi: NonCustodialBitcoinService
) : BalanceManager(
    bitcoinApi,
    CryptoCurrency.BTC
) {
    @Deprecated("Use getBalanceQuery")
    override fun getBalanceOfAddresses(xpubs: List<XPubs>): Call<BalanceResponseDto> {
        return bitcoinApi.getBalance(
            NonCustodialBitcoinService.BITCOIN,
            xpubs.legacyXpubAddresses(),
            xpubs.segwitXpubAddresses(),
            NonCustodialBitcoinService.BalanceFilter.RemoveUnspendable
        )
    }
}
