package piuk.blockchain.android.ui.transfer.send.flow

import android.app.Activity
import android.content.Intent
import android.text.Editable
import android.view.View
import android.widget.TextView
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_send_address.view.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AddressFactory
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.CryptoSingleAccount
import piuk.blockchain.android.coincore.NullAddress
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.ui.transfer.send.SendInputSheet
import piuk.blockchain.android.ui.transfer.send.SendIntent
import piuk.blockchain.android.ui.transfer.send.SendState
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import timber.log.Timber

class EnterTargetAddressSheet : SendInputSheet() {
    override val layoutResource: Int = R.layout.dialog_send_address

    private val appUtil: AppUtil by inject()
    private val coincore: Coincore by scopedInject()
    private val addressFactory: AddressFactory by scopedInject()

    private val disposables = CompositeDisposable()
    private var state: SendState = SendState()

    override fun render(newState: SendState) {
        Timber.d("!SEND!> Rendering! EnterTargetAddressSheet")

        with(dialogView) {
            if (state.sendingAccount != newState.sendingAccount) {
                from_details.account = newState.sendingAccount
                setupTransferList(newState.sendingAccount)
            }
            cta_button.isEnabled = newState.nextEnabled

            if (newState.targetAddress != NullAddress &&
                newState.targetAddress.label.isNotEmpty() &&
                address_entry.text?.isEmpty() == true) {
                    address_entry.setText(newState.targetAddress.label, TextView.BufferType.EDITABLE)
                    addressSelected(newState.targetAddress)
            }
        }
        state = newState
    }

    // TODO: THis address processing should occur viw the interactor
    private val addressTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            val address = addressFactory.parse(s.toString(), state.sendingAccount.asset)
            if (address != null) {
                addressSelected(address)
                dialogView.error_msg.invisible()
            } else {
                dialogView.error_msg.visible()
            }
        }
    }

    override fun initControls(view: View) {
        with(dialogView) {
            address_entry.addTextChangedListener(addressTextWatcher)
            btn_scan.setOnClickListener { onLaunchAddressScan() }
            cta_button.setOnClickListener { onCtaClick() }

            wallet_select.apply {
                onLoadError = { showErrorToast("Failed getting transfer wallets") }
                onAccountSelected = { accountSelected(it) }
                onEmptyList = { hideTransferList() }
            }
        }
    }

    private fun setupTransferList(account: CryptoSingleAccount) {
        dialogView.wallet_select.initialise(
            coincore[account.asset].canTransferTo(account)
        )
    }

    private fun hideTransferList() {
        dialogView.title_pick.gone()
        dialogView.wallet_select.gone()
    }

    private fun accountSelected(account: CryptoAccount) {
        if (account is CryptoSingleAccount) {
            disposables += account.receiveAddress
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                onSuccess = {
                    if (it is CryptoAddress) {
                        dialogView.address_entry.setText(it.address)
                    } else {
                        Timber.e("!SEND!> Unsupported address type selected")
                    }
                }
            )
        }
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

    private fun addressSelected(address: ReceiveAddress) {
        model.process(SendIntent.AddressSelected(address))
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
                // Just supporting ETH in this pass,
                // TODO: Replace this with a full scanning parser?
                val address = addressFactory.parse(it, CryptoCurrency.ETHER)
                if (address == null) {
                    showErrorToast("Invalid ETH address!!")
                } else {
                    addressSelected(address)
                }
            }
        }
    }

    private fun onCtaClick() =
        model.process(SendIntent.AddressSelectionConfirmed)

    companion object {
        const val SCAN_QR_ADDRESS = 2985
        fun newInstance(): EnterTargetAddressSheet =
            EnterTargetAddressSheet()
    }
}
