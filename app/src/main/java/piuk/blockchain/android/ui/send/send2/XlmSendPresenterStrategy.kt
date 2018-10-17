package piuk.blockchain.android.ui.send.send2

import android.content.Intent
import com.blockchain.sunriver.HorizonKeyPair
import com.blockchain.sunriver.XlmDataManager
import com.blockchain.sunriver.XlmTransactionSender
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.withMajorValueOrZero
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.send.SendView
import piuk.blockchain.android.ui.send.external.SendPresenterX
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.currency.CurrencyState
import timber.log.Timber

class XlmSendPresenterStrategy(
    currencyState: CurrencyState,
    private val xlmDataManager: XlmDataManager,
    private val xlmTransactionSender: XlmTransactionSender
) : SendPresenterX<SendView>() {

    private val currency: CryptoCurrency by lazy { currencyState.cryptoCurrency }
    private var cryptoTextSubject = PublishSubject.create<CryptoValue>()
    private var continueClick = PublishSubject.create<Unit>()

    private val send = cryptoTextSubject.sample(continueClick)

    override fun onContinueClicked() {
        continueClick.onNext(Unit)
    }

    override fun onSpendMaxClicked() {
        TODO("AND-1535")
    }

    override fun onBroadcastReceived() {
        TODO("AND-1535")
    }

    override fun onResume() {
    }

    override fun onCurrencySelected(currency: CryptoCurrency) {
        when (currency) {
            CryptoCurrency.XLM -> xlmSelected()
            else -> throw IllegalArgumentException("This presented is not for $currency")
        }
    }

    private fun xlmSelected() {
        view.hideFeePriority()
        view.setFeePrioritySelection(0)
        view.disableFeeDropdown()
        view.setCryptoMaxLength(15)
    }

    override fun handleURIScan(untrimmedscanData: String?) {
        TODO("AND-1535")
    }

    override fun handlePrivxScan(scanData: String?) {
        TODO("AND-1535")
    }

    override fun clearReceivingObject() {
    }

    override fun selectSendingAccount(data: Intent?, currency: CryptoCurrency) {
        TODO("AND-1535")
    }

    override fun selectReceivingAccount(data: Intent?, currency: CryptoCurrency) {
        TODO("AND-1535")
    }

    override fun selectDefaultOrFirstFundedSendingAccount() {
        xlmDataManager.defaultAccount()
            .addToCompositeDisposable(this)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = { Timber.e(it) }) {
                view.updateSendingAddress(it.label)
            }
    }

    override fun submitPayment() {
        TODO("AND-1535")
    }

    override fun shouldShowAdvancedFeeWarning(): Boolean {
        TODO("AND-1535")
    }

    override fun onCryptoTextChange(cryptoText: String) {
        cryptoTextSubject.onNext(currency.withMajorValueOrZero(cryptoText))
    }

    override fun spendFromWatchOnlyBIP38(pw: String, scanData: String) {
        TODO("AND-1535")
    }

    override fun setWarnWatchOnlySpend(warn: Boolean) {
        TODO("AND-1535")
    }

    override fun onNoSecondPassword() {
        TODO("AND-1535")
    }

    override fun onSecondPasswordValidated(validateSecondPassword: String) {
        TODO("AND-1535")
    }

    override fun disableAdvancedFeeWarning() {
        TODO("AND-1535")
    }

    override fun getBitcoinFeeOptions(): FeeOptions? {
        TODO("AND-1535")
    }

    override fun onViewReady() {
        send
            .addToCompositeDisposable(this)
            .observeOn(AndroidSchedulers.mainThread())
            .flatMapCompletable { value ->
                val toAddress = HorizonKeyPair.createValidatedPublic(view.getReceivingAddress() ?: "")
                xlmTransactionSender.sendFunds(
                    value,
                    toAddress.accountId
                ).doOnSubscribe {
                    view.showProgressDialog(R.string.app_name)
                }.doOnTerminate {
                    view.dismissProgressDialog()
                }
            }
            .subscribeBy(onError = { Timber.e(it) })
    }
}
