package piuk.blockchain.android.ui.transactionflow.flow

import android.content.Context
import android.content.res.Resources
import com.blockchain.ui.urllinks.CHECKOUT_PRICE_EXPLANATION
import com.blockchain.ui.urllinks.NETWORK_FEE_EXPLANATION
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiserImpl
import piuk.blockchain.android.util.StringUtils

class TxConfirmReadOnlyMapper(private val formatters: List<TxOptionsFormatter>) {
    fun map(property: TxConfirmationValue): Pair<String, String> {
        for (formatter in formatters) {
            formatter.format(property)?.let {
                return it
            }
        }
        throw IllegalStateException("No formatter found")
    }
}

interface TxOptionsFormatter {
    fun format(property: TxConfirmationValue): Pair<String, String>?
}

// New checkout screens
class TxConfirmReadOnlyMapperNewCheckout(private val formatters: List<TxOptionsFormatterNewCheckout>) {
    fun map(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        return when (property) {
            is TxConfirmationValue.NewTo -> formatters.first { it is NewToPropertyFormatter }.format(property)
            is TxConfirmationValue.NewFrom -> formatters.first { it is NewFromPropertyFormatter }.format(property)
            is TxConfirmationValue.NewExchangePriceConfirmation ->
                formatters.first { it is NewExchangePriceFormatter }.format(property)
            is TxConfirmationValue.NewNetworkFee -> formatters.first { it is NewNetworkFormatter }.format(property)
            is TxConfirmationValue.NewSale -> formatters.first { it is NewSalePropertyFormatter }.format(property)
            is TxConfirmationValue.NewTotal -> formatters.first { it is NewTotalFormatter }.format(property)
            else -> throw IllegalStateException("No formatter found for property: $property")
        }
    }
}

interface TxOptionsFormatterNewCheckout {
    fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any>
}

enum class ConfirmationPropertyKey {
    LABEL,
    TITLE,
    SUBTITLE,
    LINKED_NOTE,
    IS_IMPORTANT
}

class NewExchangePriceFormatter(
    private val context: Context,
    private val stringUtils: StringUtils
) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewExchangePriceConfirmation)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                R.string.quote_price, property.asset.displayTicker
            ),
            ConfirmationPropertyKey.TITLE to property.money.toStringWithSymbol(),
            ConfirmationPropertyKey.LINKED_NOTE to stringUtils.getResolvedStringWithAppendedMappedLearnMore(
                context.resources.getString(R.string.checkout_item_price_note), R.string.common_linked_learn_more,
                CHECKOUT_PRICE_EXPLANATION, context, R.color.blue_600
            )
        )
    }
}

class NewToPropertyFormatter(private val context: Context) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewTo)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.checkout_item_deposit_to),
            ConfirmationPropertyKey.TITLE to property.to
        )
    }
}

class NewSalePropertyFormatter(private val context: Context) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewSale)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.checkout_item_sale),
            ConfirmationPropertyKey.TITLE to property.exchange.toStringWithSymbol(),
            ConfirmationPropertyKey.SUBTITLE to property.amount.toStringWithSymbol()
        )
    }
}

class NewFromPropertyFormatter(private val context: Context) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewFrom)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.common_from),
            ConfirmationPropertyKey.TITLE to property.from
        )
    }
}

class NewNetworkFormatter(
    private val context: Context,
    private val stringUtils: StringUtils
) :
    TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewNetworkFee)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.checkout_item_network_fee),
            ConfirmationPropertyKey.TITLE to "- " + property.exchange.toStringWithSymbol(),
            ConfirmationPropertyKey.SUBTITLE to property.feeAmount.toStringWithSymbol(),
            ConfirmationPropertyKey.LINKED_NOTE to stringUtils.getResolvedStringWithAppendedMappedLearnMore(
                context.resources.getString(R.string.checkout_item_network_fee_note, property.asset.displayTicker),
                R.string.common_linked_learn_more, NETWORK_FEE_EXPLANATION, context, R.color.blue_600
            )
        )
    }
}

class NewTotalFormatter(private val context: Context) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewTotal)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.common_total),
            ConfirmationPropertyKey.TITLE to property.exchange.toStringWithSymbol(),
            ConfirmationPropertyKey.SUBTITLE to property.totalWithoutFee.toStringWithSymbol(),
            ConfirmationPropertyKey.IS_IMPORTANT to true
        )
    }
}

class FromPropertyFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.From)
            resources.getString(R.string.common_from) to property.from
        else null
}

class FeedTotalFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.FeedTotal)
            resources.getString(R.string.common_total) to totalAmount(
                property.amount,
                property.fee,
                property.exchangeAmount,
                property.exchangeFee
            )
        else null

    private fun totalAmount(money: Money, fee: Money, exchangeAmount: Money?, exchangeFee: Money?): String {
        return if (money.symbol == fee.symbol) {
            (money + fee).formatWithExchange(exchangeAmount)
        } else {
            money.formatWithExchange(exchangeAmount).plus(System.lineSeparator())
                .plus(fee.formatWithExchange(exchangeFee))
        }
    }
}

class ExchangePriceFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.ExchangePriceConfirmation) {
            resources.getString(
                R.string.quote_price,
                property.asset.displayTicker
            ) to property.money.toStringWithSymbol()
        } else {
            null
        }
}

class SwapExchangeRateFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.SwapExchangeRate) {
            resources.getString(R.string.exchange_rate) to resources.getString(
                R.string.current_unit_price,
                property.unitCryptoCurrency.toStringWithSymbol(), property.price.toStringWithSymbol()
            )
        } else {
            null
        }
}

class SwapReceiveFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.SwapReceiveValue) {
            resources.getString(R.string.receive) to property.receiveAmount.toStringWithSymbol()
        } else {
            null
        }
}

class TotalFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.Total)
            resources.getString(R.string.common_total) to property.total.formatWithExchange(property.exchange)
        else null
}

class ToPropertyFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.To)
            resources.getString(R.string.common_to) to property.to
        else null
}

class SwapSourcePropertyFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.SwapSourceValue)
            resources.getString(R.string.common_swap) to property.swappingAssetValue.toStringWithSymbol()
        else null
}

class SwapDestinationPropertyFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.SwapDestinationValue)
            resources.getString(R.string.common_receive) to property.receivingAssetValue.toStringWithSymbol()
        else null
}

class FiatFeePropertyFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.FiatTxFee)
            resources.getString(R.string.send_confirmation_tx_fee) to property.fee.toStringWithSymbol()
        else null
}

class EstimatedCompletionPropertyFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        when (property) {
            is TxConfirmationValue.EstimatedDepositCompletion -> resources.getString(
                R.string.send_confirmation_eta
            ) to TransactionFlowCustomiserImpl.getEstimatedTransactionCompletionTime()
            is TxConfirmationValue.EstimatedWithdrawalCompletion -> resources.getString(
                R.string.withdraw_confirmation_eta
            ) to TransactionFlowCustomiserImpl.getEstimatedTransactionCompletionTime()
            else -> null
        }
}

fun Money.formatWithExchange(exchange: Money?) =
    exchange?.let {
        "${this.toStringWithSymbol()} (${it.toStringWithSymbol()})"
    } ?: this.toStringWithSymbol()
