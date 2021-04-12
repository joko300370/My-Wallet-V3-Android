package com.blockchain.preferences

interface BankLinkingPrefs {
    fun setBankLinkingState(state: String)
    fun getBankLinkingState(): String
}