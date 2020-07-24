package piuk.blockchain.android.coincore.erc20

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.coincore.ActivitySummaryItem
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AvailableActions
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.SendProcessor
import piuk.blockchain.android.coincore.SendState
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.coincore.TransferError
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.erc20.FeedErc20Transfer
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.utils.extensions.mapList

abstract class Erc20NonCustodialAccount(
    asset: CryptoCurrency,
    override val label: String,
    private val fees: FeeDataManager,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoNonCustodialAccount(asset) {

    final override val feeAsset: CryptoCurrency? = CryptoCurrency.ETHER

    abstract val erc20Account: Erc20Account

    private val ethDataManager: EthDataManager
        get() = erc20Account.ethDataManager

    override val isDefault: Boolean = true // Only one account, so always default

    override val balance: Single<Money>
        get() = erc20Account.getBalance()
            .map { CryptoValue.fromMinor(asset, it) }

    override val activity: Single<ActivitySummaryList>
        get() {
            val feedTransactions =
                erc20Account.fetchErc20Address()
                    .flatMap { erc20Account.getTransactions() }
                    .mapList {
                        val feeObservable = ethDataManager
                            .getTransaction(it.transactionHash)
                            .map { transaction ->
                                transaction.gasUsed * transaction.gasPrice
                            }
                        FeedErc20Transfer(it, feeObservable)
                    }

            return Singles.zip(
                feedTransactions,
                erc20Account.getAccountHash(),
                ethDataManager.getLatestBlockNumber()
            ) { transactions, accountHash, latestBlockNumber ->
                transactions.map { transaction ->
                    Erc20ActivitySummaryItem(
                        asset,
                        feedTransfer = transaction,
                        accountHash = accountHash,
                        ethDataManager = ethDataManager,
                        exchangeRates = exchangeRates,
                        lastBlockNumber = latestBlockNumber.number,
                        account = this
                    ) as ActivitySummaryItem
                }
            }.doOnSuccess { setHasTransactions(it.isNotEmpty()) }
        }

    override val actions: AvailableActions = setOf(
        AssetAction.ViewActivity,
        AssetAction.NewSend,
        AssetAction.Receive,
        AssetAction.Swap
    )

    override fun createSendProcessor(sendTo: SendTarget): Single<SendProcessor> =
        when (sendTo) {
            is CryptoAddress -> Single.just(
                Erc20SendTransaction(
                    asset = asset,
                    erc20Account = erc20Account,
                    feeManager = fees,
                    sendingAccount = this,
                    sendTarget = sendTo as Erc20Address,
                    requireSecondPassword = ethDataManager.requireSecondPassword
                )
            )
            is CryptoAccount -> sendTo.receiveAddress.map {
                Erc20SendTransaction(
                    asset = asset,
                    erc20Account = erc20Account,
                    feeManager = fees,
                    sendingAccount = this,
                    sendTarget = sendTo as Erc20Address,
                    requireSecondPassword = ethDataManager.requireSecondPassword
                )
            }
            else -> Single.error(TransferError("Cannot send custodial crypto to a non-crypto target"))
        }

    override val sendState: Single<SendState>
        get() = Singles.zip(
            balance,
            ethDataManager.isLastTxPending()
        ) { balance: Money, hasUnconfirmed: Boolean ->
            when {
                balance.isZero -> SendState.NO_FUNDS
                hasUnconfirmed -> SendState.SEND_IN_FLIGHT
                else -> SendState.CAN_SEND
            }
        }
}

internal open class Erc20Address(
    final override val asset: CryptoCurrency,
    override val address: String,
    override val label: String = address
) : CryptoAddress {
    init {
        require(asset.hasFeature(CryptoCurrency.IS_ERC20))
    }
}