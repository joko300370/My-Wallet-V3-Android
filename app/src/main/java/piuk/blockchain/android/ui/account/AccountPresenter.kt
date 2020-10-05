package piuk.blockchain.android.ui.account

import android.annotation.SuppressLint
import androidx.annotation.VisibleForTesting
import com.blockchain.notifications.analytics.AddressAnalytics
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.WalletAnalytics
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.BitcoinCashWallet
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.exceptions.DecryptionException
import info.blockchain.wallet.exceptions.PayloadException
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import info.blockchain.wallet.payload.data.isArchived
import info.blockchain.wallet.util.FormatsUtil
import info.blockchain.wallet.util.PrivateKeyFactory
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.bitcoinj.core.ECKey
import org.bitcoinj.crypto.BIP38PrivateKey
import piuk.blockchain.android.R
import piuk.blockchain.android.data.coinswebsocket.strategy.CoinsWebSocketStrategy
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.LabelUtil
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.logging.AddressType
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.createAccountEvent
import piuk.blockchain.androidcoreui.utils.logging.importEvent
import timber.log.Timber
import java.math.BigInteger
import kotlin.properties.Delegates

class AccountPresenter internal constructor(
    private val payloadDataManager: PayloadDataManager,
    private val bchDataManager: BchDataManager,
    private val metadataManager: MetadataManager,
    private val fundsDataManager: TransferFundsDataManager,
    private val prefs: PersistentPrefs,
    private val appUtil: AppUtil,
    private val privateKeyFactory: PrivateKeyFactory,
    private val environmentSettings: EnvironmentConfig,
    private val analytics: Analytics,
    private val coinsWebSocketStrategy: CoinsWebSocketStrategy
) : BasePresenter<AccountView>() {

    internal var doubleEncryptionPassword: String? = null
    internal var cryptoCurrency: CryptoCurrency by Delegates.observable(
        CryptoCurrency.BTC
    ) { _, _, new ->
        check(new != CryptoCurrency.ETHER) { "Ether not a supported cryptocurrency on this page" }
        onViewReady()
    }

    internal val accountSize: Int
        get() = when (cryptoCurrency) {
            CryptoCurrency.BTC -> getBtcAccounts().size
            CryptoCurrency.BCH -> getBchAccounts().size
            else ->
                throw IllegalStateException("${cryptoCurrency.networkTicker} is not supported on this page")
        }

    override fun onViewReady() {
        if (environmentSettings.environment == Environment.TESTNET) {
            view.hideCurrencyHeader()
        }
        view.updateAccountList(getDisplayList())
        if (cryptoCurrency == CryptoCurrency.BCH) {
            view.onSetTransferLegacyFundsMenuItemVisible(false)
        } else {
            checkTransferableLegacyFunds(false, false)
        }
    }

    /**
     * Silently check if there are any spendable legacy funds that need to be sent to default
     * account. Prompt user when done calculating.
     */
    @SuppressLint("CheckResult")
    internal fun checkTransferableLegacyFunds(isAutoPopup: Boolean, showWarningDialog: Boolean) {
        compositeDisposable += fundsDataManager.transferableFundTransactionListForDefaultAccount
            .doAfterTerminate { view.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .subscribeBy(
                onNext = { (pendingList, _, _) ->
                        if (payloadDataManager.wallet!!.isUpgraded && pendingList.isNotEmpty()) {
                            view.onSetTransferLegacyFundsMenuItemVisible(true)

                            if ((prefs.isTransferAllWarningEnabled || !isAutoPopup) && showWarningDialog) {
                                view.onShowTransferableLegacyFundsWarning(isAutoPopup)
                            }
                        } else {
                            view.onSetTransferLegacyFundsMenuItemVisible(false)
                        }
                    },
                onError = {
                    Timber.e(it)
                    view.onSetTransferLegacyFundsMenuItemVisible(false)
                }
            )
    }

    private val PersistentPrefs.isTransferAllWarningEnabled
        get() = getValue(KEY_WARN_TRANSFER_ALL, true)

    /**
     * Derive new Account from seed
     *
     * @param accountLabel A label for the account to be created
     */
    @SuppressLint("CheckResult")
    internal fun createNewAccount(accountLabel: String) {
        if (LabelUtil.isExistingLabel(payloadDataManager, bchDataManager, accountLabel)) {
            view.showToast(R.string.label_name_match, ToastCustom.TYPE_ERROR)
            return
        }

        compositeDisposable += payloadDataManager.createNewAccount(accountLabel, doubleEncryptionPassword)
            .doOnNext {
                coinsWebSocketStrategy.subscribeToXpubBtc(it.xpub)
            }
            .flatMapCompletable {
                bchDataManager.createAccount(it.xpub)
                metadataManager.saveToMetadata(
                    bchDataManager.serializeForSaving(),
                    BitcoinCashWallet.METADATA_TYPE_EXTERNAL
                )
            }
            .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
            .doAfterTerminate { view.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .subscribe(
                {
                    view.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK)
                    onViewReady()
                    analytics.logEvent(WalletAnalytics.AddNewWallet)
                    Logging.logEvent(createAccountEvent(payloadDataManager.accounts.size))
                },
                { throwable ->
                    when (throwable) {
                        is DecryptionException -> view.showToast(
                            R.string.double_encryption_password_error,
                            ToastCustom.TYPE_ERROR
                        )
                        is PayloadException -> view.showToast(
                            R.string.remote_save_ko,
                            ToastCustom.TYPE_ERROR
                        )
                        else -> view.showToast(
                            R.string.unexpected_error,
                            ToastCustom.TYPE_ERROR
                        )
                    }
                }
            )
    }

    /**
     * Sync [LegacyAddress] with server after either creating a new address or updating the
     * address in some way, for instance updating its name.
     *
     * @param address The [LegacyAddress] to be sync'd with the server
     */
    @SuppressLint("CheckResult")
    internal fun updateLegacyAddress(address: LegacyAddress) {
        compositeDisposable += payloadDataManager.updateLegacyAddress(address)
            .doOnSubscribe { view.showProgressDialog(R.string.saving_address) }
            .doOnError { Timber.e(it) }
            .doAfterTerminate { view.dismissProgressDialog() }
            .subscribe(
                {
                    view.showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK)
                    analytics.logEvent(AddressAnalytics.ImportBTCAddress)
                    coinsWebSocketStrategy.subscribeToExtraBtcAddress(address.address)
                    onViewReady()
                },
                { view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR) }
            )
    }

    /**
     * Checks status of camera and updates UI appropriately
     */
    internal fun onScanButtonClicked() {
        if (!appUtil.isCameraOpen) {
            view.startScanForResult()
        } else {
            view.showToast(R.string.camera_unavailable, ToastCustom.TYPE_ERROR)
        }
    }

    /**
     * Imports BIP38 address and prompts user to rename address if successful
     *
     * @param data The address to be imported
     * @param password The BIP38 encryption passphrase
     */
    @SuppressLint("VisibleForTests")
    internal fun importBip38Address(data: String, password: String) {
        view.showProgressDialog(R.string.please_wait)
        try {
            val bip38 = BIP38PrivateKey.fromBase58(environmentSettings.bitcoinNetworkParameters, data)
            val key = bip38.decrypt(password)
            handlePrivateKey(key, doubleEncryptionPassword)
        } catch (e: Exception) {
            Timber.e(e)
            view.showToast(R.string.bip38_error, ToastCustom.TYPE_ERROR)
        } finally {
            view.dismissProgressDialog()
        }
    }

    /**
     * Handles result of address scanning operation appropriately for each possible type of address
     *
     * @param data The address to be imported
     */
    internal fun onAddressScanned(data: String?) {
        if (data == null) {
            view.showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR)
            return
        }
        try {
            val format = privateKeyFactory.getFormat(data)
            if (format != null) {
                // Private key scanned
                if (format != PrivateKeyFactory.BIP38) {
                    importNonBip38Address(format, data, doubleEncryptionPassword)
                } else {
                    view.showBip38PasswordDialog(data)
                }
            } else {
                // Watch-only address scanned
                importWatchOnlyAddress(data)
            }
        } catch (e: Exception) {
            Timber.e(e)
            view.showToast(R.string.privkey_error, ToastCustom.TYPE_ERROR)
        }
    }

    private fun importWatchOnlyAddress(address: String) {
        val addressCopy = correctAddressFormatting(address)

        if (!FormatsUtil.isValidBitcoinAddress(addressCopy)) {
            view.showToast(R.string.invalid_bitcoin_address, ToastCustom.TYPE_ERROR)
        } else if (payloadDataManager.wallet!!.legacyAddressStringList.contains(addressCopy)) {
            view.showToast(R.string.address_already_in_wallet, ToastCustom.TYPE_ERROR)
        } else {
            view.showWatchOnlyWarningDialog(addressCopy)
        }
    }

    private fun correctAddressFormatting(address: String): String {
        var addressCopy = address
        // Check for poorly formed BIP21 URIs
        if (addressCopy.startsWith("bitcoin://") && addressCopy.length > 10) {
            addressCopy = "bitcoin:" + addressCopy.substring(10)
        }

        if (FormatsUtil.isBitcoinUri(addressCopy)) {
            addressCopy = FormatsUtil.getBitcoinAddress(addressCopy)
        }

        return addressCopy
    }

    @SuppressLint("VisibleForTests", "CheckResult")
    private fun importNonBip38Address(format: String, data: String, secondPassword: String?) {
        compositeDisposable += payloadDataManager.getKeyFromImportedData(format, data)
            .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
            .doAfterTerminate { view.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .subscribe(
                { handlePrivateKey(it, secondPassword) },
                { view.showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR) }
            )
    }

    @SuppressLint("CheckResult")
    @Suppress("MemberVisibilityCanBePrivate")
    @VisibleForTesting
    internal fun handlePrivateKey(key: ECKey?, secondPassword: String?) {
        if (key != null && key.hasPrivKey()) {
            // A private key to an existing address has been scanned
            compositeDisposable += payloadDataManager.setKeyForLegacyAddress(key, secondPassword)
                .doOnError { Timber.e(it) }
                .subscribe(
                    {
                        view.showToast(
                            R.string.private_key_successfully_imported,
                            ToastCustom.TYPE_OK
                        )
                        onViewReady()
                        view.showRenameImportedAddressDialog(it)
                        analytics.logEvent(AddressAnalytics.ImportBTCAddress)
                        Logging.logEvent(importEvent(AddressType.PRIVATE_KEY))
                    },
                    {
                        view.showToast(R.string.remote_save_ko, ToastCustom.TYPE_ERROR)
                    }
                )
        } else {
            view.showToast(R.string.no_private_key, ToastCustom.TYPE_ERROR)
        }
    }

    private fun getDisplayList(): List<AccountItem> {
        return when (cryptoCurrency) {
            CryptoCurrency.BTC -> getBtcDisplayList()
            CryptoCurrency.BCH -> getBchDisplayList()
            CryptoCurrency.ETHER -> throw IllegalStateException("Ether not a supported cryptocurrency on this page")
            CryptoCurrency.XLM -> throw IllegalStateException("Xlm not a supported cryptocurrency on this page")
            CryptoCurrency.PAX -> TODO("PAX is not yet supported - AND-2003")
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
            CryptoCurrency.ALGO -> TODO("STUB: ALGO NOT IMPLEMENTED")
            CryptoCurrency.USDT -> TODO("STUB: USDT NOT IMPLEMENTED")
        }
    }

    private fun getBtcDisplayList(): List<AccountItem> {
        val accountsAndImportedList = mutableListOf<AccountItem>()
        var correctedPosition = 0

        // Create New Wallet button at top position
        accountsAndImportedList.add(AccountItem(AccountItem.TYPE_CREATE_NEW_WALLET_BUTTON))

        val defaultAccount = getBtcAccounts()[getDefaultBtcIndex()]

        for (account in getBtcAccounts()) {
            val balance = getBtcAccountBalance(account.xpub)
            var label: String? = account.label

            if (label != null && label.length > ADDRESS_LABEL_MAX_LENGTH) {
                label = """${label.substring(0, ADDRESS_LABEL_MAX_LENGTH)}..."""
            }
            if (label.isNullOrEmpty()) label = ""

            accountsAndImportedList.add(
                AccountItem(
                    correctedPosition,
                    label, null,
                    balance,
                    account.isArchived,
                    defaultAccount.xpub == account.xpub,
                    AccountItem.TYPE_ACCOUNT_BTC
                )
            )
            correctedPosition++
        }

        // Import Address button at first position after wallets
        accountsAndImportedList.add(AccountItem(AccountItem.TYPE_IMPORT_ADDRESS_BUTTON))

        for (legacyAddress in getLegacyAddresses()) {
            var label: String? = legacyAddress.label
            val address: String = legacyAddress.address ?: ""
            val balance = getBtcAddressBalance(address)

            if (label != null && label.length > ADDRESS_LABEL_MAX_LENGTH) {
                label = """${label.substring(0, ADDRESS_LABEL_MAX_LENGTH)}..."""
            }
            if (label.isNullOrEmpty()) label = ""

            accountsAndImportedList.add(
                AccountItem(
                    correctedPosition,
                    label,
                    address,
                    balance,
                    legacyAddress.isArchived,
                    false,
                    AccountItem.TYPE_ACCOUNT_BTC
                )
            )
            correctedPosition++
        }

        return accountsAndImportedList
    }

    private fun getBchDisplayList(): List<AccountItem> {
        val accountsAndImportedList = mutableListOf<AccountItem>()

        // Create New Wallet button at top position, non-clickable
        accountsAndImportedList.add(AccountItem(AccountItem.TYPE_WALLET_HEADER))

        val bchAccounts = getBchAccounts()
        val defaultAccount = bchAccounts.getOrNull(getDefaultBchIndex())

        for ((position, account) in bchAccounts.withIndex()) {
            val balance = getBchAccountBalance(account.xpub)
            var label: String? = account.label

            if (label != null && label.length > ADDRESS_LABEL_MAX_LENGTH) {
                label = """${label.substring(0, ADDRESS_LABEL_MAX_LENGTH)}..."""
            }
            if (label.isNullOrEmpty()) label = ""

            accountsAndImportedList.add(
                AccountItem(
                    position,
                    label, null,
                    balance,
                    account.isArchived,
                    defaultAccount?.xpub == account.xpub,
                    AccountItem.TYPE_ACCOUNT_BCH
                )
            )
        }

        if (bchDataManager.getImportedAddressBalance() > BigInteger.ZERO) {
            // Import Address header, non clickable
            accountsAndImportedList.add(AccountItem(AccountItem.TYPE_LEGACY_HEADER))

            val total = bchDataManager.getImportedAddressBalance()
            // Non-clickable summary
            accountsAndImportedList.add(
                AccountItem(
                    AccountItem.TYPE_LEGACY_SUMMARY,
                    getBchDisplayBalance(total)
                )
            )
        }

        return accountsAndImportedList
    }

    // region Convenience functions
    private fun getBtcAccounts(): List<Account> = payloadDataManager.accounts

    private fun getBchAccounts(): List<GenericMetadataAccount> =
        bchDataManager.getAccountMetadataList()

    private fun getLegacyAddresses(): List<LegacyAddress> = payloadDataManager.legacyAddresses

    private fun getDefaultBtcIndex(): Int = payloadDataManager.defaultAccountIndex

    private fun getDefaultBchIndex(): Int = bchDataManager.getDefaultAccountPosition()
    // endregion

    // region Balance and formatting functions
    private fun getBtcAccountBalance(xpub: String): String {
        val amount = getBalanceFromBtcAddress(xpub)
        return getUiString(amount)
    }

    private fun getBchAccountBalance(xpub: String): String {
        val amount = getBalanceFromBchAddress(xpub)
        return getUiString(amount)
    }

    private fun getBtcAddressBalance(address: String): String {
        val amount = getBalanceFromBtcAddress(address)
        return getUiString(amount)
    }

    private fun getBchDisplayBalance(amount: BigInteger): String {
        return getUiString(CryptoValue.fromMinor(CryptoCurrency.BCH, amount))
    }

    private fun getUiString(amount: CryptoValue) =
        amount.toStringWithSymbol()

    private fun getBalanceFromBtcAddress(address: String) =
        payloadDataManager.getAddressBalance(address)

    private fun getBalanceFromBchAddress(address: String) =
        CryptoValue.fromMinor(CryptoCurrency.BCH, bchDataManager.getAddressBalance(address))

    fun getDisplayableCurrencies(): Set<CryptoCurrency> =
        CryptoCurrency.values()
            .filter { !it.hasFeature(CryptoCurrency.STUB_ASSET) }
            .filter { shouldShow(it) }
            .toSet()

    private fun shouldShow(cryptoCurrency: CryptoCurrency): Boolean =
        when (cryptoCurrency) {
            CryptoCurrency.BTC,
            CryptoCurrency.BCH -> true
            CryptoCurrency.ETHER,
            CryptoCurrency.XLM,
            CryptoCurrency.PAX,
            CryptoCurrency.ALGO,
            CryptoCurrency.USDT -> false
            CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
        }

    companion object {
        internal const val KEY_WARN_TRANSFER_ALL = "WARN_TRANSFER_ALL"
        internal const val ADDRESS_LABEL_MAX_LENGTH = 17
    }
}
