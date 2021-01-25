package piuk.blockchain.android.coincore.btc

import com.blockchain.android.testutils.rxInit
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.WalletStatus
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.satoshi
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.UnspentOutput
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.itReturns
import org.amshove.kluent.mock
import org.bitcoinj.core.NetworkParameters
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.injectMocks
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager

class BtcOnChainTxEngineTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val btcDataManager: PayloadDataManager = mock()
    private val sendDataManager: SendDataManager = mock()
    private val btcNetworkParams: NetworkParameters = mock()

    private val btcFeeOptions: FeeOptions = mock {
        on { regularFee } itReturns FEE_REGULAR
        on { priorityFee } itReturns FEE_PRIORITY
    }
    private val feeManager: FeeDataManager = mock {
        on { btcFeeOptions } itReturns Observable.just(btcFeeOptions)
    }

    private val walletPreferences: WalletStatus = mock {
        on { getFeeTypeForAsset(ASSET) } itReturns FeeLevel.Regular.ordinal
    }
    private val exchangeRates: ExchangeRateDataManager = mock()

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } itReturns SELECTED_FIAT
    }

    private val subject = BtcOnChainTxEngine(
        btcDataManager = btcDataManager,
        sendDataManager = sendDataManager,
        btcNetworkParams = btcNetworkParams,
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

    private val sourceAccount: BtcCryptoWalletAccount = mock()

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
        val inputAmount = 2.bitcoin()
        val feePerKb = (FEE_REGULAR * 1000).satoshi()
        val totalFee = (FEE_REGULAR * 1000 * 3).satoshi()

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
            on { address } itReturns TARGET_ADDRESS
        }

        val totalBalance = 21.bitcoin()
        val actionableBalance = 19.bitcoin()
        val totalSweepable = totalBalance - totalFee

        whenever(sourceAccount.xpubAddress).thenReturn(SOURCE_XPUB)
        whenever(sourceAccount.accountBalance).thenReturn(Single.just(totalBalance))
        whenever(sourceAccount.actionableBalance).thenReturn(Single.just(actionableBalance))

        whenever(btcDataManager.getAddressBalance(SOURCE_XPUB)).thenReturn(totalBalance)

        val unspentOutputs: UnspentOutputs = mock {
            on { unspentOutputs } itReturns arrayListOf<UnspentOutput>(mock(), mock())
        }
        whenever(sendDataManager.getUnspentBtcOutputs(SOURCE_XPUB))
            .thenReturn(Observable.just(unspentOutputs))

        whenever(
            sendDataManager.getMaximumAvailable(
                ASSET,
                unspentOutputs,
                feePerKb
            )
        ).thenReturn(totalSweepable as CryptoValue)

        val utxoBundle: SpendableUnspentOutputs = mock {
            on { absoluteFee } itReturns totalFee.toBigInteger()
        }

        whenever(sendDataManager.getSpendableCoins(
            unspentOutputs,
            inputAmount,
            feePerKb
        )).thenReturn(utxoBundle)

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
                it.availableBalance == totalSweepable &&
                it.fees == totalFee
            }

        verify(sourceAccount).xpubAddress
        verify(sourceAccount).accountBalance
        verify(btcDataManager).getAddressBalance(SOURCE_XPUB)
        verify(feeManager).btcFeeOptions
        verify(btcFeeOptions).regularFee
        verify(unspentOutputs).unspentOutputs
        verify(sendDataManager).getUnspentBtcOutputs(SOURCE_XPUB)
        verify(sendDataManager).getMaximumAvailable(ASSET, unspentOutputs, feePerKb)
        verify(utxoBundle).absoluteFee
        verify(sendDataManager).getSpendableCoins(unspentOutputs, inputAmount, feePerKb)

        noMoreInteractions(txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly for priority fees`() {
        // Arrange
        val inputAmount = 2.bitcoin()
        val feePerKb = (FEE_PRIORITY * 1000).satoshi()
        val totalFee = (FEE_REGULAR * 1000 * 3).satoshi()

        val txTarget: CryptoAddress = mock {
            on { asset } itReturns ASSET
            on { address } itReturns TARGET_ADDRESS
        }

        val totalBalance = 21.bitcoin()
        val actionableBalance = 19.bitcoin()
        val totalSweepable = totalBalance - totalFee

        whenever(sourceAccount.xpubAddress).thenReturn(SOURCE_XPUB)
        whenever(sourceAccount.accountBalance).thenReturn(Single.just(totalBalance))
        whenever(sourceAccount.actionableBalance).thenReturn(Single.just(actionableBalance))

        whenever(btcDataManager.getAddressBalance(SOURCE_XPUB)).thenReturn(totalBalance)

        val unspentOutputs: UnspentOutputs = mock {
            on { unspentOutputs } itReturns arrayListOf<UnspentOutput>(mock(), mock())
        }
        whenever(sendDataManager.getUnspentBtcOutputs(SOURCE_XPUB))
            .thenReturn(Observable.just(unspentOutputs))

        whenever(
            sendDataManager.getMaximumAvailable(
                ASSET,
                unspentOutputs,
                feePerKb
            )
        ).thenReturn(totalSweepable as CryptoValue)

        val utxoBundle: SpendableUnspentOutputs = mock {
            on { absoluteFee } itReturns totalFee.toBigInteger()
        }

        whenever(sendDataManager.getSpendableCoins(
            unspentOutputs,
            inputAmount,
            feePerKb
        )).thenReturn(utxoBundle)

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
            feeLevel = FeeLevel.Priority
        )

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
                    it.availableBalance == totalSweepable &&
                    it.fees == totalFee
            }

        verify(sourceAccount).xpubAddress
        verify(sourceAccount).accountBalance
        verify(btcDataManager).getAddressBalance(SOURCE_XPUB)
        verify(feeManager).btcFeeOptions
        verify(btcFeeOptions).priorityFee
        verify(unspentOutputs).unspentOutputs
        verify(sendDataManager).getUnspentBtcOutputs(SOURCE_XPUB)
        verify(sendDataManager).getMaximumAvailable(ASSET, unspentOutputs, feePerKb)
        verify(utxoBundle).absoluteFee
        verify(sendDataManager).getSpendableCoins(unspentOutputs, inputAmount, feePerKb)

        noMoreInteractions(txTarget)
    }

    private fun noMoreInteractions(txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(btcDataManager)
        verifyNoMoreInteractions(sendDataManager)
        verifyNoMoreInteractions(btcNetworkParams)
        verifyNoMoreInteractions(feeManager)
        verifyNoMoreInteractions(btcFeeOptions)
        verifyNoMoreInteractions(walletPreferences)
        verifyNoMoreInteractions(sourceAccount)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(currencyPrefs)
    }

    companion object {
        private val ASSET = CryptoCurrency.BTC
        private val WRONG_ASSET = CryptoCurrency.ETHER
        private const val SOURCE_XPUB = "VALID_BTC_XPUB"
        private const val TARGET_ADDRESS = "VALID_BTC_ADDRESS"
        private const val FEE_REGULAR = 5L
        private const val FEE_PRIORITY = 11L
        private const val SELECTED_FIAT = "INR"
    }
}
