package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.android.testutils.rxInit
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimits
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Maybe
import io.reactivex.Single
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.btc.BtcCryptoWalletAccount
import piuk.blockchain.android.coincore.impl.CryptoInterestAccount
import piuk.blockchain.android.coincore.impl.injectMocks
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class InterestDepositTradingEngineTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } itReturns SELECTED_FIAT
    }
    private val exchangeRates: ExchangeRateDataManager = mock()

    private fun mockTransactionTarget() = mock<CryptoInterestAccount> {
        on { asset } itReturns ASSET
    }

    private val custodialWalletManager: CustodialWalletManager = mock()

    private lateinit var subject: InterestDepositTradingEngine

    @Before
    fun setUp() {
        injectMocks(
            module {
                scope(payloadScopeQualifier) {
                    factory {
                        currencyPrefs
                    }
                }
            }
        )
        subject = InterestDepositTradingEngine(custodialWalletManager)
    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when assets mismatched`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CryptoInterestAccount = mock {
            on { asset } itReturns WRONG_ASSET
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test
    fun `asset is returned correctly`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.sourceAsset

        // Assert
        asset shouldEqual ASSET

        verify(sourceAccount, atLeastOnce()).asset

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val limits = mock<InterestLimits> {
            on { minDepositAmount } itReturns MIN_DEPOSIT_AMOUNT
        }

        whenever(custodialWalletManager.getInterestLimits(ASSET)).thenReturn(Maybe.just(limits))

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(ASSET) &&
                    it.totalBalance == CryptoValue.zero(ASSET) &&
                    it.availableBalance == CryptoValue.zero(ASSET) &&
                    it.feeAmount == CryptoValue.zero(ASSET) &&
                    it.selectedFiat == SELECTED_FIAT &&
                    it.confirmations.isEmpty() &&
                    it.minLimit == MIN_DEPOSIT_AMOUNT &&
                    it.maxLimit == null &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset

        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(currencyPrefs).selectedFiatCurrency
        verify(sourceAccount).accountBalance

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when initialising, if getInterestLimits() returns empty, then initialisation fails`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )


        whenever(custodialWalletManager.getInterestLimits(ASSET)).thenReturn(Maybe.empty())

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(NoSuchElementException::class.java)

        verify(sourceAccount, atLeastOnce()).asset


        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(sourceAccount).accountBalance

        noMoreInteractions(sourceAccount, txTarget)

        noMoreInteractions(sourceAccount, txTarget)
    }

    private fun noMoreInteractions(sourceAccount: BlockchainAccount, txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(custodialWalletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(sourceAccount)
    }

    private fun mockSourceAccount(
        totalBalance: Money = CryptoValue.zero(ASSET),
        availableBalance: Money = CryptoValue.zero(ASSET)
    ) = mock<BtcCryptoWalletAccount> {
        on { asset } itReturns ASSET
        on { accountBalance } itReturns Single.just(totalBalance)
        on { actionableBalance } itReturns Single.just(availableBalance)
    }

    companion object {
        private val ASSET = CryptoCurrency.BTC
        private val WRONG_ASSET = CryptoCurrency.XLM
        private const val SELECTED_FIAT = "USD"
        private val MIN_DEPOSIT_AMOUNT = CryptoValue.fromMajor(ASSET, 10.toBigDecimal())
    }
}