package piuk.blockchain.android.ui.transactionflow.flow

import android.content.Context
import android.content.res.Resources
import android.text.SpannableStringBuilder
import com.blockchain.ui.urllinks.CHECKOUT_PRICE_EXPLANATION
import com.blockchain.ui.urllinks.EXCHANGE_SWAP_RATE_EXPLANATION
import com.blockchain.ui.urllinks.NETWORK_ERC20_EXPLANATION
import com.blockchain.ui.urllinks.NETWORK_FEE_EXPLANATION
import com.blockchain.wallet.DefaultLabels
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
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
class TxConfirmReadOnlyMapperNewCheckout(
    private val formatters: List<TxOptionsFormatterNewCheckout>
) {
    fun map(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        return when (property) {
            is TxConfirmationValue.NewTo -> formatters.first { it is NewToPropertyFormatter }.format(property)
            is TxConfirmationValue.NewFrom -> formatters.first { it is NewFromPropertyFormatter }.format(property)
            is TxConfirmationValue.NewExchangePriceConfirmation ->
                formatters.first { it is NewExchangePriceFormatter }.format(property)
            is TxConfirmationValue.NewNetworkFee -> formatters.first { it is NewNetworkFormatter }.format(property)
            is TxConfirmationValue.TransactionFee -> formatters.first { it is TransactionFeeFormatter }.format(property)
            is TxConfirmationValue.NewSale -> formatters.first { it is NewSalePropertyFormatter }.format(property)
            is TxConfirmationValue.NewTotal -> formatters.first { it is NewTotalFormatter }.format(property)
            is TxConfirmationValue.Amount -> formatters.first { it is AmountTotalFormatter }.format(property)
            is TxConfirmationValue.EstimatedCompletion ->
                formatters.first { it is EstimatedCompletionPropertyFormatter }
                    .format(property)
            is TxConfirmationValue.PaymentMethod ->
                formatters.first { it is PaymentMethodPropertyFormatter }.format(property)
            is TxConfirmationValue.NewSwapExchange ->
                formatters.first { it is NewSwapExchangeRateFormatter }.format(property)
            is TxConfirmationValue.CompoundNetworkFee -> formatters.first { it is CompoundNetworkFeeFormatter }
                .format(property)
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
    IS_IMPORTANT,
    FEE_ITEM_SENDING,
    FEE_ITEM_RECEIVING
}

class FeeInfo(
    val feeAmount: Money,
    val fiatAmount: Money,
    val asset: CryptoCurrency
)

class NewExchangePriceFormatter(
    private val context: Context
) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewExchangePriceConfirmation)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                R.string.quote_price, property.asset.displayTicker
            ),
            ConfirmationPropertyKey.TITLE to property.money.toStringWithSymbol(),
            ConfirmationPropertyKey.LINKED_NOTE to StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                context.resources.getString(R.string.checkout_item_price_note), R.string.common_linked_learn_more,
                CHECKOUT_PRICE_EXPLANATION, context, R.color.blue_600
            )
        )
    }
}

class NewToPropertyFormatter(
    private val context: Context,
    private val defaultLabel: DefaultLabels
) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewTo)
        return if (property.assetAction == AssetAction.Sell || property.assetAction == AssetAction.FiatDeposit) {
            mapOf(
                ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.checkout_item_deposit_to),
                ConfirmationPropertyKey.TITLE to property.txTarget.label
            )
        } else {
            require(property.sourceAccount is CryptoAccount)
            val asset = property.sourceAccount.asset
            mapOf(
                ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.checkout_item_send_to),
                ConfirmationPropertyKey.TITLE to getLabel(
                    property.txTarget.label,
                    defaultLabel.getDefaultNonCustodialWalletLabel(asset),
                    asset.displayTicker
                )
            )
        }
    }
}

