package piuk.blockchain.android.ui.transfer.send.flow

import android.app.Activity
import android.content.Intent
import android.text.Editable
import android.view.View
import android.widget.TextView
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.dialog_send_address.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.isCustodial
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.transfer.send.FlowInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.assetName
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
                setupTransferList(newState.sendingAccount)
            }
            cta_button.isEnabled = newState.nextEnabled

            if (newState.sendingAccount.isCustodial()) {
                showCustodialInput(newState)
            } else {
                showNonCustodialInput(newState)
            }

            customiser.errorFlashMessage(newState)?.let {
                error_msg.apply {
                    text = it
                    visible()
                }
            } ?: error_msg.invisible()

            title.text = customiser.selectTargetAddressTitle(newState)
        }
        state = newState
    }

    private fun showNonCustodialInput(newState: SendState) {
        val address = if (newState.sendTarget is CryptoAddress) {
            newState.sendTarget.address
        } else {
            null
        }

        val asset = newState.asset

        with(dialogView) {
            address_entry.removeTextChangedListener(addressTextWatcher)
            address_entry.setText(address, TextView.BufferType.EDITABLE)
            address_entry.hint = getString(
                R.string.send_enter_asset_address_hint,
                getString(asset.assetName())
            )
            address_entry.addTextChangedListener(addressTextWatcher)

            input_switcher.displayedChild = NONCUSTODIAL_INPUT
        }
    }

    private fun showCustodialInput(newState: SendState) {
        with(dialogView) {
            internal_warning.text = getString(
                R.string.send_internal_transfer_message,
                newState.asset.displayTicker
            )

            title_pick.gone()
            input_switcher.displayedChild = CUSTODIAL_INPUT
        }
    }

    private val addressTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            val asset = state.asset
            val address = s.toString()

            addressEntered(address, asset)
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

    private fun setupTransferList(account: CryptoAccount) {
        dialogView.wallet_select.initialise(
            coincore.canTransferTo(account).map { it.map { it as BlockchainAccount } }
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
        if (!appUtil.isCameraOpen) {
            startActivityForResult(
                Intent(activity, CaptureActivity::class.java),
                SCAN_QR_ADDRESS
            )
        } else {
            showErrorToast(R.string.camera_unavailable)
        }
    }

    private fun addressEntered(address: String, asset: CryptoCurrency) {
        model.process(SendIntent.ValidateInputTargetAddress(address, asset))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) =
        when (requestCode) {
            SCAN_QR_ADDRESS -> handleScanResult(resultCode, data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }

    private fun handleScanResult(resultCode: Int, data: Intent?) {
        Timber.d("Got QR scan result!")
        if (resultCode == Activity.RESULT_OK && data != null) {
            data.getStringExtra(CaptureActivity.SCAN_RESULT)?.let {
                val asset = state.asset
                val address = it

                addressEntered(address, asset)
            }
        }
    }

    private fun onCtaClick() =
        model.process(SendIntent.TargetSelectionConfirmed(state.sendTarget))

    companion object {
        const val SCAN_QR_ADDRESS = 2985

        private const val NONCUSTODIAL_INPUT = 0
        private const val CUSTODIAL_INPUT = 1
    }
}
