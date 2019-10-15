package piuk.blockchain.android.data.coinswebsocket

import com.blockchain.android.testutils.rxInit
import com.blockchain.network.websocket.ConnectionEvent
import com.blockchain.network.websocket.WebSocket
import com.google.gson.Gson
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ethereum.Erc20TokenData
import info.blockchain.wallet.ethereum.EthereumWallet
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.`it returns`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.android.data.coinswebsocket.service.MessagesSocketHandler
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel

class CoinsWebSocketStrategyTest {

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    private val messagesSocketHandler: MessagesSocketHandler = mock()
    private val swipeToReceiveHelper: SwipeToReceiveHelper = mock {
        on { getEthReceiveAddress() } `it returns` "0x4058a004dd718babab47e14dd0d744742e5b9903"
    }

    val wallet = mock<EthereumWallet> {
        on { getErc20TokenData(Erc20TokenData.PAX_CONTRACT_NAME) } `it returns` Erc20TokenData().also {
            Erc20TokenData.createPaxTokenData("")
        }
    }

    private val ethDataManager: EthDataManager = mock {
        on { getEthWallet() } `it returns` wallet
        on { getErc20TokenData(CryptoCurrency.PAX) } `it returns` Erc20TokenData().also {
            Erc20TokenData.createPaxTokenData("")
        }
        on { fetchEthAddress() } `it returns` Observable.just(CombinedEthModel(EthAddressResponseMap()))
    }

    private val stringUtils: StringUtils = mock {
        on { getString(R.string.app_name) } `it returns` "Blockchain"
        on { getString(R.string.received_ethereum) } `it returns` "Received Ether"
        on { getString(R.string.received_usd_pax) } `it returns` "Received USD PAX"
        on { getString(R.string.from) } `it returns` "From"
    }
    private val erc20Account: Erc20Account = mock {
        on { fetchAddressCompletable() } `it returns` Completable.complete()
    }

    private val mockWebSocket: WebSocket<String, String> = mock()
    private val webSocket = FakeWebSocket(mockWebSocket)

    private val strategy = CoinsWebSocketStrategy(
        webSocket,
        ethDataManager,
        swipeToReceiveHelper,
        stringUtils,
        Gson(),
        erc20Account
    )

    @Before
    fun setup() {
        strategy.setMessagesHandler(messagesSocketHandler)
        strategy.open()
    }

    @Test
    fun `notification should be triggered on confirmed eth transaction`() {
        webSocket.send(confirmedEtheTransaction)

        verify(mockWebSocket).open()
        verify(messagesSocketHandler).triggerNotification("Blockchain",
            "Received Ether 0.00604741 ETH",
            "Received Ether 0.00604741 ETH from 0x4058a004dd718babab47e14dd0d744742e5b9903")
    }

    @Test
    fun `notification should not be triggered on pending eth transaction`() {
        webSocket.send(pendingEthTransaction)

        verify(mockWebSocket).open()
        verify(messagesSocketHandler, never()).triggerNotification(any(), any(), any())
    }

    @Test
    fun `eth transaction should be update eth transactions and broadcasted`() {
        webSocket.send(pendingEthTransaction)

        verify(mockWebSocket).open()
        verify(ethDataManager).fetchEthAddress()
        verify(erc20Account, never()).fetchAddressCompletable()
        verify(messagesSocketHandler).sendBroadcast(BalanceFragment.ACTION_INTENT)
    }

    @Test
    fun `pax transaction should be update pax transactions and broadcasted`() {
        webSocket.send(paxTransaction)

        verify(mockWebSocket).open()
        verify(ethDataManager, never()).fetchEthAddress()
        verify(erc20Account).fetchAddressCompletable()
        verify(messagesSocketHandler).triggerNotification("Blockchain",
            "Received USD PAX 1.21 PAX",
            "Received USD PAX 1.21 PAX from 0x4058a004dd718babab47e14dd0d744742e5b9903")
        verify(messagesSocketHandler).sendBroadcast(BalanceFragment.ACTION_INTENT)
    }

