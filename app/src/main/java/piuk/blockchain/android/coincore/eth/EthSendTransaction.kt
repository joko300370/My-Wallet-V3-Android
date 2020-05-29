package piuk.blockchain.android.coincore.eth

import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import org.web3j.crypto.RawTransaction
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.FeeSchedule
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.impl.OnChainSendTransactionBase
import piuk.blockchain.androidcore.data.ethereum.EthDataManager

class EthSendTransaction(
    private val ethDataManager: EthDataManager,
    sendingAccount: CryptoSingleAccount,
    address: CryptoAddress,
    availableBalance: CryptoValue,
    requireSecondPassword: Boolean
) : OnChainSendTransactionBase(
        sendingAccount,
        address,
        availableBalance,
        requireSecondPassword
) {

//    fromLabel = pendingTransaction.sendingObject!!.label,
//    toLabel = pendingTransaction.displayableReceivingLabel ?: throw IllegalStateException("No receive label"),
//    crypto = CryptoCurrency.ETHER,
//    fiatUnit = fiatCurrency,
//    cryptoAmount = amount.toStringWithoutSymbol(),
//    cryptoFee = fee.toStringWithoutSymbol(),
//    cryptoTotal = total.toStringWithoutSymbol(),
//    fiatFee = fee.toFiat(exchangeRates, fiatCurrency).toStringWithoutSymbol(),
//    fiatAmount = amount.toFiat(exchangeRates, fiatCurrency).toStringWithoutSymbol(),
//    fiatTotal = total.toFiat(exchangeRates, fiatCurrency).toStringWithSymbol()
//

    override var amount: CryptoValue = CryptoValue.ZeroEth
        set(value) { field = value } // Do some checks here? Or not? TBD

    override var fees: FeeSchedule
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override var notes: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
        set(value) {}

    override fun canExecute(): Single<ValidationState> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun absoluteFee(): Single<CryptoValue> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createTransaction(): Single<RawTransaction> =
        Singles.zip(
            ethDataManager.fetchEthAddress()
                .singleOrError() // TODO: Push Single-ness down the stack
                .map {
                    ethDataManager.getEthResponseModel()!!.getNonce()
                },
            ethDataManager.getIfContract(address.address)
                .singleOrError()
        )
        .map { (nonce, isContract) ->
            ethDataManager.createEthTransaction(
                nonce = nonce,
                to = address.address,
                gasPriceWei = getGasPrice().amount,
                gasLimitGwei = getGasLimit(isContract).amount,
                weiValue = amount.amount
            )
        }

    override fun executeTransaction(secondPassword: String): Single<String> =
        createTransaction()
            .flatMap {
                ethDataManager.signEthTransaction(it, secondPassword)
            }
            .flatMap { ethDataManager.pushTx(it) }
            .flatMap { ethDataManager.setLastTxHashNowSingle(it) }

    private fun getGasPrice(): CryptoValue {
        // get fee from fee options
        return CryptoValue.ZeroEth
    }

    private fun getGasLimit(isContract: Boolean): CryptoValue =
        CryptoValue.ZeroEth
    //gasLimitGwei = BigInteger.valueOf(
    //if (isContract) feeOptions!!.gasLimitContract else feeOptions!!.gasLimit)
}

