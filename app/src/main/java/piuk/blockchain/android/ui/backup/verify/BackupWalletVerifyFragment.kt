package piuk.blockchain.android.ui.backup.verify

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentBackupWalletVerifyBinding
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedFragment
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment
import piuk.blockchain.android.ui.customviews.dialogs.MaterialProgressDialog
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.androidcoreui.ui.base.BaseFragment

class BackupWalletVerifyFragment : BaseFragment<BackupVerifyView, BackupVerifyPresenter>(),
    BackupVerifyView {

    private val backupVerifyPresenter: BackupVerifyPresenter by scopedInject()

    private var progressDialog: MaterialProgressDialog? = null

    private var _binding: FragmentBackupWalletVerifyBinding? = null
    private val binding: FragmentBackupWalletVerifyBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupWalletVerifyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewReady()

        with(binding) {
            buttonVerify.setOnClickListener {
                presenter.onVerifyClicked(
                    edittextFirstWord.text.toString(),
                    edittextSecondWord.text.toString(),
                    edittextThirdWord.text.toString()
                )
            }
        }
    }

    override fun showWordHints(hints: List<Int>) {
        val mnemonicRequestHint = resources.getStringArray(R.array.mnemonic_word_requests)
        with(binding) {
            edittextFirstWord.hint = mnemonicRequestHint[hints[0]]
            edittextSecondWord.hint = mnemonicRequestHint[hints[1]]
            edittextThirdWord.hint = mnemonicRequestHint[hints[2]]
        }
    }

    override fun getPageBundle(): Bundle? = arguments

    override fun createPresenter() = backupVerifyPresenter

    override fun getMvpView() = this

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(
            requireContext()
        ).apply {
            setMessage("${getString(R.string.please_wait)}…")
            setCancelable(false)
            show()
        }
    }

    override fun hideProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog?.dismiss()
        }
    }

    override fun showCompletedFragment() {
        popAllAndStartFragment(
            BackupWalletCompletedFragment.newInstance(),
            BackupWalletCompletedFragment.TAG
        )
    }

    override fun showStartingFragment() {
        popAllAndStartFragment(BackupWalletStartingFragment(), BackupWalletStartingFragment.TAG)
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.run {
            val view = currentFocus
            if (view != null) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
        _binding = null
    }

    private fun popAllAndStartFragment(fragment: Fragment, tag: String) {
        fragmentManager?.run {
            popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(tag)
                .commit()
        }
    }
}