class EstimatedCompletionPropertyFormatter(private val context: Context) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.send_confirmation_eta),
            ConfirmationPropertyKey.TITLE to TransactionFlowCustomiserImpl.getEstimatedTransactionCompletionTime()
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

class PaymentMethodPropertyFormatter(private val context: Context) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.PaymentMethod)
        return mapOf(
            if (property.assetAction == AssetAction.FiatDeposit) {
                ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.payment_method)
            } else {
                ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.checkout_item_withdraw_to)
            },
            ConfirmationPropertyKey.TITLE to property.paymentTitle,
            if (property.accountType.isNullOrBlank()) {
                ConfirmationPropertyKey.SUBTITLE to context.getString(
                    R.string.checkout_item_account_number, property.paymentSubtitle
                )
            } else {
                ConfirmationPropertyKey.SUBTITLE to property.paymentSubtitle
            }
        )
    }
}

class NewFromPropertyFormatter(
    private val context: Context,
    private val defaultLabel: DefaultLabels
) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewFrom)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.common_from),
            property.sourceAsset?.let {
                ConfirmationPropertyKey.TITLE to getLabel(
                    property.sourceAccount.label,
                    defaultLabel.getDefaultNonCustodialWalletLabel(it),
                    it.displayTicker
                )
            } ?: ConfirmationPropertyKey.TITLE to property.sourceAccount.label
        )
    }
}

class TransactionFeeFormatter(
    private val context: Context
) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.TransactionFee)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.checkout_item_fee_to),
            ConfirmationPropertyKey.TITLE to property.feeAmount.toStringWithSymbol()
        )
    }
}

class NewNetworkFormatter(
    private val context: Context,
    private val assetResources: AssetResources
) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewNetworkFee)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(
                R.string.checkout_item_network_fee, property.asset.displayTicker
            ),
            ConfirmationPropertyKey.TITLE to property.exchange.toStringWithSymbol(),
            ConfirmationPropertyKey.SUBTITLE to property.feeAmount.toStringWithSymbol(),
            if (property.asset.hasFeature(CryptoCurrency.IS_ERC20)) {
                ConfirmationPropertyKey.LINKED_NOTE to StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(R.string.swap_erc_20_tooltip),
                    R.string.common_linked_learn_more, NETWORK_ERC20_EXPLANATION, context, R.color.blue_600
                )
            } else {
                ConfirmationPropertyKey.LINKED_NOTE to StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        R.string.checkout_item_network_fee_note, assetResources.assetName(property.asset)
                    ),
                    R.string.common_linked_learn_more, NETWORK_FEE_EXPLANATION, context, R.color.blue_600
                )
            }
        )
    }
}

