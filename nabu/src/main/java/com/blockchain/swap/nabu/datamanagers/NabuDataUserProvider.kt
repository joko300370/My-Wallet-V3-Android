package com.blockchain.swap.nabu.datamanagers

import com.blockchain.swap.nabu.models.responses.nabu.NabuUser
import com.blockchain.swap.nabu.NabuToken
import io.reactivex.Single

interface NabuDataUserProvider {

    fun getUser(): Single<NabuUser>
}

internal class NabuDataUserProviderNabuDataManagerAdapter(
    private val nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager
) : NabuDataUserProvider {

    override fun getUser(): Single<NabuUser> =
        nabuToken
            .fetchNabuToken()
            .flatMap(nabuDataManager::getUser)
}
