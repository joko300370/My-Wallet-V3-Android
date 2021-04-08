package com.blockchain.preferences

interface BankLinkingPrefs {
    fun setBankLinkingInfo(bankLinkingInfo: String)
    fun getBankLinkingInfo(): String
    fun clearBankLinkingInfo()
    fun getPaymentApprovalConsumed(): Boolean
    fun setPaymentApprovalConsumed(state: Boolean)
    fun setFiatDepositApprovalInProgress(state: String)
    fun getFiatDepositApprovalInProgress(): String
}