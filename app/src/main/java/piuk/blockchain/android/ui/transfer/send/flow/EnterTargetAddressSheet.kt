package piuk.blockchain.android.ui.transfer.send.flow

import android.app.Activity
import android.content.Intent
import android.text.Editable
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_send_address.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.scan.QrScanHandler
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import timber.log.Timber

class EnterTargetAddressSheet(
    host: SlidingModalBottomDialog.Host
) : FlowInputSheet(host) {
    override val layoutResource: Int = R.layout.dialog_send_address

    private val appUtil: AppUtil by inject()
    private val coincore: Coincore by scopedInject()
    private val customiser: SendFlowCustomiser by inject()

    private val disposables = CompositeDisposable()
    private var state: SendState = SendState()

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterTargetAddressSheet")

        with(dialogView) {
            if (state.sendingAccount != newState.sendingAccount) {
                from_details.updateAccount(newState.sendingAccount, disposables)
                setupTransferList(newState.sendingAccount, newState)
            }
            cta_button.isEnabled = newState.nextEnabled

            if (customiser.selectTargetShowManualEnterAddress(newState)) {
                showManualAddressEntry(newState)
            } else {
                hideManualAddressEntry(newState)
            }

            customiser.errorFlashMessage(newState)?.let {
                address_entry.setBackgroundColor(
                    ContextCompat.getColor(requireContext(), R.color.red_000))
                error_msg.apply {
                    text = it
                    visible()
                }
            } ?: hideErrorState()

            title.text = customiser.selectTargetAddressTitle(newState)
        }
        state = newState
    }

    private fun hideErrorState() {
        dialogView.error_msg.invisible()
        dialogView.address_entry.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.grey_000))
    }

    private fun showManualAddressEntry(newState: SendState) {
        val address = if (newState.sendTarget is CryptoAddress) {
            newState.sendTarget.address
        } else {
            ""
        }

        with(dialogView) {
            address_entry.removeTextChangedListener(addressTextWatcher)
            if (address.isNotEmpty()) {
                address_entry.setText(address, TextView.BufferType.EDITABLE)
            }
            address_entry.hint = customiser.selectTargetAddressInputHint(newState)
            address_entry.addTextChangedListener(addressTextWatcher)

            input_switcher.displayedChild = NONCUSTODIAL_INPUT
        }
    }

    private fun hideManualAddressEntry(newState: SendState) {
        val msg = customiser.selectTargetNoAddressMessageText(newState)

        with(dialogView) {
            if (msg != null) {
                input_switcher.visible()
                no_manual_enter_msg.text = msg

                internal_send_close.setOnClickListener {
                    input_switcher.gone()
                }

                title_pick.gone()
                input_switcher.displayedChild = CUSTODIAL_INPUT
            } else {
                input_switcher.gone()
                title_pick.gone()
            }
        }
    }

    private val addressTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            val address = s.toString()

            if (address.isEmpty()) {
                model.process(SendIntent.EnteredAddressReset)
            } else {
                val asset = state.asset
                addressEntered(address, asset)
            }
        }
    }

    override fun initControls(view: View) {
        with(dialogView) {
            address_entry.addTextChangedListener(addressTextWatcher)
            btn_scan.setOnClickListener { onLaunchAddressScan() }
            cta_button.setOnClickListener { onCtaClick() }

            wallet_select.apply {
                onLoadError = {
                    showErrorToast("Failed getting transfer wallets")
                    hideTransferList()
                }
                onAccountSelected = { accountSelected(it) }
                onEmptyList = { hideTransferList() }
            }
        }
    }

    private fun setupTransferList(account: CryptoAccount, state: SendState) {
        val accountFilter = customiser.targetAccountFilter(state)
        dialogView.wallet_select.initialise(
            coincore.canTransferTo(account)
                .map {
                    it.filter(accountFilter).map { it as BlockchainAccount }
                }
        )
    }

    private fun hideTransferList() {
        dialogView.title_pick.gone()
        dialogView.wallet_select.gone()
    }

    private fun accountSelected(account: BlockchainAccount) {
        require(account is SingleAccount)
        model.process(SendIntent.TargetSelectionConfirmed(account))
    }

    private fun onLaunchAddressScan() {
        QrScanHandler.requestScanPermissions(
            activity = requireActivity(),
            rootView = dialogView
        ) {
            QrScanHandler.startQrScanActivity(
                this,
                appUtil
                // this::class.simpleName ?: "Unknown"
            )
        }
    }

    private fun addressEntered(address: String, asset: CryptoCurrency) {
        model.process(SendIntent.ValidateInputTargetAddress(address, asset))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
        when (requestCode) {
            QrScanHandler.SCAN_URI_RESULT -> handleScanResult(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }

    private fun handleScanResult(resultCode: Int, data: Intent?) {
        Timber.d("Got QR scan result!")
        if (resultCode == Activity.RESULT_OK && data != null) {
            data.getStringExtra(CaptureActivity.SCAN_RESULT)?.let { rawScan ->
                disposables += QrScanHandler.processScan(rawScan, false)
                    .flatMapMaybe { QrScanHandler.selectAssetTargetFromScan(state.asset, it) }
                    .subscribeBy(
                        onSuccess = {
                            model.process(SendIntent.TargetSelectionConfirmed(it))
                        },
                        onComplete = {
                            ToastCustom.makeText(
                                requireContext(),
                                getText(R.string.scan_mismatch_transaction_target),
                                ToastCustom.LENGTH_SHORT,
                                ToastCustom.TYPE_GENERAL
                            )
                        },
                        onError = {
                            ToastCustom.makeText(
                                requireContext(),
                                getText(R.string.scan_failed),
                                ToastCustom.LENGTH_SHORT,
                                ToastCustom.TYPE_GENERAL
                            )
                        }
                    )
            }
        }
    }

    private fun onCtaClick() =
        model.process(SendIntent.TargetSelectionConfirmed(state.sendTarget))

    companion object {
        private const val NONCUSTODIAL_INPUT = 0
        private const val CUSTODIAL_INPUT = 1
    }
}
