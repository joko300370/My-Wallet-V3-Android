package piuk.blockchain.android.ui.kyc.invalidcountry

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentKycInvalidCountryBinding
import piuk.blockchain.android.ui.customviews.dialogs.MaterialProgressDialog
import piuk.blockchain.android.ui.kyc.ParentActivityDelegate
import piuk.blockchain.android.ui.kyc.navhost.KycProgressListener
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment

class KycInvalidCountryFragment :
    BaseMvpFragment<KycInvalidCountryView, KycInvalidCountryPresenter>(),
    KycInvalidCountryView {

    private var _binding: FragmentKycInvalidCountryBinding? = null
    private val binding: FragmentKycInvalidCountryBinding
        get() = _binding!!

    override val displayModel by unsafeLazy {
        KycInvalidCountryFragmentArgs.fromBundle(arguments ?: Bundle()).countryDisplayModel
    }
    private val presenter: KycInvalidCountryPresenter by scopedInject()
    private val progressListener: KycProgressListener by ParentActivityDelegate(
        this
    )
    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKycInvalidCountryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressListener.setHostTitle(R.string.kyc_country_selection_title_1)

        with(binding) {
            textViewKycInvalidCountryHeader.text = getString(R.string.kyc_invalid_country_header, displayModel.name)
            textViewKycInvalidCountryMessage.text = getString(R.string.kyc_invalid_country_message, displayModel.name)
            textViewKycNoThanks.setOnClickListener { presenter.onNoThanks() }
            buttonKycInvalidCountryMessageMe.setOnClickListener { presenter.onNotifyMe() }
        }
        onViewReady()
    }

    override fun finishPage() {
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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

    override fun createPresenter(): KycInvalidCountryPresenter = presenter

    override fun getMvpView(): KycInvalidCountryView = this
}
