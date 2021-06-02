package piuk.blockchain.android.ui.kyc.resubmission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.ui.extensions.throttledClicks
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycResubmissionSplashBinding
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.reentry.KycNavigator
import timber.log.Timber

class KycResubmissionSplashFragment : Fragment() {

    private var _binding: FragmentKycResubmissionSplashBinding? = null
    private val binding: FragmentKycResubmissionSplashBinding
        get() = _binding!!

    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )

    private val kycNavigator: KycNavigator by scopedInject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycResubmissionSplashBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycResubmission)

        progressListener.setHostTitle(R.string.kyc_resubmission_splash_title)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private val disposable = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        disposable += binding.buttonKycResubmissionSplashNext
            .throttledClicks()
            .flatMapSingle { kycNavigator.findNextStep() }
            .subscribeBy(
                onNext = { navigate(it) },
                onError = { Timber.e(it) }
            )
    }

    override fun onPause() {
        disposable.clear()
        super.onPause()
    }
}
