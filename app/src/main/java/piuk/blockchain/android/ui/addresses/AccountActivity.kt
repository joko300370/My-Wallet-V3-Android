package piuk.blockchain.android.ui.addresses

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import androidx.annotation.StringRes
import androidx.recyclerview.widget.LinearLayoutManager
import com.blockchain.featureflags.InternalFeatureFlagApi
import com.blockchain.koin.scopedInject
import com.blockchain.ui.password.SecondPasswordHandler
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.databinding.ActivityAccountsBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.addresses.adapter.AccountAdapter
import piuk.blockchain.android.ui.addresses.adapter.AccountListItem
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.android.ui.customviews.BlockchainListDividerDecor
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.transactionflow.DialogFlow
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.android.ui.scan.QrScanActivity
import piuk.blockchain.android.ui.scan.QrExpected
import piuk.blockchain.android.ui.scan.QrScanActivity.Companion.getRawScanData
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber

class AccountActivity : MvpActivity<AccountView, AccountPresenter>(),
                        AccountView,
                        AccountAdapter.Listener,
                        AccountEditSheet.Host,
                        DialogFlow.FlowHost {

    private val rxBus: RxBus by inject()
    private val secondPasswordHandler: SecondPasswordHandler by scopedInject()
    private val features: InternalFeatureFlagApi by inject()
    private val compositeDisposable = CompositeDisposable()

    private val binding: ActivityAccountsBinding by lazy {
        ActivityAccountsBinding.inflate(layoutInflater)
    }

    private val accountsAdapter: AccountAdapter by unsafeLazy {
        AccountAdapter(this, features)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        val toolbarBinding = ToolbarGeneralBinding.bind(binding.root)
        setupToolbar(toolbarBinding.toolbarGeneral, R.string.drawer_addresses)

        with(binding.currencyHeader) {
            setCurrentlySelectedCurrency(CryptoCurrency.BTC)
            setSelectionListener { presenter.refresh(it) }
        }

        with(binding.recyclerviewAccounts) {
            layoutManager = LinearLayoutManager(this@AccountActivity)
            itemAnimator = null
            setHasFixedSize(true)
            adapter = accountsAdapter
            addItemDecoration(
                BlockchainListDividerDecor(context)
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> consume { onBackPressed() }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (binding.currencyHeader.isOpen()) {
            binding.currencyHeader.close()
        } else {
            super.onBackPressed()
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            with(binding.currencyHeader) {
                if (isTouchOutside(event) && isOpen()) {
                    close()
                    return false
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onCreateNewClicked() {
        Timber.d("Click new account")
        createNewAccount()
    }

    override fun onAccountClicked(account: CryptoAccount) {
        Timber.d("Click ${account.label}")
        showBottomSheet(AccountEditSheet.newInstance(account))
    }

    override fun onImportAddressClicked() {
        Timber.d("Click import account")
        compositeDisposable += secondPasswordHandler.secondPassword(this)
            .subscribeBy(
                onSuccess = { showScanActivity() },
                onComplete = { showScanActivity() },
                onError = { }
            )
    }

    private fun showScanActivity() {
        QrScanActivity.start(this, QrExpected.IMPORT_KEYS_QR)
    }

    private fun createNewAccount() {
        compositeDisposable += secondPasswordHandler.secondPassword(this)
            .subscribeBy(
                onSuccess = { password ->
                    promptForAccountLabel(
                        ctx = this@AccountActivity,
                        title = R.string.create_a_new_wallet,
                        msg = R.string.create_a_new_wallet_helper_text,
                        okAction = { presenter.createNewAccount(it, password) }
                    )
                },
                onComplete = {
                    promptForAccountLabel(
                        ctx = this@AccountActivity,
                        title = R.string.create_a_new_wallet,
                        msg = R.string.create_a_new_wallet_helper_text,
                        okAction = { presenter.createNewAccount(it) }
                    )
                },
                onError = { }
            )
    }

    override fun renderAccountList(
        asset: CryptoCurrency,
        internal: List<CryptoNonCustodialAccount>,
        imported: List<CryptoNonCustodialAccount>
    ) {
        accountsAdapter.items = mutableListOf<AccountListItem>().apply {
            if (internal.isNotEmpty()) {
                add(AccountListItem.InternalHeader(enableCreate = asset == CryptoCurrency.BTC))
                addAll(internal.map { AccountListItem.Account(it) })
            }

            add(AccountListItem.ImportedHeader(enableImport = asset == CryptoCurrency.BTC))
            if (imported.isNotEmpty()) {
                addAll(imported.map { AccountListItem.Account(it) })
            }
        }.toList()
    }

    private val event by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    override fun onResume() {
        super.onResume()
        presenter.refresh(binding.currencyHeader.getSelectedCurrency())
    }

    override fun onPause() {
        super.onPause()
        rxBus.unregister(ActionEvent::class.java, event)
        compositeDisposable.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == QrScanActivity.SCAN_URI_RESULT && data.getRawScanData() != null) {
            data.getRawScanData()?.let {
                handleImportScan(it)
            } ?: showError(R.string.privkey_error)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun handleImportScan(scanData: String) {
        val walletPassword = secondPasswordHandler.verifiedPassword
        if (presenter.importRequiresPassword(scanData)) {
            promptImportKeyPassword(this) { password ->
                presenter.importScannedAddress(scanData, password, walletPassword)
            }
        } else {
            presenter.importScannedAddress(scanData, walletPassword)
        }
    }

    override fun onStartTransferFunds(account: CryptoNonCustodialAccount) {
        launchFlow(account)
    }

    override fun onSheetClosed() {
        presenter.refresh(binding.currencyHeader.getSelectedCurrency())
    }

    override fun showRenameImportedAddressDialog(account: CryptoNonCustodialAccount) =
        promptForAccountLabel(
            ctx = this,
            title = R.string.app_name,
            msg = R.string.label_address,
            initialText = account.label,
            okAction = { presenter.updateImportedAddressLabel(it, account) },
            okBtnText = R.string.save_name,
            cancelText = R.string.polite_no
        )

    override fun showError(@StringRes message: Int) =
        toast(message, ToastCustom.TYPE_ERROR)

    override fun showSuccess(@StringRes message: Int) {
        toast(message, ToastCustom.TYPE_OK)
        presenter.refresh(binding.currencyHeader.getSelectedCurrency())
    }

    override fun showTransferFunds(account: CryptoNonCustodialAccount) {
        promptTransferFunds(this) { launchFlow(account) }
    }

    private fun launchFlow(sourceAccount: CryptoAccount) {
        TransactionFlow(
            sourceAccount = sourceAccount,
            action = AssetAction.Send
        ).apply {
            startFlow(
                fragmentManager = supportFragmentManager,
                host = this@AccountActivity
            )
        }
    }

    override fun onFlowFinished() {
        presenter.refresh(binding.currencyHeader.getSelectedCurrency())
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
    }

    override val presenter: AccountPresenter by scopedInject()
    override val view: AccountView
        get() = this
}
