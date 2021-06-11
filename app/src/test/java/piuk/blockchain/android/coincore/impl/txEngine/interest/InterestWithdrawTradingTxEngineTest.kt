package piuk.blockchain.android.coincore.impl.txEngine.interest

import com.blockchain.android.testutils.rxInit
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.Product
import com.blockchain.nabu.datamanagers.repositories.interest.InterestLimits
import com.blockchain.nabu.models.data.CryptoWithdrawalFeeAndLimit
import com.blockchain.preferences.CurrencyPrefs
import com.nhaarman.mockito_kotlin.any
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
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.CryptoInterestAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.coincore.impl.injectMocks
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import java.math.BigDecimal
import java.math.BigInteger

class InterestWithdrawTradingTxEngineTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } itReturns SELECTED_FIAT
    }
    private val exchangeRates: ExchangeRateDataManager = mock {
        on { getLastPrice(any(), any()) } itReturns BigDecimal.TEN
    }

    private fun mockTransactionTarget() = mock<CustodialTradingAccount> {
        on { asset } itReturns ASSET
    }

    private val custodialWalletManager: CustodialWalletManager = mock()

    private lateinit var subject: InterestWithdrawTradingTxEngine

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
        subject = InterestWithdrawTradingTxEngine(custodialWalletManager)
    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when assets mismatched`() {
        val sourceAccount = mockSourceAccount()
        val txTarget: CustodialTradingAccount = mock {
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
            on { maxWithdrawalAmount } itReturns MAX_WITHDRAW_AMOUNT
        }

        val fees = mock<CryptoWithdrawalFeeAndLimit> {
            on { minLimit } itReturns MIN_WITHDRAW_AMOUNT
            on { fee } itReturns BigInteger.ZERO
        }

        whenever(custodialWalletManager.getInterestLimits(ASSET)).thenReturn(Maybe.just(limits))
        whenever(custodialWalletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)).thenReturn(
            Single.just(fees)
        )

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
                    it.minLimit == CryptoValue.fromMinor(ASSET, fees.minLimit) &&
                    it.maxLimit == limits.maxWithdrawalAmount &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset

        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(custodialWalletManager).fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)
        verify(currencyPrefs).selectedFiatCurrency
        verify(sourceAccount).actionableBalance

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
        whenever(custodialWalletManager.fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)).thenReturn(
            Single.just(mock())
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(NoSuchElementException::class.java)

        verify(sourceAccount, atLeastOnce()).asset

        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(custodialWalletManager).fetchCryptoWithdrawFeeAndMinLimit(ASSET, Product.SAVINGS)
        verify(sourceAccount).actionableBalance

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when initialising, if fetchCryptoWithdrawFeeAndMinLimit() returns error, then initialisation fails`() {
        // Arrange
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        whenever(custodialWalletManager.getInterestLimits(ASSET)).thenReturn(
            Maybe.just(mock())
        )
        whenever(
            custodialWalletManager.fetchCryptoWithdrawFeeAndMinLimit(
                ASSET, Product.SAVINGS
            )
        ).thenReturn(
            Single.error(Exception())
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertError(Exception::class.java)

        verify(sourceAccount, atLeastOnce()).asset

        verify(custodialWalletManager).getInterestLimits(ASSET)
        verify(custodialWalletManager).fetchCryptoWithdrawFeeAndMinLimit(
            ASSET, Product.SAVINGS
        )
        verify(sourceAccount).actionableBalance

        noMoreInteractions(sourceAccount, txTarget)
    }

    @Test
    fun `when building confirmations, it add the right ones`() {
        val sourceAccount = mockSourceAccount()
        val txTarget = mockTransactionTarget()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val money = CryptoValue.fromMajor(ASSET, 10.toBigDecimal())
        val mockPendingTx =
            PendingTx(money, money, money, money, money, FeeSelection(), "USD", listOf(), money, money)

        // Act
        subject.doBuildConfirmations(mockPendingTx)
            .test()
            .assertValue { pTx ->
                pTx.confirmations.find { it is TxConfirmationValue.NewFrom } != null &&
                    pTx.confirmations.find { it is TxConfirmationValue.NewTo } != null &&
                    pTx.confirmations.find { it is TxConfirmationValue.NewTotal } != null
            }
            .assertNoErrors()
            .assertComplete()
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
    ) = mock<CryptoInterestAccount> {
        on { asset } itReturns ASSET
        on { accountBalance } itReturns Single.just(totalBalance)
        on { actionableBalance } itReturns Single.just(availableBalance)
    }

    companion object {
        private val ASSET = CryptoCurrency.BTC
        private val WRONG_ASSET = CryptoCurrency.XLM
        private const val SELECTED_FIAT = "USD"
        private val MIN_WITHDRAW_AMOUNT = 1.toBigInteger()
        private val MAX_WITHDRAW_AMOUNT = CryptoValue.fromMajor(ASSET, 10.toBigDecimal())
    }
}