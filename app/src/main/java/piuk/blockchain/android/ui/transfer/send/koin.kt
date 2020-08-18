package piuk.blockchain.android.ui.transfer.send

import android.content.Context
import com.blockchain.koin.payloadScope
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import piuk.blockchain.android.ui.transfer.send.flow.SendFlowCustomiser
import piuk.blockchain.android.ui.transfer.send.flow.SendFlowCustomiserImpl

val sendFlowScope = named("SendScope")

val transferModule = module {

    factory {
        SendFlowCustomiserImpl(
            resources = get<Context>().resources
        )
    }.bind(SendFlowCustomiser::class)

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
