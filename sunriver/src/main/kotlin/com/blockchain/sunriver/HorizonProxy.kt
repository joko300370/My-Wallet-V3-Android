package com.blockchain.sunriver

import info.blockchain.balance.CryptoValue
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Network
import org.stellar.sdk.Server
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.responses.AccountResponse
import org.stellar.sdk.responses.operations.OperationResponse

internal class HorizonProxy(url: String) {

    private val server = Server(url)

    init {
        if (url.contains("test")) {
            Network.useTestNetwork()
        } else {
            Network.usePublicNetwork()
        }
    }

    fun accountExists(accountId: String) = findAccount(accountId) != null

    fun getBalance(accountId: String): CryptoValue {
        val account = findAccount(accountId)
        return account?.balances?.firstOrNull {
            it.assetType == "native" && it.assetCode == null
        }?.balance?.let { CryptoValue.lumensFromMajor(it.toBigDecimal()) }
            ?: CryptoValue.ZeroXlm
    }

    private fun findAccount(accountId: String): AccountResponse? {
        val accounts = server.accounts()
        return try {
            accounts.account(KeyPair.fromAccountId(accountId))
        } catch (e: ErrorResponse) {
            if (e.code == 404) {
                null
            } else {
                throw e
            }
        }
    }

    fun getTransactionList(accountId: String): List<OperationResponse> = try {
        server.operations()
            .forAccount(KeyPair.fromAccountId(accountId))
            .execute()
            .records
    } catch (e: ErrorResponse) {
        if (e.code == 404) {
            emptyList()
        } else {
            throw e
        }
    }
}
