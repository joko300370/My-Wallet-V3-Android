package piuk.blockchain.android.ui.transfer.send

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.UiThread
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class TestSendContainerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_send_container)
    }



    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(
                Intent(ctx, TestSendContainerActivity::class.java)
            )
        }
    }
}

class TestSendFragment : MviFragment<SendModel, SendIntent, SendState>(),
    SendInputSheet.Host {

    override val model: SendModel by scopedInject()

    @UiThread
    override fun render(newState: SendState) {

//        // Update/show bottom sheet
//        if (this.state?.showAssetSheetFor != newState.showAssetSheetFor) {
//            showAssetSheet(newState.showAssetSheetFor)
//        } else {
//            if (this.state?.showDashboardSheet != newState.showDashboardSheet) {
//                showPromoSheet(newState)
//            }
//        }

    }

//    private fun showPromoSheet(state: DashboardState) {
//        showBottomSheet(
//            when (state.showDashboardSheet) {
//                DashboardSheet.STX_AIRDROP_COMPLETE -> AirdropStatusSheet.newInstance(
//                    blockstackCampaignName
//                )
//                DashboardSheet.CUSTODY_INTRO -> CustodyWalletIntroSheet.newInstance()
//                DashboardSheet.SIMPLE_BUY_PAYMENT -> BankDetailsBottomSheet.newInstance()
//                DashboardSheet.BACKUP_BEFORE_SEND -> ForceBackupForSendSheet.newInstance()
//                DashboardSheet.BASIC_WALLET_TRANSFER -> BasicTransferToWallet.newInstance(state.transferFundsCurrency!!)
//                DashboardSheet.SIMPLE_BUY_CANCEL_ORDER -> {
//                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_PROMPT)
//                    SimpleBuyCancelOrderBottomSheet.newInstance(true)
//                }
//                null -> null
//            }
//        )
//    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_dashboard)

    // SendInputSheet.Host
    override fun onSheetClosed() {
        model.process(ClearBottomSheet)
    }

    companion object {
        fun newInstance() = TestSendFragment()
    }
}


abstract class SendInputSheet : SlidingModalBottomDialog() {

    interface Host {
        fun onSheetClosed()
    }
}