package com.blockchain.preferences

interface SimpleBuyPrefs {
    fun simpleBuyState(): String?
    fun updateSimpleBuyState(simpleBuyState: String)
    fun clearState()
    fun setFlowStartedAtLeastOnce()
    fun flowStartedAtLeastOnce(): Boolean
}