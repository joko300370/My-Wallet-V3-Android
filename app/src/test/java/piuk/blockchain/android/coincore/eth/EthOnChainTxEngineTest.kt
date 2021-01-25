package piuk.blockchain.android.coincore.eth

import com.blockchain.android.testutils.rxInit
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.testutils.ether
import com.blockchain.testutils.gwei
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeLimits
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Observable
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
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.injectMocks
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager

class EthOnChainTxEngineTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val ethDataManager: EthDataManager = mock()
    private val ethFeeOptions: FeeOptions = mock()

    private val feeManager: FeeDataManager = mock {
        on { ethFeeOptions } itReturns Observable.just(ethFeeOptions)
    }
    private val walletPreferences: WalletStatus = mock {
        on { getFeeTypeForAsset(ASSET) } itReturns FeeLevel.Regular.ordinal
    }
    private val exchangeRates: ExchangeRateDataManager = mock()

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } itReturns SELECTED_FIAT
    }

    private val subject = EthOnChainTxEngine(
        ethDataManager = ethDataManager,
        feeManager = feeManager,
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
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
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
                it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()

        verify(walletPreferences).getFeeTypeForAsset(ASSET)
        verify(currencyPrefs).selectedFiatCurrency

        noMoreInteractions(txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for regular fees`() {
        // Arrange
        withDefaultFeeOptions()

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
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

        val inputAmount = 2.ether()
        val expectedFee = (GAS_LIMIT * FEE_REGULAR).gwei()
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
        verify(feeManager).ethFeeOptions
        verify(ethFeeOptions).gasLimit
        verify(ethFeeOptions).regularFee

        noMoreInteractions(txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for priority fees`() {
        // Arrange
        withDefaultFeeOptions()

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
        }

        val totalBalance = 21.ether()
        val actionableBalance = 20.ether()
        whenever(sourceAccount.asset).thenReturn(ASSET)
        whenever(sourceAccount.accountBalance).thenReturn(Single.just(totalBalance))
        whenever(sourceAccount.actionableBalance).thenReturn(Single.just(actionableBalance))

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(CryptoCurrency.ETHER),
            totalBalance = CryptoValue.zero(CryptoCurrency.ETHER),
            availableBalance = CryptoValue.zero(CryptoCurrency.ETHER),
            fees = CryptoValue.zero(CryptoCurrency.ETHER),
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.Priority
        )

        val inputAmount = 2.ether()
        val expectedFee = (GAS_LIMIT * FEE_PRIORITY).gwei()
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
        verify(feeManager).ethFeeOptions
        verify(ethFeeOptions).gasLimit
        verify(ethFeeOptions).priorityFee

        noMoreInteractions(txTarget)
    }

    private fun withDefaultFeeOptions() {
        whenever(ethFeeOptions.gasLimit).thenReturn(GAS_LIMIT)
        whenever(ethFeeOptions.priorityFee).thenReturn(FEE_PRIORITY)
        whenever(ethFeeOptions.regularFee).thenReturn(FEE_REGULAR)
        whenever(ethFeeOptions.gasLimitContract).thenReturn(GAS_LIMIT_CONTRACT)
        whenever(ethFeeOptions.limits).thenReturn(FeeLimits(FEE_REGULAR, FEE_PRIORITY))
    }

    private fun noMoreInteractions(txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(feeManager)
        verifyNoMoreInteractions(ethFeeOptions)
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.ETHER
        private val WRONG_ASSET = CryptoCurrency.BTC
        private const val GAS_LIMIT = 3000L
        private const val GAS_LIMIT_CONTRACT = 5000L
        private const val FEE_PRIORITY = 5L
        private const val FEE_REGULAR = 2L
        private const val SELECTED_FIAT = "INR"
    }
}
