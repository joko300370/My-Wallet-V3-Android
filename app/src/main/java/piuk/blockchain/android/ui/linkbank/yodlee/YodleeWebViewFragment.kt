package piuk.blockchain.android.ui.linkbank.yodlee

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.blockchain.nabu.models.data.YodleeAttributes
import com.blockchain.notifications.analytics.Analytics
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.android.synthetic.main.fragment_yodlee_webview.*
import org.json.JSONException
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible
import timber.log.Timber
import java.net.URLEncoder

class YodleeWebViewFragment : Fragment(R.layout.fragment_yodlee_webview), FastLinkInterfaceHandler.FastLinkListener,
    YodleeWebClient.YodleeWebClientInterface {

    private val analytics: Analytics by inject()

    private val attributes: YodleeAttributes by lazy {
        arguments?.getSerializable(ATTRIBUTES) as YodleeAttributes
    }

    private val linkingBankId: String by lazy {
        arguments?.getString(LINKING_BANK_ID) ?: ""
    }

    private val accessTokenKey = "accessToken"
    private val bearerParam: String by lazy { "Bearer ${attributes.token}" }
    private val extraParamsKey = "extraParams"
    private val extraParamConfigName: String
        get() = "configName=${attributes.configName}"
    private val extraParamEncoding = "UTF-8"

    private val yodleeQuery: String by lazy {
        Uri.Builder()
            .appendQueryParameter(accessTokenKey, bearerParam)
            .appendQueryParameter(extraParamsKey, URLEncoder.encode(extraParamConfigName, extraParamEncoding))
            .build().query ?: ""
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setupToolbar(R.string.link_a_bank)

        setupWebView()
        yodlee_retry.setOnClickListener {
            loadYodlee()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings: WebSettings = yodlee_webview.settings
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }

        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        yodlee_webview.webViewClient = YodleeWebClient(this)
        yodlee_webview.addJavascriptInterface(FastLinkInterfaceHandler(this), "YWebViewHandler")
    }

    override fun onResume() {
        super.onResume()
        loadYodlee()
    }

    private fun loadYodlee() {
        requireActivity().runOnUiThread {
            yodlee_loading_group.visible()
            yodlee_webview.clearCache(true)
            yodlee_webview.gone()
            yodlee_retry.gone()

            yodlee_webview.postUrl(attributes.fastlinkUrl, yodleeQuery.toByteArray())
        }
    }

    override fun flowSuccess(providerAccountId: String, accountId: String) {
        analytics.logEvent(SimpleBuyAnalytics.ACH_SUCCESS)
        navigator().launchBankLinking(
            accountProviderId = providerAccountId,
            accountId = accountId,
            bankId = linkingBankId
        )
    }

    override fun flowError(error: FastLinkInterfaceHandler.FastLinkFlowError, reason: String?) {
        requireActivity().runOnUiThread {
            when (error) {
                FastLinkInterfaceHandler.FastLinkFlowError.FLOW_QUIT_BY_USER -> {
                    analytics.logEvent(SimpleBuyAnalytics.ACH_CLOSE)
                    yodlee_webview.gone()
                    yodlee_status_label.gone()
                    yodlee_subtitle.gone()
                    yodlee_retry.gone()
                    navigator().retry()
                }
                FastLinkInterfaceHandler.FastLinkFlowError.JSON_PARSING -> {
                    analytics.logEvent(SimpleBuyAnalytics.ACH_ERROR)
                    showError(getString(R.string.yodlee_parsing_error))
                }
                FastLinkInterfaceHandler.FastLinkFlowError.OTHER -> {
                    analytics.logEvent(SimpleBuyAnalytics.ACH_ERROR)
                    showError(getString(R.string.yodlee_unexpected_error))
                }
            }
        }
    }

    private fun showError(errorText: String) {
        yodlee_webview.gone()
        yodlee_status_label.text = errorText
        yodlee_subtitle.gone()
        yodlee_retry.visible()
        yodlee_retry.setOnClickListener { loadYodlee() }
    }

    override fun openExternalUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        requireContext().startActivity(intent)
    }

    override fun pageFinishedLoading() {
        yodlee_webview.visible()
        yodlee_loading_group.gone()
    }

    private fun navigator(): YodleeLinkingFlowNavigator =
        (activity as? YodleeLinkingFlowNavigator)
        ?: throw IllegalStateException("Parent must implement YodleeLinkingFlowNavigator")

    companion object {
        private const val ATTRIBUTES: String = "ATTRIBUTES"
        private const val LINKING_BANK_ID: String = "LINKING_BANK_ID"

        fun newInstance(
            attributes: YodleeAttributes,
            bankId: String
        ): YodleeWebViewFragment = YodleeWebViewFragment().apply {
            arguments = Bundle().apply {
                putSerializable(ATTRIBUTES, attributes)
                putString(LINKING_BANK_ID, bankId)
            }
        }
    }
}