class CompoundNetworkFeeFormatter(
    private val context: Context,
    private val assetResources: AssetResources
) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.CompoundNetworkFee)
        return mapOf(
            ConfirmationPropertyKey.LABEL to buildFeeLabel(property.feeLevel),
            ConfirmationPropertyKey.TITLE to getEstimatedFee(property.sendingFeeInfo, property.receivingFeeInfo),
            ConfirmationPropertyKey.FEE_ITEM_SENDING to property.sendingFeeInfo,
            ConfirmationPropertyKey.FEE_ITEM_RECEIVING to property.receivingFeeInfo,
            ConfirmationPropertyKey.LINKED_NOTE to buildLinkedNoteItem(
                property.sendingFeeInfo, property.receivingFeeInfo, property.ignoreErc20LinkedNote
            )
        ).filterNotNullValues()
    }

    private fun buildFeeLabel(feeLevel: FeeLevel?): String = when (feeLevel) {
        FeeLevel.Regular -> context.resources.getString(
            R.string.checkout_item_network_fee_level_label, context.resources.getString(R.string.fee_options_regular)
        )
        FeeLevel.Priority -> context.resources.getString(
            R.string.checkout_item_network_fee_level_label, context.resources.getString(R.string.fee_options_priority)
        )
        FeeLevel.Custom -> context.resources.getString(
            R.string.checkout_item_network_fee_level_label, context.resources.getString(R.string.fee_options_custom)
        )
        FeeLevel.None, null -> context.resources.getString(R.string.checkout_item_network_fee_label)
    }

    private fun buildLinkedNoteItem(
        sendingFeeInfo: FeeInfo?,
        receivingFeeInfo: FeeInfo?,
        ignoreErc20LinkedNote: Boolean
    ): SpannableStringBuilder? {
        return when {
            sendingFeeInfo != null && receivingFeeInfo != null -> getDoubleFeeNote(sendingFeeInfo, receivingFeeInfo)
            sendingFeeInfo != null && receivingFeeInfo == null -> getSingleFeeNote(
                sendingFeeInfo, ignoreErc20LinkedNote
            )
            sendingFeeInfo == null && receivingFeeInfo != null -> getSingleFeeNote(
                receivingFeeInfo, ignoreErc20LinkedNote
            )
            else -> {
                SpannableStringBuilder().append(context.resources.getString(R.string.checkout_fee_free))
            }
        }
    }

    private fun getDoubleFeeNote(sendingFeeInfo: FeeInfo, receivingFeeInfo: FeeInfo): SpannableStringBuilder? =
        when {
            !sendingFeeInfo.asset.hasFeature(CryptoCurrency.IS_ERC20) &&
                !receivingFeeInfo.asset.hasFeature(CryptoCurrency.IS_ERC20) -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        R.string.checkout_dual_fee_note, assetResources.getDisplayName(sendingFeeInfo.asset),
                        assetResources.getDisplayName(receivingFeeInfo.asset)
                    ),
                    R.string.checkout_fee_link, NETWORK_FEE_EXPLANATION, context, R.color.blue_600
                )
            }
            sendingFeeInfo.asset.hasFeature(CryptoCurrency.IS_ERC20) &&
                !receivingFeeInfo.asset.hasFeature(CryptoCurrency.IS_ERC20) -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        R.string.checkout_one_erc_20_one_not_fee_note,
                        assetResources.getDisplayName(sendingFeeInfo.asset),
                        assetResources.getDisplayName(CryptoCurrency.ETHER),
                        assetResources.getDisplayName(receivingFeeInfo.asset)
                    ),
                    R.string.checkout_fee_link, NETWORK_ERC20_EXPLANATION, context, R.color.blue_600
                )
            }
            !sendingFeeInfo.asset.hasFeature(CryptoCurrency.IS_ERC20) &&
                receivingFeeInfo.asset.hasFeature(CryptoCurrency.IS_ERC20) -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        R.string.checkout_one_erc_20_one_not_fee_note,
                        assetResources.getDisplayName(receivingFeeInfo.asset),
                        assetResources.getDisplayName(CryptoCurrency.ETHER),
                        assetResources.getDisplayName(sendingFeeInfo.asset)
                    ),
                    R.string.checkout_fee_link, NETWORK_ERC20_EXPLANATION, context, R.color.blue_600
                )
            }
            sendingFeeInfo.asset.hasFeature(CryptoCurrency.IS_ERC20) &&
                receivingFeeInfo.asset.hasFeature(CryptoCurrency.IS_ERC20) -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        R.string.checkout_both_erc_20_fee_note,
                        assetResources.getDisplayName(sendingFeeInfo.asset),
                        assetResources.getDisplayName(sendingFeeInfo.asset)
                    ),
                    R.string.checkout_fee_link, NETWORK_ERC20_EXPLANATION, context, R.color.blue_600
                )
            }
            else -> {
                null
            }
        }

    private fun getSingleFeeNote(item: FeeInfo, ignoreErc20LinkedNote: Boolean): SpannableStringBuilder =
        when {
            !item.asset.hasFeature(CryptoCurrency.IS_ERC20) || ignoreErc20LinkedNote -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        R.string.checkout_one_fee_note, assetResources.getDisplayName(item.asset)
                    ),
                    R.string.checkout_fee_link, NETWORK_FEE_EXPLANATION, context, R.color.blue_600
                )
            }
            else -> {
                StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                    context.resources.getString(
                        R.string.checkout_one_erc_20_fee_note,
                        assetResources.getDisplayName(item.asset)
                    ),
                    R.string.checkout_fee_link, NETWORK_ERC20_EXPLANATION, context, R.color.blue_600
                )
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun Map<ConfirmationPropertyKey, Any?>.filterNotNullValues(): Map<ConfirmationPropertyKey, Any> =
        this.filterValues { it != null } as Map<ConfirmationPropertyKey, Any>

    private fun getEstimatedFee(sendingFeeInfo: FeeInfo?, receivingFeeInfo: FeeInfo?): String =
        when {
            sendingFeeInfo != null && receivingFeeInfo != null -> {
                val addedFees = sendingFeeInfo.fiatAmount.plus(receivingFeeInfo.fiatAmount)
                context.getString(R.string.checkout_item_network_fee_estimate, addedFees.toStringWithSymbol())
            }
            sendingFeeInfo != null && receivingFeeInfo == null -> {
                context.getString(
                    R.string.checkout_item_network_fee_estimate, sendingFeeInfo.fiatAmount.toStringWithSymbol()
                )
            }
            sendingFeeInfo == null && receivingFeeInfo != null -> {
                context.getString(
                    R.string.checkout_item_network_fee_estimate, receivingFeeInfo.fiatAmount.toStringWithSymbol()
                )
            }
            else -> {
                context.resources.getString(R.string.common_free)
            }
        }
}

