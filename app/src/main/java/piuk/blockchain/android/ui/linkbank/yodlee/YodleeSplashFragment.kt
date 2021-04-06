package piuk.blockchain.android.ui.linkbank.yodlee

import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.fragment.app.Fragment
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.nabu.models.data.YodleeAttributes
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.ui.urllinks.YODLEE_LEARN_MORE
import kotlinx.android.synthetic.main.fragment_simple_buy_yodlee_splash.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.linkbank.BankAuthAnalytics
import piuk.blockchain.android.ui.linkbank.BankAuthFlowNavigator
import piuk.blockchain.android.ui.linkbank.bankAuthEvent
import piuk.blockchain.android.util.StringUtils

class YodleeSplashFragment : Fragment(R.layout.fragment_simple_buy_yodlee_splash) {

    private val stringUtils: StringUtils by inject()
    private val analytics: Analytics by inject()

    private val attributes: YodleeAttributes by lazy {
        arguments?.getSerializable(ATTRS_KEY) as YodleeAttributes
    }

    private val linkingBankId: String by lazy {
        arguments?.getString(LINKING_BANK_ID) ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val learnMoreMap = mapOf<String, Uri>("yodlee_learn_more" to Uri.parse(YODLEE_LEARN_MORE))

        yodlee_splash_blurb.movementMethod = LinkMovementMethod.getInstance()
        yodlee_splash_blurb.text =
            stringUtils.getStringWithMappedAnnotations(R.string.yodlee_splash_blurb, learnMoreMap, requireActivity())

        yodlee_splash_cta.setOnClickListener {
            analytics.logEvent(bankAuthEvent(BankAuthAnalytics.SPLASH_CTA, BankPartner.YODLEE))
            navigator().launchYodleeWebview(attributes, linkingBankId)
        }

        analytics.logEvent(bankAuthEvent(BankAuthAnalytics.SPLASH_SEEN, BankPartner.YODLEE))
    }

    companion object {
        private const val ATTRS_KEY: String = "ATTRS_KEY"
        private const val LINKING_BANK_ID: String = "LINKING_BANK_ID"

        fun newInstance(attributes: YodleeAttributes, bankId: String): YodleeSplashFragment =
            YodleeSplashFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(ATTRS_KEY, attributes)
                    putString(LINKING_BANK_ID, bankId)
                }
            }
    }

    private fun navigator(): BankAuthFlowNavigator =
        (activity as? BankAuthFlowNavigator)
        ?: throw IllegalStateException("Parent must implement BankAuthFlowNavigator")
}