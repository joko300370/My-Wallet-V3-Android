package com.blockchain.preferences

interface BankLinkingPrefs {
    fun setBankLinkingState(state: String)
    fun getBankLinkingState(): String

    fun setDynamicOneTimeTokenUrl(path: String)
    fun getDynamicOneTimeTokenUrl(): String
}