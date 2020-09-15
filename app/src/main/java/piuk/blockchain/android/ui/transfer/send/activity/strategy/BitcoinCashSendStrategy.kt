package piuk.blockchain.android.ui.transfer.send.activity.strategy

import android.annotation.SuppressLint
import com.blockchain.annotations.CommonCode
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.SendAnalytics
import com.blockchain.remoteconfig.CoinSelectionRemoteConfig
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.swap.nabu.NabuToken
import com.blockchain.swap.nabu.datamanagers.NabuDataManager
import com.blockchain.swap.nabu.models.nabu.NabuApiException
import com.blockchain.swap.nabu.models.nabu.NabuErrorCodes
import com.blockchain.swap.nabu.models.nabu.State
import com.google.android.material.snackbar.Snackbar
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.exceptions.HDWalletException
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
import org.bitcoinj.core.Address
import org.bitcoinj.core.ECKey
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.bch.BchCryptoWalletAccount
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.android.ui.chooser.WalletAccountHelper
import piuk.blockchain.android.ui.transfer.send.activity.FeeType
import piuk.blockchain.android.ui.transfer.send.activity.PendingTransaction
import piuk.blockchain.android.ui.transfer.send.activity.SendView
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.math.BigDecimal
import java.math.BigInteger
import java.util.HashMap
import java.util.concurrent.TimeUnit

