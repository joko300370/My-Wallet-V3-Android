package piuk.blockchain.android.ui.transfer.send

import com.blockchain.koin.payloadScope
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.core.qualifier.named
import org.koin.dsl.module

val sendFlowScope = named("SendScope")

val transferModule = module {

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
