package piuk.blockchain.android.ui.activity.detail
// 
// import com.blockchain.android.testutils.rxInit
// import com.blockchain.sunriver.XlmDataManager
// import com.blockchain.testutils.usd
// import com.nhaarman.mockito_kotlin.atLeastOnce
// import com.nhaarman.mockito_kotlin.mock
// import com.nhaarman.mockito_kotlin.whenever
// import info.blockchain.balance.AccountReference
// import info.blockchain.balance.CryptoCurrency
// import info.blockchain.wallet.multiaddress.TransactionSummary
// import info.blockchain.wallet.payload.data.Wallet
// import io.reactivex.Observable
// import io.reactivex.Single
// import org.amshove.kluent.any
// import org.amshove.kluent.mock
// import org.apache.commons.lang3.tuple.Pair
// import org.junit.Before
// import org.junit.Rule
// import org.junit.Test
// import org.mockito.Answers
// import org.mockito.Mockito.verify
// import org.mockito.Mockito.verifyNoMoreInteractions
// import piuk.blockchain.android.R
// import piuk.blockchain.android.coincore.AssetTokenLookup
// import piuk.blockchain.android.coincore.impl.AssetTokensBase
// import piuk.blockchain.android.util.StringUtils
// import piuk.blockchain.androidcore.data.api.EnvironmentConfig
// import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
// import piuk.blockchain.androidcore.data.ethereum.EthDataManager
// import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
// import piuk.blockchain.androidcore.data.payload.PayloadDataManager
// import piuk.blockchain.android.coincore.ActivitySummaryItem
// import piuk.blockchain.android.coincore.model.TestActivitySummaryItem
// import piuk.blockchain.android.ui.activity.detail.TransactionHelper
// import piuk.blockchain.androidcore.utils.PersistentPrefs
// import java.math.BigInteger
// import java.util.HashMap
// import java.util.Locale
// 
// class TransactionInOutMapperTest {
// 
//     private val assetSelect: AssetTokenLookup = mock()
//     private val assetTokens: AssetTokensBase = mock()
// 
//     private val transactionHelper: TransactionHelper = mock()
//     private val prefsUtil: PersistentPrefs = mock()
//     private val stringUtils: StringUtils = mock()
//     private val view: TransactionDetailView = mock()
//     private val exchangeRateManager: ExchangeRateDataManager = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
//     private val payloadDataManager: PayloadDataManager = mock()
//     private val ethDataManager: EthDataManager = mock(defaultAnswer = Answers.RETURNS_DEEP_STUBS)
//     private val bchDataManager: BchDataManager = mock()
//     private val xlmDataManager: XlmDataManager = mock()
//     private val environmentConfig: EnvironmentConfig = mock()
// 
//     private lateinit var subject: TransactionDetailPresenter
// 
//     @get:Rule
//     val rxSchedulers = rxInit {
//         mainTrampoline()
//         ioTrampoline()
//     }
// 
//     @Before
//     fun setUp() {
//         whenever(prefsUtil.selectedFiatCurrency).thenReturn("USD")
//         Locale.setDefault(Locale("EN", "US"))
// 
//         whenever(assetSelect[any()]).thenReturn(assetTokens)
// 
//         subject = TransactionDetailPresenter(
//             assetLookup = assetSelect,
//             inputOutputMapper = inputOutMapper,
//             prefs = prefsUtil,
//             stringUtils = stringUtils,
//             exchangeRateDataManager = exchangeRateManager
//          )
//          subject.initView(view)
//         subject.onViewReady()
//      }
// 
//      @Test
//      fun onViewReadyKeyOutOfBounds() {
//          //   Arrange
// 
//          whenever(view.txHashDetailLookup()).thenReturn(null)
// 
//          whenever(transactionListDataManager.getTransactionList())
//              .thenReturn(listOf<ActivitySummaryItem>(displayable1, displayable2, displayable3))
// 
//          //   Act
//          subject.onViewReady()
// 
//          //   Assert
// 
//          verify(view).txHashDetailLookup()
//          verify(view).pageFinish()
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun onViewReadyIntentPositionInvalid() {
//          //   Arrange
// 
//          whenever(view.txHashDetailLookup()).thenReturn(null)
// 
//          //   Act
//          subject.onViewReady()
// 
//          //   Assert
// 
//          verify(view).txHashDetailLookup()
//          verify(view).pageFinish()
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun onViewReadyIntentHashNotFound() {
//          //   Arrange
//          val txHash = "TX_HASH"
// 
//          whenever(view.txHashDetailLookup()).thenReturn(txHash)
// 
//          whenever(transactionListDataManager.getTxFromHash(txHash)).thenReturn(Single.error(Throwable()))
// 
//          //   Act
//          subject.onViewReady()
// 
//          //   Assert
// 
//          verify(view).txHashDetailLookup()
//          verify(view).pageFinish()
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun onViewReadyTransactionFoundInList() {
//          //   Arrange
//          whenever(displayable1.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
//          whenever(displayable1.direction).thenReturn(TransactionSummary.Direction.TRANSFERRED)
//          whenever(displayable1.hash).thenReturn("txMoved_hash")
//          whenever(displayable1.total).thenReturn(BigInteger.valueOf(1_000L))
//          whenever(displayable1.fee).thenReturn(Observable.just(BigInteger.valueOf(10000L)))
// 
//          whenever(displayable2.hash).thenReturn("")
//          whenever(displayable3.hash).thenReturn("")
// 
// 
//          whenever(view.txHashDetailLookup()).thenReturn(null)
// 
//          val mockPayload: Wallet = mock()
//          whenever(mockPayload.txNotes).thenReturn(HashMap())
// 
//          whenever(payloadDataManager.wallet).thenReturn(mockPayload)
//          whenever(transactionListDataManager.getTransactionList())
//              .thenReturn(listOf<ActivitySummaryItem>(displayable1, displayable2, displayable3))
// 
//          whenever(stringUtils.getString(R.string.transaction_detail_pending))
//              .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
// 
//          val inputs = HashMap<String, BigInteger>()
//          val outputs = HashMap<String, BigInteger>()
//          inputs["addr1"] = BigInteger.valueOf(1000L)
//          outputs["addr2"] = BigInteger.valueOf(2000L)
//          val pair = Pair.of(inputs, outputs)
//          whenever(transactionHelper.filterNonChangeAddresses(any())).thenReturn(pair)
//          whenever(payloadDataManager.addressToLabel("addr1")).thenReturn("account1")
//          whenever(payloadDataManager.addressToLabel("addr2")).thenReturn("account2")
//          whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
//              .thenReturn(Single.just(1000.usd()))
//          whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_transferred))
//              .thenReturn("Value when moved: ")
// 
//          //   Act
//          subject.onViewReady()
// 
//          //   Assert
// 
//          verify(view).txHashDetailLookup()
// 
//          verify(view).setStatus(
//              CryptoCurrency.BTC,
//              "Pending (0/3 Confirmations)",
//              "txMoved_hash"
//          )
//          verify(view).setTransactionType(TransactionSummary.Direction.TRANSFERRED, false)
//          verify(view).setTransactionColour(R.color.product_grey_transferred_50)
//          verify(view).setDescription(null)
//          verify(view).setDate(any())
//          verify(view).setToAddresses(any())
//          verify(view).setFromAddress(any())
//          verify(view).setFee("0.0001 BTC")
//          verify(view).setTransactionValue(any())
//          verify(view).setTransactionValueFiat(any())
//          verify(view).onDataLoaded()
//          verify(view).setIsDoubleSpend(any())
//          verify(view).updateFeeFieldVisibility(any())
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun onViewReadyTransactionFoundViaHash() {
//          //   Arrange
//          whenever(displayable1.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
//          whenever(displayable1.direction).thenReturn(TransactionSummary.Direction.TRANSFERRED)
//          whenever(displayable1.hash).thenReturn("txMoved_hash")
//          whenever(displayable1.total).thenReturn(BigInteger.valueOf(1_000L))
//          whenever(displayable1.fee).thenReturn(Observable.just(BigInteger.valueOf(1L)))
// 
//          whenever(view.positionDetailLookup()).thenReturn(-1)
//          whenever(view.txHashDetailLookup()).thenReturn("txMoved_hash")
// 
//          val mockPayload: Wallet = mock()
//          whenever(mockPayload.txNotes).thenReturn(HashMap())
//          whenever(payloadDataManager.wallet).thenReturn(mockPayload)
//          whenever(transactionListDataManager.getTxFromHash("txMoved_hash"))
//              .thenReturn(Single.just(displayable1))
//          whenever(stringUtils.getString(R.string.transaction_detail_pending))
//              .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
// 
//          val inputs = HashMap<String, BigInteger>()
//          val outputs = HashMap<String, BigInteger>()
//          inputs["addr1"] = BigInteger.valueOf(1000L)
//          outputs["addr2"] = BigInteger.valueOf(2000L)
//          val pair = Pair.of(inputs, outputs)
//          whenever(transactionHelper.filterNonChangeAddresses(any())).thenReturn(pair)
//          whenever(payloadDataManager.addressToLabel("addr1")).thenReturn("account1")
//          whenever(payloadDataManager.addressToLabel("addr2")).thenReturn("account2")
//          whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
//              .thenReturn(Single.just(1000.usd()))
//          whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_transferred))
//              .thenReturn("Value when moved: ")
// 
//          //   Act
//          subject.onViewReady()
// 
//          //   Assert
//          verify(view).positionDetailLookup()
//          verify(view).txHashDetailLookup()
//          verify(view).setStatus(
//              CryptoCurrency.BTC,
//              "Pending (0/3 Confirmations)",
//              "txMoved_hash"
//          )
//          verify(view).setTransactionType(TransactionSummary.Direction.TRANSFERRED, false)
//          verify(view).setTransactionColour(R.color.product_grey_transferred_50)
//          verify(view).setDescription(null)
//          verify(view).setDate(any())
//          verify(view).setToAddresses(any())
//          verify(view).setFromAddress(any())
//          verify(view).setFee("0.00000001 BTC")
//          verify(view).setTransactionValue(any())
//          verify(view).setTransactionValueFiat(any())
//          verify(view).onDataLoaded()
//          verify(view).setIsDoubleSpend(any())
//          verify(view).updateFeeFieldVisibility(any())
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun onViewReadyTransactionFoundViaHashEthereum() {
//          //   Arrange
//          whenever(displayable1.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
//          whenever(displayable1.direction).thenReturn(TransactionSummary.Direction.SENT)
//          whenever(displayable1.hash).thenReturn("hash")
//          whenever(displayable1.total).thenReturn(BigInteger.valueOf(1_000L))
//          whenever(displayable1.fee).thenReturn(Observable.just(BigInteger.valueOf(3000000000L)))
//          val maps = HashMap<String, BigInteger>()
//          maps[""] = BigInteger.TEN
//          whenever(displayable1.inputsMap).thenReturn(maps)
//          whenever(displayable1.outputsMap).thenReturn(maps)
// 
//          whenever(view.txHashDetailLookup()).thenReturn("hash")
// 
//          whenever(transactionListDataManager.getTxFromHash("hash")).thenReturn(Single.just(displayable1))
//          whenever(stringUtils.getString(R.string.transaction_detail_pending))
//              .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
//          whenever(stringUtils.getString(R.string.eth_default_account_label))
//              .thenReturn("My Ethereum Wallet")
// 
//          val inputs = HashMap<String, BigInteger>()
//          val outputs = HashMap<String, BigInteger>()
//          inputs["addr1"] = BigInteger.valueOf(1000L)
//          outputs["addr2"] = BigInteger.valueOf(2000L)
//          val pair = Pair.of(inputs, outputs)
//          whenever(transactionHelper.filterNonChangeAddresses(any())).thenReturn(pair)
//          whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
//              .thenReturn(Single.just(1000.usd()))
//          whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_sent))
//              .thenReturn("Value when sent: ")
//          whenever(ethDataManager.getEthResponseModel()!!.getAddressResponse()!!.account).thenReturn("")
//          whenever(ethDataManager.getTransactionNotes("hash")).thenReturn("note")
// 
//          //   Act
//          subject.onViewReady()
// 
//          //   Assert
//          verify(view).txHashDetailLookup()
// 
//          verify(view).setStatus(CryptoCurrency.ETHER, "Pending (0/12 Confirmations)", "hash")
//          verify(view).setTransactionType(TransactionSummary.Direction.SENT, false)
//          verify(view).setTransactionColour(R.color.product_red_sent_50)
//          verify(view).setDescription(any())
//          verify(view).setDate(any())
//          verify(view).setToAddresses(any())
//          verify(view).setFromAddress(any())
//          verify(view).setFee(any())
//          verify(view).setTransactionValue(any())
//          verify(view).setTransactionValueFiat(any())
//          verify(view).onDataLoaded()
//          verify(view).updateFeeFieldVisibility(any())
//          verify(view).setIsDoubleSpend(any())
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun `onViewReady transaction found via hash xlm`() {
//          //   Arrange
//          whenever(displayable1.cryptoCurrency).thenReturn(CryptoCurrency.XLM)
//          whenever(displayable1.direction).thenReturn(TransactionSummary.Direction.SENT)
//          whenever(displayable1.hash).thenReturn("hash")
//          whenever(displayable1.total).thenReturn(BigInteger.valueOf(1_000L))
//          whenever(displayable1.fee).thenReturn(Observable.just(BigInteger.valueOf(396684365L)))
//          val maps = HashMap<String, BigInteger>()
//          maps[""] = BigInteger.TEN
//          whenever(displayable1.inputsMap).thenReturn(maps)
//          whenever(displayable1.outputsMap).thenReturn(maps)
// 
// 
//          whenever(view.txHashDetailLookup()).thenReturn("hash")
// 
//          whenever(transactionListDataManager.getTxFromHash("hash"))
//              .thenReturn(Single.just(displayable1))
//          whenever(stringUtils.getString(R.string.transaction_detail_pending))
//              .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
//          whenever(stringUtils.getString(R.string.xlm_default_account_label))
//              .thenReturn("My Lumens Wallet")
//          whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_sent))
//              .thenReturn("Value when sent: ")
//          whenever(xlmDataManager.defaultAccount())
//              .thenReturn(Single.just(AccountReference.Xlm("My Lumens Wallet", "Account ID")))
//          whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
//              .thenReturn(Single.just(1000.usd()))
// 
//          //   Act
//          subject.onViewReady()
// 
//          //   Assert
// 
//          verify(view).txHashDetailLookup()
//          verify(view).setStatus(CryptoCurrency.XLM, "Pending (0/1 Confirmations)", "hash")
//          verify(view).setTransactionType(TransactionSummary.Direction.SENT, false)
//          verify(view).setTransactionColour(R.color.product_red_sent_50)
//          verify(view).setDescription(any())
//          verify(view).setDate(any())
//          verify(view).setToAddresses(any())
//          verify(view).setFromAddress(any())
//          verify(view, atLeastOnce()).setFee("39.6684365 XLM")
//          verify(view).hideDescriptionField()
//          verify(view).setTransactionValue(any())
//          verify(view).setTransactionValueFiat(any())
//          verify(view).updateFeeFieldVisibility(any())
//          verify(view).onDataLoaded()
//          verify(view).setIsDoubleSpend(any())
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun `onViewReady transaction found via hash pax`() {
//          //   Arrange
//          whenever(displayable1.cryptoCurrency).thenReturn(CryptoCurrency.PAX)
//          whenever(displayable1.direction).thenReturn(TransactionSummary.Direction.SENT)
//          whenever(displayable1.hash).thenReturn("hash")
//          whenever(displayable1.total).thenReturn(BigInteger.valueOf(1_000L))
//          whenever(displayable1.fee).thenReturn(Observable.just(BigInteger.valueOf(1547644353574L)))
//          val maps = HashMap<String, BigInteger>()
//          maps[""] = BigInteger.TEN
//          whenever(displayable1.inputsMap).thenReturn(maps)
//          whenever(displayable1.outputsMap).thenReturn(maps)
// 
// 
//          whenever(view.txHashDetailLookup()).thenReturn("hash")
// 
//          whenever(transactionListDataManager.getTxFromHash("hash"))
//              .thenReturn(Single.just(displayable1))
//          whenever(stringUtils.getString(R.string.transaction_detail_pending))
//              .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
//          whenever(stringUtils.getString(R.string.pax_default_account_label))
//              .thenReturn("My Usd pax Wallet")
//          whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_sent))
//              .thenReturn("Value when sent: ")
//          whenever(ethDataManager.getEthResponseModel()!!.getAddressResponse()!!.account).thenReturn("")
//          whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
//              .thenReturn(Single.just(1000.usd()))
//          whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX).txNotes["hash"]).thenReturn("note")
// 
//          //   Act
//          subject.onViewReady()
//          //   Assert
// 
//          verify(view).txHashDetailLookup()
//          verify(view).setStatus(CryptoCurrency.PAX, "Pending (0/12 Confirmations)", "hash")
//          verify(view).setTransactionType(TransactionSummary.Direction.SENT, false)
//          verify(view).setTransactionColour(R.color.product_red_sent_50)
//          verify(view).setDescription(any())
//          verify(view).setDate(any())
//          verify(view).setToAddresses(any())
//          verify(view).setFromAddress(any())
//          verify(view).setFee("0.00000154 ETH")
//          verify(view).setTransactionValue(any())
//          verify(view).setTransactionValueFiat(any())
//          verify(view).updateFeeFieldVisibility(any())
//          verify(view).onDataLoaded()
//          verify(view).setIsDoubleSpend(any())
//          verifyNoMoreInteractions(view)
//      }
// 
// 
// //      @Test
// //      fun getTransactionValueStringTransferred() {
// //          //   Arrange
// //          val displayable: BtcActivitySummaryItem = mock()
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
// //          whenever(displayable.direction).thenReturn(TransactionSummary.Direction.SENT)
// //          whenever(displayable.total).thenReturn(BigInteger.valueOf(1_000L))
// //          whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
// //              .thenReturn(Single.just(1000.usd()))
// //          whenever(stringUtils.getString(any())).thenReturn("Value when transferred: ")
// //          whenever(prefsUtil.selectedFiatCurrency).thenReturn("USD")
// //          //   Act
// //          val observer = subject.getTransactionValueString("USD", displayable).test()
// //          //   Assert
// //          verify(exchangeRateFactory).getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any())
// //          assertEquals("Value when transferred: $1,000.00", observer.values()[0])
// //          observer.onComplete()
// //          observer.assertNoErrors()
// //      }
// // 
// //      @Test
// //      fun updateTransactionNoteBtcSuccess() {
// //          //   Arrange
// //          val displayable: BtcActivitySummaryItem = mock()
// //          whenever(displayable.hash).thenReturn("hash")
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
// //          subject.activitySummaryItem = displayable
// //          whenever(payloadDataManager.updateTransactionNotes(any(), any()))
// //              .thenReturn(Completable.complete())
// //          //   Act
// //          subject.updateTransactionNote("note")
// //          //   Assert
// //          verify(payloadDataManager).updateTransactionNotes("hash", "note")
// // 
// //          verify(view).showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK)
// //          verify(view).setDescription("note")
// //      }
// // 
// //      @Test
// //      fun updateTransactionNoteEthSuccess() {
// //          //   Arrange
// //          val displayable: BtcActivitySummaryItem = mock()
// //          whenever(displayable.hash).thenReturn("hash")
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
// //          subject.activitySummaryItem = displayable
// //          whenever(ethDataManager.updateTransactionNotes(any(), any()))
// //              .thenReturn(Completable.complete())
// //          //   Act
// //          subject.updateTransactionNote("note")
// //          //   Assert
// //          verify(ethDataManager).updateTransactionNotes("hash", "note")
// // 
// //          verify(view).showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK)
// //          verify(view).setDescription("note")
// //      }
// // 
// //      @Test
// //      fun updateTransactionNotePaxSuccess() {
// //          //   Arrange
// //          val displayable: Erc20ActivitySummaryItem = mock()
// //          whenever(displayable.hash).thenReturn("hash")
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.PAX)
// //          subject.activitySummaryItem = displayable
// //          whenever(ethDataManager.updateErc20TransactionNotes(any(), any()))
// //              .thenReturn(Completable.complete())
// //          //   Act
// //          subject.updateTransactionNote("note")
// //          //   Assert
// //          verify(ethDataManager).updateErc20TransactionNotes("hash", "note")
// // 
// //          verify(view).showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK)
// //          verify(view).setDescription("note")
// //      }
// // 
// //      @Test
// //      fun updateTransactionNoteFailure() {
// //          //   Arrange
// //          val displayable: BtcActivitySummaryItem = mock()
// //          whenever(displayable.hash).thenReturn("hash")
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
// //          subject.activitySummaryItem = displayable
// //          whenever(payloadDataManager.updateTransactionNotes(any(), any()))
// //              .thenReturn(Completable.error(Throwable()))
// //          //   Act
// //          subject.updateTransactionNote("note")
// //          //   Assert
// //          verify(payloadDataManager).updateTransactionNotes("hash", "note")
// // 
// //          verify(view).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
// //      }
// // 
// //      @Test(expected = IllegalArgumentException::class)
// //      fun updateTransactionNoteBchSuccess() {
// //          //   Arrange
// //          val displayable: BtcActivitySummaryItem = mock()
// //          whenever(displayable.hash).thenReturn("hash")
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BCH)
// //          subject.activitySummaryItem = displayable
// //          whenever(ethDataManager.updateTransactionNotes(any(), any()))
// //              .thenReturn(Completable.complete())
// //          //   Act
// //          subject.updateTransactionNote("note")
// //          //   Assert
// //      }
// // 
// //      @Test
// //      fun getTransactionNoteBtc() {
// //          //   Arrange
// //          val displayable: BtcActivitySummaryItem = mock()
// //          whenever(displayable.hash).thenReturn("hash")
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
// //          subject.activitySummaryItem = displayable
// //          whenever(payloadDataManager.getTransactionNotes("hash")).thenReturn("note")
// //          //   Act
// //          val value = subject.transactionNote
// //          //   Assert
// //          assertEquals("note", value)
// //          verify(payloadDataManager).getTransactionNotes("hash")
// //      }
// // 
// //      @Test
// //      fun getTransactionNoteEth() {
// //          //   Arrange
// //          val displayable: BtcActivitySummaryItem = mock()
// //          whenever(displayable.hash).thenReturn("hash")
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
// //          subject.activitySummaryItem = displayable
// //          whenever(ethDataManager.getTransactionNotes("hash")).thenReturn("note")
// //          //   Act
// //          val value = subject.transactionNote
// //          //   Assert
// //          assertEquals("note", value)
// //          verify(ethDataManager).getTransactionNotes("hash")
// //      }
// // 
// //      @Test
// //      fun getTransactionNotePax() {
// //          //   Arrange
// //          val displayable: Erc20ActivitySummaryItem = mock()
// //          whenever(displayable.hash).thenReturn("hash")
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.PAX)
// //          subject.activitySummaryItem = displayable
// //          whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX).txNotes["hash"]).thenReturn("note")
// //          //   Act
// //          val value = subject.transactionNote
// //          //   Assert
// //          assertEquals("note", value)
// //          verify(ethDataManager.getErc20TokenData(CryptoCurrency.PAX), times(2)).txNotes
// //      }
// // 
// //      @Test
// //      fun getTransactionNoteBch() {
// //          //   Arrange
// //          val displayable: BchActivitySummaryItem = mock()
// //          whenever(displayable.hash).thenReturn("hash")
// //          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BCH)
// //          subject.activitySummaryItem = displayable
// //          //   Act
// //          val value = subject.transactionNote
// //          //   Assert
// //          assertEquals("", value)
// //      }
// // 
// //      @Test
// //      fun `getTransactionHash Bch`() {
// //          subject.activitySummaryItem = mock<BchActivitySummaryItem> {
// //              on { hash } `it returns` "hash1"
// //              on { cryptoCurrency } `it returns` CryptoCurrency.BCH
// //          }
// //          subject.transactionHash `should equal` TransactionHash(CryptoCurrency.BCH, "hash1")
// //      }
// // 
// //      @Test
// //      fun `getTransactionHash Pax`() {
// //          subject.activitySummaryItem = mock<Erc20ActivitySummaryItem> {
// //              on { hash } `it returns` "hash1"
// //              on { cryptoCurrency } `it returns` CryptoCurrency.PAX
// //          }
// //          subject.transactionHash `should equal` TransactionHash(CryptoCurrency.PAX, "hash1")
// //      }
// // 
// //      @Test
// //      fun `getTransactionHash Eth`() {
// //          subject.activitySummaryItem = mock<EthActivitySummaryItem> {
// //              on { hash } `it returns` "hash2"
// //              on { cryptoCurrency } `it returns` CryptoCurrency.ETHER
// //          }
// //          subject.transactionHash `should equal` TransactionHash(CryptoCurrency.ETHER, "hash2")
// //      }
// 
//      @Test
//      fun setTransactionStatusNoConfirmations() {
//          //   Arrange
//          whenever(stringUtils.getString(R.string.transaction_detail_pending))
//              .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
//          //   Act
//          subject.setConfirmationStatus(CryptoCurrency.ETHER, "hash", 0)
//          //   Assert
//          verify(view).setStatus(CryptoCurrency.ETHER, "Pending (0/12 Confirmations)", "hash")
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun setTransactionStatusConfirmed() {
//          //   Arrange
//          whenever(stringUtils.getString(R.string.transaction_detail_confirmed)).thenReturn("Confirmed")
//          //   Act
//          subject.setConfirmationStatus(CryptoCurrency.BTC, "hash", 3)
//          //   Assert
//          verify(view).setStatus(CryptoCurrency.BTC, "Confirmed", "hash")
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun setTransactionColorMove() {
//          //   Arrange
//          val item: TestActivitySummaryItem = mock()
//          whenever(displayable.confirmations).thenReturn(0)
//          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
//          whenever(displayable.direction).thenReturn(TransactionSummary.Direction.TRANSFERRED)
// 
//          //   Act
//          subject.setTransactionColor(displayable)
// 
//          //   Assert
//          verify(view).setTransactionColour(R.color.product_grey_transferred_50)
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun setTransactionColorMoveConfirmed() {
//          //   Arrange
//          val displayable: TestActivitySummaryItem = mock()
//          whenever(displayable.confirmations).thenReturn(3)
//          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
//          whenever(displayable.direction).thenReturn(TransactionSummary.Direction.TRANSFERRED)
//          //   Act
//          subject.setTransactionColor(displayable)
//          //   Assert
//          verify(view).setTransactionColour(R.color.product_grey_transferred)
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun setTransactionColorSent() {
//          //   Arrange
//          val displayable: TestActivitySummaryItem = mock()
//          whenever(displayable.confirmations).thenReturn(2)
//          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
//          whenever(displayable.direction).thenReturn(TransactionSummary.Direction.SENT)
//          //   Act
//          subject.setTransactionColor(displayable)
//          //   Assert
//          verify(view).setTransactionColour(R.color.product_red_sent_50)
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun setTransactionColorSentConfirmed() {
//          //   Arrange
//          val displayable: TestActivitySummaryItem = mock()
//          whenever(displayable.confirmations).thenReturn(3)
//          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
//          whenever(displayable.direction).thenReturn(TransactionSummary.Direction.SENT)
//          //   Act
//          subject.setTransactionColor(displayable)
//          //   Assert
//          verify(view).setTransactionColour(R.color.product_red_sent)
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun setTransactionColorReceived() {
//          //   Arrange
//          val item: TestActivitySummaryItem = mock()
//          whenever(item.confirmations).thenReturn(7)
//          whenever(item.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
//          whenever(item.direction).thenReturn(TransactionSummary.Direction.RECEIVED)
// 
// 
//          //   Act
//          subject.setTransactionColor(displayable)
// 
//          //   Assert
//          verify(view).setTransactionColour(R.color.product_green_received_50)
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun setTransactionColorReceivedConfirmed() {
//          //   Arrange
//          val displayable: TestActivitySummaryItem = mock()
//          whenever(displayable.confirmations).thenReturn(3)
//          whenever(displayable.direction).thenReturn(TransactionSummary.Direction.RECEIVED)
//          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
//          //   Act
//          subject.setTransactionColor(displayable)
//          //   Assert
//          verify(view).setTransactionColour(R.color.product_green_received)
//          verifyNoMoreInteractions(view)
//      }
// 
//      @Test
//      fun `fee should be hidden if transaction is a fee one`() {
//          //   Arrange
//          val displayable: EthActivitySummaryItem = mock()
//          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
//          whenever(displayable.direction).thenReturn(TransactionSummary.Direction.SENT)
//          whenever(displayable.isFeeTransaction).thenReturn(true)
//          whenever(displayable.hash).thenReturn("hash")
//          whenever(displayable.total).thenReturn(BigInteger.valueOf(1_000L))
//          whenever(displayable.fee).thenReturn(Observable.just(BigInteger.valueOf(1547644353574L)))
//          val maps = HashMap<String, BigInteger>()
//          maps[""] = BigInteger.TEN
//          whenever(displayable.inputsMap).thenReturn(maps)
//          whenever(displayable.outputsMap).thenReturn(maps)
// 
//          whenever(view.positionDetailLookup()).thenReturn(-1)
//          whenever(view.txHashDetailLookup()).thenReturn("hash")
// 
//          whenever(transactionListDataManager.getTxFromHash("hash"))
//              .thenReturn(Single.just(displayable))
//          whenever(stringUtils.getString(R.string.transaction_detail_pending))
//              .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
//          whenever(stringUtils.getString(R.string.pax_default_account_label))
//              .thenReturn("My Usd pax Wallet")
//          whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_sent))
//              .thenReturn("Value when sent: ")
//          whenever(ethDataManager.getEthResponseModel()!!.getAddressResponse()!!.account).thenReturn("")
//          whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
//              .thenReturn(Single.just(1000.usd()))
//          whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX).txNotes["hash"]).thenReturn("note")
// 
//          //   Act
//          subject.onViewReady()
// 
//          //   Assert
//          verify(view).updateFeeFieldVisibility(false)
//      }
// 
//      @Test
//      fun `fee should be hidden if transaction is a receive one`() {
//          //   Arrange
//          val displayable: EthActivitySummaryItem = mock()
//          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
//          whenever(displayable.direction).thenReturn(TransactionSummary.Direction.RECEIVED)
//          whenever(displayable.isFeeTransaction).thenReturn(false)
//          whenever(displayable.hash).thenReturn("hash")
//          whenever(displayable.total).thenReturn(BigInteger.valueOf(1_000L))
//          whenever(displayable.fee).thenReturn(Observable.just(BigInteger.valueOf(1547644353574L)))
//          val maps = HashMap<String, BigInteger>()
//          maps[""] = BigInteger.TEN
//          whenever(displayable.inputsMap).thenReturn(maps)
//          whenever(displayable.outputsMap).thenReturn(maps)
// 
//          whenever(view.positionDetailLookup()).thenReturn(-1)
//          whenever(view.txHashDetailLookup()).thenReturn("hash")
// 
//          whenever(transactionListDataManager.getTxFromHash("hash"))
//              .thenReturn(Single.just(displayable))
//          whenever(stringUtils.getString(R.string.transaction_detail_pending))
//              .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
//          whenever(stringUtils.getString(R.string.pax_default_account_label))
//              .thenReturn("My Usd pax Wallet")
//          whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_sent))
//              .thenReturn("Value when sent: ")
//          whenever(ethDataManager.getEthResponseModel()!!.getAddressResponse()!!.account).thenReturn("")
//          whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
//              .thenReturn(Single.just(1000.usd()))
//          whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX).txNotes["hash"]).thenReturn("note")
// 
//          //   Act
//          subject.onViewReady()
// 
//          //   Assert
//          verify(view).updateFeeFieldVisibility(false)
//      }
// 
//      @Test
//      fun `fee should not be hidden if transaction is a sent one`() {
//          //   Arrange
//          val item: TestActivitySummaryItem = mock()
// 
//          whenever(assetTokens.findCachedActivityItem(VALID_TX_HASH)).thenReturn(item)
// 
//          whenever(displayable.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
//          whenever(displayable.direction).thenReturn(TransactionSummary.Direction.SENT)
//          whenever(displayable.hash).thenReturn("hash")
//          whenever(displayable.total).thenReturn(BigInteger.valueOf(1_000L))
//          whenever(displayable.fee).thenReturn(Observable.just(BigInteger.valueOf(1547644353574L)))
// 
//          val maps = mapOf("" to BigInteger.TEN)
//          whenever(displayable.inputsMap).thenReturn(maps)
//          whenever(displayable.outputsMap).thenReturn(maps)
// 
//          whenever(stringUtils.getString(R.string.transaction_detail_pending))
//              .thenReturn("Pending (%1\$s/%2\$s Confirmations)")
// 
//          whenever(stringUtils.getString(R.string.pax_default_account_label))
//              .thenReturn("My Usd pax Wallet")
// 
//          whenever(stringUtils.getString(R.string.transaction_detail_value_at_time_sent))
//              .thenReturn("Value when sent: ")
// 
//          whenever(ethDataManager.getEthResponseModel()!!.getAddressResponse()!!.account)
//              .thenReturn("")
// 
//          whenever(exchangeRateFactory.getHistoricPrice(value = any(), fiat = any(), timeInSeconds = any()))
//              .thenReturn(Single.just(1000.usd()))
// 
//          whenever(ethDataManager.getErc20TokenData(CryptoCurrency.PAX).txNotes["hash"]).thenReturn("note")
// 
//          //   Act
//          subject.showDetailsForTransaction(CryptoCurrency.ETHER, VALID_TX_HASH)
// 
//          //   Assert
//          verify(view).updateFeeFieldVisibility(true)
//      }
// 
// //     test for empty hash
// //     test for unavailable hash
// 
//      companion object {
//          private const val VALID_TX_HASH = "valid_hash"
//      }
//  }