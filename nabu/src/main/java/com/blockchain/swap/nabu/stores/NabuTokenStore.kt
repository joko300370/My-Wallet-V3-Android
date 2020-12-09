package com.blockchain.swap.nabu.stores

import com.blockchain.swap.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.utils.Optional
import io.reactivex.Observable

interface NabuTokenStore {

    fun getAccessToken(): Observable<Optional<NabuSessionTokenResponse>>
}