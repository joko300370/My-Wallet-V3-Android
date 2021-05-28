package piuk.blockchain.android.ui.transfer.receive

import android.os.Bundle
import android.view.View
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.ui.customviews.account.DefaultCellDecorator
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalyticsAccountType
import piuk.blockchain.android.ui.transfer.AccountSelectorFragment
import piuk.blockchain.android.ui.transfer.analytics.TransferAnalyticsEvent
import piuk.blockchain.android.ui.upsell.KycUpgradePromptManager

class TransferReceiveFragment : AccountSelectorFragment() {

    private val disposables = CompositeDisposable()
    private val upsellManager: KycUpgradePromptManager by scopedInject()
    private val analytics: Analytics by inject()

    override val fragmentAction: AssetAction
        get() = AssetAction.Receive

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setEmptyStateDetails(
            R.string.common_empty_title,
            R.string.common_empty_details, R.string.common_empty_cta
        ) {
            refreshItems()
        }

        initialiseAccountSelectorWithHeader(
            statusDecorator = {
                DefaultCellDecorator()
            },
            onAccountSelected = ::doOnAccountSelected,
            title = R.string.transfer_receive_crypto_title,
            label = R.string.transfer_receive_crypto_label,
            icon = R.drawable.ic_receive_blue_circle
        )
    }

    override fun onDetach() {
        super.onDetach()
        disposables.dispose()
    }

    private fun doOnAccountSelected(account: BlockchainAccount) {
        require(account is CryptoAccount)
        disposables += upsellManager.queryUpsell(AssetAction.Receive, account)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { type ->
                if (type == KycUpgradePromptManager.Type.NONE) {
                    ReceiveSheet.newInstance(account).show(childFragmentManager, BOTTOM_SHEET)
                } else {
                    KycUpgradePromptManager.getUpsellSheet(type).show(childFragmentManager, BOTTOM_SHEET)
                }
                analytics.logEvent(
                    TransferAnalyticsEvent.ReceiveAccountSelected(
                        TxFlowAnalyticsAccountType.fromAccount(account),
                        account.asset.networkTicker
                    )
                )
            }
    }

    companion object {
        const val BOTTOM_SHEET = "BOTTOM_SHEET"

        fun newInstance() = TransferReceiveFragment()
    }
}