class NewSwapExchangeRateFormatter(
    private val context: Context
) :
    TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.NewSwapExchange)
        return mapOf(
            ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.exchange_rate),
            ConfirmationPropertyKey.TITLE to property.unitCryptoCurrency.toStringWithSymbol(),
            ConfirmationPropertyKey.SUBTITLE to property.price.toStringWithSymbol(),
            ConfirmationPropertyKey.LINKED_NOTE to StringUtils.getResolvedStringWithAppendedMappedLearnMore(
                context.resources.getString(
                    R.string.checkout_swap_exchange_note, property.price.symbol, property.unitCryptoCurrency.symbol
                ),
                R.string.common_linked_learn_more, EXCHANGE_SWAP_RATE_EXPLANATION, context, R.color.blue_600
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
            ConfirmationPropertyKey.SUBTITLE to property.totalWithFee.toStringWithSymbol(),
            ConfirmationPropertyKey.IS_IMPORTANT to true
        )
    }
}

class AmountTotalFormatter(private val context: Context) : TxOptionsFormatterNewCheckout {
    override fun format(property: TxConfirmationValue): Map<ConfirmationPropertyKey, Any> {
        require(property is TxConfirmationValue.Amount)
        return mapOf(
            if (property.isImportant) {
                ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.common_total)
            } else {
                ConfirmationPropertyKey.LABEL to context.resources.getString(R.string.amount)
            },
            ConfirmationPropertyKey.TITLE to property.amount.toStringWithSymbol(),
            ConfirmationPropertyKey.IS_IMPORTANT to property.isImportant
        )
    }
}

class FromPropertyFormatter(private val resources: Resources) : TxOptionsFormatter {
    override fun format(property: TxConfirmationValue): Pair<String, String>? =
        if (property is TxConfirmationValue.From)
            resources.getString(R.string.common_from) to property.from
        else null
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

fun Money.formatWithExchange(exchange: Money?) =
    exchange?.let {
        "${this.toStringWithSymbol()} (${it.toStringWithSymbol()})"
    } ?: this.toStringWithSymbol()

fun getLabel(label: String, defaultLabel: String, displayTicker: String): String =
    if (label.isEmpty() || label == defaultLabel) {
        "$displayTicker $defaultLabel"
    } else {
        label
    }