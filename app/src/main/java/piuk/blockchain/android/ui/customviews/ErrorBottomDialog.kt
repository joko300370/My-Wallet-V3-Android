package piuk.blockchain.android.ui.customviews

import android.os.Bundle
import android.os.Parcelable
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.viewbinding.ViewBinding
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R

abstract class ErrorBottomDialog<E : ViewBinding> : BottomSheetDialogFragment() {

    @Parcelize
    data class
    Content(
        val title: CharSequence,
        val description: CharSequence = "",
        val descriptionToFormat: Pair<Int, String>? = null,
        @StringRes val ctaButtonText: Int = 0,
        @StringRes val dismissText: Int = 0,
        @DrawableRes val icon: Int
    ) : Parcelable

    val analytics: Analytics by inject()

    val clicksDisposable = CompositeDisposable()

    private var _binding: E? = null

    val binding: E
        get() = _binding!!

    private lateinit var content: Content

    var onCtaClick: () -> Unit = {}
    var onDismissClick: (() -> Unit) = { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analytics.logEvent(AnalyticsEvents.SwapErrorDialog)

        content = arguments?.getParcelable(ARG_CONTENT) ?: throw IllegalStateException("No content provided")
    }

    abstract fun initBinding(inflater: LayoutInflater, container: ViewGroup?): E

    abstract fun init(content: Content)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = layoutInflater.cloneInContext(contextThemeWrapper)
        _binding = initBinding(themedInflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        init(content)
    }

    override fun onPause() {
        clicksDisposable.clear()
        super.onPause()
    }

    companion object {
        private const val ARG_CONTENT = "arg_content"
    }
}
