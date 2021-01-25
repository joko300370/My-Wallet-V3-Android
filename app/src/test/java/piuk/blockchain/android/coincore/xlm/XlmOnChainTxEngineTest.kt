package piuk.blockchain.android.coincore.xlm

import com.blockchain.android.testutils.rxInit
import com.blockchain.fees.FeeType
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmFeesFetcher
import com.blockchain.testutils.lumens
import com.blockchain.testutils.stroops
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.injectMocks
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import timber.log.Timber

class XlmOnChainTxEngineTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val xlmDataManager: XlmDataManager = mock()
    private val walletOptionsDataManager: WalletOptionsDataManager = mock {
        on { isXlmAddressExchange(TARGET_ADDRESS) } itReturns false
        on { isXlmAddressExchange(TARGET_EXCHANGE_ADDRESS) } itReturns true
    }
    private val xlmFeesFetcher: XlmFeesFetcher = mock {
        on { operationFee(FeeType.Regular) } itReturns Single.just(FEE_REGULAR)
    }

    private val walletPreferences: WalletStatus = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } itReturns SELECTED_FIAT
    }

    private val subject = XlmOnChainTxEngine(
        xlmDataManager = xlmDataManager,
        xlmFeesFetcher = xlmFeesFetcher,
        walletOptionsDataManager = walletOptionsDataManager,
        requireSecondPassword = false,
        walletPreferences = walletPreferences
    )

    @Before
    fun setup() {
        injectMocks(
            module {
                scope(payloadScopeQualifier) {
                    factory {
                        currencyPrefs
                    }
                }
            }
        )
    }

    @After
    fun teardown() {
        stopKoin()
    }

    private val sourceAccount: CryptoAccount = mock()

    @Test
    fun `inputs validate when correct`() {
        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        whenever(sourceAccount.asset).thenReturn(ASSET)

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).asset

        noMoreInteractions(txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Asset incorrect`() {
        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        whenever(sourceAccount.asset).thenReturn(WRONG_ASSET)

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(txTarget).asset
        verify(sourceAccount).asset

        noMoreInteractions(txTarget)
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        whenever(sourceAccount.asset).thenReturn(ASSET)

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.asset

        // Assert
        assertEquals(asset, ASSET)
        verify(sourceAccount).asset

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised for non-exchange address`() {
        // Arrange
        val txTarget: XlmAddress = mock {
            on { asset } itReturns ASSET
            on { address } itReturns TARGET_ADDRESS
            on { memo } itReturns MEMO_TEXT
        }

        whenever(sourceAccount.asset).thenReturn(ASSET)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(ASSET) &&
                it.totalBalance == CryptoValue.zero(ASSET) &&
                it.availableBalance == CryptoValue.zero(ASSET) &&
                it.fees == CryptoValue.zero(ASSET) &&
                it.selectedFiat == SELECTED_FIAT &&
                it.feeLevel == FeeLevel.Regular &&
                it.customFeeAmount == -1L &&
                it.confirmations.isEmpty() &&
                it.minLimit == null &&
                it.maxLimit == null &&
                it.validationState == ValidationState.UNINITIALISED &&
                it.engineState.size == 1 &&
                it.engineState[STATE_MEMO]?.let { memo ->
                    memo is TxConfirmationValue.Memo &&
                    memo.text == MEMO_TEXT &&
                    !memo.isRequired &&
                    memo.id == null &&
                    memo.editable
                } ?: false
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget).address
        verify(txTarget).memo
        verify(currencyPrefs).selectedFiatCurrency
        verify(walletOptionsDataManager).isXlmAddressExchange(TARGET_ADDRESS)

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised for exchange address`() {
        // Arrange
        val txTarget: XlmAddress = mock {
            on { asset } itReturns ASSET
            on { address } itReturns TARGET_EXCHANGE_ADDRESS
            on { memo } itReturns MEMO_TEXT
        }

        whenever(sourceAccount.asset).thenReturn(ASSET)

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                Timber.d("$it")
                it.amount == CryptoValue.zero(ASSET) &&
                    it.totalBalance == CryptoValue.zero(ASSET) &&
                    it.availableBalance == CryptoValue.zero(ASSET) &&
                    it.fees == CryptoValue.zero(ASSET) &&
                    it.selectedFiat == SELECTED_FIAT &&
                    it.feeLevel == FeeLevel.Regular &&
                    it.customFeeAmount == -1L &&
                    it.confirmations.isEmpty() &&
                    it.minLimit == null &&
                    it.maxLimit == null &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.size == 1 &&
                    it.engineState[STATE_MEMO]?.let { memo ->
                        memo is TxConfirmationValue.Memo &&
                            memo.text == MEMO_TEXT &&
                            memo.isRequired &&
                            memo.id == null &&
                            memo.editable
                    } ?: false
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget).address
        verify(txTarget).memo
        verify(currencyPrefs).selectedFiatCurrency
        verify(walletOptionsDataManager).isXlmAddressExchange(TARGET_EXCHANGE_ADDRESS)

        noMoreInteractions(txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        // Arrange
        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        val totalBalance = 21.lumens()
        val actionableBalance = 20.lumens()
        whenever(sourceAccount.asset).thenReturn(ASSET)
        whenever(sourceAccount.accountBalance).thenReturn(Single.just(totalBalance))
        whenever(sourceAccount.actionableBalance).thenReturn(Single.just(actionableBalance))

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(ASSET),
            totalBalance = CryptoValue.zero(ASSET),
            availableBalance = CryptoValue.zero(ASSET),
            fees = CryptoValue.zero(ASSET),
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.Regular
        )

        val inputAmount = 2.lumens()
        val expectedFee = FEE_REGULAR
        val expectedAvailable = actionableBalance - expectedFee

        // Act
        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                it.totalBalance == totalBalance &&
                it.availableBalance == expectedAvailable &&
                it.fees == expectedFee
            }

        verify(sourceAccount).accountBalance
        verify(sourceAccount).actionableBalance
        verify(xlmFeesFetcher).operationFee(FeeType.Regular)

        noMoreInteractions(txTarget)
    }

    private fun noMoreInteractions(txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(xlmDataManager)
        verifyNoMoreInteractions(xlmFeesFetcher)
        verifyNoMoreInteractions(walletOptionsDataManager)
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.XLM
        private val WRONG_ASSET = CryptoCurrency.BTC
        private const val TARGET_ADDRESS = "VALID_NON_EXCHANGE_XLM_ADDRESS"
        private const val TARGET_EXCHANGE_ADDRESS = "VALID_EXCHANGE_XLM_ADDRESS"
        private const val MEMO_TEXT = "ADDRESS_PART_FOR_MEMO"
        private val FEE_REGULAR = 2000.stroops()
        private const val SELECTED_FIAT = "INR"
    }
}