class YodleeWebClient(private val listener: YodleeWebClientInterface) : WebViewClient() {
    interface YodleeWebClientInterface {
        fun pageFinishedLoading()
    }

    override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
        if (BuildConfig.DEBUG) {
            Timber.e("Yodlee SSL error: $error")
            handler?.proceed()
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        listener.pageFinishedLoading()
    }

    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        view.loadUrl(url)
        return true
    }
}

class FastLinkInterfaceHandler(private val listener: FastLinkListener) {
    private val gson = Gson()

    interface FastLinkListener {
        fun flowSuccess(providerAccountId: String, accountId: String)
        fun flowError(error: FastLinkFlowError, reason: String? = null)
        fun openExternalUrl(url: String)
    }

    enum class FastLinkFlowError {
        JSON_PARSING,
        FLOW_QUIT_BY_USER,
        OTHER
    }

    @JavascriptInterface
    fun postMessage(data: String?) {
        try {
            val message = gson.fromJson(data, FastLinkMessage::class.java)

            when (message.type) {
                MessageType.POST_MESSAGE -> {
                    handlePostMessage(message)
                }
                MessageType.OPEN_EXTERNAL_URL -> {
                    message.data.externalUrl?.let {
                        listener.openExternalUrl(it)
                    } ?: listener.flowError(FastLinkFlowError.OTHER)
                }
            }
        } catch (e: JSONException) {
            listener.flowError(FastLinkFlowError.JSON_PARSING)
        }
    }

    private fun handlePostMessage(message: FastLinkMessage) {
        with(message.data) {
            if (action != null) {
                when (action) {
                    MessageAction.EXIT -> {
                        handleExitAction()
                    }
                }
            } else {
                // TODO no action but we may already have providerAccountId
            }
        }
    }

    private fun MessageData.handleExitAction() {
        when {
            status != null -> {
                handleMessageStatus(status, reason)
            }
            sites?.isNotEmpty() == true -> {
                val site = sites[0]
                site.status?.let {
                    handleMessageStatusFromSite(it, site)
                } ?: listener.flowError(FastLinkFlowError.OTHER)
            }
            else -> {
                listener.flowError(FastLinkFlowError.OTHER)
            }
        }
    }

    private fun MessageData.handleMessageStatus(status: MessageStatus, reason: String?) {
        when (status) {
            MessageStatus.FLOW_SUCCESS -> {
                listener.flowSuccess(providerAccountId = providerAccountId, accountId = accountId)
            }
            MessageStatus.FLOW_ABANDONED,
            MessageStatus.FLOW_CLOSED,
            MessageStatus.FLOW_FAILED -> {
                listener.flowError(FastLinkFlowError.FLOW_QUIT_BY_USER, reason)
            }
        }
    }

    private fun handleMessageStatusFromSite(status: MessageStatus, site: SiteData) {
        when (status) {
            MessageStatus.FLOW_SUCCESS -> {
                val pId = site.providerAccountId ?: kotlin.run {
                    listener.flowError(FastLinkFlowError.OTHER, "Provide ID not found")
                    return
                }
                val acId = site.accountId ?: kotlin.run {
                    listener.flowError(FastLinkFlowError.OTHER, "Account ID not found")
                    return
                }
                listener.flowSuccess(accountId = acId, providerAccountId = pId)
            }
            MessageStatus.FLOW_ABANDONED,
            MessageStatus.FLOW_CLOSED,
            MessageStatus.FLOW_FAILED -> {
                listener.flowError(FastLinkFlowError.FLOW_QUIT_BY_USER, site.reason)
            }
        }
    }

    private data class FastLinkMessage(val type: MessageType, val data: MessageData)

    private enum class MessageType {
        POST_MESSAGE,
        OPEN_EXTERNAL_URL
    }

    private enum class MessageStatus {
        @SerializedName("SUCCESS")
        FLOW_SUCCESS,

        @SerializedName("ACTION_ABANDONED")
        FLOW_ABANDONED,

        @SerializedName("USER_CLOSE_ACTION")
        FLOW_CLOSED,

        @SerializedName("FAILED")
        FLOW_FAILED
    }

    private enum class MessageAction {
        @SerializedName("exit")
        EXIT
    }

    private data class MessageData(
        val action: MessageAction?,
        val status: MessageStatus?,
        val sites: List<SiteData>?,
        val providerAccountId: String,
        val accountId: String,
        val providerName: String?,
        val additionalStatus: String?,
        @SerializedName("url")
        val externalUrl: String?,
        val reason: String?
    )

    private data class SiteData(
        val reason: String?,
        val status: MessageStatus?,
        val providerId: String?,
        val providerAccountId: String?,
        val accountId: String?,
        val providerName: String?
    )
}