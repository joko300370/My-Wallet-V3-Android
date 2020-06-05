package piuk.blockchain.android.ui.transfer.send

import android.content.ComponentCallbacks
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent

private const val SCOPE_ID = "SENDING_SCOPE_ID"

internal fun createSendScope(): Scope =
    KoinJavaComponent.getKoin().createScope(SCOPE_ID, sendFlowScope)

internal fun sendScope(): Scope =
    KoinJavaComponent.getKoin().getScope(SCOPE_ID)

internal fun closeSendScope() =
    sendScope().close()

internal inline fun <reified T : Any> ComponentCallbacks.sendInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = sendScope().inject<T>(qualifier, parameters)