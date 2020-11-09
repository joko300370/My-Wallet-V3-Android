package com.blockchain.sunriver

import com.blockchain.sunriver.models.XlmTransaction
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.withMajorValue
import org.stellar.sdk.KeyPair
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.OperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse

internal fun List<OperationResponse>.map(accountId: String, horizonProxy: HorizonProxy): List<XlmTransaction> =
    filter { it is CreateAccountOperationResponse || it is PaymentOperationResponse }
        .map {
            mapOperationResponse(
                it,
                accountId,
                horizonProxy
            )
        }

internal fun mapOperationResponse(
    operationResponse: OperationResponse,
    usersAccountId: String,
    horizonProxy: HorizonProxy
): XlmTransaction =
    when (operationResponse) {
        is CreateAccountOperationResponse -> operationResponse.mapCreate(usersAccountId, horizonProxy)
        is PaymentOperationResponse -> operationResponse.mapPayment(usersAccountId, horizonProxy)
        else -> throw IllegalArgumentException("Unsupported operation type ${operationResponse.javaClass.simpleName}")
    }

private fun CreateAccountOperationResponse.mapCreate(
    usersAccountId: String,
    horizonProxy: HorizonProxy
): XlmTransaction {
    return try {
        val transactionResponse = horizonProxy.getTransaction(transactionHash)
        val fee = CryptoValue.fromMinor(CryptoCurrency.XLM, transactionResponse.feeCharged.toBigInteger())
        toXlmTransaction(usersAccountId, startingBalance, account, funder, fee)
    } catch (e: Throwable) {
        // There's a bug in the xlm sdk (horizonProxy.getTransaction()) which throws a
        // NoSuchMethodError when parsing a int memo on pre jdk 1.8 devices
        // In this case, we can't know the fee but everything else is known, so:
        toXlmTransaction(usersAccountId, startingBalance, account, funder, CryptoValue.ZeroXlm)
    }
}

private fun PaymentOperationResponse.mapPayment(
    usersAccountId: String,
    horizonProxy: HorizonProxy
): XlmTransaction {
    return try {
        val transactionResponse = horizonProxy.getTransaction(transactionHash)
        val fee = CryptoValue.fromMinor(CryptoCurrency.XLM, transactionResponse.feeCharged.toBigInteger())
        toXlmTransaction(usersAccountId, amount, to, from, fee)
    } catch (e: Throwable) {
        // There's a bug in the xlm sdk (horizonProxy.getTransaction()) which throws a
        // NoSuchMethodError when parsing a int memo on pre jdk 1.8 devices
        // In this case, we can't know the fee but everything else is known, so:
        toXlmTransaction(usersAccountId, amount, to, from, CryptoValue.ZeroXlm)
    }
}

private fun OperationResponse.toXlmTransaction(
    usersAccountId: String,
    amount: String,
    to: String,
    from: String,
    fee: CryptoValue
) = XlmTransaction(
        timeStamp = createdAt,
        value = deltaValueForAccount(usersAccountId, KeyPair.fromAccountId(from), amount),
        fee = fee,
        hash = transactionHash,
        to = KeyPair.fromAccountId(to).toHorizonKeyPair().neuter(),
        from = KeyPair.fromAccountId(from).toHorizonKeyPair().neuter()
    )

private fun deltaValueForAccount(
    usersAccountId: String,
    from: KeyPair,
    value: String
): CryptoValue {
    val valueAsBigDecimal = value.toBigDecimal()
    val deltaForThisAccount =
        if (from.accountId == usersAccountId) {
            valueAsBigDecimal.negate()
        } else {
            valueAsBigDecimal
        }
    return CryptoCurrency.XLM.withMajorValue(deltaForThisAccount)
}
