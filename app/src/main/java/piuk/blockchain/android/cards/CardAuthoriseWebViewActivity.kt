package piuk.blockchain.android.cards

import android.content.Intent
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.everypay_authorise_3ds.*
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.util.Locale

class CardAuthoriseWebViewActivity : AppCompatActivity() {

    private val exitLink by unsafeLazy {
        intent.getStringExtra(EXIT_LINK) ?: throw IllegalStateException("")
    }

    private val startAuthLink by unsafeLazy {
        intent.getStringExtra(AUTH_LINK) ?: throw IllegalStateException("")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.everypay_authorise_3ds)
        initWebView()
    }

    private fun initWebView() {
        val settings: WebSettings = webview.settings
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportMultipleWindows(true)
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        webview.loadUrl(startAuthLink)
        webview.webViewClient = WebClientImpl(exitLink) {
            val data = Intent()
            setResult(RESULT_OK, data)
            finish()
        }
    }

    private class WebClientImpl(private val exitLink: String, private val onBrowserFlowEnded: () -> Unit) :
        WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Timber.d("shouldOverrideUrlLoading: $url")
            if (isBrowserFlowEndUrl(url)) {
                onBrowserFlowEnded()
                return true
            }
            return super.shouldOverrideUrlLoading(view, url)
        }

        private fun isBrowserFlowEndUrl(url: String): Boolean =
            url.toLowerCase(Locale.ENGLISH).startsWith(exitLink)
    }

    companion object {
        private const val AUTH_LINK = "AUTH_LINK_KEY"
        private const val EXIT_LINK = "EXIT_LINK_KEY"

        fun start(fragment: Fragment, link: String, exitLink: String) {
            val intent = Intent(fragment.activity, CardAuthoriseWebViewActivity::class.java).apply {
                putExtra(AUTH_LINK, link)
                putExtra(EXIT_LINK, exitLink)
            }
            fragment.startActivityForResult(intent, CardVerificationFragment.EVERYPAY_AUTH_REQUEST_CODE)
        }
    }
}