package piuk.blockchain.android.coincore.impl.txEngine.sell

import com.blockchain.android.testutils.rxInit
import com.blockchain.koin.payloadScopeQualifier
import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.TransferDirection
import com.blockchain.nabu.datamanagers.TransferLimits
import com.blockchain.nabu.datamanagers.TransferQuote
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.models.responses.nabu.NabuApiException
import com.blockchain.nabu.models.responses.nabu.NabuErrorCodes
import com.blockchain.nabu.service.TierService
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.testutils.bitcoin
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Observable
import io.reactivex.Single
import junit.framework.Assert
import org.amshove.kluent.itReturns
import org.bitcoinj.core.NetworkParameters
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.btc.BtcAddress
import piuk.blockchain.android.coincore.btc.BtcCryptoWalletAccount
import piuk.blockchain.android.coincore.impl.CustodialTradingAccount
import piuk.blockchain.android.coincore.impl.injectMocks
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.impl.txEngine.PricedQuote
import piuk.blockchain.android.coincore.impl.txEngine.TransferQuotesEngine
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class NonCustodialSellEngineTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private val walletManager: CustodialWalletManager = mock()
    private val quotesEngine: TransferQuotesEngine = mock()
    private val kycTierService: TierService = mock()
    private val btcNetworkParams: NetworkParameters = mock()
    private val environmentConfig: EnvironmentConfig = mock {
        on { bitcoinNetworkParameters } itReturns btcNetworkParams
    }

    private val exchangeRates: ExchangeRateDataManager = mock {
        on { getLastPrice(SRC_ASSET, TGT_ASSET) } itReturns EXCHANGE_RATE
    }

    private val currencyPrefs: CurrencyPrefs = mock {
        on { selectedFiatCurrency } itReturns SELECTED_FIAT
    }

    private val onChainEngine: OnChainTxEngineBase = mock()

    private val subject = NonCustodialSellTxEngine(
        engine = onChainEngine,
        walletManager = walletManager,
        quotesEngine = quotesEngine,
        kycTierService = kycTierService,
        environmentConfig = environmentConfig
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

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: BtcCryptoWalletAccount = mock {
            on { asset } itReturns SRC_ASSET
        }

        val txTarget: FiatAccount = mock {
            on { fiatCurrency } itReturns TGT_ASSET
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        // Assert
        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).fiatCurrency
        verify(quotesEngine).start(
            TransferDirection.FROM_USERKEY,
            CurrencyPair.CryptoToFiatCurrencyPair(SRC_ASSET, TGT_ASSET)
        )

        noMoreInteractions(txTarget)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Account incorrect`() {
        val sourceAccount: CustodialTradingAccount = mock {
            on { asset } itReturns WRONG_ASSET
        }

        val txTarget: FiatAccount = mock {
            on { fiatCurrency } itReturns TGT_ASSET
        }

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
        val sourceAccount: BtcCryptoWalletAccount = mock {
            on { asset } itReturns SRC_ASSET
        }

        val txTarget: CryptoAccount = mock {
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
        val sourceAccount: BtcCryptoWalletAccount = mock {
            on { asset } itReturns SRC_ASSET
        }

        val txTarget: FiatAccount = mock {
            on { fiatCurrency } itReturns TGT_ASSET
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val asset = subject.asset

        // Assert
        Assert.assertEquals(asset, SRC_ASSET)

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).fiatCurrency
        verify(quotesEngine).start(
            TransferDirection.FROM_USERKEY,
            CurrencyPair.CryptoToFiatCurrencyPair(SRC_ASSET, TGT_ASSET)
        )

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        whenUserIsGold()
        whenOnChainEngineInitOK(totalBalance, availableBalance)

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: FiatAccount = mock {
            on { fiatCurrency } itReturns TGT_ASSET
        }

        val txQuote: TransferQuote = mock {
            on { sampleDepositAddress } itReturns SAMPLE_DEPOSIT_ADDRESS
        }

        val pricedQuote: PricedQuote = mock {
            on { transferQuote } itReturns txQuote
        }

        whenever(quotesEngine.pricedQuote).thenReturn(Observable.just(pricedQuote))

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(SRC_ASSET) &&
                it.totalBalance == totalBalance &&
                it.availableBalance == availableBalance &&
                it.fees == CryptoValue.zero(SRC_ASSET) &&
                it.selectedFiat == TGT_ASSET &&
                it.feeLevel == FeeLevel.Priority &&
                it.customFeeAmount == -1L &&
                it.confirmations.isEmpty() &&
                it.minLimit == MIN_GOLD_LIMIT_ASSET &&
                it.maxLimit == MAX_GOLD_LIMIT_ASSET &&
                it.validationState == ValidationState.UNINITIALISED &&
                it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).fiatCurrency
        verifyQuotesEngineStarted()
        verifyOnChainEngineStarted(sourceAccount)
        verifyLimitsFetched()
        verify(quotesEngine).pricedQuote
        verify(exchangeRates).getLastPrice(SRC_ASSET, TGT_ASSET)
        verify(environmentConfig).bitcoinNetworkParameters

        noMoreInteractions(txTarget)
    }

    @Test
    fun `PendingTx initialisation when limit reached`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: FiatAccount = mock {
            on { fiatCurrency } itReturns TGT_ASSET
        }

        val error: NabuApiException = mock {
            on { getErrorCode() } itReturns NabuErrorCodes.PendingOrdersLimitReached
        }

        whenever(quotesEngine.pricedQuote).thenReturn(Observable.error(error))

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        // Act
        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == CryptoValue.zero(SRC_ASSET) &&
                it.totalBalance == CryptoValue.zero(SRC_ASSET) &&
                it.availableBalance == CryptoValue.zero(SRC_ASSET) &&
                it.fees == CryptoValue.zero(SRC_ASSET) &&
                it.selectedFiat == TGT_ASSET &&
                it.feeLevel == FeeLevel.None &&
                it.customFeeAmount == -1L &&
                it.confirmations.isEmpty() &&
                it.minLimit == null &&
                it.maxLimit == null &&
                it.validationState == ValidationState.PENDING_ORDERS_LIMIT_REACHED &&
                it.engineState.isEmpty()
            }
            .assertNoErrors()
            .assertComplete()

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).fiatCurrency
        verifyQuotesEngineStarted()
        verify(quotesEngine).pricedQuote

        noMoreInteractions(txTarget)
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        // Arrange
        val totalBalance: Money = 21.bitcoin()
        val availableBalance: Money = 20.bitcoin()

        val sourceAccount = fundedSourceAccount(totalBalance, availableBalance)

        val txTarget: FiatAccount = mock {
            on { fiatCurrency } itReturns TGT_ASSET
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val inputAmount = 2.bitcoin()
        val expectedFee = 0.bitcoin()

        val pendingTx = PendingTx(
            amount = CryptoValue.zero(SRC_ASSET),
            totalBalance = CryptoValue.zero(SRC_ASSET),
            availableBalance = CryptoValue.zero(SRC_ASSET),
            fees = CryptoValue.zero(SRC_ASSET),
            selectedFiat = TGT_ASSET,
            feeLevel = FeeLevel.Priority
        )

        whenever(onChainEngine.doUpdateAmount(inputAmount, pendingTx))
            .thenReturn(
                Single.just(
                    pendingTx.copy(
                        amount = inputAmount,
                        totalBalance = totalBalance,
                        availableBalance = availableBalance,
                        fees = expectedFee
                    )
                )
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
                it.availableBalance == availableBalance &&
                it.fees == expectedFee
            }

        verify(sourceAccount, atLeastOnce()).asset
        verify(txTarget, atLeastOnce()).fiatCurrency
        verify(quotesEngine).start(
            TransferDirection.FROM_USERKEY,
            CurrencyPair.CryptoToFiatCurrencyPair(SRC_ASSET, TGT_ASSET)
        )
        verify(quotesEngine).updateAmount(inputAmount)

        noMoreInteractions(txTarget)
    }

    private fun fundedSourceAccount(totalBalance: Money, availableBalance: Money) =
        mock<BtcCryptoWalletAccount> {
            on { asset } itReturns SRC_ASSET
            on { accountBalance } itReturns Single.just(totalBalance)
            on { actionableBalance } itReturns Single.just(availableBalance)
        }

    private fun whenOnChainEngineInitOK(totalBalance: Money, availableBalance: Money) {
        val initialisedPendingTx = PendingTx(
            amount = CryptoValue.zero(SRC_ASSET),
            totalBalance = totalBalance,
            availableBalance = availableBalance,
            fees = CryptoValue.zero(SRC_ASSET),
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.Priority
        )
        whenever(onChainEngine.doInitialiseTx()).thenReturn(Single.just(initialisedPendingTx))
    }

    private fun whenUserIsGold() {
        val kycTiers: KycTiers = mock()
        whenever(kycTierService.tiers()).thenReturn(Single.just(kycTiers))

        whenever(walletManager.getSwapLimits(TGT_ASSET))
            .itReturns(
                Single.just(
                    TransferLimits(
                        minLimit = MIN_GOLD_LIMIT,
                        maxOrder = MAX_GOLD_ORDER,
                        maxLimit = MAX_GOLD_LIMIT
                    )
                )
            )
    }

    private fun verifyLimitsFetched() {
        verify(kycTierService).tiers()
        verify(walletManager).getSwapLimits(TGT_ASSET)
    }

    private fun verifyOnChainEngineStarted(srcAccount: CryptoAccount) {
        verify(onChainEngine).start(
            sourceAccount = eq(srcAccount),
            txTarget = argThat { this is BtcAddress && address == SAMPLE_DEPOSIT_ADDRESS },
            exchangeRates = eq(exchangeRates),
            refreshTrigger = any()
        )
    }

    private fun verifyQuotesEngineStarted() {
        verify(quotesEngine).start(
            TransferDirection.FROM_USERKEY,
            CurrencyPair.CryptoToFiatCurrencyPair(SRC_ASSET, TGT_ASSET)
        )
    }

    private fun noMoreInteractions(txTarget: TransactionTarget) {
        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(walletManager)
        verifyNoMoreInteractions(currencyPrefs)
        verifyNoMoreInteractions(exchangeRates)
        verifyNoMoreInteractions(quotesEngine)
        verifyNoMoreInteractions(kycTierService)
        verifyNoMoreInteractions(environmentConfig)
    }

    companion object {
        private const val SELECTED_FIAT = "INR"
        private val SRC_ASSET = CryptoCurrency.BTC
        private const val TGT_ASSET = "EUR"
        private val WRONG_ASSET = CryptoCurrency.BTC
        private val EXCHANGE_RATE = 2.toBigDecimal() // 1 btc == 2 EUR

        private const val SAMPLE_DEPOSIT_ADDRESS = "initial quote deposit address"

        private val MIN_GOLD_LIMIT = FiatValue.fromMajor(TGT_ASSET, 100.toBigDecimal())
        private val MAX_GOLD_ORDER = FiatValue.fromMajor(TGT_ASSET, 500.toBigDecimal())
        private val MAX_GOLD_LIMIT = FiatValue.fromMajor(TGT_ASSET, 2000.toBigDecimal())

        private val MIN_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 50.toBigDecimal())
        private val MAX_GOLD_ORDER_ASSET = CryptoValue.fromMajor(SRC_ASSET, 250.toBigDecimal())
        private val MAX_GOLD_LIMIT_ASSET = CryptoValue.fromMajor(SRC_ASSET, 1000.toBigDecimal())
    }
}
