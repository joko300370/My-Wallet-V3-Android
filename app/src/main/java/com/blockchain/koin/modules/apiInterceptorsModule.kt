package com.blockchain.koin.modules

import android.os.Build
import com.blockchain.network.modules.OkHttpInterceptors
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.facebook.stetho.okhttp3.StethoInterceptor
import info.blockchain.wallet.api.Environment
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.api.interceptors.ApiInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.DeviceIdInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.UserAgentInterceptor
import piuk.blockchain.androidcore.utils.PersistentPrefs

val apiInterceptorsModule = module {

    single {
        val env: EnvironmentConfig = get()
        val versionName = BuildConfig.VERSION_NAME.removeSuffix(BuildConfig.VERSION_NAME_SUFFIX)
        OkHttpInterceptors(
            if (env.isRunningInDebugMode()) {
                listOfNotNull(
                    // Stetho for debugging network ops via Chrome
                    StethoInterceptor(),
                    // Add logging for debugging purposes
                    ApiInterceptor(),
                    // Add header in all requests
                    UserAgentInterceptor(versionName, Build.VERSION.RELEASE),
                    DeviceIdInterceptor(prefs = lazy { get<PersistentPrefs>() }, get()),
                    if (env.environment != Environment.PRODUCTION) {
                            ChuckerInterceptor.Builder(androidContext())
                                .build()
                    } else {
                        null
                    }
                )
            } else {
                listOf(
                    // Add header in all requests
                    UserAgentInterceptor(versionName, Build.VERSION.RELEASE),
                    DeviceIdInterceptor(prefs = lazy { get<PersistentPrefs>() }, get())
                )
            })
    }
}
