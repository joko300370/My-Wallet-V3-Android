package com.blockchain.koin

import org.koin.core.scope.Scope
import org.koin.java.KoinJavaComponent

private const val SCOPE_ID = "SCOPE_ID"

val payloadScope: Scope
    get() = KoinJavaComponent.getKoin().getOrCreateScope(SCOPE_ID, payloadScopeQualifier)
