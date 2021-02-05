package piuk.blockchain.android.ui.pairingcode

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.ui.urllinks.WEB_WALLET_LOGIN_URI
import org.koin.android.ext.android.get
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.ActivityPairingCodeBinding
import piuk.blockchain.android.databinding.ToolbarGeneralBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

@Suppress("UNUSED_PARAMETER")
class PairingCodeActivity : BaseMvpActivity<PairingCodeView, PairingCodePresenter>(),
    PairingCodeView {

    private lateinit var binding: ActivityPairingCodeBinding

    private val presenter: PairingCodePresenter by scopedInject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPairingCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        get<Analytics>().logEvent(AnalyticsEvents.WebLogin)

        with(binding) {
            val toolbarBinding = ToolbarGeneralBinding.bind(root)
            setupToolbar(toolbarBinding.toolbarGeneral, R.string.pairing_code_log_in)
            pairingFirstStep.text = getString(R.string.pairing_code_instruction_1, WEB_WALLET_LOGIN_URI)
            buttonQrToggle.setOnClickListener { onClickQRToggle() }
        }
        onViewReady()
    }

    override fun onSupportNavigateUp(): Boolean =
        consume { onBackPressed() }

    override fun onQrLoaded(bitmap: Bitmap) {
        with(binding) {
            val width = resources.displayMetrics.widthPixels
            val height = width * bitmap.height / bitmap.width
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

            ivQr.setImageBitmap(scaledBitmap)
            ivQr.visible()
        }
    }

    override fun showError(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun showProgressSpinner() {
        binding.progressBar.visible()
    }

    override fun hideProgressSpinner() {
        binding.progressBar.gone()
    }

    override fun enforceFlagSecure() = true

    override fun createPresenter() = presenter

    override fun getView(): PairingCodeView = this

    private fun onClickQRToggle() {
        with(binding) {
            if (instructionLayout.visibility == View.VISIBLE) {
                // Show pairing QR
                instructionLayout.gone()
                buttonQrToggle.setText(R.string.pairing_code_hide_qr)
                qrLayout.visible()
                ivQr.gone()

                presenter.generatePairingQr()
            } else {
                // Hide pairing QR
                buttonQrToggle.setText(R.string.pairing_code_show_qr)
                qrLayout.gone()
                instructionLayout.visible()
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, PairingCodeActivity::class.java)
            context.startActivity(intent)
        }
    }
}