class BitcoinCashSendStrategy(
    private val walletAccountHelper: WalletAccountHelper,
    private val payloadDataManager: PayloadDataManager,
    private val stringUtils: StringUtils,
    private val sendDataManager: SendDataManager,
    private val dynamicFeeCache: DynamicFeeCache,
    private val feeDataManager: FeeDataManager,
    private val privateKeyFactory: PrivateKeyFactory,
    private val environmentSettings: EnvironmentConfig,
    private val bchDataManager: BchDataManager,
    private val exchangeRates: ExchangeRateDataManager,
    private val coinSelectionRemoteConfig: CoinSelectionRemoteConfig,
    private val nabuDataManager: NabuDataManager,
    private val nabuToken: NabuToken,
    private val envSettings: EnvironmentConfig,
    private val pitLinking: PitLinking,
    private val analytics: Analytics,
    private val prefs: PersistentPrefs,
    environmentConfig: EnvironmentConfig
) : SendStrategy<SendView>(prefs) {

    override fun onViewAttached() { }
    override fun onViewDetached() { }

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
        view?.let {
            reset()
            it.hideFeePriority()
            it.setFeePrioritySelection(0)
            it.disableFeeDropdown()
            it.setCryptoMaxLength(17)
            calculateSpendableAmounts(spendAll = false, amountToSendText = "0")
            it.enableInput()
        }
    }

    override fun selectReceivingAccount(account: JsonSerializableAccount?) {
        when (account) {
            is LegacyAddress -> onReceivingBchLegacyAddressSelected(account)
            is GenericMetadataAccount -> onReceivingBchAccountSelected(account)
            else ->
                throw IllegalArgumentException("No method for handling ${account?.javaClass?.simpleName} available")
        }
    }

    private val pendingTransaction by unsafeLazy { PendingTransaction() }
    private val unspentApiResponsesBch by unsafeLazy { HashMap<String, UnspentOutputs>() }
    private val networkParameters = environmentConfig.bitcoinNetworkParameters

    private var feeOptions: FeeOptions? = null
    private var textChangeSubject = PublishSubject.create<String>()
    private var absoluteSuggestedFee = BigInteger.ZERO
    private var maxAvailable = CryptoValue.ZeroBch
    private var verifiedSecondPassword: String? = null

    /**
     * External changes.
     * Possible currency change, Account/address archive, Balance change
     */
    override fun onBroadcastReceived() {
        resetAccountList()
    }

    override fun isAddressValid(address: String) =
        FormatsUtil.isValidBCHAddress(envSettings.bitcoinNetworkParameters, address)

    override fun onViewReady() {
        resetAccountList()
        setupTextChangeSubject()
    }

    override fun onResume() {
    }

    override fun reset() {
        super.reset()
        pendingTransaction.clear()
        view?.let {
            absoluteSuggestedFee = BigInteger.ZERO
            resetAccountList()
        }
    }

    @SuppressLint("CheckResult")
    override fun onContinueClicked() {
        view?.showProgressDialog(R.string.app_name)

        checkManualAddressInput()

        compositeDisposable += isValidBitcoincashAddress()
            .map {
                if (!it) {
                    // Warn user if address is in base58 format since this might be a btc address
                    pendingTransaction.warningText = stringUtils.getString(R.string.bch_address_warning)
                    pendingTransaction.warningSubText = stringUtils.getString(R.string.bch_address_warning_subtext)
                }
            }
            .flatMap { Observable.just(validateBitcoinCashTransaction()) }
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate { view?.dismissProgressDialog() }
            .subscribe(
                { (validated, errorMessage) ->
                    if (validated) {
                        view?.showSecondPasswordDialogIfRequired()
                    } else {
                        view?.showSnackbar(errorMessage, Snackbar.LENGTH_LONG)
                    }
                },
                { Timber.e(it) }
            )
    }

    /**
     * Executes transaction
     */
    override fun submitPayment() {
        submitTransaction()
    }

    @SuppressLint("CheckResult")
    private fun submitTransaction() {
        view?.showProgressDialog(R.string.app_name)

        pendingTransaction.receivingAddress =
            getFullBitcoinCashAddressFormat(pendingTransaction.receivingAddress)

        compositeDisposable += getBchChangeAddress()!!
            .doOnError {
                view?.dismissProgressDialog()
                view?.dismissConfirmationDialog()
                view?.showTransactionFailed()
            }
            .map { pendingTransaction.changeAddress = it }
            .flatMap { getBchKeys() }
            .flatMap {
                sendDataManager.submitBchPayment(
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
                    logPaymentSentEvent(true, CryptoCurrency.BCH, pendingTransaction.bigIntAmount)

                    clearBchUnspentResponseCache()
                    view?.dismissProgressDialog()
                    view?.dismissConfirmationDialog()
                    incrementBchReceiveAddress()
                    analytics.logEvent(SendAnalytics.SummarySendSuccess(CryptoCurrency.BCH))
                    handleSuccessfulPayment(hash, CryptoCurrency.BCH)
                },
                {
                    Timber.e(it)
                    view?.dismissProgressDialog()
                    view?.dismissConfirmationDialog()
                    view?.showSnackbar(
                        stringUtils.getString(R.string.transaction_failed),
                        it.message,
                        Snackbar.LENGTH_INDEFINITE)
                    analytics.logEvent(SendAnalytics.SummarySendFailure(CryptoCurrency.BCH))

                    logPaymentSentEvent(false, CryptoCurrency.BCH, pendingTransaction.bigIntAmount)
                }
            )
    }

    private fun getBchKeys(): Observable<List<ECKey>> {
        return if (pendingTransaction.isHD(CryptoCurrency.BCH)) {
            // TODO(accountObject should rather contain keys for signing, not metadata)
            val account = pendingTransaction.sendingObject!!.accountObject as GenericMetadataAccount

            if (payloadDataManager.isDoubleEncrypted) {
                payloadDataManager.decryptHDWallet(networkParameters, verifiedSecondPassword)
                bchDataManager.decryptWatchOnlyWallet(payloadDataManager.mnemonic)
            }

            val hdAccountList = bchDataManager.getAccountList()
            val acc = hdAccountList.find {
                it.node.serializePubB58(environmentSettings.bitcoinCashNetworkParameters) == account.xpub
            } ?: throw HDWalletException("No matching private key found for ${account.xpub}")

            Observable.just(
                bchDataManager.getHDKeysForSigning(
                    acc,
                    pendingTransaction.unspentOutputBundle!!.spendableOutputs
                )
            )
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

    private fun getBchChangeAddress(): Observable<String>? {
        return if (pendingTransaction.isHD(CryptoCurrency.BCH)) {
            val account = pendingTransaction.sendingObject!!.accountObject as GenericMetadataAccount
            val position = bchDataManager.getAccountMetadataList().indexOfFirst { it.xpub == account.xpub }
            bchDataManager.getNextChangeCashAddress(position)
        } else {
            val legacyAddress = pendingTransaction.senderAsLegacyAddress
            Observable.just(
                Address.fromBase58(
                    environmentSettings.bitcoinCashNetworkParameters,
                    legacyAddress.address
                ).toCashAddress()
            )
        }
    }

    private fun clearBchUnspentResponseCache() {
        if (pendingTransaction.isHD(CryptoCurrency.BCH)) {
            val account = pendingTransaction.sendingObject!!.accountObject as GenericMetadataAccount
            unspentApiResponsesBch.remove(account.xpub)
        } else {
            val legacyAddress = pendingTransaction.senderAsLegacyAddress
            unspentApiResponsesBch.remove(legacyAddress.address)
        }
    }

    private fun incrementBchReceiveAddress() {
        if (pendingTransaction.isHD(CryptoCurrency.BCH)) {
            val account = pendingTransaction.sendingObject!!.accountObject as GenericMetadataAccount
            bchDataManager.incrementNextChangeAddress(account.xpub)
            bchDataManager.incrementNextReceiveAddress(account.xpub)
            updateInternalBchBalances()
        }
    }

    private fun handleSuccessfulPayment(hash: String, cryptoCurrency: CryptoCurrency): String {
        view?.showTransactionSuccess(cryptoCurrency)

        pendingTransaction.clear()
        unspentApiResponsesBch.clear()

        return hash
    }

    /**
     * Update balance immediately after spend - until refresh from server
     */
    private fun updateInternalBchBalances() {
        try {
            val totalSent = pendingTransaction.bigIntAmount.add(pendingTransaction.bigIntFee)
            if (pendingTransaction.isHD(CryptoCurrency.BCH)) {
                val account =
                    pendingTransaction.sendingObject!!.accountObject as GenericMetadataAccount
                bchDataManager.subtractAmountFromAddressBalance(account.xpub, totalSent)
            } else {
                val address = pendingTransaction.senderAsLegacyAddress
                bchDataManager.subtractAmountFromAddressBalance(address.address, totalSent)
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
        view?.showPaymentDetails(getConfirmationDetails(), null, null, false)
    }

    private fun checkManualAddressInput() {
        val address = view?.getReceivingAddress()
        address?.let {
            // Only if valid address so we don't override with a label
            if (FormatsUtil.isValidBCHAddress(
                    environmentSettings.bitcoinCashNetworkParameters,
                    address
                ) ||
                FormatsUtil.isValidBitcoinAddress(address)
            )
                pendingTransaction.receivingAddress = address
        }
    }

    private fun getFullBitcoinCashAddressFormat(cashAddress: String): String {
        return if (!cashAddress.startsWith(environmentSettings.bitcoinCashNetworkParameters.bech32AddressPrefix) &&
            FormatsUtil.isValidBCHAddress(
                environmentSettings.bitcoinCashNetworkParameters,
                cashAddress
            )
        ) {
            environmentSettings.bitcoinCashNetworkParameters.bech32AddressPrefix +
                    environmentSettings.bitcoinCashNetworkParameters.bech32AddressSeparator.toChar() +
                    cashAddress
        } else {
            cashAddress
        }
    }

    private fun getConfirmationDetails(): PaymentConfirmationDetails {
        val pending = pendingTransaction

        val total = CryptoValue.fromMinor(CryptoCurrency.BCH, pendingTransaction.total)
        val amount = CryptoValue.fromMinor(CryptoCurrency.BCH, pendingTransaction.bigIntAmount)
        val fee = CryptoValue.fromMinor(CryptoCurrency.BCH, pendingTransaction.bigIntFee)

        return PaymentConfirmationDetails(
            fromLabel = pending.sendingObject!!.label,
            toLabel = pending.displayableReceivingLabel!!.removeBchUri(),
            crypto = CryptoCurrency.BCH,
            fiatUnit = fiatCurrency,
            cryptoTotal = total.toStringWithoutSymbol(),
            cryptoAmount = amount.toStringWithoutSymbol(),
            cryptoFee = fee.toStringWithoutSymbol(),
            fiatFee = fee.toFiat(exchangeRates, fiatCurrency).toStringWithoutSymbol(),
            fiatAmount = amount.toFiat(exchangeRates, fiatCurrency).toStringWithoutSymbol(),
            fiatTotal = total.toFiat(exchangeRates, fiatCurrency).toStringWithSymbol(),
            warningText = pending.warningText,
            warningSubtext = pending.warningSubText
        )
    }

    override fun processURIScanAddress(address: String) {
        pendingTransaction.receivingObject = null
        pendingTransaction.receivingAddress = address
        view?.updateReceivingAddress(address)
    }

    private fun resetAccountList() {
        compositeDisposable += pitLinking.isPitLinked().filter { it }.flatMapSingle {
            nabuToken.fetchNabuToken()
        }.flatMap {
            nabuDataManager.fetchCryptoAddressFromThePit(it, CryptoCurrency.BCH)
        }.applySchedulers()
            .doOnSubscribe {
            view?.updateReceivingHintAndAccountDropDowns(
                CryptoCurrency.BCH,
                getAddressList().size,
                false
            )
        }.subscribeBy(onError = {
            view?.updateReceivingHintAndAccountDropDowns(
                CryptoCurrency.BCH,
                getAddressList().size,
                it is NabuApiException && it.getErrorCode() == NabuErrorCodes.Bad2fa
            ) { view?.show2FANotAvailableError() }
        }) {
            pitAccount =
                PitAccount(
                    stringUtils.getFormattedString(
                        R.string.exchange_default_account_label,
                        CryptoCurrency.BCH.displayTicker
                    ),
                    it.address
                )
            view?.updateReceivingHintAndAccountDropDowns(CryptoCurrency.BCH,
                getAddressList().size,
                it.state == State.ACTIVE && it.address.isNotEmpty()) {
                view?.fillOrClearAddress()
            }
        }
    }

    override fun clearReceivingObject() {
        pendingTransaction.receivingObject = null
    }

    private fun clearCryptoAmount() {
        view?.updateCryptoAmount(CryptoValue.zero(CryptoCurrency.BCH))
    }

    private fun getAddressList(): List<ItemAccount> = walletAccountHelper.getAccountItems(CryptoCurrency.BCH)

    /**
     * Get cached dynamic fee from new Fee options endpoint
     */
    @SuppressLint("CheckResult")
    private fun getSuggestedFee() {
        compositeDisposable += feeDataManager.bchFeeOptions
            .doOnSubscribe { feeOptions = dynamicFeeCache.bchFeeOptions!! }
            .doOnNext { dynamicFeeCache.bchFeeOptions = it }
            .subscribe(
                { /* No-op */ },
                {
                    Timber.e(it)
                    view?.showSnackbar(R.string.confirm_payment_fee_sync_error, Snackbar.LENGTH_LONG)
                    view?.finishPage()
                }
            )
    }

    override fun getFeeOptions(): FeeOptions? = dynamicFeeCache.bchFeeOptions

    private fun getFeePerKbFromPriority(@FeeType.FeePriorityDef feePriorityTemp: Int): BigInteger {
        getSuggestedFee()

        if (feeOptions == null) {
            // This is a stopgap in case of failure to prevent crashes.
            return BigInteger.ZERO
        }

        return when (feePriorityTemp) {
            FeeType.FEE_OPTION_CUSTOM -> BigInteger.valueOf(view!!.getCustomFeeValue() * 1000)
            FeeType.FEE_OPTION_PRIORITY -> BigInteger.valueOf(feeOptions!!.priorityFee * 1000)
            FeeType.FEE_OPTION_REGULAR -> BigInteger.valueOf(feeOptions!!.regularFee * 1000)
            else -> BigInteger.valueOf(feeOptions!!.regularFee * 1000)
        }
    }

    /**
     * Retrieves unspent api data in memory. If not in memory yet, it will be retrieved and added.
     */
    private fun getUnspentApiResponse(address: String): Observable<UnspentOutputs> {
        return if (bchDataManager.getAddressBalance(address).toLong() > 0) {
            return if (unspentApiResponsesBch.containsKey(address)) {
                Observable.just(unspentApiResponsesBch[address])
            } else {
                sendDataManager.getUnspentBchOutputs(address)
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

        val cryptoValue = CryptoValue(CryptoCurrency.BCH, absoluteSuggestedFee)
        view?.updateFeeAmount(cryptoValue, cryptoValue.toFiat(exchangeRates, fiatCurrency))
    }

    private fun updateMaxAvailable(balanceAfterFee: BigInteger) {
        maxAvailable = CryptoValue.fromMinor(CryptoCurrency.BCH, balanceAfterFee)
        view?.showMaxAvailable()

        // Format for display
        view?.updateMaxAvailable(
            stringUtils.getString(R.string.max_available) + " ${maxAvailable.toStringWithSymbol()}"
        )

        if (balanceAfterFee <= Payment.DUST) {
            view?.updateMaxAvailable(stringUtils.getString(R.string.insufficient_funds))
            view?.updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            view?.updateMaxAvailableColor(R.color.primary_blue_accent)
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
        view?.setSendButtonEnabled(true)
        view?.hideMaxAvailable()
        view?.clearWarning()

        val feePerKb = getFeePerKbFromPriority(view!!.getFeePriority())

        calculateUnspentBch(spendAll, amountToSendText, feePerKb)
    }

    @SuppressLint("CheckResult")
    private fun calculateUnspentBch(
        spendAll: Boolean,
        amountToSendText: String?,
        feePerKb: BigInteger
    ) {
        val sendingObj = pendingTransaction.sendingObject

        if (sendingObj?.address == null) {
            // This shouldn't happen, but handle case anyway in case of low memory scenario onBitcoinCashChosen()
            return
        }

        val address = sendingObj.address

        Observables.zip(
            getUnspentApiResponse(address),
            coinSelectionRemoteConfig.enabled.toObservable()
        )
            .debounce(200, TimeUnit.MILLISECONDS)
            .applySchedulers()
            .subscribe(
                { (coins, newCoinSelectionEnabled) ->
                    val amountToSend = getSatoshisFromText(amountToSendText, getDefaultDecimalSeparator())
                    // Future use. There might be some unconfirmed funds. Not displaying a warning currently
                    // (to line up with iOS and Web wallet)
                    if (coins.notice != null) {
                        view?.updateWarning(coins.notice)
                    } else {
                        view?.clearWarning()
                    }

                    updateFee(getSuggestedAbsoluteFee(
                        coins,
                        CryptoValue.fromMinor(CryptoCurrency.BCH, amountToSend),
                        feePerKb,
                        newCoinSelectionEnabled
                    ))

                    suggestedFeePayment(
                        coins,
                        CryptoValue.fromMinor(CryptoCurrency.BCH, amountToSend),
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
        var amount = amountToSend.toBigInteger()

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
            view?.updateCryptoAmount(CryptoValue(CryptoCurrency.BCH, sweepableAmount))
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
            val legacyAddress = pendingTransaction.sendingObject?.accountObject as LegacyAddress
            setTempLegacyAddressPrivateKey(legacyAddress, key)
        } catch (e: Exception) {
            view?.showSnackbar(R.string.no_private_key, Snackbar.LENGTH_LONG)
            Timber.e(e)
        }
    }

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {
        compositeDisposable += sendDataManager.getEcKeyFromBip38(
            pw,
            scanData,
            environmentSettings.bitcoinNetworkParameters
        ).subscribe(
            {
                val legacyAddress = pendingTransaction.senderAsLegacyAddress
                setTempLegacyAddressPrivateKey(legacyAddress, it)
            },
            { view?.showSnackbar(R.string.bip38_error, Snackbar.LENGTH_LONG) }
        )
    }

    private fun setTempLegacyAddressPrivateKey(legacyAddress: LegacyAddress, key: ECKey) {
        if (key.hasPrivKey() && legacyAddress.address == key.toAddress(
                environmentSettings.bitcoinNetworkParameters
            ).toString()
        ) {
            // Create copy, otherwise pass by ref will override private key in wallet payload
            pendingTransaction.sendingObject!!.accountObject = LegacyAddress().apply {
                setPrivateKeyFromBytes(key.privKeyBytes)
                address = key.toAddress(environmentSettings.bitcoinNetworkParameters).toString()
                label = legacyAddress.label
                tag = PendingTransaction.WATCH_ONLY_SPEND_TAG
            }

            showPaymentReview()
        } else {
            view?.showSnackbar(R.string.invalid_private_key, Snackbar.LENGTH_LONG)
        }
    }

    override fun setSendingAccount(account: CryptoAccount) {
        require(account is BchCryptoWalletAccount)

        pendingTransaction.sendingObject = ItemAccount(
            label = account.label,
            accountObject = account.internalAccount,
            address = account.internalAccount.xpub
        )

        view?.updateSendingAddress(account.label)
        calculateSpendableAmounts(false, "0")
    }

    private fun onReceivingBchLegacyAddressSelected(legacyAddress: LegacyAddress) {

        var cashAddress = legacyAddress.address

        if (!FormatsUtil.isValidBCHAddress(
                environmentSettings.bitcoinCashNetworkParameters,
                legacyAddress.address
            ) &&
            FormatsUtil.isValidBitcoinAddress(legacyAddress.address)
        ) {
            cashAddress = Address.fromBase58(
                environmentSettings.bitcoinCashNetworkParameters,
                legacyAddress.address
            ).toCashAddress()
        }

        var label = legacyAddress.label
        if (label.isNullOrEmpty()) {
            label = cashAddress.removeBchUri()
        }

        pendingTransaction.receivingObject = ItemAccount(
            label = label,
            accountObject = legacyAddress,
            address = cashAddress
        )
        pendingTransaction.receivingAddress = cashAddress

        view?.updateReceivingAddress(label.removeBchUri())
    }

    private fun onReceivingBchAccountSelected(account: GenericMetadataAccount) {
        var label = account.label
        if (label.isNullOrEmpty()) {
            label = account.xpub
        }

        pendingTransaction.receivingObject = ItemAccount(
            label = label,
            accountObject = account,
            address = account.xpub
        )

        view?.updateReceivingAddress(label)

        val position =
            bchDataManager.getAccountMetadataList().indexOfFirst { it.xpub == account.xpub }

        compositeDisposable += bchDataManager.getNextReceiveCashAddress(position)
            .doOnNext { pendingTransaction.receivingAddress = it }
            .subscribe(
                { /* No-op */ },
                { view?.showSnackbar(R.string.unexpected_error, Snackbar.LENGTH_LONG) }
            )
    }

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

    private fun isValidBitcoincashAddress() =
        Observable.just(
            FormatsUtil.isValidBCHAddress(
                environmentSettings.bitcoinCashNetworkParameters,
                pendingTransaction.receivingAddress
            )
        )

    private fun validateBitcoinCashTransaction(): Pair<Boolean, Int> {
        var validated = true
        var errorMessage = R.string.unexpected_error

        if (pendingTransaction.receivingAddress.isEmpty()) {
            errorMessage = R.string.bch_invalid_address
            validated = false

            // Same amount validation as bitcoin
        } else if (!isValidBitcoinAmount(pendingTransaction.bigIntAmount)) {
            errorMessage = R.string.invalid_amount
            validated = false
        } else if (pendingTransaction.unspentOutputBundle == null) {
            errorMessage = R.string.no_confirmed_funds
            validated = false
        } else if (maxAvailable < CryptoValue.fromMinor(CryptoCurrency.BCH, pendingTransaction.bigIntAmount)) {
            errorMessage = R.string.insufficient_funds
            validated = false
        } else if (pendingTransaction.unspentOutputBundle!!.spendableOutputs.isEmpty()) {
            errorMessage = R.string.insufficient_funds
            validated = false
        }

        return Pair.of(validated, errorMessage)
    }
}

fun String.removeBchUri(): String = this.replace("bitcoincash:", "")

@CommonCode("Also in BitcoinSendStrategy")
private fun getSatoshisFromText(text: String?, decimalSeparator: String): BigInteger {
    if (text == null || text.isEmpty()) return BigInteger.ZERO

    val amountToSend = stripSeparator(text, decimalSeparator)

    val amount = try {
        java.lang.Double.parseDouble(amountToSend)
    } catch (e: NumberFormatException) {
        0.0
    }

    return BigDecimal.valueOf(amount)
        .multiply(BigDecimal.valueOf(100000000))
        .toBigInteger()
}

private fun stripSeparator(text: String, decimalSeparator: String) =
    text.trim { it <= ' ' }
        .replace(" ", "")
        .replace(decimalSeparator, ".")
