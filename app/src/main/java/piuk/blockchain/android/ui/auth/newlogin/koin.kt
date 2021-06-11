package piuk.blockchain.android.ui.auth.newlogin

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module

val secureChannelModule = module {

    scope(payloadScopeQualifier) {

        factory {
            AuthNewLoginModel(
                initialState = AuthNewLoginState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                environmentConfig = get(),
                crashLogger = get(),
                secureChannelManager = get(),
                secureChannelPrefs = get(),
                walletApi = get()
            )
        }
    }
}