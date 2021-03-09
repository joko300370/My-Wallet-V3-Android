package com.blockchain.nabu.datamanagers

import com.blockchain.nabu.api.status.StatusService
import com.blockchain.nabu.models.responses.status.Component
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

interface ApiStatus {
    fun isHealthy(): Single<Boolean>
}

/*Logic: if there is an incident with a component name = Wallet whose component status is NOT Operational,*/
class BlockchainApiStatus(private val statusService: StatusService) : ApiStatus {
    override fun isHealthy(): Single<Boolean> {
        return statusService.apiIncidents().map {
            val walletComponents =
                it.incidents.map { incident -> incident.components }.flatten()
                    .filter { component -> component.name == Component.WALLET }

            walletComponents.all { component ->
                component.status == Component.OPERATIONAL
            }
        }.subscribeOn(Schedulers.io())
    }
}