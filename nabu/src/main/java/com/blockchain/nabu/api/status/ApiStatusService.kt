package com.blockchain.nabu.api.status

import com.blockchain.nabu.models.responses.status.ApiIncidentsResponse
import io.reactivex.Single
import retrofit2.http.GET

interface ApiStatusService {
    @GET(INCIDENTS_PATH)
    fun apiIncidents(): Single<ApiIncidentsResponse>
}

private const val INCIDENTS_PATH = "api/v2/incidents.json"