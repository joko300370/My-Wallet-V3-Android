package com.blockchain.sunriver

import com.blockchain.testutils.lumens
import com.blockchain.testutils.stroops
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.ManageDataOperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import java.util.Locale

class HorizonOperationMappingTest {

    @Before
    fun setup() {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `map response rejects unsupported types`() {
        val unsupportedResponse: ManageDataOperationResponse = mock();
        {
            mapOperationResponse(unsupportedResponse, "", givenHorizonProxy(100))
        } `should throw` IllegalArgumentException::class
    }

    @Test
    fun `map payment operation where account is the receiver`() {
        val myAccount = "GDCERC7BR5N6NFK5B74XTTTA5OLC3YPWODQ5CHKRCRU6IVXFYP364JG7"
        val otherAccount = "GBPF72LVHGENTAC6JCBDU6KG6GNTQIHTTIYZGURQQL3CWXEBVNSUVFPL"
        mapOperationResponse(mock<PaymentOperationResponse> {
            on { from } `it returns`otherAccount
            on { to } `it returns` myAccount
            on { transactionHash } `it returns` "ABCD"
            on { createdAt } `it returns` "TIME"
            on { amount } `it returns` 50.lumens().toStringWithoutSymbol()
        }, myAccount, givenHorizonProxy(100))
            .apply {
                hash `should equal` "ABCD"
                timeStamp `should equal` "TIME"
                fee `should equal` 100.stroops()
                from.accountId `should equal` otherAccount
                to.accountId `should equal` myAccount
                value `should equal` 50.lumens()
            }
    }

    @Test
    fun `map payment operation where account is the sender`() {
        val myAccount = "GDCERC7BR5N6NFK5B74XTTTA5OLC3YPWODQ5CHKRCRU6IVXFYP364JG7"
        val otherAccount = "GBPF72LVHGENTAC6JCBDU6KG6GNTQIHTTIYZGURQQL3CWXEBVNSUVFPL"
        mapOperationResponse(mock<PaymentOperationResponse> {
            on { from } `it returns`myAccount
            on { to } `it returns`otherAccount
            on { transactionHash } `it returns` "ABCD"
            on { createdAt } `it returns` "TIME"
            on { amount } `it returns` 50.lumens().toStringWithoutSymbol()
        }, myAccount, givenHorizonProxy(100))
            .apply {
                hash `should equal` "ABCD"
                timeStamp `should equal` "TIME"
                fee `should equal` 100.stroops()
                from.accountId `should equal` myAccount
                to.accountId `should equal` otherAccount
                value `should equal` (-50).lumens()
            }
    }

    @Test
    fun `map create operation where account is the receiver`() {
        val myAccount = "GDCERC7BR5N6NFK5B74XTTTA5OLC3YPWODQ5CHKRCRU6IVXFYP364JG7"
        val otherAccount = "GBPF72LVHGENTAC6JCBDU6KG6GNTQIHTTIYZGURQQL3CWXEBVNSUVFPL"
        mapOperationResponse(mock<CreateAccountOperationResponse> {
            on { funder } `it returns` otherAccount
            on { account } `it returns` myAccount
            on { transactionHash } `it returns` "ABCD"
            on { createdAt } `it returns` "TIME"
            on { startingBalance } `it returns` 100.lumens().toStringWithoutSymbol()
        }, myAccount, givenHorizonProxy(100))
            .apply {
                hash `should equal` "ABCD"
                timeStamp `should equal` "TIME"
                fee `should equal` 100.stroops()
                from.accountId `should equal` otherAccount
                to.accountId `should equal` myAccount
                value `should equal` 100.lumens()
            }
    }

    @Test
    fun `map create operation where account is the sender`() {
        val myAccount = "GDCERC7BR5N6NFK5B74XTTTA5OLC3YPWODQ5CHKRCRU6IVXFYP364JG7"
        val otherAccount = "GBPF72LVHGENTAC6JCBDU6KG6GNTQIHTTIYZGURQQL3CWXEBVNSUVFPL"
        mapOperationResponse(mock<CreateAccountOperationResponse> {
            on { funder } `it returns` myAccount
            on { account } `it returns` otherAccount
            on { transactionHash } `it returns` "ABCD"
            on { createdAt } `it returns` "TIME"
            on { startingBalance } `it returns` 100.lumens().toStringWithoutSymbol()
        }, myAccount, givenHorizonProxy(100))
            .apply {
                hash `should equal` "ABCD"
                timeStamp `should equal` "TIME"
                fee `should equal` 100.stroops()
                from.accountId `should equal` myAccount
                to.accountId `should equal` otherAccount
                value `should equal` (-100).lumens()
            }
    }

    private fun givenHorizonProxy(fee: Long): HorizonProxy {
        val mockTx: TransactionResponse = mock {
            on { feeCharged } `it returns` fee
        }
        return mock {
            on { getTransaction(any()) } `it returns` mockTx
        }
    }
}
