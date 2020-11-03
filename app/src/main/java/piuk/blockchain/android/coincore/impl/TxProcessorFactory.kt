package piuk.blockchain.android.coincore.impl

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.datamanagers.SwapDirection
import com.blockchain.swap.nabu.datamanagers.repositories.QuotesProvider
import com.blockchain.swap.nabu.service.TierService
import io.reactivex.Single
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.TradingAccount
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TransferError
import piuk.blockchain.android.coincore.impl.txEngine.BtcBitpayTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.CustodialSellTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.InterestDepositTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.impl.txEngine.TradingToOnChainTxEngine
import piuk.blockchain.android.coincore.impl.txEngine.swap.OnChainSwapEngine
import piuk.blockchain.android.coincore.impl.txEngine.swap.TradingToTradingSwapTxEngine
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class TxProcessorFactory(
    private val bitPayManager: BitPayDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val walletManager: CustodialWalletManager,
    private val walletPrefs: WalletStatus,
    private val quotesProvider: QuotesProvider,
    private val analytics: Analytics,
    private val kycTierService: TierService,
    private val environmentConfig: EnvironmentConfig
) {
    fun createProcessor(
        source: CryptoAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> =
        when (source) {
            is CryptoNonCustodialAccount -> createOnChainProcessor(source, target, action)
            is CustodialTradingAccount -> createTradingProcessor(source, target, action)
            else -> Single.error(NotImplementedError())
        }

    private fun createOnChainProcessor(
        source: CryptoNonCustodialAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Single<TransactionProcessor> {
        val engine = source.createTxEngine() as OnChainTxEngineBase

        return when (target) {
            is BitPayInvoiceTarget -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = BtcBitpayTxEngine(
                        bitPayDataManager = bitPayManager,
                        walletPrefs = walletPrefs,
                        assetEngine = engine,
                        analytics = analytics
                    )
                )
            )
            is CryptoInterestAccount -> target.receiveAddress.map {
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = it,
                    engine = InterestDepositTxEngine(
                        onChainTxEngine = engine
                    )
                )
            }
            is CryptoAddress -> Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = engine
                )
            )
            is CryptoAccount -> if (action != AssetAction.Swap) target.receiveAddress.map {
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = it,
                    engine = engine
                )
            } else Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = OnChainSwapEngine(
                        isNoteSupported = false,
                        quotesProvider = quotesProvider,
                        walletManager = walletManager,
                        tiersService = kycTierService,
                        engine = engine,
                        environmentConfig = environmentConfig,
                        direction = if (target is CustodialTradingAccount)
                            SwapDirection.FROM_USERKEY else SwapDirection.ON_CHAIN
                    )
                )
            )

            else -> Single.error(TransferError("Cannot send non-custodial crypto to a non-crypto target"))
        }
    }

    private fun createTradingProcessor(
        source: CustodialTradingAccount,
        target: TransactionTarget,
        action: AssetAction
    ) = when (target) {
        is CryptoAddress ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = TradingToOnChainTxEngine(
                        walletManager = walletManager,
                        isNoteSupported = source.isNoteSupported
                    )
                )
            )
        is TradingAccount ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = TradingToTradingSwapTxEngine(
                        isNoteSupported = source.isNoteSupported,
                        walletManager = walletManager,
                        quotesProvider = quotesProvider,
                        kycTierService = kycTierService
                    )
                )
            )
        is CryptoAccount -> target.receiveAddress.map {
            TransactionProcessor(
                exchangeRates = exchangeRates,
                sourceAccount = source,
                txTarget = it,
                engine = TradingToOnChainTxEngine(
                    walletManager = walletManager,
                    isNoteSupported = source.isNoteSupported
                )
            )
        }
        is FiatAccount ->
            Single.just(
                TransactionProcessor(
                    exchangeRates = exchangeRates,
                    sourceAccount = source,
                    txTarget = target,
                    engine = CustodialSellTxEngine(
                        walletManager = walletManager
                    )
                )
            )
        else -> Single.error(TransferError("Cannot send custodial crypto to a non-crypto target"))
    }
}
