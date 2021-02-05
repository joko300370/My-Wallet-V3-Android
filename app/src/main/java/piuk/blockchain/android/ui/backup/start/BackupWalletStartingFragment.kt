package piuk.blockchain.android.ui.backup.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.R
import com.blockchain.koin.scopedInject
import com.blockchain.koin.scopedInjectActivity
import com.blockchain.ui.password.SecondPasswordHandler
import piuk.blockchain.android.databinding.FragmentBackupStartBinding
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment
import piuk.blockchain.androidcoreui.ui.base.BaseFragment

class BackupWalletStartingFragment :
    BaseFragment<BackupWalletStartingView, BackupWalletStartingPresenter>(),
    BackupWalletStartingView {

    private val backupWalletStartingPresenter: BackupWalletStartingPresenter by scopedInject()

    private val secondPasswordHandler: SecondPasswordHandler by scopedInjectActivity()

    private var _binding: FragmentBackupStartBinding? = null
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBackupStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStart.setOnClickListener {
            secondPasswordHandler.validate(
                requireContext(),
                object : SecondPasswordHandler.ResultListener {
                    override fun onNoSecondPassword() {
                        loadFragmentWordListFragment()
                    }

                    override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                        loadFragmentWordListFragment(validatedSecondPassword)
                    }
                }
            )
        }
    }

    override fun createPresenter() = backupWalletStartingPresenter

    override fun getMvpView() = this

    private fun loadFragmentWordListFragment(secondPassword: String? = null) {
        val fragment = BackupWalletWordListFragment().apply {
            secondPassword?.let {
                arguments = Bundle().apply {
                    putString(
                        BackupWalletWordListFragment.ARGUMENT_SECOND_PASSWORD,
                        it
                    )
                }
            }
        }
        activity?.run {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    companion object {
        const val TAG = "BackupWalletStartingFragment"
    }
}