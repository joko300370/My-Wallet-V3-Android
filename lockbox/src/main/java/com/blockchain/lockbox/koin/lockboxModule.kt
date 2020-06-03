package com.blockchain.lockbox.koin

import com.blockchain.accounts.AccountList
import com.blockchain.koin.lockbox
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.lockbox.data.remoteconfig.LockboxRemoteConfig
import com.blockchain.lockbox.ui.LockboxLandingPresenter
import com.blockchain.remoteconfig.FeatureFlag
import org.koin.dsl.bind
import org.koin.dsl.module

val lockboxModule = module {

    scope(payloadScopeQualifier) {

        factory { LockboxDataManager(get(), get(lockbox)) }

        factory(lockbox) { get<LockboxDataManager>() }.bind(AccountList::class)

        factory { LockboxLandingPresenter(get(), get()) }
    }

    factory(lockbox) { LockboxRemoteConfig(get()) }
        .bind(FeatureFlag::class)
}
