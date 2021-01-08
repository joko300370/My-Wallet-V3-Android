package piuk.blockchain.android.ui.linkbank

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.nabu.models.data.LinkBankTransfer
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.ui.linkbank.yodlee.YodleeLinkingFlowNavigator

class LinkBankActivity : BlockchainActivity(), YodleeLinkingFlowNavigator {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_activity)
        setSupportActionBar(toolbar_general)
        if (savedInstanceState == null) {
            println(
                "bank ${intent.getSerializableExtra(LINK_BANK_TRANSFER_KEY)}"
            )
        }
    }

    override val alwaysDisableScreenshots: Boolean
        get() = false

    override fun launchYodleeSplash(fastLinkUrl: String, accessToken: String, configName: String) {
        TODO("Not yet implemented")
    }

    override fun launchYodleeWebview(fastLinkUrl: String, accessToken: String, configName: String) {
        TODO("Not yet implemented")
    }

    override fun linkBankWithPartner(bankTransfer: LinkBankTransfer) {
        TODO("Not yet implemented")
    }

    override fun launchBankLinking(accountProviderId: String, accountId: String) {
        TODO("Not yet implemented")
    }

    override fun launchBankLinkingWithError(errorState: ErrorState) {
        TODO("Not yet implemented")
    }

    override fun retry() {
        TODO("Not yet implemented")
    }

    override fun bankLinkingFinished() {
        TODO("Not yet implemented")
    }

    override fun bankLinkingCancelled() {
        TODO("Not yet implemented")
    }

    companion object {
        private const val LINK_BANK_TRANSFER_KEY = "LINK_BANK_TRANSFER_KEY"

        fun newInstance(linkBankTransfer: LinkBankTransfer, context: Context): Intent {
            val intent = Intent(context, LinkBankActivity::class.java)
            intent.putExtra(LINK_BANK_TRANSFER_KEY, linkBankTransfer)
            return intent
        }
    }
}