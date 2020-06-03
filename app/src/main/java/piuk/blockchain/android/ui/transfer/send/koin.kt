package piuk.blockchain.android.ui.transfer.send

import android.content.ComponentCallbacks
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent

val sendFlowScope = named("SendScope")

val transferModule = module {

    scope(sendFlowScope) {

        scoped {
            SendInteractor(
                coincore = get()
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

private const val SCOPE_ID = "SENDING_SCOPE_ID"

val sendScope: Scope
    get() = KoinJavaComponent.getKoin().getOrCreateScope(SCOPE_ID, sendFlowScope)

inline fun <reified T : Any> ComponentCallbacks.sendInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = sendScope.inject<T>(qualifier, parameters)
