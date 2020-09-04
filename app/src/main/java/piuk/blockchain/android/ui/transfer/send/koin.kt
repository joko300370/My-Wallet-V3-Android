package piuk.blockchain.android.ui.transfer.send

import android.content.Context
import com.blockchain.koin.payloadScope
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.transfer.send.flow.FeePropertyFormatter
import piuk.blockchain.android.ui.transfer.send.flow.ExchangePriceFormatter
import piuk.blockchain.android.ui.transfer.send.flow.FeedTotalFormatter
import piuk.blockchain.android.ui.transfer.send.flow.FromPropertyFormatter
import piuk.blockchain.android.ui.transfer.send.flow.SendFlowCustomiser
import piuk.blockchain.android.ui.transfer.send.flow.SendFlowCustomiserImpl
import piuk.blockchain.android.ui.transfer.send.flow.ToPropertyFormatter
import piuk.blockchain.android.ui.transfer.send.flow.TotalFormatter
import piuk.blockchain.android.ui.transfer.send.flow.TxConfirmReadOnlyMapper
import piuk.blockchain.android.ui.transfer.send.flow.TxOptionsFormatter

val sendFlowScope = named("SendScope")

val transferModule = module {

    factory {
        SendFlowCustomiserImpl(
            resources = get<Context>().resources
        )
    }.bind(SendFlowCustomiser::class)

    factory {
        ExchangePriceFormatter(
            resources = get<Context>().resources
        )
    }.bind(TxOptionsFormatter::class)

    factory {
        TotalFormatter(
            resources = get<Context>().resources
        )
    }.bind(TxOptionsFormatter::class)

    factory {
        FromPropertyFormatter(
            resources = get<Context>().resources
        )
    }.bind(TxOptionsFormatter::class)

    factory {
        FeedTotalFormatter(
            resources = get<Context>().resources
        )
    }.bind(TxOptionsFormatter::class)

    factory {
        FeePropertyFormatter(
            resources = get<Context>().resources
        )
    }.bind(TxOptionsFormatter::class)

    factory {
        ToPropertyFormatter(
            resources = get<Context>().resources
        )
    }.bind(TxOptionsFormatter::class)

    factory {
        TxConfirmReadOnlyMapper(
            formatters = getAll()
        )
    }

    scope(sendFlowScope) {

        scoped {
            SendInteractor(
                coincore = payloadScope.get(),
                currencyPrefs = payloadScope.get(),
                addressFactory = payloadScope.get()
            )
        }

        scoped {
            SendModel(
                initialState = SendState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get()
            )
        }
    }
}
