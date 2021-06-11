package piuk.blockchain.android.ui.backup.completed

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentBackupCompleteBinding
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.setOnClickListenerDebounced
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWalletCompletedFragment :
    BaseFragment<BackupWalletCompletedView, BackupWalletCompletedPresenter>(),
    BackupWalletCompletedView {

    private val presenter: BackupWalletCompletedPresenter by scopedInject()

    private var _binding: FragmentBackupCompleteBinding? = null
    private val binding: FragmentBackupCompleteBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBackupCompleteBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonBackupDone.setOnClickListenerDebounced { onBackupDone() }
        binding.buttonBackupAgain.setOnClickListenerDebounced { onBackupAgainRequested() }

        onViewReady()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun showLastBackupDate(lastBackup: Long) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = dateFormat.format(Date(lastBackup * 1000))
        val message = String.format(resources.getString(R.string.backup_last), date)
        binding.subheadingDate.text = message
    }

    override fun hideLastBackupDate() {
        binding.subheadingDate.gone()
    }

    override fun createPresenter() = presenter

    override fun getMvpView() = this

    private fun onBackupAgainRequested() {
        activity?.run {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, BackupWalletStartingFragment())
                .addToBackStack(BackupWalletStartingFragment.TAG)
                .commit()
        }
    }

    private fun onBackupDone() {
        activity?.apply {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    companion object {

        const val TAG = "BackupWalletCompletedFragment"

        fun newInstance(): BackupWalletCompletedFragment = BackupWalletCompletedFragment()
    }
}
