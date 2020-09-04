package piuk.blockchain.android.ui.transfer.send.flow

import android.content.res.Resources
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxOptionValue
import java.lang.IllegalStateException

class TxConfirmReadOnlyMapper(private val formatters: List<TxOptionsFormatter>) {
    fun map(property: TxOptionValue): Pair<String, String> {
        for (formatter in formatters) {
            formatter.format(property)?.let {
                return it
            }
        }
        throw IllegalStateException("No formatter found")
    }
}

interface TxOptionsFormatter {
    fun format(property: TxOptionValue): Pair<String, String>?
}

class FromPropertyFormatter(private val resources: Resources) : TxOptionsFormatter {

    override fun format(property: TxOptionValue): Pair<String, String>? =
        if (property is TxOptionValue.From)
            resources.getString(R.string.common_from) to property.from
        else null
}

class FeedTotalFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxOptionValue): Pair<String, String>? =
        if (property is TxOptionValue.FeedTotal)
            resources.getString(R.string.common_total) to totalAmount(property.amount, property.fee)
        else null

    private fun totalAmount(money: Money, fee: Money): String {
        return if (money.symbol == fee.symbol) {
            (money + fee).toStringWithSymbol()
        } else {
            "${money.toStringWithSymbol()} (${fee.toStringWithSymbol()})"
        }
    }
}

class FeePropertyFormatter(private val resources: Resources) : TxOptionsFormatter {

    override fun format(property: TxOptionValue): Pair<String, String>? =
        if (property is TxOptionValue.Fee) {
            val feeTitle = resources.getString(
                R.string.common_spaced_strings,
                resources.getString(R.string.send_confirmation_fee),
                resources.getString(R.string.send_confirmation_regular_estimation)
            )
            feeTitle to property.fee.toStringWithSymbol()
        } else null
}

class ExchangePriceFormatter(private val resources: Resources) : TxOptionsFormatter {

    override fun format(property: TxOptionValue): Pair<String, String>? =
        if (property is TxOptionValue.ExchangePriceOption)
            resources.getString(R.string.sell_quote_price, property.asset) to property.money.toStringWithSymbol()
        else null
}

class TotalFormatter(private val resources: Resources) : TxOptionsFormatter {

    override fun format(property: TxOptionValue): Pair<String, String>? =
        if (property is TxOptionValue.Total)
            resources.getString(R.string.common_total) to property.total.toStringWithSymbol()
        else null
}

class ToPropertyFormatter(private val resources: Resources) : TxOptionsFormatter {

    override fun format(property: TxOptionValue): Pair<String, String>? =
        if (property is TxOptionValue.To)
            resources.getString(R.string.common_to) to property.to
        else null
}