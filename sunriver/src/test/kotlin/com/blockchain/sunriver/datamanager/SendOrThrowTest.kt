package com.blockchain.sunriver.datamanager

import com.blockchain.sunriver.SendDetails
import com.blockchain.sunriver.SendException
import com.blockchain.sunriver.SendFundsResult
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.sendFundsOrThrow
import com.blockchain.testutils.ether
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.AccountReference
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be`
import org.junit.Test

class SendOrThrowTest {

    @Test
    fun `successful send`() {
        val details = SendDetails(
            from = AccountReference.Ethereum("", ""),
            toAddress = "",
            value = 100.ether(),
            fee = 1.ether()
        )
        val fundsResult = SendFundsResult(
            errorCode = 0,
            confirmationDetails = null,
            hash = "123",
            sendDetails = details
        )
        val mock = mock<XlmDataManager> {
            on { sendFunds(details) } `it returns` Single.just(
                fundsResult
            )
        }
        mock.sendFundsOrThrow(details)
            .test()
            .values().single() `should be` fundsResult
    }

    @Test
    fun `failed send - non-zero error code`() {
        val details = SendDetails(
            from = AccountReference.Ethereum("", ""),
            toAddress = "",
            value = 100.ether(),
            fee = 1.ether()
        )
        val fundsResult = SendFundsResult(
            errorCode = 1,
            confirmationDetails = null,
            hash = "123",
            sendDetails = details
        )
        val mock = mock<XlmDataManager> {
            on { sendFunds(details) } `it returns` Single.just(
                fundsResult
            )
        }
        mock.sendFundsOrThrow(details)
            .test()
            .assertError(SendException::class.java)
            .assertError { (it as SendException).details === details }
    }

    @Test
    fun `failed send - no hash`() {
        val details = SendDetails(
            from = AccountReference.Ethereum("", ""),
            toAddress = "",
            value = 100.ether(),
            fee = 1.ether()
        )
        val fundsResult = SendFundsResult(
            errorCode = 0,
            confirmationDetails = null,
            hash = null,
            sendDetails = details
        )
        val mock = mock<XlmDataManager> {
            on { sendFunds(details) } `it returns` Single.just(
                fundsResult
            )
        }
        mock.sendFundsOrThrow(details)
            .test()
            .assertError(SendException::class.java)
            .assertError { (it as SendException).details === details }
    }
}