//class EtherSendStrategy(
//    private val walletAccountHelper: WalletAccountHelper,
//    private val payloadDataManager: PayloadDataManager,
//    private val ethDataManager: EthDataManager,
//    private val stringUtils: StringUtils,
//    private val dynamicFeeCache: DynamicFeeCache,
//    private val feeDataManager: FeeDataManager,
//    private val exchangeRates: ExchangeRateDataManager,
//    private val analytics: Analytics,
//    prefs: CurrencyPrefs,
//    currencyState: CurrencyState,
//) {
//
////    private val networkParameters = environmentConfig.bitcoinNetworkParameters
//
//    private var feeOptions: FeeOptions? = null
//
//    private var absoluteSuggestedFee = BigInteger.ZERO
//
//    override fun getFeeOptions(): FeeOptions? = dynamicFeeCache.ethFeeOptions
//
//    /**
//     * Update absolute fee with smallest denomination of crypto currency (satoshi, wei, etc)
//     */
//    private fun updateFee(fee: BigInteger) {
//        absoluteSuggestedFee = fee
//
//        val cryptoValue = CryptoValue(CryptoCurrency.ETHER, absoluteSuggestedFee)
//        view?.updateFeeAmount(cryptoValue, cryptoValue.toFiat(exchangeRates, fiatCurrency))
//    }
//
//
//    private fun calculateSpendableAmounts(spendAll: Boolean, amountToSendText: String?) {
//
//        getSuggestedFee()
//        getAccountResponse(spendAll, amountToSendText)
//    }
//
//    /**
//     * Get cached dynamic fee from new Fee options endpoint
//     */
//    private fun getSuggestedFee() {
//        compositeDisposable += feeDataManager.ethFeeOptions
//            .doOnSubscribe { feeOptions = dynamicFeeCache.ethFeeOptions!! }
//            .doOnNext { dynamicFeeCache.ethFeeOptions = it }
//            .subscribe(
//                { /* No-op */ },
//                {
//                    Timber.e(it)
//                }
//            )
//    }
//
//    private fun getAccountResponse(spendAll: Boolean, amountToSendText: String?) {
//        view?.showMaxAvailable()
//
//        if (ethDataManager.getEthResponseModel() == null) {
//            compositeDisposable += ethDataManager.fetchEthAddress()
//                .doOnError { view?.showSnackbar(R.string.api_fail, Snackbar.LENGTH_INDEFINITE) }
//                .subscribe { calculateUnspent(it, spendAll, amountToSendText) }
//        } else {
//            ethDataManager.getEthResponseModel()?.let {
//                calculateUnspent(it, spendAll, amountToSendText)
//            }
//        }
//    }
//
//    private fun calculateUnspent(combinedEthModel: CombinedEthModel, spendAll: Boolean, amountToSendText: String?) {
//
//        val amountToSendSanitised = if (amountToSendText.isNullOrEmpty()) "0" else amountToSendText
//
//        val gwei = BigDecimal.valueOf(feeOptions!!.gasLimit * feeOptions!!.regularFee)
//        val wei = Convert.toWei(gwei, Convert.Unit.GWEI)
//
//        updateFee(wei.toBigInteger())
//        pendingTransaction.bigIntFee = wei.toBigInteger()
//
//        val addressResponse = combinedEthModel.getAddressResponse()
//        maxAvailable = addressResponse!!.balance.minus(wei.toBigInteger())
//        maxAvailable = maxAvailable.max(BigInteger.ZERO)
//
//        val availableEth = Convert.fromWei(maxAvailable.toString(), Convert.Unit.ETHER)
//        val cryptoValue = CryptoValue.fromMajor(CryptoCurrency.ETHER, availableEth ?: BigDecimal.ZERO)
//
//        if (spendAll) {
//            view?.updateCryptoAmount(cryptoValue)
//            pendingTransaction.bigIntAmount = availableEth.toBigInteger()
//        } else {
//            pendingTransaction.bigIntAmount = getWeiFromText(amountToSendSanitised, getDefaultDecimalSeparator())
//        }
//
//        // Check if any pending ether txs exist and warn user
//        compositeDisposable += isLastTxPending()
//            .subscribeBy(
//                onSuccess = { /* No-op */ },
//                onError = { Timber.e(it) }
//            )
//    }
//
//    private fun validateTransaction(): Observable<Pair<Boolean, Int>> {
//        return if (pendingTransaction.receivingAddress.isEmpty()) {
//            Observable.just(Pair(false, R.string.eth_invalid_address))
//        } else {
//            var validated = true
//            var errorMessage = R.string.unexpected_error
//            if (!FormatsUtil.isValidEthereumAddress(pendingTransaction.receivingAddress)) {
//                errorMessage = R.string.eth_invalid_address
//                validated = false
//            }
//
//            // Validate amount
//            if (!isValidAmount(pendingTransaction.bigIntAmount)) {
//                errorMessage = R.string.invalid_amount
//                validated = false
//            }
//
//            // Validate sufficient funds
//            if (maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
//                errorMessage = R.string.insufficient_funds
//                validated = false
//            }
//            Observable.just(Pair(validated, errorMessage))
//        }.flatMap { errorPair ->
//            if (errorPair.first) {
//                // Validate address does not have unconfirmed funds
//                isLastTxPending().toObservable()
//            } else {
//                Observable.just(errorPair)
//            }
//        }
//    }
//
//    private fun isLastTxPending() =
//        ethDataManager.isLastTxPending()
//            .observeOn(AndroidSchedulers.mainThread())
//            .map { hasUnconfirmed: Boolean ->
//
//                if (hasUnconfirmed) {
//                    view?.disableInput()
//                    view?.updateMaxAvailable(stringUtils.getString(R.string.eth_unconfirmed_wait))
//                    view?.updateMaxAvailableColor(R.color.product_red_medium)
//                } else {
//                    view?.enableInput()
//                }
//
//                val errorMessage = R.string.eth_unconfirmed_wait
//                Pair(!hasUnconfirmed, errorMessage)
//            }
//}