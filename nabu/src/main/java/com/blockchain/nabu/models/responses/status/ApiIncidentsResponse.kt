package com.blockchain.nabu.models.responses.status

class ApiIncidentsResponse(val incidents: List<Incident>)

class Incident(val components: List<Component>)

class Component(val name: String, val status: String) {
    companion object {
        const val WALLET = "Wallet"
        const val OPERATIONAL = "Operational"
    }
}