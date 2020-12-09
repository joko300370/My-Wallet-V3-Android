package piuk.blockchain.android.simplebuy.yodlee

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.blockchain.ui.urllinks.YODLEE_LEARN_MORE
import com.blockchain.ui.urllinks.YODLEE_PP
import com.blockchain.ui.urllinks.YODLEE_TOS
import kotlinx.android.synthetic.main.fragment_simple_buy_yodlee_splash.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyNavigator
import piuk.blockchain.android.simplebuy.SimpleBuyScreen
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.StringUtils

class YodleeSplashFragment : Fragment(R.layout.fragment_simple_buy_yodlee_splash), SimpleBuyScreen {

    private val stringUtils: StringUtils by inject()

    private val fastLinkUrl: String by lazy {
        arguments?.getString(FAST_LINK_URL) ?: ""
    }

    private val accessToken: String by lazy {
        arguments?.getString(ACCESS_TOKEN) ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setupToolbar(R.string.link_a_bank)

        val learnMoreMap = mapOf<String, Uri>("yodlee_learn_more" to Uri.parse(YODLEE_LEARN_MORE))

        val tosAndPPMap = mapOf<String, Uri>(
            "yodlee_tos" to Uri.parse(YODLEE_TOS),
            "yodlee_pp" to Uri.parse(YODLEE_PP)
        )

        yodlee_splash_blurb.text =
            stringUtils.getStringWithMappedLinks(R.string.yodlee_splash_blurb, learnMoreMap, requireActivity())
        yodlee_splash_tos_pp.text =
            stringUtils.getStringWithMappedLinks(R.string.yodlee_splash_tos_pp, tosAndPPMap, requireActivity())

        yodlee_splash_cta.setOnClickListener {
            navigator().launchYodleeWebview(fastLinkUrl, accessToken)
        }
    }

    companion object {
        private const val FAST_LINK_URL: String = "FAST_LINK_URL"
        private const val ACCESS_TOKEN: String = "ACCESS_TOKEN"

        fun newInstance(fastLinkUrl: String, accessToken: String): YodleeSplashFragment =
            YodleeSplashFragment().apply {
                arguments = Bundle().apply {
                    putString(FAST_LINK_URL, fastLinkUrl)
                    putString(ACCESS_TOKEN, accessToken)
                }
            }
    }

    override fun navigator(): SimpleBuyNavigator =
        (activity as? SimpleBuyNavigator)
            ?: throw IllegalStateException("Parent must implement SimpleBuyNavigator")

    override fun onBackPressed(): Boolean = true
}