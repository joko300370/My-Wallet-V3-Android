package piuk.blockchain.android.ui.send.strategy

import android.annotation.SuppressLint
import com.google.android.material.snackbar.Snackbar
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.NabuApiException
import com.blockchain.swap.nabu.models.nabu.NabuErrorCodes
import com.blockchain.swap.nabu.models.nabu.State
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.SendAnalytics
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.account.PitAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.lang.IllegalStateException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.TimeUnit

class EtherSendStrategy(
    private val walletAccountHelper: WalletAccountHelper,
    private val payloadDataManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val stringUtils: StringUtils,
    private val dynamicFeeCache: DynamicFeeCache,
    private val feeDataManager: FeeDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val nabuDataManager: NabuDataManager,
    private val nabuToken: NabuToken,
    private val pitLinking: PitLinking,
    private val analytics: Analytics,
    prefs: CurrencyPrefs,
    currencyState: CurrencyState,
    environmentConfig: EnvironmentConfig
) : SendStrategy<SendView>(currencyState, prefs) {

    override fun onViewAttached() {}
    override fun onViewDetached() {}

    override val alwaysDisableScreenshots = false
    override val enableLogoutTimer = false

    private var pitAccount: PitAccount? = null

    override fun onPitAddressSelected() {
        pitAccount?.let {
            pendingTransaction.receivingObject = ItemAccount(
                label = it.label,
                address = it.address
            )
            pendingTransaction.receivingAddress = it.address
            view?.updateReceivingAddress(it.label)
        }
    }

    override fun onPitAddressCleared() {
        pendingTransaction.receivingObject = null
        view?.updateReceivingAddress("")
    }

    override fun onCurrencySelected() {
        currencyState.cryptoCurrency = CryptoCurrency.ETHER
        onEtherChosen()
    }

    override fun selectSendingAccount(account: JsonSerializableAccount?) {
        throw IllegalArgumentException("Multiple accounts not supported for ETH")
    }

    override fun selectReceivingAccount(account: JsonSerializableAccount?) {
        throw IllegalArgumentException("Multiple accounts not supported for ETH")
    }

    private val pendingTransaction by unsafeLazy { PendingTransaction() }
    private val networkParameters = environmentConfig.bitcoinNetworkParameters

    private var feeOptions: FeeOptions? = null
    private var textChangeSubject = PublishSubject.create<String>()
    private var absoluteSuggestedFee = BigInteger.ZERO
    private var maxAvailable = BigInteger.ZERO
    private var verifiedSecondPassword: String? = null

    /**
     * External changes.
     * Possible currency change, Account/address archive, Balance change
     */
    override fun onBroadcastReceived() {
        resetAccountList()
    }

    override fun onViewReady() {
        resetAccountList()
        setupTextChangeSubject()
    }

    override fun onResume() {
        onEtherChosen()
    }

    private fun onEtherChosen() {
        view?.let {
            reset()
            it.hideFeePriority()
            it.setFeePrioritySelection(0)
            it.disableFeeDropdown()
            it.setCryptoMaxLength(30)
        }
    }

    override fun reset() {
        super.reset()
        pendingTransaction.clear()
        absoluteSuggestedFee = BigInteger.ZERO

        view?.let {
            resetAccountList()
            selectDefaultOrFirstFundedSendingAccount()
        }
    }

    private fun resetAccountList() {
        compositeDisposable += pitLinking.isPitLinked().filter { it }
            .flatMapSingle { nabuToken.fetchNabuToken() }
            .flatMap {
                nabuDataManager.fetchCryptoAddressFromThePit(it, CryptoCurrency.ETHER)
            }.applySchedulers().doOnSubscribe {
                view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.ETHER, 1, false)
            }.subscribeBy(onError = {
                view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.ETHER,
                    1,
                    it is NabuApiException && it.getErrorCode() == NabuErrorCodes.Bad2fa
                ) { view?.show2FANotAvailableError() }
            }) {
                pitAccount = PitAccount(
                    stringUtils.getFormattedString(
                        R.string.exchange_default_account_label, CryptoCurrency.ETHER.displayTicker
                    ),
                    it.address
                )
                view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.ETHER,
                    1,
                    it.state == State.ACTIVE && it.address.isNotEmpty()
                ) { view?.fillOrClearAddress() }
            }
    }

    override fun processURIScanAddress(address: String) {
        pendingTransaction.receivingAddress = address
        view?.updateReceivingAddress(address)
    }

    @SuppressLint("CheckResult")
    override fun onContinueClicked() {
        view?.showProgressDialog(R.string.app_name)

        checkManualAddressInput()

        compositeDisposable += validateTransaction()
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate { view?.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .subscribe(
                { (validated, errorMessage) ->
                    when {
                        //  Checks if second pw needed then -> onNoSecondPassword()
                        validated -> view?.showSecondPasswordDialog()
                        errorMessage == R.string.eth_support_contract_not_allowed -> view?.showEthContractSnackbar()
                        else -> view?.showSnackbar(errorMessage, Snackbar.LENGTH_LONG)
                    }
                },
                {
                    view?.showSnackbar(R.string.unexpected_error, Snackbar.LENGTH_LONG)
                    view?.finishPage()
                }
            )
    }

    /**
     * Executes transaction
     */
    override fun submitPayment() {
        submitTransaction()
    }

    override fun isAddressValid(address: String) =
        FormatsUtil.isValidEthereumAddress(address)

    @SuppressLint("CheckResult")
    private fun submitTransaction() {
        compositeDisposable += createEthTransaction()
            .flatMap {
                if (payloadDataManager.isDoubleEncrypted) {
                    payloadDataManager.decryptHDWallet(networkParameters, verifiedSecondPassword)
                }

                val ecKey = EthereumAccount.deriveECKey(
                    payloadDataManager.wallet!!.hdWallets[0].masterKey, 0
                )
                return@flatMap ethDataManager.signEthTransaction(it, ecKey)
            }
            .flatMap { ethDataManager.pushEthTx(it) }
            .flatMap { ethDataManager.setLastTxHashObservable(it, System.currentTimeMillis()) }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view?.showProgressDialog(R.string.app_name) }
            .doOnError {
                view?.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_LONG)
            }
            .doOnTerminate {
                view?.dismissProgressDialog()
                view?.dismissConfirmationDialog()
            }
            .subscribe(
                {
                    logPaymentSentEvent(true, CryptoCurrency.ETHER, pendingTransaction.bigIntAmount)
                    analytics.logEvent(SendAnalytics.SummarySendSuccess(CryptoCurrency.ETHER))
                    // handleSuccessfulPayment(...) clears PendingTransaction object
                    handleSuccessfulPayment(it)
                },
                {
                    Timber.e(it)
                    logPaymentSentEvent(false, CryptoCurrency.ETHER, pendingTransaction.bigIntAmount)
                    view?.showSnackbar(R.string.transaction_failed, Snackbar.LENGTH_LONG)
                    analytics.logEvent(SendAnalytics.SummarySendFailure(CryptoCurrency.ETHER))
                }
            )
    }

    private fun createEthTransaction(): Observable<RawTransaction> {
        val feeGwei = BigDecimal.valueOf(feeOptions!!.regularFee)
        val feeWei = Convert.toWei(feeGwei, Convert.Unit.GWEI)

        return Observables.zip(ethDataManager.fetchEthAddress()
            .map { ethDataManager.getEthResponseModel()!!.getNonce() },
            ethDataManager.getIfContract(pendingTransaction.receivingAddress))
            .map { (nonce, isContract) ->
                ethDataManager.createEthTransaction(
                    nonce = nonce,
                    to = pendingTransaction.receivingAddress,
                    gasPriceWei = feeWei.toBigInteger(),
                    gasLimitGwei = BigInteger.valueOf(
                        if (isContract) feeOptions!!.gasLimitContract
                        else feeOptions!!.gasLimit
                    ),
                    weiValue = pendingTransaction.bigIntAmount
                )
            }
    }

    private fun handleSuccessfulPayment(hash: String): String {
        view?.showTransactionSuccess(CryptoCurrency.ETHER)
        pendingTransaction.clear()
        return hash
    }

    override fun onNoSecondPassword() {
        showPaymentReview()
    }

    override fun onSecondPasswordValidated(secondPassword: String) {
        verifiedSecondPassword = secondPassword
        showPaymentReview()
    }

    private fun showPaymentReview() {
        view?.showPaymentDetails(getConfirmationDetails(), null, null, false)
    }

    private fun checkManualAddressInput() {
        val address = view?.getReceivingAddress()
        address?.let {
            // Only if valid address so we don't override with a label
            if (FormatsUtil.isValidEthereumAddress(address)) {
                pendingTransaction.receivingAddress = address
            }
        }
    }

    private fun getConfirmationDetails(): PaymentConfirmationDetails {
        val pendingTransaction = pendingTransaction

        val amount = CryptoValue.fromMinor(CryptoCurrency.ETHER, pendingTransaction.bigIntAmount)
        val fee = CryptoValue.fromMinor(CryptoCurrency.ETHER, pendingTransaction.bigIntFee)
        val total = amount + fee

        return PaymentConfirmationDetails(
            fromLabel = pendingTransaction.sendingObject!!.label,
            toLabel = pendingTransaction.displayableReceivingLabel ?: throw IllegalStateException("No receive label"),
            crypto = CryptoCurrency.ETHER,
            fiatUnit = fiatCurrency,
            cryptoAmount = amount.toStringWithoutSymbol(),
            cryptoFee = fee.toStringWithoutSymbol(),
            cryptoTotal = total.toStringWithoutSymbol(),
            fiatFee = fee.toFiat(exchangeRates, fiatCurrency).toStringWithoutSymbol(),
            fiatAmount = amount.toFiat(exchangeRates, fiatCurrency).toStringWithoutSymbol(),
            fiatTotal = total.toFiat(exchangeRates, fiatCurrency).toStringWithSymbol()
        )
    }

    override fun clearReceivingObject() {}

    override fun selectDefaultOrFirstFundedSendingAccount() {
        val accountItem = walletAccountHelper.getDefaultOrFirstFundedAccount(CryptoCurrency.ETHER) ?: return
        view?.updateSendingAddress(accountItem.label)
        pendingTransaction.sendingObject = accountItem
    }

    /**
     * Get cached dynamic fee from new Fee options endpoint
     */
    @SuppressLint("CheckResult")
    private fun getSuggestedFee() {
        compositeDisposable += feeDataManager.ethFeeOptions
            .doOnSubscribe { feeOptions = dynamicFeeCache.ethFeeOptions!! }
            .doOnNext { dynamicFeeCache.ethFeeOptions = it }
            .subscribe(
                { /* No-op */ },
                {
                    Timber.e(it)
                    view?.showSnackbar(R.string.confirm_payment_fee_sync_error, Snackbar.LENGTH_LONG)
                    view?.finishPage()
                }
            )
    }

    override fun getFeeOptions(): FeeOptions? = dynamicFeeCache.ethFeeOptions

    /**
     * Update absolute fee with smallest denomination of crypto currency (satoshi, wei, etc)
     */
    private fun updateFee(fee: BigInteger) {
        absoluteSuggestedFee = fee

        val cryptoValue = CryptoValue(CryptoCurrency.ETHER, absoluteSuggestedFee)
        view?.updateFeeAmount(cryptoValue, cryptoValue.toFiat(exchangeRates, fiatCurrency))
    }

    override fun onCryptoTextChange(cryptoText: String) {
        textChangeSubject.onNext(cryptoText)
    }

    override fun onAddressTextChange(address: String) {}

    /**
     * Calculate amounts on crypto text change
     */
    private fun setupTextChangeSubject() {
        textChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                calculateSpendableAmounts(spendAll = false, amountToSendText = it)
            }
            .emptySubscribe()
    }

    override fun onSpendMaxClicked() {
        calculateSpendableAmounts(spendAll = true, amountToSendText = null)
    }

    private fun calculateSpendableAmounts(spendAll: Boolean, amountToSendText: String?) {
        view?.setSendButtonEnabled(true)
        view?.hideMaxAvailable()
        view?.clearWarning()

        getSuggestedFee()
        getAccountResponse(spendAll, amountToSendText)
    }

    @SuppressLint("CheckResult")
    private fun getAccountResponse(spendAll: Boolean, amountToSendText: String?) {
        view?.showMaxAvailable()

        if (ethDataManager.getEthResponseModel() == null) {
            compositeDisposable += ethDataManager.fetchEthAddress()
                .doOnError { view?.showSnackbar(R.string.api_fail, Snackbar.LENGTH_INDEFINITE) }
                .subscribe { calculateUnspent(it, spendAll, amountToSendText) }
        } else {
            ethDataManager.getEthResponseModel()?.let {
                calculateUnspent(it, spendAll, amountToSendText)
            }
        }
    }

    private fun calculateUnspent(combinedEthModel: CombinedEthModel, spendAll: Boolean, amountToSendText: String?) {

        val amountToSendSanitised = if (amountToSendText.isNullOrEmpty()) "0" else amountToSendText

        val gwei = BigDecimal.valueOf(feeOptions!!.gasLimit * feeOptions!!.regularFee)
        val wei = Convert.toWei(gwei, Convert.Unit.GWEI)

        updateFee(wei.toBigInteger())
        pendingTransaction.bigIntFee = wei.toBigInteger()

        val addressResponse = combinedEthModel.getAddressResponse()
        maxAvailable = addressResponse!!.balance.minus(wei.toBigInteger())
        maxAvailable = maxAvailable.max(BigInteger.ZERO)

        val availableEth = Convert.fromWei(maxAvailable.toString(), Convert.Unit.ETHER)
        val cryptoValue = CryptoValue.fromMajor(CryptoCurrency.ETHER, availableEth ?: BigDecimal.ZERO)

        if (spendAll) {
            view?.updateCryptoAmount(cryptoValue)
            pendingTransaction.bigIntAmount = availableEth.toBigInteger()
        } else {
            pendingTransaction.bigIntAmount = getWeiFromText(amountToSendSanitised, getDefaultDecimalSeparator())
        }

        // Format for display
        view?.updateMaxAvailable("${stringUtils.getString(R.string.max_available)} ${cryptoValue.toStringWithSymbol()}")

        // No dust in Ethereum
        if (maxAvailable <= BigInteger.ZERO) {
            view?.updateMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view?.updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            view?.updateMaxAvailableColor(R.color.primary_blue_accent)
        }

        // Check if any pending ether txs exist and warn user
        compositeDisposable += isLastTxPending()
            .subscribeBy(
                onSuccess = { /* No-op */ },
                onError = { Timber.e(it) }
            )
    }

    override fun handlePrivxScan(scanData: String?) {}

    @SuppressLint("CheckResult")
    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {
    }

    private fun isValidAmount(bAmount: BigInteger?): Boolean {
        return (bAmount != null && bAmount > BigInteger.ZERO)
    }

    private fun validateTransaction(): Observable<Pair<Boolean, Int>> {
        return if (pendingTransaction.receivingAddress.isEmpty()) {
            Observable.just(Pair(false, R.string.eth_invalid_address))
        } else {
            var validated = true
            var errorMessage = R.string.unexpected_error
            if (!FormatsUtil.isValidEthereumAddress(pendingTransaction.receivingAddress)) {
                errorMessage = R.string.eth_invalid_address
                validated = false
            }

            // Validate amount
            if (!isValidAmount(pendingTransaction.bigIntAmount)) {
                errorMessage = R.string.invalid_amount
                validated = false
            }

            // Validate sufficient funds
            if (maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
                errorMessage = R.string.insufficient_funds
                validated = false
            }
            Observable.just(Pair(validated, errorMessage))
        }.flatMap { errorPair ->
            if (errorPair.first) {
                // Validate address does not have unconfirmed funds
                isLastTxPending().toObservable()
            } else {
                Observable.just(errorPair)
            }
        }
    }

    private fun isLastTxPending() =
        ethDataManager.isLastTxPending()
            .observeOn(AndroidSchedulers.mainThread())
            .map { hasUnconfirmed: Boolean ->

                if (hasUnconfirmed) {
                    view?.disableInput()
                    view?.updateMaxAvailable(stringUtils.getString(R.string.eth_unconfirmed_wait))
                    view?.updateMaxAvailableColor(R.color.product_red_medium)
                } else {
                    view?.enableInput()
                }

                val errorMessage = R.string.eth_unconfirmed_wait
                Pair(!hasUnconfirmed, errorMessage)
            }
}

private fun getWeiFromText(text: String?, decimalSeparator: String): BigInteger {
    if (text == null || text.isEmpty()) return BigInteger.ZERO

    val amountToSend = stripSeparator(text, decimalSeparator)
    return Convert.toWei(amountToSend, Convert.Unit.ETHER).toBigInteger()
}

private fun stripSeparator(text: String, decimalSeparator: String) =
    text.trim { it <= ' ' }
        .replace(" ", "")
        .replace(decimalSeparator, ".")