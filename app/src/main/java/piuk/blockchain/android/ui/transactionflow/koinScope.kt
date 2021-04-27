package piuk.blockchain.android.ui.transactionflow

import android.content.ComponentCallbacks
import org.koin.core.KoinComponent
import org.koin.core.error.ScopeNotCreatedException
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent
import timber.log.Timber

private const val SCOPE_ID = "TRANSACTION_SCOPE_ID"

internal fun createTransactionScope(): Scope =
    KoinJavaComponent.getKoin().createScope(
        SCOPE_ID,
        transactionFlowScope
    )

internal fun transactionScope(): Scope =
    KoinJavaComponent.getKoin().getScope(SCOPE_ID)

internal fun transactionScopeOrNull(): Scope? =
    KoinJavaComponent.getKoin().getScopeOrNull(SCOPE_ID)

internal fun closeTransactionScope() =
    try {
        transactionScope().close()
    } catch (t: ScopeNotCreatedException) {
        Timber.e("Cannot close a non-existent scope")
    }

internal inline fun <reified T : Any> ComponentCallbacks.transactionInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = transactionScope().inject<T>(qualifier, parameters)

internal inline fun <reified T> KoinComponent.transactionInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) { transactionScope().get<T>(qualifier, parameters) }
