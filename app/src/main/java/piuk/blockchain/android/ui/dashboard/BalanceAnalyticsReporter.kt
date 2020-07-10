package piuk.blockchain.android.ui.dashboard

import com.blockchain.notifications.analytics.UserAnalytics
import com.blockchain.notifications.analytics.UserProperty
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import java.math.BigDecimal

class BalanceAnalyticsReporter(
    private val analytics: UserAnalytics
) {
    private val collectedBalances = mutableMapOf<CryptoCurrency, Money>()
    private val assetCount = CryptoCurrency.activeCurrencies().size

    private var totalBalance: Money? = null

    fun gotAssetBalance(crypto: CryptoCurrency, amount: Money) {
        collectedBalances[crypto] = amount
        if (collectedBalances.size == assetCount) {
            sendAssetData()
            sendBalanceData()
        }
    }

    private fun sendAssetData() {
        val funded = collectedBalances.filterValues { v -> v.isPositive }.keys.joinToString { "." }

        analytics.logUserProperty(
            UserProperty(
                UserAnalytics.FUNDED_COINS,
                funded
            )
        )
    }

    private fun sendBalanceData() {
        val balance = totalBalance ?: return

        val value = when (balance.toBigDecimal()) {
            RANGE_1 -> BUCKET_1
            RANGE_2 -> BUCKET_2
            RANGE_3 -> BUCKET_3
            RANGE_4 -> BUCKET_4
            else -> BUCKET_5
        }

        val currency = balance.currencyCode

        analytics.logUserProperty(
            UserProperty(
                UserAnalytics.USD_BALANCE,
                "$value $currency"
            )
        )
    }

    fun updateFiatTotal(fiatBalance: Money?) {
        totalBalance = fiatBalance
    }

    companion object {
        private const val BUCKET_1 = "0"
        private const val BUCKET_2 = "1-10"
        private const val BUCKET_3 = "11-100"
        private const val BUCKET_4 = "101-1000"
        private const val BUCKET_5 = "1001"

        private val RANGE_1 = BigDecimal.ZERO
        private val RANGE_2 = BigDecimal(1)..BigDecimal(10)
        private val RANGE_3 = BigDecimal(11)..BigDecimal(100)
        private val RANGE_4 = BigDecimal(101)..BigDecimal(1000)
    }
}
