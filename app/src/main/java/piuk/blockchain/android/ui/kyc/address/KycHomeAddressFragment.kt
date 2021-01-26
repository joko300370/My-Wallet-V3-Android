package piuk.blockchain.android.ui.kyc.address

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.navigation.fragment.NavHostFragment.findNavController
import com.blockchain.koin.scopedInject
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvents
import com.blockchain.notifications.analytics.logEvent
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.places.AutocompleteFilter
import com.google.android.gms.location.places.ui.PlaceAutocomplete
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.jakewharton.rx.replayingShare
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.fragment_billing_address.*
import kotlinx.android.synthetic.main.fragment_kyc_home_address.*
import kotlinx.android.synthetic.main.fragment_kyc_home_address.btn_next
import kotlinx.android.synthetic.main.fragment_kyc_home_address.country_text
import kotlinx.android.synthetic.main.fragment_kyc_home_address.flag_icon
import kotlinx.android.synthetic.main.fragment_kyc_home_address.state
import kotlinx.android.synthetic.main.fragment_kyc_home_address.state_input
import org.koin.android.ext.android.inject
import piuk.blockchain.android.KycNavXmlDirections
import piuk.blockchain.android.R
import piuk.blockchain.android.cards.CountryPickerItem
import piuk.blockchain.android.cards.PickerItem
import piuk.blockchain.android.cards.PickerItemListener
import piuk.blockchain.android.cards.SearchPickerItemBottomSheet
import piuk.blockchain.android.cards.StatePickerItem
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.customviews.dialogs.MaterialProgressDialog
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.address.models.AddressDialog
import piuk.blockchain.android.ui.kyc.address.models.AddressIntent
import piuk.blockchain.android.ui.kyc.address.models.AddressModel
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.android.ui.kyc.navhost.models.KycStep
import piuk.blockchain.android.ui.kyc.navigate
import piuk.blockchain.android.ui.kyc.profile.models.ProfileModel
import piuk.blockchain.android.util.US
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.inflate
import piuk.blockchain.android.util.visibleIf
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.ui.customviews.toast
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class KycHomeAddressFragment : BaseMvpFragment<KycHomeAddressView, KycHomeAddressPresenter>(),
    KycHomeAddressView, PickerItemListener {

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
            "",
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
    ) = container?.inflate(R.layout.fragment_kyc_home_address)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        logEvent(AnalyticsEvents.KycAddress)

        progressListener.setHostTitle(R.string.kyc_address_title)
        progressListener.incrementProgress(KycStep.AddressPage)
        setUpCountryPicker()
        setUpStatePicker()
        setUpDateOfBirthPicker()

        onViewReady()
    }

    private fun setUpDateOfBirthPicker() {
        date_of_birth.setOnClickListener {
            val builder = MaterialDatePicker.Builder.datePicker().apply {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.YEAR, -18)
                val constraintsBuilder = CalendarConstraints.Builder()
                constraintsBuilder.setEnd(calendar.timeInMillis)
                constraintsBuilder.setOpenAt(calendar.timeInMillis)
                this.setCalendarConstraints(constraintsBuilder.build())
            }
            val picker = builder.build()

            fragmentManager?.let {
                picker.show(it, picker.toString())
            }
            picker.addOnPositiveButtonClickListener { dateInMillis ->
                val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault())
                val date = dateFormat.format(Date(dateInMillis))
                date_of_birth.setText(date)
            }
        }
    }

    private fun setUpStatePicker() {
        state.setOnClickListener {
            SearchPickerItemBottomSheet.newInstance(US.values().map {
                StatePickerItem(it.ANSIAbbreviation, it.unabbreviated)
            }
            ).show(childFragmentManager, MviFragment.BOTTOM_SHEET)
        }
    }

    private fun setUpCountryPicker() {
        country_header.setOnClickListener {
            SearchPickerItemBottomSheet.newInstance(Locale.getISOCountries().toList().map {
                CountryPickerItem(it)
            }).show(childFragmentManager, MviFragment.BOTTOM_SHEET)
        }

        onItemPicked(CountryPickerItem())
    }

    override fun continueToMobileVerification(countryCode: String) {
        closeKeyboard()
        navigate(KycNavXmlDirections.actionStartMobileVerification(countryCode))
    }

    @Suppress("ConstantConditionIf")
    override fun continueToVeriffSplash(countryCode: String) {
        closeKeyboard()
        navigate(KycNavXmlDirections.actionStartVeriff(countryCode))
    }

    override fun tier1Complete() {
        closeKeyboard()
        navigate(KycHomeAddressFragmentDirections.actionTier1Complete())
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
        /* editTextFirstLine.setText(line1)
         editTextAptName.setText(line2)
         editTextCity.setText(city)
         editTextState.setText(state)
         editTextZipCode.setText(postCode)
         editTextCountry.setText(countryName)*/
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
    /*
    private fun subscribeToViewObservables() {
        if (compositeDisposable.size() == 0) {
            compositeDisposable +=
                buttonNext
                    .throttledClicks()
                    .subscribeBy(
                        onNext = {
                            presenter.onContinueClicked(progressListener.campaignType)
                            analytics.logEvent(KYCAnalyticsEvents.AddressChanged)
                        },
                        onError = { Timber.e(it) }
                    )

            compositeDisposable += editTextFirstLine
                .onDelayedChange(KycStep.AddressFirstLine)
                .doOnNext { addressSubject.onNext(AddressIntent.FirstLine(it)) }
                .subscribe()
            compositeDisposable += editTextAptName
                .onDelayedChange(KycStep.AptNameOrNumber)
                .doOnNext { addressSubject.onNext(AddressIntent.SecondLine(it)) }
                .subscribe()
            compositeDisposable += editTextCity
                .onDelayedChange(KycStep.City)
                .doOnNext { addressSubject.onNext(AddressIntent.City(it)) }
                .subscribe()
            compositeDisposable += editTextState
                .onDelayedChange(KycStep.State)
                .doOnNext { addressSubject.onNext(AddressIntent.State(it)) }
                .subscribe()
            compositeDisposable += editTextZipCode
                .onDelayedChange(KycStep.ZipCode)
                .doOnNext { addressSubject.onNext(AddressIntent.PostCode(it)) }
                .subscribe()

            compositeDisposable +=
                searchViewAddress.getEditText()
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
    }*/

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun finishPage() {
        findNavController(this).popBackStack()
    }

    override fun setButtonEnabled(enabled: Boolean) {
        btn_next.isEnabled = enabled
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
    /*
        private fun localiseUi() {
            if (profileModel.countryCode.equals("US", ignoreCase = true)) {
                searchViewAddress.queryHint = getString(
                    R.string.kyc_address_search_hint,
                    getString(R.string.kyc_address_search_hint_zipcode)
                )
                setHint(textInputAddress1, getString(R.string.kyc_address_street_line_1), true)
                setHint(textInputAddress2, getString(R.string.kyc_address_street_line_2), false)
                setHint(textInputCity, getString(R.string.kyc_address_address_city_hint), true)
                setHint(textInputLayoutState, getString(R.string.kyc_address_address_state_hint), true)
                setHint(textInputLayoutZipCode, getString(R.string.kyc_address_address_zip_code_hint), true)
            } else {
                searchViewAddress.queryHint = getString(
                    R.string.kyc_address_search_hint,
                    getString(R.string.kyc_address_search_hint_postcode)
                )
                setHint(textInputAddress1, getString(R.string.kyc_address_address_line_1), true)
                setHint(textInputAddress2, getString(R.string.kyc_address_address_line_2), false)
                setHint(textInputCity, getString(R.string.kyc_address_city_town_village), true)
                setHint(textInputLayoutState, getString(R.string.kyc_address_state_region_province_county), true)
                setHint(textInputLayoutZipCode, getString(R.string.kyc_address_postal_code), false)
            }

            editTextCountry.setText(
                Locale(
                    Locale.getDefault().displayLanguage,
                    profileModel.countryCode
                ).displayCountry
            )
        }*/

    /* private fun setHint(textInput: TextInputLayout, hint: String, isRequired: Boolean) {
         textInput.hint = if (isRequired) "$hint*" else hint
     }*/

    /* private fun TextView.onDelayedChange(kycStep: KycStep): Observable<String> =
         this.afterTextChangeEvents()
             .debounce(300, TimeUnit.MILLISECONDS)
             .map { it.editable()?.toString() ?: "" }
             .skipFirstUnless { !it.isEmpty() }
             .observeOn(AndroidSchedulers.mainThread())
             .distinctUntilChanged()
             .doOnNext { updateProgress(mapToCompleted(it), kycStep) }*/

    /*
        private fun mapToCompleted(text: String): Boolean = !text.isEmpty()
    */

    private fun updateProgress(stepCompleted: Boolean, kycStep: KycStep) {
        if (stepCompleted) {
            progressListener.incrementProgress(kycStep)
        } else {
            progressListener.decrementProgress(kycStep)
        }
    }

    /*   private fun setupImeOptions() {
           val editTexts = listOf(
               editTextFirstLine,
               editTextAptName,
               editTextCity,
               editTextState,
               editTextZipCode
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
       }*/

    override fun onItemPicked(item: PickerItem) {
        when (item) {
            is CountryPickerItem -> {
                country_text.text = item.label
                flag_icon.text = item.icon
                configureUiForCountry(item.code == "US")
            }
            is StatePickerItem -> {
                state.setText(item.code)
            }
        }
    }

    private fun configureUiForCountry(isUsa: Boolean) {
        zip_input.visibleIf { isUsa }
        post_code_input.visibleIf { !isUsa }
        state_input.visibleIf { isUsa }
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
