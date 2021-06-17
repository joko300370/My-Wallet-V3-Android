package info.blockchain.wallet.payload

import com.blockchain.api.NonCustodialBitcoinService
import com.blockchain.api.bitcoin.data.BalanceResponseDto
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.data.XPubs
import retrofit2.Call

import java.math.BigInteger

abstract class BalanceManager constructor(
    val bitcoinApi: NonCustodialBitcoinService,
    private val cryptoCurrency: CryptoCurrency
) {
    private var balanceMap: CryptoBalanceMap

    val walletBalance: BigInteger
        get() = balanceMap.totalSpendable.toBigInteger()

    val importedAddressesBalance: BigInteger
        get() = balanceMap.totalSpendableImported.toBigInteger()

    private val balanceQuery: BalanceCall
        get() = BalanceCall(bitcoinApi, cryptoCurrency)

    init {
        balanceMap = CryptoBalanceMap.zero(cryptoCurrency)
    }

    fun subtractAmountFromAddressBalance(address: String, amount: BigInteger) {
        balanceMap = balanceMap.subtractAmountFromAddress(address, CryptoValue(cryptoCurrency, amount))
    }

    fun getAddressBalance(xpub: XPubs): CryptoValue =
        (getXpubBalance(xpub.forDerivation(XPub.Format.LEGACY)) +
            getXpubBalance(xpub.forDerivation(XPub.Format.SEGWIT))) as CryptoValue

    private fun getXpubBalance(xpub: XPub?): CryptoValue =
        xpub?.address?.let {
            return balanceMap[it]
        } ?: CryptoValue.zero(cryptoCurrency)

    fun updateAllBalances(
        xpubs: List<XPubs>,
        importedAddresses: List<String>
    ) {
        balanceMap = calculateCryptoBalanceMap(
            cryptoCurrency = cryptoCurrency,
            balanceQuery = balanceQuery,
            xpubs = xpubs,
            imported = importedAddresses
        )
    }

    @Deprecated("Use getBalanceQuery")
    abstract fun getBalanceOfAddresses(xpubs: List<XPubs>): Call<BalanceResponseDto>
}
