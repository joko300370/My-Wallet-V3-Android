package piuk.blockchain.android.ui.addresses

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import info.blockchain.balance.CryptoCurrency
import org.koin.core.KoinComponent
import org.koin.core.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.databinding.ViewExpandingCurrencyHeaderBinding
import piuk.blockchain.android.util.invisible
import piuk.blockchain.android.util.setAnimationListener
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy

class ExpandableCurrencyHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs), KoinComponent {

    private lateinit var selectionListener: (CryptoCurrency) -> Unit

    private val analytics: Analytics by inject()
    private val assetResources: AssetResources by scopedInject()
    private val coincore: Coincore by scopedInject()

    private var expanded = false
    private var firstOpen = true
    private var collapsedHeight: Int = 0
    private var contentHeight: Int = 0
    private var contentWidth: Int = 0
    private var selectedCurrency = CryptoCurrency.BTC
    private val arrowDrawable: Drawable? by unsafeLazy {
        VectorDrawableCompat.create(
            resources,
            R.drawable.vector_expand_more,
            ContextThemeWrapper(context, R.style.AppTheme).theme
        )?.run {
            DrawableCompat.wrap(this)
        }
    }

    private val binding: ViewExpandingCurrencyHeaderBinding by lazy {
        ViewExpandingCurrencyHeaderBinding.inflate(LayoutInflater.from(context), this, true)
    }

    init {
        // Inflate layout
        coincore.cryptoAssets
            .map { it.asset }
            .filter { it.hasFeature(CryptoCurrency.MULTI_WALLET) }
            .forEach { currency ->
                textView(currency)?.apply {
                    setOnClickListener { closeLayout(currency) }
                }
            }
        binding.textviewSelectedCurrency.apply {
            // Hide selector on first load
            invisible()
            setCompoundDrawablesWithIntrinsicBounds(null, null, arrowDrawable, null)
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        with(binding) {
            linearLayoutCoinSelection.invisible()
            textviewSelectedCurrency.setOnClickListener { animateLayout(true) }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        with(binding) {
            contentFrame.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            textviewSelectedCurrency.measure(MeasureSpec.UNSPECIFIED, heightMeasureSpec)
            collapsedHeight = textviewSelectedCurrency.measuredHeight
            contentWidth = contentFrame.measuredWidth + textviewSelectedCurrency.measuredWidth
            contentHeight = contentFrame.measuredHeight

            super.onMeasure(widthMeasureSpec, heightMeasureSpec)

            if (firstOpen) {
                contentFrame.layoutParams.width = contentWidth
                contentFrame.layoutParams.height = collapsedHeight
                firstOpen = false
            }

            val width = textviewSelectedCurrency.measuredWidth + contentFrame.measuredWidth
            val height = contentFrame.measuredHeight

            setMeasuredDimension(width, height)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        outlineProvider = CustomOutline(w, h)
    }

    fun setSelectionListener(selectionListener: (CryptoCurrency) -> Unit) {
        this.selectionListener = selectionListener
    }

    fun setCurrentlySelectedCurrency(cryptoCurrency: CryptoCurrency) {
        selectedCurrency = cryptoCurrency
        updateCurrencyUi(selectedCurrency)
    }

    private fun textView(cryptoCurrency: CryptoCurrency): TextView? =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> binding.textviewBitcoin
            CryptoCurrency.BCH -> binding.textviewBitcoinCash
            else -> null
        }

    fun isOpen() = expanded

    fun close() {
        if (isOpen()) closeLayout(null)
    }

    private fun animateLayout(expanding: Boolean) {
        with(binding) {
            if (expanding) {
                textviewSelectedCurrency.setOnClickListener(null)
                val animation = AlphaAnimation(1.0f, 0.0f).apply { duration = 250 }
                textviewSelectedCurrency.startAnimation(animation)
                animation.setAnimationListener {
                    onAnimationEnd {
                        textviewSelectedCurrency.alpha = 0.0f
                        startContentAnimation()
                    }
                }
            } else {
                textviewSelectedCurrency.setOnClickListener { animateLayout(true) }
                startContentAnimation()
            }
        }
    }

    private fun startContentAnimation() {
        val animation: Animation = if (expanded) {
            binding.linearLayoutCoinSelection.invisible()
            ExpandAnimation(contentHeight, collapsedHeight)
        } else {
            this@ExpandableCurrencyHeader.invalidate()
            ExpandAnimation(collapsedHeight, contentHeight)
        }

        animation.duration = 250
        animation.setAnimationListener {
            onAnimationEnd {
                expanded = !expanded
                if (expanded) {
                    binding.linearLayoutCoinSelection.visible()
                }
                if (expanded) {
                    analytics.logEvent(AnalyticsEvents.OpenAssetsSelector)
                } else {
                    analytics.logEvent(AnalyticsEvents.CloseAssetsSelector)
                }
            }
        }

        binding.contentFrame.startAnimation(animation)
    }

    private fun updateCurrencyUi(asset: CryptoCurrency) {
        binding.textviewSelectedCurrency.run {
            val title = resources.getString(assetResources.assetNameRes(asset))
            text = title.toUpperCase()

            setCompoundDrawablesWithIntrinsicBounds(
                AppCompatResources.getDrawable(context, assetResources.coinIconWhite(asset)),
                null,
                arrowDrawable,
                null
            )
            visible()
        }
    }

    /**
     * Pass null as the parameter here to close the view without triggering any [CryptoCurrency]
     * change listeners.
     */
    private fun closeLayout(cryptoCurrency: CryptoCurrency?) {
        // Update UI
        cryptoCurrency?.run { setCurrentlySelectedCurrency(this) }
        // Trigger layout change
        animateLayout(false)
        // Fade in title
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f).apply { duration = 250 }
        binding.textviewSelectedCurrency.startAnimation(alphaAnimation)
        alphaAnimation.setAnimationListener {
            onAnimationEnd {
                binding.textviewSelectedCurrency.alpha = 1.0f
                // Inform parent of currency selection once animation complete to avoid glitches
                cryptoCurrency?.run { selectionListener(this) }
            }
        }
    }

    fun getSelectedCurrency() = selectedCurrency

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private inner class CustomOutline constructor(
        var width: Int,
        var height: Int
    ) : ViewOutlineProvider() {

        override fun getOutline(view: View, outline: Outline) {
            outline.setRect(0, 0, width, height)
        }
    }

    private inner class ExpandAnimation(private val startHeight: Int, endHeight: Int) :
        Animation() {

        private val deltaHeight: Int = endHeight - startHeight

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val params = binding.contentFrame.layoutParams
            params.height = (startHeight + deltaHeight * interpolatedTime).toInt()
            binding.contentFrame.layoutParams = params
        }

        override fun willChangeBounds(): Boolean = true
    }

    fun isTouchOutside(event: MotionEvent): Boolean {
        val viewRect = Rect()
        getGlobalVisibleRect(viewRect)
        return !viewRect.contains(event.rawX.toInt(), event.rawY.toInt())
    }
}