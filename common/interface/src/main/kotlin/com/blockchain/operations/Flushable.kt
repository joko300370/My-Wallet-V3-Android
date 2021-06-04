package com.blockchain.operations

import io.reactivex.Completable

interface Flushable {
    fun flush(): Completable
}

interface AppStartUpFlushable : Flushable {
    val tag: String
}