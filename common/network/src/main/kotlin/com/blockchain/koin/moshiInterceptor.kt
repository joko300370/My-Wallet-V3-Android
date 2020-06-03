package com.blockchain.koin

import com.blockchain.network.modules.MoshiBuilderInterceptor
import com.squareup.moshi.Moshi
import org.koin.core.module.Module
import org.koin.core.qualifier.StringQualifier

fun Module.moshiInterceptor(qualifier: StringQualifier, function: (builder: Moshi.Builder) -> Unit) =
    single(qualifier) {
        object : MoshiBuilderInterceptor {
            override fun intercept(builder: Moshi.Builder) {
                function(builder)
            }
        } as MoshiBuilderInterceptor
    }
