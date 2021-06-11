package piuk.blockchain.android.ui.kyc.address

import android.content.Intent
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.extensions.nextAfterOrNull
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.KYCAnalyticsEvents
import com.blockchain.notifications.analytics.logEvent
import com.blockchain.ui.extensions.throttledClicks
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.jakewharton.rx.replayingShare
import com.jakewharton.rxbinding2.widget.afterTextChangeEvents
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.koin.android.ext.android.inject
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycHomeAddressBinding
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.dialogs.MaterialProgressDialog
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.address.models.AddressDialog
import piuk.blockchain.android.ui.kyc.address.models.AddressIntent
import piuk.blockchain.android.ui.kyc.address.models.AddressModel
import piuk.blockchain.android.ui.kyc.extensions.skipFirstUnless
import piuk.blockchain.android.ui.kyc.navhost.KycNavHostActivity
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navhost.models.KycStep
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.gone
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit

class KycHomeAddressFragment : BaseMvpFragment<KycHomeAddressView, KycHomeAddressPresenter>(),
    KycHomeAddressView {

    private var _binding: FragmentKycHomeAddressBinding? = null
    private val binding: FragmentKycHomeAddressBinding
        get() = _binding!!

    private val presenter: KycHomeAddressPresenter by scopedInject()
    private val analytics: Analytics by inject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )
    private val compositeDisposable = CompositeDisposable()
    private var progressDialog: MaterialProgressDialog? = null
    override val profileModel: ProfileModel by unsafeLazy {
        KycHomeAddressFragmentArgs.fromBundle(arguments ?: Bundle()).profileModel
    }
    private val initialState by unsafeLazy {
        AddressModel(
            "",
            null,
            "",
            profileModel.stateCode ?: "",
            "",
            profileModel.countryCode
        )
    }
    private val addressSubject = PublishSubject.create<AddressIntent>()
    override val address: Observable<AddressModel> by unsafeLazy {
        AddressDialog(addressSubject, initialState).viewModel
            .replayingShare()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycHomeAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycAddress)
        progressListener.setHostTitle(R.string.kyc_address_title)

        setupImeOptions()
        localiseUi()

        onViewReady()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @Suppress("ConstantConditionIf")
    override fun continueToVeriffSplash(countryCode: String) {
        closeKeyboard()
        navigate(KycNavXmlDirections.actionStartVeriff(countryCode))
    }

    override fun tier1Complete() {
        closeKeyboard()
        activity?.setResult(KycNavHostActivity.RESULT_KYC_FOR_TIER_COMPLETE)
        activity?.finish()
    }

    override fun onSddVerified() {
        activity?.setResult(KycNavHostActivity.RESULT_KYC_FOR_SDD_COMPLETE)
        activity?.finish()
    }

    override fun continueToTier2MoreInfoNeeded(countryCode: String) {
        closeKeyboard()
        navigate(KycNavXmlDirections.actionStartTier2NeedMoreInfo(countryCode))
    }

    override fun restoreUiState(
        line1: String,
        line2: String?,
        city: String,
        state: String?,
        postCode: String,
        countryName: String
    ) {
        with(binding) {
            editTextKycAddressFirstLine.setText(line1)
            editTextKycAddressAptName.setText(line2)
            editTextKycAddressCity.setText(city)
            editTextKycAddressState.setText(state)
            editTextKycAddressZipCode.setText(postCode)
            editTextKycAddressCountry.setText(countryName)
        }
    }

    private fun startPlacesActivityForResult() {
        val typeFilter = AutocompleteFilter.Builder()
            .setCountry(address.blockingFirst().country)
            .build()

        PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_FULLSCREEN)
            .setFilter(typeFilter)
            .build(requireActivity())
            .run { startActivityForResult(this, REQUEST_CODE_PLACE_AUTOCOMPLETE) }
    }

    private fun showRecoverableErrorDialog() {
        GoogleApiAvailability.getInstance()
            .getErrorDialog(
                requireActivity(),
                GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                    context
                ),
                REQUEST_CODE_PLAY_SERVICES_RESOLUTION
            )
            .show()
    }

    private fun showUnrecoverableErrorDialog() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.kyc_address_google_not_available)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PLACE_AUTOCOMPLETE) {
            when (resultCode) {
                RESULT_CANCELED -> Unit
                RESULT_OK -> updateAddress(data)
                PlaceAutocomplete.RESULT_ERROR -> logPlacesError(data)
            }
        }
    }

    private fun logPlacesError(data: Intent?) {
        val status = PlaceAutocomplete.getStatus(requireActivity(), data)
        Timber.e("${status.statusMessage}")
        toast(R.string.kyc_address_error_loading_places, ToastCustom.TYPE_ERROR)
    }

    private fun updateAddress(data: Intent?) {
        subscribeToViewObservables()
        try {
            val place = PlaceAutocomplete.getPlace(requireActivity(), data)
            val address =
                Geocoder(context, Locale.getDefault())
                    .getFromLocation(place.latLng.latitude, place.latLng.longitude, 1)
                    ?.firstOrNull()

            if (address != null) {
                with(binding) {
                    editTextKycAddressFirstLine.setText(address.thoroughfare ?: address.subThoroughfare)
                    editTextKycAddressAptName.setText(address.featureName)
                    editTextKycAddressCity.setText(address.locality ?: address.subAdminArea)
                    editTextKycAddressState.setText(address.adminArea)
                    editTextKycAddressZipCode.setText(address.postalCode)
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeToViewObservables()
        analytics.logEvent(KYCAnalyticsEvents.AddressScreenSeen)
    }

    private fun subscribeToViewObservables() {
        if (compositeDisposable.size() == 0) {
            with(binding) {
                compositeDisposable += buttonKycAddressNext
                    .throttledClicks()
                    .subscribeBy(
                        onNext = {
                            presenter.onContinueClicked(progressListener.campaignType)
                            analytics.logEvent(KYCAnalyticsEvents.AddressChanged)
                        },
                        onError = { Timber.e(it) }
                    )

                compositeDisposable += editTextKycAddressFirstLine
                    .onDelayedChange(KycStep.AddressFirstLine)
                    .doOnNext { addressSubject.onNext(AddressIntent.FirstLine(it)) }
                    .subscribe()
                compositeDisposable += editTextKycAddressAptName
                    .onDelayedChange(KycStep.AptNameOrNumber)
                    .doOnNext { addressSubject.onNext(AddressIntent.SecondLine(it)) }
                    .subscribe()
                compositeDisposable += editTextKycAddressCity
                    .onDelayedChange(KycStep.City)
                    .doOnNext { addressSubject.onNext(AddressIntent.City(it)) }
                    .subscribe()

                compositeDisposable += editTextKycAddressZipCode
                    .onDelayedChange(KycStep.ZipCode)
                    .doOnNext { addressSubject.onNext(AddressIntent.PostCode(it)) }
                    .subscribe()

                addressSubject.onNext(AddressIntent.State(profileModel.stateCode ?: ""))

                compositeDisposable += editTextKycAddressState
                    .onDelayedChange(KycStep.State)
                    .filter { !profileModel.isInUs() }
                    .doOnNext { addressSubject.onNext(AddressIntent.State(it)) }
                    .subscribe()

                compositeDisposable += searchViewKycAddress.getEditText()
                    .apply { isFocusable = false }
                    .throttledClicks()
                    .subscribeBy(
                        onNext = {
                            try {
                                startPlacesActivityForResult()
                            } catch (e: GooglePlayServicesRepairableException) {
                                showRecoverableErrorDialog()
                            } catch (e: GooglePlayServicesNotAvailableException) {
                                showUnrecoverableErrorDialog()
                            }
                        }
                    )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun finishPage() {
        findNavController(this).popBackStack()
    }

    override fun setButtonEnabled(enabled: Boolean) {
        binding.buttonKycAddressNext.isEnabled = enabled
    }

    override fun showErrorToast(message: Int) {
        toast(message, ToastCustom.TYPE_ERROR)
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(requireContext()).apply {
            setOnCancelListener { presenter.onProgressCancelled() }
            setMessage(R.string.kyc_country_selection_please_wait)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    private fun ProfileModel.isInUs() =
        countryCode.equals("US", ignoreCase = true)

    private fun localiseUi() {
        with(binding) {
            if (profileModel.isInUs()) {
                searchViewKycAddress.queryHint = getString(
                    R.string.kyc_address_search_hint,
                    getString(R.string.kyc_address_search_hint_zipcode)
                )
                inputLayoutKycAddressFirstLine.hint = getString(R.string.kyc_address_address_line_1)
                inputLayoutKycAddressAptName.hint = getString(R.string.kyc_address_address_line_2)
                inputLayoutKycAddressCity.hint = getString(R.string.kyc_address_address_city_hint)
                inputLayoutKycAddressState.hint = getString(R.string.kyc_address_address_state_hint)
                inputLayoutKycAddressZipCode.hint = getString(R.string.kyc_address_address_zip_code_hint_1)
                inputLayoutKycAddressState.editText?.isEnabled = false
            } else {
                searchViewKycAddress.queryHint = getString(
                    R.string.kyc_address_search_hint,
                    getString(R.string.kyc_address_search_hint_postcode)
                )
                inputLayoutKycAddressFirstLine.hint = getString(R.string.kyc_address_address_line_1)
                inputLayoutKycAddressAptName.hint = getString(R.string.kyc_address_address_line_2)
                inputLayoutKycAddressCity.hint = getString(R.string.address_city)
                inputLayoutKycAddressState.gone()
                inputLayoutKycAddressZipCode.hint = getString(R.string.kyc_address_postal_code)
                inputLayoutKycAddressState.editText?.isEnabled = true
            }

            editTextKycAddressCountry.setText(
                Locale(
                    Locale.getDefault().displayLanguage,
                    profileModel.countryCode
                ).displayCountry
            )

            editTextKycAddressState.setText(
                profileModel.stateName ?: ""
            )
        }
    }

    private fun TextView.onDelayedChange(kycStep: KycStep): Observable<String> =
        this.afterTextChangeEvents()
            .debounce(300, TimeUnit.MILLISECONDS)
            .map { it.editable()?.toString() ?: "" }
            .skipFirstUnless { !it.isEmpty() }
            .observeOn(AndroidSchedulers.mainThread())
            .distinctUntilChanged()

    private fun setupImeOptions() {
        with(binding) {
            val editTexts = listOf(
                editTextKycAddressFirstLine,
                editTextKycAddressAptName,
                editTextKycAddressCity,
                editTextKycAddressState,
                editTextKycAddressZipCode
            )

            editTexts.forEach { editText ->
                editText.setOnEditorActionListener { _, i, _ ->
                    consume {
                        when (i) {
                            EditorInfo.IME_ACTION_NEXT ->
                                editTexts.nextAfterOrNull { it === editText }?.requestFocus()
                            EditorInfo.IME_ACTION_DONE ->
                                closeKeyboard()
                        }
                    }
                }
            }
        }
    }

    private fun closeKeyboard() {
        (requireActivity() as? AppCompatActivity)?.let {
            ViewUtils.hideKeyboard(it)
        }
    }

    override fun createPresenter(): KycHomeAddressPresenter = presenter

    override fun getMvpView(): KycHomeAddressView = this

    private fun SearchView.getEditText(): EditText = this.findViewById(R.id.search_src_text)

    companion object {

        private const val REQUEST_CODE_PLACE_AUTOCOMPLETE = 707
        private const val REQUEST_CODE_PLAY_SERVICES_RESOLUTION = 708
    }
}