    private class FakeWebSocket(mock: WebSocket<String, String>) : WebSocket<String, String> by mock {
        private val _sendSubject = PublishSubject.create<String>()

        override val connectionEvents: Observable<ConnectionEvent>
            get() = Observable.just(ConnectionEvent.Connected)

        override fun send(message: String) {
            _sendSubject.onNext(message)
        }

        override val responses: Observable<String>
            get() = _sendSubject
    }

    private val confirmedEtheTransaction =
        "{\"coin\":\"eth\",\"entity\":\"account\",\"address\":\"0x4058a004dd718babab47e14dd0d744742e5b9903\"," +
                "\"txHash\":\"0xe1ff1e0ea7023c80308302d809684f90d1c094f969a13343e6081197f3552c97\"," +
                "\"transaction\":{\"hash\":" +
                "\"0xe1ff1e0ea7023c80308302d809684f90d1c094f969a13343e6081197f3552c97\",\"blockHash\":\"0xd240c9a0" +
                "9f605854926d4259c6ea95d72553087a7a20b25a34f26189d9a6930e\",\"blockNumber\":8381040,\"from\":\"0x4" +
                "058a004dd718babab47e14dd0d744742e5b9903\",\"to\":\"0x4058a004dd718babab47e14dd0d744742e5b9903\",\"co" +
                "ntractAddress\":\"0x\",\"value\":\"6047410000000000\",\"nonce\":171,\"gasPrice\":\"4000000000\",\"ga" +
                "sLimit\":21000,\"gasUsed\":21000,\"data\":\"\",\"transactionIndex\":59,\"success\":true,\"err" +
                "or\":\"\",\"firstSeen\":0,\"timestamp\":1566220763,\"state\":\"confirmed\"}}"
    private val pendingEthTransaction =
        "{\"coin\":\"eth\",\"entity\":\"account\",\"address\":\"0x4058a004dd718babab47e14dd0d744742e5b9903\",\"txHa" +
                "sh\":\"0xe1ff1e0ea7023c80308302d809684f90d1c094f969a13343e6081197f3552c97\",\"transaction\"" +
                ":{\"hash\":\"0xe1ff1e0ea7023c80308302d809684f90d1c094" +
                "f969a13343e6081197f3552c97\",\"blockHash\":\"0xd240c9a09f605" +
                "854926d4259c6ea95d72553087a7a20b25a34f26189d9a6930e\",\"blockNumber\":8381040,\"from\":\"0x4058a004" +
                "dd718babab47e14dd0d744742e5b9903\",\"to\":\"0x4058a004dd718babab47e14dd0d744742e5b9903\",\"contract" +
                "Address\":\"0x\",\"value\":\"6047410000000000\",\"nonce\":171,\"gasPrice\":\"4000000000\",\"gasLi" +
                "mit\":21000,\"gasUsed\":21000,\"data\":\"\",\"transactionIndex\":59,\"success\":true,\"error\":" +
                "\"\",\"firstSeen\":0,\"timestamp\":1566220763,\"state\":\"pending\"}}"

    private val paxTransaction =
        "{\"coin\":\"eth\",\"entity\":\"token_account\",\"param\":{\"accountAddress\":\"0x4058a004dd718babab47e14dd0" +
                "d744742e5b9903\",\"tokenAddress\":\"0x8e870d67f660d95d5be530380d0ec0bd388289e1\"},\"tokenTransf" +
                "er\":{\"blockHash\":\"0x1293676c93d91660ca4ec40df09b6ec4fa080138d975c19813b914befc1187c\",\"transact" +
                "ionHash\":\"0x3cd2e95358c58af6e9ecd2f0af6739c3db945e2259bf2a4bc91fb5e2f397ad89\",\"blockNumber\":83" +
                "62036,\"tokenHash\":\"0x8e870d67f660d95d5be530380d0ec0bd388289e1\",\"logIndex\":67,\"from\":\"0x4058" +
                "a004dd718babab47e14dd0d744742e5b9903\",\"to\":\"0x4058a004dd718babab47e14dd0d744742e5b9903\",\"val" +
                "ue\":1210000000000000000,\"decimals\":18,\"timestamp\":0}}"
}