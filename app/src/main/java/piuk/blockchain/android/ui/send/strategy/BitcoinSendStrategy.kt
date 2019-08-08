package piuk.blockchain.android.ui.send.strategy

import android.annotation.SuppressLint
import android.support.design.widget.Snackbar
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.State
import com.blockchain.nabu.NabuToken
import com.blockchain.remoteconfig.CoinSelectionRemoteConfig
import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PrivateKeyFactory
import info.blockchain.wallet.util.Tools
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.ECKey
import piuk.blockchain.android.R
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.account.PitAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.send.FeeType
import piuk.blockchain.android.ui.send.PendingTransaction
import piuk.blockchain.android.ui.send.SendModel
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.FiatExchangeRates
import piuk.blockchain.androidcore.data.exchangerate.toFiat
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.util.HashMap
import java.util.concurrent.TimeUnit

class BitcoinSendStrategy(
    private val walletAccountHelper: WalletAccountHelper,
    private val payloadDataManager: PayloadDataManager,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val stringUtils: StringUtils,
    private val sendDataManager: SendDataManager,
    private val dynamicFeeCache: DynamicFeeCache,
    private val feeDataManager: FeeDataManager,
    private val privateKeyFactory: PrivateKeyFactory,
    private val environmentSettings: EnvironmentConfig,
    private val currencyFormatter: CurrencyFormatManager,
    private val exchangeRates: FiatExchangeRates,
    private val prefs: PersistentPrefs,
    private val pitLinking: PitLinking,
    private val coinSelectionRemoteConfig: CoinSelectionRemoteConfig,
    private val nabuDataManager: NabuDataManager,
    private val nabuToken: NabuToken,
    currencyState: CurrencyState
) : SendStrategy<SendView>(currencyState) {

    private var pitAccount: PitAccount? = null

    override fun onPitAddressSelected() {
        pitAccount?.let {
            pendingTransaction.receivingObject = ItemAccount(
                it.label,
                null,
                null,
                null,
                null,
                it.address
            )
            pendingTransaction.receivingAddress = it.address
            view.updateReceivingAddress(it.label)
        }
    }

    override fun onPitAddressCleared() {
        pendingTransaction.receivingObject = null
        view.updateReceivingAddress("")
    }

    override fun onCurrencySelected() {
        currencyState.cryptoCurrency = CryptoCurrency.BTC
        onBitcoinChosen()
    }

    override fun selectSendingAccount(account: JsonSerializableAccount?) {
        when (account) {
            null -> selectDefaultOrFirstFundedSendingAccount()
            is LegacyAddress -> onSendingBtcLegacyAddressSelected(account)
            is Account -> onSendingBtcAccountSelected(account)
            else -> throw IllegalArgumentException("No method for handling ${account.javaClass.simpleName} available")
        }
    }

    override fun selectReceivingAccount(account: JsonSerializableAccount?) {
        when (account) {
            is LegacyAddress -> onReceivingBtcLegacyAddressSelected(account)
            is Account -> onReceivingBtcAccountSelected(account)
            else -> throw IllegalArgumentException("No method for handling ${account?.javaClass?.simpleName} available")
        }
    }

    private val pendingTransaction by unsafeLazy { PendingTransaction() }
    private val unspentApiResponsesBtc by unsafeLazy { HashMap<String, UnspentOutputs>() }

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

    override fun processURIScanAddress(address: String) {
        pendingTransaction.receivingObject = null
        pendingTransaction.receivingAddress = address
        view.updateReceivingAddress(address)
    }

    override fun onViewReady() {
        resetAccountList()
        setupTextChangeSubject()
    }

    override fun onResume() {
        onBitcoinChosen()
    }

    private fun onBitcoinChosen() {
        view?.let {
            reset()
            it.showFeePriority()
            it.enableFeeDropdown()
            it.setCryptoMaxLength(17)
            calculateSpendableAmounts(spendAll = false, amountToSendText = "0")
            it.enableInput()
        }
    }

    override fun reset() {
        super.reset()
        pendingTransaction.clear()
        view?.let {
            absoluteSuggestedFee = BigInteger.ZERO
            resetAccountList()
            selectDefaultOrFirstFundedSendingAccount()
        }
    }

    @SuppressLint("CheckResult")
    override fun onContinueClicked() {
        view?.showProgressDialog(R.string.app_name)

        checkManualAddressInput()

        Observable.just(validateBitcoinTransaction())
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate { view?.dismissProgressDialog() }
            .addToCompositeDisposable(this)
            .subscribe({ (validated, errorMessage) ->
                if (validated) {
                    if (pendingTransaction.isWatchOnly) {
                        // returns to spendFromWatchOnly*BIP38 -> showPaymentReview()
                        val address = pendingTransaction.sendingObject!!.accountObject as LegacyAddress
                        view.showSpendFromWatchOnlyWarning((address).address)
                    } else if (pendingTransaction.isWatchOnly && verifiedSecondPassword != null) {
                        // Second password already verified
                        showPaymentReview()
                    } else {
                        // Checks if second pw needed then -> onNoSecondPassword()
                        view.showSecondPasswordDialog()
                    }
                } else {
                    view.showSnackbar(errorMessage, Snackbar.LENGTH_LONG)
                }
            }, { Timber.e(it) })
    }

    /**
     * Executes transaction
     */
    override fun submitPayment() {
        submitBitcoinTransaction()
    }

    @SuppressLint("CheckResult")
    private fun submitBitcoinTransaction() {
        view.showProgressDialog(R.string.app_name)

        getBtcChangeAddress()!!
            .addToCompositeDisposable(this)
            .doOnError {
                view.dismissProgressDialog()
                view.dismissConfirmationDialog()
                view.showTransactionFailed()
            }
            .map { pendingTransaction.changeAddress = it }
            .flatMap { getBtcKeys() }
            .flatMap {
                sendDataManager.submitBtcPayment(
                    pendingTransaction.unspentOutputBundle!!,
                    it,
                    pendingTransaction.receivingAddress,
                    pendingTransaction.changeAddress,
                    pendingTransaction.bigIntFee,
                    pendingTransaction.bigIntAmount
                )
            }
            .subscribe(
                { hash ->
                    logPaymentSentEvent(true, CryptoCurrency.BTC, pendingTransaction.bigIntAmount)

                    clearBtcUnspentResponseCache()
                    view.dismissProgressDialog()
                    view.dismissConfirmationDialog()
                    incrementBtcReceiveAddress()
                    handleSuccessfulPayment(hash, CryptoCurrency.BTC)
                },
                {
                    Timber.e(it)
                    view.dismissProgressDialog()
                    view.dismissConfirmationDialog()
                    view.showSnackbar(
                        stringUtils.getString(R.string.transaction_failed),
                        it.message,
                        Snackbar.LENGTH_INDEFINITE
                    )

                    logPaymentSentEvent(false, CryptoCurrency.BTC, pendingTransaction.bigIntAmount)
                }
            )
    }

    private fun getBtcKeys(): Observable<List<ECKey>> {
        return if (pendingTransaction.isHD(CryptoCurrency.BTC)) {
            val account = pendingTransaction.sendingObject!!.accountObject as Account

            if (payloadDataManager.isDoubleEncrypted) {
                payloadDataManager.decryptHDWallet(verifiedSecondPassword)
            }
            Observable.just(payloadDataManager.getHDKeysForSigning(account, pendingTransaction.unspentOutputBundle!!))
        } else {
            val legacyAddress = pendingTransaction.senderAsLegacyAddress

            if (legacyAddress.tag == PendingTransaction.WATCH_ONLY_SPEND_TAG) {
                val ecKey = Tools.getECKeyFromKeyAndAddress(legacyAddress.privateKey, legacyAddress.address)
                Observable.just(listOf(ecKey))
            } else {
                Observable.just(listOf(payloadDataManager.getAddressECKey(legacyAddress, verifiedSecondPassword)!!))
            }
        }
    }

    private fun getBtcChangeAddress(): Observable<String>? {
        return if (pendingTransaction.isHD(CryptoCurrency.BTC)) {
            val account = pendingTransaction.sendingObject!!.accountObject as Account
            payloadDataManager.getNextChangeAddress(account)
        } else {
            val legacyAddress = pendingTransaction.senderAsLegacyAddress
            Observable.just(legacyAddress.address)
        }
    }

    private fun clearBtcUnspentResponseCache() {
        if (pendingTransaction.isHD(CryptoCurrency.BTC)) {
            val account = pendingTransaction.sendingObject!!.accountObject as Account
            unspentApiResponsesBtc.remove(account.xpub)
        } else {
            val legacyAddress = pendingTransaction.senderAsLegacyAddress
            unspentApiResponsesBtc.remove(legacyAddress.address)
        }
    }

    private fun incrementBtcReceiveAddress() {
        if (pendingTransaction.isHD(CryptoCurrency.BTC)) {
            val account = pendingTransaction.sendingObject!!.accountObject as Account
            payloadDataManager.incrementChangeAddress(account)
            payloadDataManager.incrementReceiveAddress(account)
            updateInternalBtcBalances()
        }
    }

    private fun handleSuccessfulPayment(hash: String, cryptoCurrency: CryptoCurrency): String {
        view?.showTransactionSuccess(cryptoCurrency)

        pendingTransaction.clear()
        unspentApiResponsesBtc.clear()

        return hash
    }

    /**
     * Update balance immediately after spend - until refresh from server
     */
    private fun updateInternalBtcBalances() {
        try {
            val totalSent = pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee)
            if (pendingTransaction.isHD(CryptoCurrency.BTC)) {
                val account = pendingTransaction.sendingObject?.accountObject as Account
                payloadDataManager.subtractAmountFromAddressBalance(
                    account.xpub,
                    totalSent.toLong()
                )
            } else {
                val address = pendingTransaction.senderAsLegacyAddress
                payloadDataManager.subtractAmountFromAddressBalance(
                    address.address,
                    totalSent.toLong()
                )
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun onNoSecondPassword() {
        showPaymentReview()
    }

    override fun onSecondPasswordValidated(secondPassword: String) {
        verifiedSecondPassword = secondPassword
        showPaymentReview()
    }

    private fun showPaymentReview() {
        val paymentDetails = getConfirmationDetails()

        if (paymentDetails.isLargeTransaction) {
            view.showLargeTransactionWarning()
        }
        view.showPaymentDetails(getConfirmationDetails(), null, null, true)
    }

    private fun checkManualAddressInput() {
        val address = view.getReceivingAddress()
        address?.let {
            // Only if valid address so we don't override with a label
            if (FormatsUtil.isValidBitcoinAddress(address)) {
                pendingTransaction.receivingAddress = address
            }
        }
    }

    private fun getConfirmationDetails(): PaymentConfirmationDetails {
        val pendingTransaction = pendingTransaction

        val details = PaymentConfirmationDetails()

        details.fromLabel = pendingTransaction.sendingObject?.label ?: ""
        details.toLabel = pendingTransaction.displayableReceivingLabel?.removeBchUri() ?: ""

        details.cryptoUnit = CryptoCurrency.BTC.symbol
        details.fiatUnit = prefs.selectedFiatCurrency
        details.fiatSymbol = currencyFormatter.getFiatSymbol(
            currencyFormatter.fiatCountryCode
        )

        details.isLargeTransaction = isLargeTransaction()
        details.btcSuggestedFee = currencyFormatter.getTextFromSatoshis(
            absoluteSuggestedFee,
            getDefaultDecimalSeparator()
        )

        details.cryptoTotal = currencyFormatter.getTextFromSatoshis(
            pendingTransaction.total,
            getDefaultDecimalSeparator()
        )
        details.cryptoAmount = currencyFormatter.getTextFromSatoshis(
            pendingTransaction.bigIntAmount,
            getDefaultDecimalSeparator()
        )
        details.cryptoFee = currencyFormatter.getTextFromSatoshis(
            pendingTransaction.bigIntFee,
            getDefaultDecimalSeparator()
        )

        details.fiatFee = currencyFormatter.getFormattedFiatValueFromSelectedCoinValue(
            pendingTransaction.bigIntFee.toBigDecimal()
        )
        details.fiatAmount =
            currencyFormatter.getFormattedFiatValueFromSelectedCoinValue(
                pendingTransaction.bigIntAmount.toBigDecimal()
            )
        details.fiatTotal =
            currencyFormatter.getFormattedFiatValueFromSelectedCoinValue(
                pendingTransaction.total.toBigDecimal()
            )

        return details
    }

    private fun resetAccountList() {
        compositeDisposable += pitLinking.isPitLinked().filter { it }.flatMapSingle {
            nabuToken.fetchNabuToken()
        }.flatMap {
            nabuDataManager.fetchCryptoAddressFromThePit(it, CryptoCurrency.BTC.symbol)
        }.applySchedulers().doOnSubscribe {
            view.updateReceivingHintAndAccountDropDowns(
                CryptoCurrency.BTC,
                getAddressList().size,
                false
            )
        }.subscribeBy(onError = {
            view.updateReceivingHintAndAccountDropDowns(
                CryptoCurrency.BTC,
                getAddressList().size,
                false
            )
        }) {
            pitAccount = PitAccount(stringUtils.getFormattedString(R.string.pit_default_account_label,
                CryptoCurrency.BTC.symbol), it.address)
            view.updateReceivingHintAndAccountDropDowns(
                CryptoCurrency.BTC,
                getAddressList().size,
                it.state == State.ACTIVE && it.address.isNotEmpty()
            )
        }
    }

    override fun clearReceivingObject() {
        pendingTransaction.receivingObject = null
    }

    private fun clearCryptoAmount() {
        view.updateCryptoAmount(CryptoValue.zero(CryptoCurrency.BTC))
    }

    private fun getAddressList(): List<ItemAccount> = walletAccountHelper.getAccountItems(CryptoCurrency.BTC)

    override fun selectDefaultOrFirstFundedSendingAccount() {
        val accountItem = walletAccountHelper.getDefaultOrFirstFundedAccount() ?: return
        view.updateSendingAddress(accountItem.label ?: accountItem.address!!)
        pendingTransaction.sendingObject = accountItem
    }

    /**
     * Get cached dynamic fee from new Fee options endpoint
     */
    @SuppressLint("CheckResult")
    private fun getSuggestedFee() {
        val observable = feeDataManager.btcFeeOptions
            .doOnSubscribe { feeOptions = dynamicFeeCache.btcFeeOptions!! }
            .doOnNext { dynamicFeeCache.btcFeeOptions = it }

        observable.addToCompositeDisposable(this)
            .subscribe(
                { /* No-op */ },
                {
                    Timber.e(it)
                    view.showSnackbar(R.string.confirm_payment_fee_sync_error, Snackbar.LENGTH_LONG)
                    view.finishPage()
                }
            )
    }

    override fun getFeeOptions(): FeeOptions? = dynamicFeeCache.btcFeeOptions

    private fun getFeePerKbFromPriority(@FeeType.FeePriorityDef feePriorityTemp: Int): BigInteger {
        getSuggestedFee()

        if (feeOptions == null) {
            // This is a stopgap in case of failure to prevent crashes.
            return BigInteger.ZERO
        }

        return when (feePriorityTemp) {
            FeeType.FEE_OPTION_CUSTOM -> BigInteger.valueOf(view.getCustomFeeValue() * 1000)
            FeeType.FEE_OPTION_PRIORITY -> BigInteger.valueOf(feeOptions!!.priorityFee * 1000)
            FeeType.FEE_OPTION_REGULAR -> BigInteger.valueOf(feeOptions!!.regularFee * 1000)
            else -> BigInteger.valueOf(feeOptions!!.regularFee * 1000)
        }
    }

    /**
     * Retrieves unspent api data in memory. If not in memory yet, it will be retrieved and added.
     */
    private fun getUnspentApiResponse(address: String): Observable<UnspentOutputs> {

        return if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            return if (unspentApiResponsesBtc.containsKey(address)) {
                Observable.just(unspentApiResponsesBtc[address])
            } else {
                sendDataManager.getUnspentBtcOutputs(address)
            }
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    @Throws(UnsupportedEncodingException::class)
    private fun getSuggestedAbsoluteFee(
        coins: UnspentOutputs,
        amountToSend: CryptoValue,
        feePerKb: BigInteger,
        useNewCoinSelection: Boolean
    ): BigInteger {
        val spendableCoins = sendDataManager.getSpendableCoins(coins, amountToSend, feePerKb, useNewCoinSelection)
        return spendableCoins.absoluteFee
    }

    /**
     * Update absolute fee with smallest denomination of crypto currency (satoshi, wei, etc)
     */
    private fun updateFee(fee: BigInteger) {
        absoluteSuggestedFee = fee

        val cryptoValue = CryptoValue(CryptoCurrency.BTC, absoluteSuggestedFee)
        view.updateFeeAmount(cryptoValue, cryptoValue.toFiat(exchangeRates))
    }

    private fun updateMaxAvailable(balanceAfterFee: BigInteger) {
        maxAvailable = balanceAfterFee
        view.showMaxAvailable()

        // Format for display
        view.updateMaxAvailable(
            stringUtils.getString(R.string.max_available) +
                    " ${currencyFormatter.getFormattedSelectedCoinValueWithUnit(maxAvailable)}"
        )

        if (balanceAfterFee <= Payment.DUST) {
            view.updateMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view.updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            view.updateMaxAvailableColor(R.color.primary_blue_accent)
        }
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
        view.setSendButtonEnabled(true)
        view.hideMaxAvailable()
        view.clearWarning()

        val feePerKb = getFeePerKbFromPriority(view.getFeePriority())
        calculateUnspentBtc(spendAll, amountToSendText, feePerKb)
    }

    private fun onReceivingBtcLegacyAddressSelected(legacyAddress: LegacyAddress) {
        var label = legacyAddress.label
        if (label.isNullOrEmpty()) {
            label = legacyAddress.address
        }

        pendingTransaction.receivingObject = ItemAccount(
            label,
            null,
            null,
            null,
            legacyAddress,
            legacyAddress.address
        )
        pendingTransaction.receivingAddress = legacyAddress.address

        view.updateReceivingAddress(label)

        if (legacyAddress.isWatchOnly && shouldWarnWatchOnly()) {
            view.showWatchOnlyWarning(legacyAddress.address)
        }
    }

    private fun shouldWarnWatchOnly(): Boolean =
        prefs.getValue(PersistentPrefs.KEY_WARN_WATCH_ONLY_SPEND, true)

    @SuppressLint("CheckResult")
    private fun onReceivingBtcAccountSelected(account: Account) {
        var label = account.label
        if (label.isNullOrEmpty()) {
            label = account.xpub
        }

        pendingTransaction.receivingObject = ItemAccount(
            label,
            null,
            null,
            null,
            account,
            account.xpub
        )

        view.updateReceivingAddress(label)

        payloadDataManager.getNextReceiveAddress(account)
            .doOnNext { pendingTransaction.receivingAddress = it }
            .addToCompositeDisposable(this)
            .subscribe(
                { /* No-op */ },
                { view.showSnackbar(R.string.unexpected_error, Snackbar.LENGTH_LONG) }
            )
    }

    @SuppressLint("CheckResult")
    private fun calculateUnspentBtc(
        spendAll: Boolean,
        amountToSendText: String?,
        feePerKb: BigInteger
    ) {
        val sendingObj = pendingTransaction.sendingObject

        if (sendingObj?.address == null) {
            // This shouldn't happen, but handle case anyway in case of low memory scenario onBitcoinCashChosen()
            return
        }

        val address = sendingObj.address!!

        Observables.zip(
            getUnspentApiResponse(address),
            coinSelectionRemoteConfig.enabled.toObservable()
        )
            .debounce(200, TimeUnit.MILLISECONDS)
            .applySchedulers()
            .subscribe(
                { (coins, newCoinSelectionEnabled) ->
                    val amountToSend = currencyFormatter.getSatoshisFromText(
                        amountToSendText,
                        getDefaultDecimalSeparator()
                    )

                    // Future use. There might be some unconfirmed funds. Not displaying a warning currently (to line up with iOS and Web wallet)
                    if (coins.notice != null) {
                        view.updateWarning(coins.notice)
                    } else {
                        view.clearWarning()
                    }

                    updateFee(getSuggestedAbsoluteFee(
                        coins,
                        CryptoValue.bitcoinFromSatoshis(amountToSend),
                        feePerKb,
                        newCoinSelectionEnabled
                    ))

                    suggestedFeePayment(
                        coins,
                        CryptoValue.bitcoinFromSatoshis(amountToSend),
                        spendAll,
                        feePerKb,
                        newCoinSelectionEnabled
                    )
                },
                { throwable ->
                    Timber.e(throwable)
                    // No unspent outputs
                    updateMaxAvailable(BigInteger.ZERO)
                    updateFee(BigInteger.ZERO)
                    pendingTransaction.unspentOutputBundle = null
                }
            )
    }

    /**
     * Payment will use suggested dynamic fee
     */
    @Throws(UnsupportedEncodingException::class)
    private fun suggestedFeePayment(
        coins: UnspentOutputs,
        amountToSend: CryptoValue,
        spendAll: Boolean,
        feePerKb: BigInteger,
        useNewCoinSelection: Boolean
    ) {
        var amount = amountToSend.amount

        // Calculate sweepable amount to display max available
        val sweepBundle = sendDataManager.getMaximumAvailable(
            amountToSend.currency,
            coins,
            feePerKb,
            useNewCoinSelection
        )
        val sweepableAmount = sweepBundle.left

        updateMaxAvailable(sweepableAmount)

        if (spendAll) {
            amount = sweepableAmount
            view?.updateCryptoAmount(CryptoValue(CryptoCurrency.BTC, sweepableAmount))
        }

        val unspentOutputBundle = sendDataManager.getSpendableCoins(
            coins,
            amountToSend,
            feePerKb,
            useNewCoinSelection
        )

        pendingTransaction.bigIntAmount = amount
        pendingTransaction.unspentOutputBundle = unspentOutputBundle
        pendingTransaction.bigIntFee = pendingTransaction.unspentOutputBundle!!.absoluteFee
    }

    override fun handlePrivxScan(scanData: String?) {
        if (scanData == null) return

        val format = privateKeyFactory.getFormat(scanData)

        if (format == null) {
            view?.showSnackbar(R.string.privkey_error, Snackbar.LENGTH_LONG)
            return
        }

        when (format) {
            PrivateKeyFactory.BIP38 -> view?.showBIP38PassphrasePrompt(scanData) // BIP38 needs passphrase
            else -> spendFromWatchOnlyNonBIP38(format, scanData)
        }
    }

    private fun spendFromWatchOnlyNonBIP38(format: String, scanData: String) {
        try {
            val key = privateKeyFactory.getKey(format, scanData)
            val legacyAddress = pendingTransaction.senderAsLegacyAddress
            setTempLegacyAddressPrivateKey(legacyAddress, key)
        } catch (e: Exception) {
            view?.showSnackbar(R.string.no_private_key, Snackbar.LENGTH_LONG)
            Timber.e(e)
        }
    }

    @SuppressLint("CheckResult")
    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {
        sendDataManager.getEcKeyFromBip38(
            pw,
            scanData,
            environmentSettings.bitcoinNetworkParameters
        ).addToCompositeDisposable(this)
            .subscribe(
                {
                    val legacyAddress = pendingTransaction.senderAsLegacyAddress
                    setTempLegacyAddressPrivateKey(legacyAddress, it)
                },
                { view?.showSnackbar(R.string.bip38_error, Snackbar.LENGTH_LONG) }
            )
    }

    private fun setTempLegacyAddressPrivateKey(legacyAddress: LegacyAddress, key: ECKey?) {
        if (key != null && key.hasPrivKey() && legacyAddress.address == key.toAddress(
                environmentSettings.bitcoinNetworkParameters
            ).toString()
        ) {

            // Create copy, otherwise pass by ref will override private key in wallet payload
            val tempLegacyAddress = LegacyAddress()
            tempLegacyAddress.setPrivateKeyFromBytes(key.privKeyBytes)
            tempLegacyAddress.address = key.toAddress(environmentSettings.bitcoinNetworkParameters).toString()
            tempLegacyAddress.label = legacyAddress.label
            tempLegacyAddress.tag = PendingTransaction.WATCH_ONLY_SPEND_TAG
            pendingTransaction.sendingObject!!.accountObject = tempLegacyAddress

            showPaymentReview()
        } else {
            view?.showSnackbar(R.string.invalid_private_key, Snackbar.LENGTH_LONG)
        }
    }

    private fun onSendingBtcLegacyAddressSelected(legacyAddress: LegacyAddress) {
        var label = legacyAddress.label
        if (label.isNullOrEmpty()) {
            label = legacyAddress.address
        }

        pendingTransaction.sendingObject = ItemAccount(
            label,
            null,
            null,
            null,
            legacyAddress,
            legacyAddress.address
        )

        view.updateSendingAddress(label)
        calculateSpendableAmounts(false, "0")
    }

    private fun onSendingBtcAccountSelected(account: Account) {
        var label = account.label
        if (label.isNullOrEmpty()) {
            label = account.xpub
        }

        pendingTransaction.sendingObject = ItemAccount(
            label,
            null,
            null,
            null,
            account,
            account.xpub
        )

        view.updateSendingAddress(label)
        calculateSpendableAmounts(false, "0")
    }

    private fun String.removeBchUri(): String = this.replace("bitcoincash:", "")

//    private fun selectSendingAccountBtc(data: Intent?) {
//        try {
//            val type: Class<*> =
//                Class.forName(data?.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_OBJECT_TYPE))
//            val any = ObjectMapper().readValue(
//                data!!.getStringExtra(AccountChooserActivity.EXTRA_SELECTED_ITEM),
//                type
//            )
//
//            when (any) {
//                is LegacyAddress -> onSendingBtcLegacyAddressSelected(any)
//                is Account -> onSendingBtcAccountSelected(any)
//                else -> throw IllegalArgumentException("No method for handling $type available")
//            }
//        } catch (e: ClassNotFoundException) {
//            Timber.e(e)
//            selectDefaultOrFirstFundedSendingAccount()
//        } catch (e: IOException) {
//            Timber.e(e)
//            selectDefaultOrFirstFundedSendingAccount()
//        }
//    }

    private fun isValidBitcoinAmount(bAmount: BigInteger?): Boolean {
        if (bAmount == null) {
            return false
        }

        // Test that amount is more than dust
        if (bAmount.compareTo(Payment.DUST) == -1) {
            return false
        }

        // Test that amount does not exceed btc limit
        if (bAmount.compareTo(BigInteger.valueOf(2_100_000_000_000_000L)) == 1) {
            clearCryptoAmount()
            return false
        }

        // Test that amount is not zero
        return bAmount >= BigInteger.ZERO
    }

    private fun validateBitcoinTransaction(): Pair<Boolean, Int> {
        var validated = true
        var errorMessage = R.string.unexpected_error

        if (!FormatsUtil.isValidBitcoinAddress(pendingTransaction.receivingAddress)
        ) {
            errorMessage = R.string.invalid_bitcoin_address
            validated = false
        } else if (!isValidBitcoinAmount(pendingTransaction.bigIntAmount)) {
            errorMessage = R.string.invalid_amount
            validated = false
        } else if (pendingTransaction.unspentOutputBundle == null ||
            pendingTransaction.unspentOutputBundle!!.spendableOutputs == null
        ) {
            errorMessage = R.string.no_confirmed_funds
            validated = false
        } else if (maxAvailable == null || maxAvailable.compareTo(pendingTransaction.bigIntAmount) == -1) {
            errorMessage = R.string.insufficient_funds
            validated = false
        } else if (pendingTransaction.unspentOutputBundle!!.spendableOutputs.isEmpty()) {
            errorMessage = R.string.insufficient_funds
            validated = false
        }

        return Pair.of(validated, errorMessage)
    }

    /**
     * Returns true if bitcoin transaction is large by checking against 3 criteria:
     *
     * If the fee > $0.50
     * If the Tx size is over 1kB
     * If the ratio of fee/amount is over 1%
     */
    private fun isLargeTransaction(): Boolean {
        val usdValue = CryptoValue(CryptoCurrency.BTC, absoluteSuggestedFee)
            .toFiat(exchangeRateFactory, "USD")
        val txSize = sendDataManager.estimateSize(
            pendingTransaction.unspentOutputBundle!!.spendableOutputs.size,
            2
        ) // assume change
        val relativeFee = absoluteSuggestedFee.toDouble() / pendingTransaction.bigIntAmount.toDouble() * 100.0

        return usdValue.toBigDecimal() > SendModel.LARGE_TX_FEE.toBigDecimal() &&
                txSize > SendModel.LARGE_TX_SIZE &&
                relativeFee > SendModel.LARGE_TX_PERCENTAGE
    }
}
