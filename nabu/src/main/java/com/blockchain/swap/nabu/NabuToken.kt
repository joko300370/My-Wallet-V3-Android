package com.blockchain.swap.nabu

import com.blockchain.swap.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import io.reactivex.Single

interface NabuToken {

    /**
     * Find or creates the token
     */
    fun fetchNabuToken(currency: String? = null, action: String? = null): Single<NabuOfflineTokenResponse>
}
