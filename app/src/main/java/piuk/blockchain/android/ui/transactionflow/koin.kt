package piuk.blockchain.android.ui.transactionflow

import android.content.Context
import com.blockchain.koin.payloadScope
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.engine.TransactionInteractor
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TxFlowErrorReporting
import piuk.blockchain.android.ui.transactionflow.flow.ActiveTransactionFlow
import piuk.blockchain.android.ui.transactionflow.flow.AmountFormatter
import piuk.blockchain.android.ui.transactionflow.flow.CompoundNetworkFeeFormatter
import piuk.blockchain.android.ui.transactionflow.flow.EstimatedCompletionPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.ExchangePriceFormatter
import piuk.blockchain.android.ui.transactionflow.flow.FromPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.NetworkFormatter
import piuk.blockchain.android.ui.transactionflow.flow.PaymentMethodPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.SalePropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.SwapExchangeRateFormatter
import piuk.blockchain.android.ui.transactionflow.flow.ToPropertyFormatter
import piuk.blockchain.android.ui.transactionflow.flow.TotalFormatter
import piuk.blockchain.android.ui.transactionflow.flow.TransactionFeeFormatter
import piuk.blockchain.android.ui.transactionflow.flow.TxConfirmReadOnlyMapperCheckout
import piuk.blockchain.android.ui.transactionflow.flow.TxOptionsFormatterCheckout
import piuk.blockchain.android.ui.transactionflow.flow.customisations.EnterAmountCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.SourceSelectionCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TargetSelectionCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionConfirmationCustomisations
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiser
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionFlowCustomiserImpl
import piuk.blockchain.android.ui.transactionflow.flow.customisations.TransactionProgressCustomisations

val transactionFlowScope = named("TransactionScope")

val transactionModule = module {

    factory {
        TransactionFlowCustomiserImpl(
            resources = get<Context>().resources,
            assetResources = get(),
            stringUtils = get()
        )
    }.bind(TransactionFlowCustomiser::class)
        .bind(EnterAmountCustomisations::class)
        .bind(SourceSelectionCustomisations::class)
        .bind(TargetSelectionCustomisations::class)
        .bind(TransactionConfirmationCustomisations::class)
        .bind(TransactionProgressCustomisations::class)

    factory {
        TransactionLauncher(
            flags = get(),
            context = get()
        )
    }

    factory {
        ExchangePriceFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        ToPropertyFormatter(
            context = get(),
            defaultLabel = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        FromPropertyFormatter(
            context = get(),
            defaultLabel = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        SalePropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        EstimatedCompletionPropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        PaymentMethodPropertyFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        SwapExchangeRateFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        NetworkFormatter(
            context = get(),
            assetResources = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        TransactionFeeFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        CompoundNetworkFeeFormatter(
            context = get(),
            assetResources = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        TotalFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        AmountFormatter(
            context = get()
        )
    }.bind(TxOptionsFormatterCheckout::class)

    factory {
        TxConfirmReadOnlyMapperCheckout(
            formatters = getAll()
        )
    }

    factory {
        TxFlowAnalytics(
            analytics = get(),
            crashLogger = get()
        )
    }

    factory {
        TxFlowErrorReporting(
            crashLogger = get()
        )
    }

    scope(transactionFlowScope) {

        scoped {
            TransactionInteractor(
                coincore = payloadScope.get(),
                addressFactory = payloadScope.get(),
                custodialRepository = payloadScope.get(),
                custodialWalletManager = payloadScope.get(),
                currencyPrefs = get(),
                eligibilityProvider = payloadScope.get(),
                accountsSorting = get(),
                linkedBanksFactory = payloadScope.get(),
                bankLinkingPrefs = payloadScope.get()
            )
        }

        // hack. find a better way to handle flow navigation (Rx Activity result)
        scoped {
            ActiveTransactionFlow()
        }

        scoped {
            TransactionModel(
                initialState = TransactionState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                errorLogger = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }
    }
}
