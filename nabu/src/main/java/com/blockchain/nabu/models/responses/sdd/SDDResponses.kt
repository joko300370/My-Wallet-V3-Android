package com.blockchain.nabu.models.responses.sdd

data class SDDEligibilityResponse(val eligible: Boolean, val tier: Int)

data class SDDStatusResponse(val verified: Boolean, val taskComplete: Boolean)