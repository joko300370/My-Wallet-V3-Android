package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.PaymentLimits
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import org.amshove.kluent.itReturns
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class FiatWithdrawalTxEngineTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private lateinit var subject: FiatWithdrawalTxEngine
    private val walletManager: CustodialWalletManager = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()

    @Before
    fun setup() {
        subject = FiatWithdrawalTxEngine(walletManager)
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: FiatAccount = mock()
        val txTarget: LinkedBankAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(sourceAccount)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Account incorrect`() {
        val sourceAccount: CryptoAccount = mock()
        val txTarget: LinkedBankAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when target account incorrect`() {
        val sourceAccount: FiatAccount = mock()
        val txTarget: CryptoAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        val limits = PaymentLimits(FiatValue.fromMinor(TGT_ASSET, 100L), FiatValue.fromMinor(TGT_ASSET, 1000L))
        whenever(walletManager.getBankTransferLimits(TGT_ASSET, true)).thenReturn(Single.just(limits))
        val zeroFiat = FiatValue.zero(SELECTED_FIAT)

        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
            on { actionableBalance } itReturns Single.just(zeroFiat)
            on { accountBalance } itReturns Single.just(zeroFiat)
        }
        val txTarget: LinkedBankAccount = mock {
            on { fiatCurrency } itReturns TGT_ASSET
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == zeroFiat &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.feeForFullAvailable == zeroFiat &&
                    it.feeAmount == zeroFiat &&
                    it.selectedFiat == SELECTED_FIAT &&
                    it.confirmations.isEmpty() &&
                    it.minLimit == limits.min &&
                    it.maxLimit == zeroFiat &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection()
        )

        val inputAmount = FiatValue.fromMinor(SELECTED_FIAT, 1000L)

        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.feeAmount == zeroFiat
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
    }

    @Test
    fun `validate amount when pendingTx uninitialised`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            validationState = ValidationState.UNINITIALISED,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection()
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == zeroFiat &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.feeAmount == zeroFiat
            }
            .assertValue { verifyFeeLevels(it.feeSelection) }
    }

    @Test
    fun `validate amount when limits not set`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 1000L)
        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(),
            minLimit = null,
            maxLimit = null
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.UNKNOWN_ERROR
            }
    }

    @Test
    fun `validate amount when under min limit`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 1000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.UNDER_MIN_LIMIT
            }
    }

    @Test
    fun `validate amount when over max limit`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 1000000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.OVER_MAX_LIMIT
            }
    }

    @Test
    fun `validate amount when over available balance`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 3000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.INSUFFICIENT_FUNDS
            }
    }

    @Test
    fun `validate amount when correct`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val balance = FiatValue.fromMinor(SELECTED_FIAT, 4000L)
        val amount = FiatValue.fromMinor(SELECTED_FIAT, 3000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = balance,
            availableBalance = balance,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == pendingTx.amount &&
                    it.minLimit == pendingTx.minLimit &&
                    it.maxLimit == pendingTx.maxLimit &&
                    it.validationState == ValidationState.CAN_EXECUTE
            }
    }

    @Test
    fun `executing tx works`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock {
            on { receiveAddress } itReturns Single.just(bankAccountAddress)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 3000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        whenever(walletManager.createWithdrawOrder(amount, bankAccountAddress.address)).thenReturn(
            Completable.complete()
        )

        subject.doExecute(
            pendingTx, ""
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it is TxResult.UnHashedTxResult &&
                    it.amount == pendingTx.amount
            }

        verify(walletManager).createWithdrawOrder(amount, bankAccountAddress.address)
    }

    @Test
    fun `executing tx throws exception`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock {
            on { receiveAddress } itReturns Single.just(bankAccountAddress)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 3000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            feeForFullAvailable = zeroFiat,
            feeAmount = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeSelection = FeeSelection(),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        val exception = IllegalStateException("")
        whenever(walletManager.createWithdrawOrder(amount, bankAccountAddress.address)).thenReturn(
            Completable.error(exception)
        )

        subject.doExecute(
            pendingTx, ""
        ).test()
            .assertError {
                it == exception
            }

        verify(walletManager).createWithdrawOrder(amount, bankAccountAddress.address)
    }

    private fun verifyFeeLevels(feeSelection: FeeSelection) =
        feeSelection.selectedLevel == FeeLevel.None &&
            feeSelection.availableLevels == setOf(FeeLevel.None) &&
            feeSelection.availableLevels.contains(feeSelection.selectedLevel) &&
            feeSelection.customAmount == -1L &&
            feeSelection.asset == null

    companion object {
        private const val SELECTED_FIAT = "USD"
        private const val TGT_ASSET = "USD"
    }
}