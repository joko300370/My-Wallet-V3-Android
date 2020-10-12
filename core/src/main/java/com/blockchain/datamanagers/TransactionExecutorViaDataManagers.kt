package com.blockchain.datamanagers

import com.blockchain.account.DefaultAccountDataManager
import com.blockchain.datamanagers.fees.BitcoinLikeFees
import com.blockchain.datamanagers.fees.EthereumFees
import com.blockchain.datamanagers.fees.NetworkFees
import com.blockchain.datamanagers.fees.XlmFees
import com.blockchain.datamanagers.fees.feeForType
import com.blockchain.fees.FeeType
import com.blockchain.logging.SwapDiagnostics
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.remoteconfig.CoinSelectionRemoteConfig
import com.blockchain.sunriver.Memo
import com.blockchain.sunriver.SendDetails
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.sendFundsOrThrow
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.payment.sum
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import org.bitcoinj.core.ECKey
import org.web3j.crypto.RawTransaction
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.androidcore.data.ethereum.exceptions.TransactionInProgressException
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.extensions.logAnalyticsError
import timber.log.Timber
import java.math.BigInteger

internal class TransactionExecutorViaDataManagers(
    private val payloadDataManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val paxAccount: Erc20Account,
    private val usdtAccount: Erc20Account,
    private val sendDataManager: SendDataManager,
    private val addressResolver: AddressResolver,
    private val accountLookup: AccountLookup,
    private val defaultAccountDataManager: DefaultAccountDataManager,
    private val ethereumAccountWrapper: EthereumAccountWrapper,
    private val xlmSender: XlmDataManager,
    private val coinSelectionRemoteConfig: CoinSelectionRemoteConfig,
    private val analytics: Analytics
) : TransactionExecutor {

    override fun executeTransaction(
        amount: CryptoValue,
        destination: String,
        sourceAccount: AccountReference,
        fees: NetworkFees,
        feeType: FeeType,
        memo: Memo?,
        diagnostics: SwapDiagnostics?
    ): Single<String> =
        when (amount.currency) {
            CryptoCurrency.BTC -> sendBtcTransaction(
                amount,
                destination,
                sourceAccount.toJsonAccount(),
                (fees as BitcoinLikeFees).feeForType(feeType),
                diagnostics
            )
            CryptoCurrency.ETHER -> sendEthTransaction(
                amount,
                destination,
                sourceAccount.toJsonAccount(),
                fees as EthereumFees,
                feeType
            )
            CryptoCurrency.BCH -> sendBchTransaction(
                amount,
                destination,
                sourceAccount.toJsonAccount(),
                (fees as BitcoinLikeFees).feeForType(feeType),
                diagnostics
            )
            CryptoCurrency.XLM -> xlmSender.sendFundsOrThrow(
                SendDetails(
                    from = sourceAccount,
                    value = amount,
                    toAddress = destination,
                    toLabel = "",
                    fee = (fees as XlmFees).feeForType(feeType),
                    memo = memo
                )
            ).logAnalyticsError(analytics).map { it.hash!! }
            CryptoCurrency.PAX,
            CryptoCurrency.USDT ->
                sendErc20Transaction(fees as EthereumFees, destination, amount, amount.currency)
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> TODO("STUB: ALGO NOT IMPLEMENTED")
        }

    private fun sendErc20Transaction(
        feeOptions: EthereumFees,
        receivingAddress: String,
        cryptoValue: CryptoValue,
        asset: CryptoCurrency
    ) = createErc20Transaction(asset, feeOptions, receivingAddress, cryptoValue.toBigInteger())
        .flatMap {
            val ecKey =
                EthereumAccount.deriveECKey(payloadDataManager.wallet!!.hdWallets[0].masterKey,
                    0)
            return@flatMap ethDataManager.signEthTransaction(it, ecKey)
        }
        .flatMap { ethDataManager.pushEthTx(it).logAnalyticsError(analytics) }
        .flatMap { ethDataManager.setLastTxHashObservable(it, System.currentTimeMillis()) }
        .subscribeOn(Schedulers.io())
        .singleOrError()

    private fun createErc20Transaction(
        asset: CryptoCurrency,
        ethFees: EthereumFees,
        receivingAddress: String,
        amount: BigInteger
    ): Observable<RawTransaction> {
        val feeWei = ethFees.gasPriceRegularInWei

        return ethDataManager.fetchEthAddress()
            .map { ethDataManager.getEthResponseModel()!!.getNonce() }
            .map {
                when (asset) {
                    CryptoCurrency.PAX -> {
                        paxAccount.createTransaction(
                            nonce = it,
                            to = receivingAddress,
                            contractAddress = ethDataManager.getErc20TokenData(
                                asset).contractAddress,
                            gasPriceWei = feeWei,
                            gasLimitGwei = ethFees.gasLimitInGwei,
                            amount = amount)
                    }
                    CryptoCurrency.USDT -> {
                        usdtAccount.createTransaction(
                            nonce = it,
                            to = receivingAddress,
                            contractAddress = ethDataManager.getErc20TokenData(
                                asset).contractAddress,
                            gasPriceWei = feeWei,
                            gasLimitGwei = ethFees.gasLimitInGwei,
                            amount = amount)
                    }
                    else -> {
                        throw IllegalStateException(
                            "This should not happen, did we add a new ERC-20 asset?")
                    }
                }
            }
    }

    override fun getMaximumSpendable(
        account: AccountReference,
        fees: NetworkFees,
        feeType: FeeType
    ): Single<CryptoValue> =
        when (account) {
            is AccountReference.BitcoinLike ->
                account.getMaximumSpendable(
                    fees as BitcoinLikeFees,
                    feeType
                )
            is AccountReference.Ethereum -> getMaxEther(fees as EthereumFees, feeType)
            is AccountReference.Xlm -> defaultAccountDataManager.getMaxSpendableAfterFees(feeType)
            is AccountReference.Pax -> getMaxSpendablePax()
            is AccountReference.Stx -> TODO("STUB: STX NOT IMPLEMENTED")
            is AccountReference.Usdt -> getMaxSpendableUsdt()
        }

    override fun getFeeForTransaction(
        amount: CryptoValue,
        sourceAccount: AccountReference,
        fees: NetworkFees,
        feeType: FeeType
    ): Single<CryptoValue> =
        when (sourceAccount) {
            is AccountReference.BitcoinLike -> calculateBitcoinLikeFee(
                sourceAccount,
                amount,
                (fees as BitcoinLikeFees).feeForType(feeType)
            )
            is AccountReference.Pax,
            is AccountReference.Usdt,
            is AccountReference.Ethereum -> {
                when (feeType) {
                    FeeType.Regular -> (fees as EthereumFees).absoluteRegularFeeInWei.just()
                    FeeType.Priority -> (fees as EthereumFees).absolutePriorityFeeInWei.just()
                }
            }
            is AccountReference.Xlm -> (fees as XlmFees).feeForType(feeType).just()
            is AccountReference.Stx -> TODO("STUB: STX NOT IMPLEMENTED")
        }

    override fun getChangeAddress(
        accountReference: AccountReference
    ): Single<String> =
        addressResolver.addressPairForAccount(accountReference).map { it.changeAddress }

    override fun getReceiveAddress(
        accountReference: AccountReference
    ): Single<String> =
        addressResolver.addressPairForAccount(accountReference).map { it.receivingAddress }

    private fun calculateBitcoinLikeFee(
        account: AccountReference.BitcoinLike,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<CryptoValue> =
        getUnspentOutputs(account.xpub, amount.currency)
            .zipWith(coinSelectionRemoteConfig.enabled)
            .map { (unspentOutputs, newCoinSelectionEnabled) ->
                getSuggestedAbsoluteFee(unspentOutputs, amount, feePerKb, newCoinSelectionEnabled)
            }
            .map { CryptoValue(amount.currency, it) }

    private fun getSuggestedAbsoluteFee(
        coins: UnspentOutputs,
        amountToSend: CryptoValue,
        feePerKb: BigInteger,
        useNewCoinSelection: Boolean
    ): BigInteger =
        sendDataManager.getSpendableCoins(coins, amountToSend, feePerKb,
            useNewCoinSelection).absoluteFee

    private fun AccountReference.BitcoinLike.getMaximumSpendable(
        fees: BitcoinLikeFees,
        feeType: FeeType
    ): Single<CryptoValue> =
        getUnspentOutputs(xpub, cryptoCurrency)
            .zipWith(coinSelectionRemoteConfig.enabled)
            .map { (unspentOutputs, newCoinSelectionEnabled) ->
                CryptoValue(
                    cryptoCurrency,
                    sendDataManager.getMaximumAvailable(
                        cryptoCurrency,
                        unspentOutputs,
                        fees.feeForType(feeType),
                        newCoinSelectionEnabled
                    ).left
                )
            }
            .doOnError { Timber.e(it) }
            .onErrorReturn { CryptoValue.zero(cryptoCurrency) }

    private fun getMaxEther(fees: EthereumFees, feeType: FeeType): Single<CryptoValue> =
        ethDataManager.fetchEthAddress()
            .map {
                val fee = when (feeType) {
                    is FeeType.Regular -> fees.absoluteRegularFeeInWei
                    is FeeType.Priority -> fees.absolutePriorityFeeInWei
                }
                (it.getAddressResponse()!!.balance - fee.toBigInteger()).max(BigInteger.ZERO)
            }
            .map { CryptoValue.fromMinor(CryptoCurrency.ETHER, it) }
            .doOnError { Timber.e(it) }
            .onErrorReturn { CryptoValue.ZeroEth }
            .singleOrError()

    private fun getMaxSpendablePax(): Single<CryptoValue> =
        paxAccount.getBalance()
            .map { CryptoValue.fromMinor(CryptoCurrency.PAX, it) }
            .doOnError { Timber.e(it) }
            .onErrorReturn { CryptoValue.ZeroPax }

    private fun getMaxSpendableUsdt(): Single<CryptoValue> =
        usdtAccount.getBalance()
            .map { CryptoValue.fromMinor(CryptoCurrency.USDT, it) }
            .doOnError { Timber.e(it) }
            .onErrorReturn { CryptoValue.ZeroUsdt }

    private fun sendBtcTransaction(
        amount: CryptoValue,
        destination: String,
        account: Account,
        feePerKb: BigInteger,
        diagnostics: SwapDiagnostics?
    ): Single<String> = sendBitcoinStyleTransaction(
        amount,
        destination,
        account,
        feePerKb,
        addressResolver.getChangeAddress(account),
        diagnostics
    )

    private fun sendBchTransaction(
        amount: CryptoValue,
        destination: String,
        account: GenericMetadataAccount,
        feePerKb: BigInteger,
        diagnostics: SwapDiagnostics?
    ): Single<String> = sendBitcoinStyleTransaction(
        amount,
        destination,
        account.getHdAccount(),
        feePerKb,
        addressResolver.getChangeAddress(account),
        diagnostics
    )

    private fun sendBitcoinStyleTransaction(
        amount: CryptoValue,
        destination: String,
        account: Account,
        feePerKb: BigInteger,
        changeAddress: Single<String>,
        diagnostics: SwapDiagnostics?
    ): Single<String> = getSpendableCoins(account.xpub, amount, feePerKb)
        .flatMap { spendable ->
            diagnostics?.apply {
                log("sendBTC-style Tx")
                logBalance(CryptoValue(amount.currency, spendable.spendableOutputs.sum()))
                logAbsoluteFee(spendable.absoluteFee)
                logSwapAmount(amount)
            }
            getSigningKeys(account, spendable)
                .flatMap { signingKeys ->
                    changeAddress
                        .flatMap {
                            submitBitcoinStylePayment(
                                amount,
                                spendable,
                                signingKeys,
                                destination,
                                it,
                                spendable.absoluteFee
                            ).logAnalyticsError(analytics)
                        }
                }
        }

    private fun sendEthTransaction(
        amount: CryptoValue,
        destination: String,
        account: EthereumAccount,
        fees: EthereumFees,
        feeType: FeeType
    ): Single<String> =
        ethDataManager.isLastTxPending()
            .doOnSuccess {
                if (it == true)
                    throw TransactionInProgressException(
                        "Transaction pending, user cannot send funds at this time")
            }
            .flatMap {
                ethDataManager.fetchEthAddress()
                    .map {
                        val gasPrice = when (feeType) {
                            FeeType.Regular -> fees.gasPriceRegularInWei
                            FeeType.Priority -> fees.gasPricePriorityInWei
                        }
                        ethDataManager.createEthTransaction(
                            nonce = ethDataManager.getEthResponseModel()!!.getNonce(),
                            to = destination,
                            gasPriceWei = gasPrice,
                            gasLimitGwei = fees.gasLimitInGwei,
                            weiValue = amount.toBigInteger()
                        )
                    }
                    .map {
                        account.signTransaction(
                            it,
                            ethereumAccountWrapper.deriveECKey(payloadDataManager.masterKey, 0)
                        )
                    }
                    .flatMap { ethDataManager.pushEthTx(it).logAnalyticsError(analytics) }
                    .flatMap {
                        ethDataManager.setLastTxHashObservable(it, System.currentTimeMillis())
                    }
                    .subscribeOn(Schedulers.io())
                    .singleOrError()
            }

    private fun getSpendableCoins(
        address: String,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<SpendableUnspentOutputs> = getUnspentOutputs(address, amount.currency)
        .zipWith(coinSelectionRemoteConfig.enabled)
        .subscribeOn(Schedulers.io())
        .map { (unspentOutputs, newCoinSelectionEnabled) ->
            sendDataManager.getSpendableCoins(unspentOutputs, amount, feePerKb,
                newCoinSelectionEnabled)
        }

    private fun getUnspentOutputs(
        address: String,
        currency: CryptoCurrency
    ): Single<UnspentOutputs> =
        when (currency) {
            CryptoCurrency.BTC -> sendDataManager.getUnspentBtcOutputs(address)
            CryptoCurrency.BCH -> sendDataManager.getUnspentBchOutputs(address)
            CryptoCurrency.ETHER,
            CryptoCurrency.XLM,
            CryptoCurrency.PAX,
            CryptoCurrency.STX,
            CryptoCurrency.ALGO,
            CryptoCurrency.USDT -> throw IllegalArgumentException(
                "${currency.networkTicker} does not have unspent outputs")
        }.subscribeOn(Schedulers.io())
            .singleOrError()

    private fun submitBitcoinStylePayment(
        amount: CryptoValue,
        unspent: SpendableUnspentOutputs,
        signingKeys: List<ECKey>,
        depositAddress: String,
        changeAddress: String,
        absoluteFee: BigInteger
    ): Single<String> = when (amount.currency) {
        CryptoCurrency.BTC -> sendDataManager.submitBtcPayment(
            unspent,
            signingKeys,
            depositAddress,
            changeAddress,
            absoluteFee,
            amount.toBigInteger()
        )
        CryptoCurrency.BCH -> sendDataManager.submitBchPayment(
            unspent,
            signingKeys,
            depositAddress,
            changeAddress,
            absoluteFee,
            amount.toBigInteger()
        )
        CryptoCurrency.ETHER,
        CryptoCurrency.XLM,
        CryptoCurrency.PAX,
        CryptoCurrency.STX,
        CryptoCurrency.ALGO,
        CryptoCurrency.USDT -> throw IllegalArgumentException(
            "${amount.currency.networkTicker} not supported by this method")
    }.subscribeOn(Schedulers.io())
        .singleOrError()

    private fun getSigningKeys(
        account: Account,
        spendable: SpendableUnspentOutputs
    ): Single<List<ECKey>> =
        payloadDataManager.getHDKeysForSigning(account, spendable).just()

    private fun GenericMetadataAccount.getHdAccount(): Account =
        payloadDataManager.getAccountForXPub(this.xpub)

    private fun <T> T.just(): Single<T> = Single.just(this)

    private fun BitcoinLikeFees.feeForType(feeType: FeeType): BigInteger = when (feeType) {
        FeeType.Regular -> this.regularFeePerKb
        FeeType.Priority -> this.priorityFeePerKb
    }

    private inline fun <reified T> AccountReference.toJsonAccount() =
        accountLookup.getAccountFromAddressOrXPub(this) as T
}
