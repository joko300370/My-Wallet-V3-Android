package com.blockchain.koin

import android.content.ComponentCallbacks
import org.koin.core.KoinComponent
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

inline fun <reified T : Any> ComponentCallbacks.scopedInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
) = payloadScope.inject<T>(qualifier, parameters)

inline fun <reified T> KoinComponent.scopedInject(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null
): Lazy<T> =
    lazy(LazyThreadSafetyMode.NONE) { payloadScope.get<T>(qualifier, parameters) }