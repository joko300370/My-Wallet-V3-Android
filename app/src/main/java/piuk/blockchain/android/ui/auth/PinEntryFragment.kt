package piuk.blockchain.android.ui.auth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.RESULT_CANCELED
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.blockchain.koin.scopedInject
import com.blockchain.ui.dialog.MaterialProgressDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.connectivity.ConnectivityStatus
import piuk.blockchain.android.databinding.FragmentPinEntryBinding
import piuk.blockchain.android.ui.customviews.PinEntryKeypad
import piuk.blockchain.android.ui.debug.DebugOptionsBottomDialog
import piuk.blockchain.android.ui.fingerprint.FingerprintDialog
import piuk.blockchain.android.ui.fingerprint.FingerprintStage
import piuk.blockchain.android.ui.home.MobileNoticeDialogFragment
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.ui.start.PasswordRequiredActivity
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.copyHashOnLongClick
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.annotations.Thunk
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.visibleIf

internal class PinEntryFragment : BaseFragment<PinEntryView, PinEntryPresenter>(),
    PinEntryView {

    private val pinEntryPresenter: PinEntryPresenter by scopedInject()
    private val environmentConfig: EnvironmentConfig by inject()
    private val appUtil: AppUtil by inject()

    private val _pinBoxList = mutableListOf<ImageView>()
    override val pinBoxList: List<ImageView>
        get() = _pinBoxList

    private var materialProgressDialog: MaterialProgressDialog? = null
    private var binding: FragmentPinEntryBinding? = null
    private var fingerprintDialog: FingerprintDialog? = null
    private var listener: OnPinEntryFragmentInteractionListener? = null
    private val clearPinNumberRunnable = ClearPinNumberRunnable()
    private var isPaused = false

    private var backPressed: Long = 0

    val isValidatingPinForResult: Boolean
        get() = presenter?.isForValidatingPinForResult ?: false

    private val compositeDisposable = CompositeDisposable()

    private val isAfterWalletCreation: Boolean by lazy {
        arguments?.getBoolean(KEY_IS_AFTER_WALLET_CREATION, false) ?: false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_pin_entry, container, false)

        // Set title state
        if (presenter.isCreatingNewPin) {
            binding?.titleBox?.setText(R.string.create_pin)
        } else {
            binding?.titleBox?.setText(R.string.pin_entry)
            presenter.fetchInfoMessage()
        }

        binding?.let {
            _pinBoxList.add(it.pinBox0)
            _pinBoxList.add(it.pinBox1)
            _pinBoxList.add(it.pinBox2)
            _pinBoxList.add(it.pinBox3)
        }

        showConnectionDialogIfNeeded()
        binding?.swipeHintLayout?.setOnClickListener { listener?.onSwipePressed() }

        presenter.onViewReady()
        presenter.checkForceUpgradeStatus(BuildConfig.VERSION_NAME)

        if (arguments != null) {
            val showSwipeHint = arguments!!.getBoolean(KEY_SHOW_SWIPE_HINT)
            if (!showSwipeHint) {
                binding?.swipeHintLayout?.visibility = View.INVISIBLE
            }
        }

        binding?.keyboard?.setPadClickedListener(object :
            PinEntryKeypad.OnPinEntryPadClickedListener {
            override fun onNumberClicked(number: String) {
                presenter.onPadClicked(number)
            }

            override fun onDeleteClicked() {
                presenter.onDeleteClicked()
            }
        })

        if (environmentConfig.shouldShowDebugMenu()) {
            ToastCustom.makeText(
                activity,
                "Current environment: " + environmentConfig.environment.getName(),
                ToastCustom.LENGTH_SHORT,
                ToastCustom.TYPE_GENERAL)

            binding?.buttonSettings?.visibility = View.VISIBLE
            binding?.buttonSettings?.setOnClickListener {
                if (activity != null) {
                    DebugOptionsBottomDialog.show(requireFragmentManager())
                }
            }
        }

        binding?.textViewVersionCode?.text =
            "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        return binding?.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPinEntryFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnPinEntryFragmentInteractionListener")
        }
    }

    override fun showFingerprintDialog(pincode: String) {
        // Show icon for relaunching dialog
        binding?.fingerprintLogo?.visibility = View.VISIBLE
        binding?.fingerprintLogo?.setOnClickListener { presenter.checkFingerprintStatus() }
        // Show dialog itself if not already showing
        if (fingerprintDialog == null && presenter.canShowFingerprintDialog()) {
            fingerprintDialog =
                FingerprintDialog.newInstance(pincode, FingerprintStage.AUTHENTICATE)
            fingerprintDialog?.setAuthCallback(object : FingerprintDialog.FingerprintAuthCallback {
                override fun onAuthenticated(data: String?) {
                    dismissFingerprintDialog()
                    presenter.loginWithDecryptedPin(data ?: "")
                }

                override fun onCanceled() {
                    dismissFingerprintDialog()
                    showKeyboard()
                }
            })

            HANDLER.postDelayed({
                activity?.let {
                    if (it.isFinishing.not() && !isPaused && fingerprintDialog != null) {
                        it.supportFragmentManager
                            .beginTransaction()
                            .add(fingerprintDialog!!, FingerprintDialog.TAG)
                            .commitAllowingStateLoss()
                    }
                } ?: run { fingerprintDialog = null }
            }, 200)

            hideKeyboard()
        }
    }

    override fun showKeyboard() {
        if (activity != null && binding!!.keyboard.visibility == View.INVISIBLE) {
            val bottomUp = AnimationUtils.loadAnimation(activity, R.anim.bottom_up)
            binding!!.keyboard.startAnimation(bottomUp)
            binding!!.keyboard.visibility = View.VISIBLE
        }
    }

    private fun hideKeyboard() {
        if (activity != null && binding!!.keyboard.visibility == View.VISIBLE) {
            val bottomUp = AnimationUtils.loadAnimation(activity, R.anim.top_down)
            binding!!.keyboard.startAnimation(bottomUp)
            binding!!.keyboard.visibility = View.INVISIBLE
        }
    }

    private fun showConnectionDialogIfNeeded() {
        if (context != null) {
            if (!ConnectivityStatus.hasConnectivity(context)) {
                AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                    .setMessage(getString(R.string.check_connectivity_exit))
                    .setCancelable(false)
                    .setPositiveButton(
                        R.string.dialog_continue) { dialog, id -> restartPageAndClearTop() }
                    .create()
                    .show()
            }
        }
    }

    override fun showMaxAttemptsDialog() {
        if (context != null) {
            AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.password_or_wipe)
                .setCancelable(false)
                .setPositiveButton(
                    R.string.use_password) { dialog, whichButton -> showValidationDialog() }
                .setNegativeButton(
                    R.string.wipe_wallet) { dialog, whichButton -> presenter.resetApp() }
                .show()
        }
    }

    fun onBackPressed() {
        if (presenter?.isForValidatingPinForResult == true) {
            finishWithResultCanceled()
        } else if (presenter?.allowExit() == true) {
            if (backPressed + BuildConfig.EXIT_APP_COOLDOWN_MILLIS > System.currentTimeMillis()) {
                presenter.clearLoginState()
                return
            } else {
                showToast(R.string.exit_confirm, ToastCustom.TYPE_GENERAL)
            }

            backPressed = System.currentTimeMillis()
        }
    }

    override fun showWalletVersionNotSupportedDialog(walletVersion: String?) {
        if (context != null && walletVersion != null) {
            AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setMessage(String.format(getString(R.string.unsupported_encryption_version),
                    walletVersion))
                .setCancelable(false)
                .setPositiveButton(
                    R.string.exit) { dialog, whichButton -> presenter.clearLoginState() }
                .setNegativeButton(R.string.logout) { dialog, which ->
                    presenter.clearLoginState()
                    restartApp()
                }
                .show()
        }
    }

    private fun restartApp() {
        val appUtil: AppUtil = get()
        appUtil.restartApp(LauncherActivity::class.java)
    }

    override fun clearPinBoxes() {
        HANDLER.postDelayed(clearPinNumberRunnable, 200)
    }

    override fun goToPasswordRequiredActivity() {
        val intent = Intent(context, PasswordRequiredActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun goToUpgradeWalletActivity() {
        val intent = Intent(context, UpgradeWalletActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun setTitleString(@StringRes title: Int) {
        HANDLER.postDelayed({ binding?.titleBox?.setText(title) }, 200)
    }

    override fun setTitleVisibility(@ViewUtils.Visibility visibility: Int) {
        binding?.titleBox?.visibility = visibility
    }

    fun resetPinEntry() {
        if (activity != null && !activity!!.isFinishing && presenter != null) {
            presenter.clearPinBoxes()
        }
    }

    fun allowExit(): Boolean {
        return presenter?.allowExit() ?: true
    }

    override fun restartPageAndClearTop() {
        val intent = Intent(context, PinEntryActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun showCommonPinWarning(callback: DialogButtonCallback) {
        if (context != null) {
            AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                .setTitle(R.string.common_pin_dialog_title)
                .setMessage(R.string.common_pin_dialog_message)
                .setPositiveButton(
                    R.string.common_pin_dialog_try_again) { _, _ -> callback.onPositiveClicked() }
                .setNegativeButton(
                    R.string.common_pin_dialog_continue) { _, _ -> callback.onNegativeClicked() }
                .setCancelable(false)
                .create()
                .show()
        }
    }

    override fun showValidationDialog() {
        if (context != null) {
            val password = AppCompatEditText(context!!)
            password.inputType =
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or
                        InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            password.setHint(R.string.password)

            AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(getString(R.string.password_entry))
                .setView(ViewUtils.getAlertDialogPaddedView(context, password))
                .setCancelable(false)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    restartApp()
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    val pw = password.text.toString()

                    if (pw.isNotEmpty()) {
                        presenter.validatePassword(pw)
                    } else {
                        presenter.incrementFailureCountAndRestart()
                    }
                }.show()
        }
    }

    override fun showAccountLockedDialog() {
        if (context != null) {
            AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
                .setTitle(R.string.account_locked_title)
                .setMessage(R.string.account_locked_message)
                .setCancelable(false)
                .setPositiveButton(R.string.exit) { dialogInterface, i -> activity!!.finish() }
                .create()
                .show()
        }
    }

    private fun isNotFinishing(): Boolean {
        val a = activity
        return (a != null && !a.isFinishing)
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        if (isNotFinishing()) {
            ToastCustom.makeText(context, getString(message), ToastCustom.LENGTH_LONG, toastType)
        }
    }

    override fun showProgressDialog(@StringRes messageId: Int, suffix: String?) {
        dismissProgressDialog()
        materialProgressDialog = MaterialProgressDialog(requireContext()).apply {
            setCancelable(false)
            if (suffix != null) {
                setMessage(getString(messageId) + suffix)
            } else {
                setMessage(getString(messageId))
            }

            if (isNotFinishing()) {
                show()
            }
        }
    }

    override fun dismissProgressDialog() {
        if (materialProgressDialog != null && materialProgressDialog!!.isShowing) {
            materialProgressDialog!!.dismiss()
            materialProgressDialog = null
        }
    }

    override fun onResume() {
        super.onResume()
        isPaused = false
        presenter.clearPinBoxes()
        presenter.checkFingerprintStatus()
    }

    override fun finishWithResultOk(pin: String) {
        val bundle = Bundle()
        bundle.putString(KEY_VALIDATED_PIN, pin)
        val intent = Intent()
        intent.putExtras(bundle)
        activity?.setResult(RESULT_OK, intent)
        activity?.finish()
    }

    private fun finishWithResultCanceled() {
        val intent = Intent()
        activity?.setResult(RESULT_CANCELED, intent)
        activity?.finish()
    }

    override fun appNeedsUpgrade(isForced: Boolean) {
        if (context == null) return
        val appUpdateManager = AppUpdateManagerFactory.create(context)
        if (isForced) {
            compositeDisposable.add(updateInfo(appUpdateManager).subscribe { appUpdateInfoTask ->
                if (canTriggerAnUpdateOfType(AppUpdateType.IMMEDIATE,
                        appUpdateInfoTask) && activity != null
                ) {
                    updateForcedNatively(appUpdateManager, appUpdateInfoTask.result)
                } else {
                    handleForcedUpdateFromStore()
                }
            })
        } else {
            compositeDisposable.add(updateInfo(appUpdateManager).subscribe { appUpdateInfoTask ->
                if (canTriggerAnUpdateOfType(AppUpdateType.FLEXIBLE,
                        appUpdateInfoTask) && activity != null
                ) {
                    updateFlexibleNatively(appUpdateManager, appUpdateInfoTask.result)
                }
            })
        }
    }

    private fun updateInfo(appUpdateManager: AppUpdateManager): Observable<Task<AppUpdateInfo>> {
        return Observable.fromCallable { appUpdateManager.appUpdateInfo }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun canTriggerAnUpdateOfType(
        updateAvailabilityType: Int,
        appUpdateInfoTask: Task<AppUpdateInfo>
    ): Boolean {
        return (appUpdateInfoTask.result.updateAvailability() ==
                UpdateAvailability.UPDATE_AVAILABLE ||
                appUpdateInfoTask.result.updateAvailability() ==
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) &&
                appUpdateInfoTask.result.isUpdateTypeAllowed(updateAvailabilityType)
    }

    @Throws(IntentSender.SendIntentException::class)
    private fun updateFlexibleNatively(
        appUpdateManager: AppUpdateManager,
        appUpdateInfo: AppUpdateInfo
    ) {

        val updatedListener = object : InstallStateUpdatedListener {
            override fun onStateUpdate(installState: InstallState) {
                if (installState.installStatus() == InstallStatus.DOWNLOADED) {
                    appUpdateManager.completeUpdate()
                }
                if (shouldBeUnregistered(installState.installStatus())) {
                    appUpdateManager.unregisterListener(this)
                }
            }
        }
        appUpdateManager.registerListener(updatedListener)
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            activity,
            PinEntryActivity.REQUEST_CODE_UPDATE)
    }

    private fun shouldBeUnregistered(installStatus: Int): Boolean {
        return installStatus == InstallStatus.CANCELED ||
                installStatus == InstallStatus.DOWNLOADED ||
                installStatus == InstallStatus.INSTALLED ||
                installStatus == InstallStatus.FAILED
    }

    @Throws(IntentSender.SendIntentException::class)
    private fun updateForcedNatively(
        appUpdateManager: AppUpdateManager,
        appUpdateInfo: AppUpdateInfo
    ) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.IMMEDIATE,
            activity,
            PinEntryActivity.REQUEST_CODE_UPDATE)
    }

    private fun handleForcedUpdateFromStore() {
        val alertDialog = AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.force_upgrade_message)
            .setPositiveButton(R.string.update, null)
            .setNegativeButton(R.string.exit, null)
            .setCancelable(false)
            .create()

        alertDialog.show()
        // Buttons are done this way to avoid dismissing the dialog
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val appPackageName = context?.packageName
            try {
                startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
            } catch (e: ActivityNotFoundException) {
                // Device doesn't have the Play Store installed, direct them to the official
                // store web page anyway
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
            }
        }
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setOnClickListener { v -> presenter.clearLoginState() }
    }

    override val pageIntent: Intent?
        get() = activity?.intent

    override fun onPause() {
        super.onPause()
        isPaused = true
        dismissFingerprintDialog()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        dismissProgressDialog()
        super.onDestroy()
    }

    @Thunk
    internal fun dismissFingerprintDialog() {
        if (fingerprintDialog?.isVisible == true) {
            fingerprintDialog!!.dismissAllowingStateLoss()
            fingerprintDialog = null
        }

        // Hide if fingerprint unlock has become unavailable
        if (!presenter.ifShouldShowFingerprintLogin) {
            binding?.fingerprintLogo?.visibility = View.GONE
        }
    }

    private inner class ClearPinNumberRunnable : Runnable {
        override fun run() {
            pinBoxList.forEach {
                it.setImageResource(R.drawable.rounded_view_blue_white_border)
            }
        }
    }

    override fun showMobileNotice(mobileNoticeDialog: MobileNoticeDialog) {
        if (activity?.isFinishing == false && fragmentManager != null) {
            val alertFragment = MobileNoticeDialogFragment.newInstance(mobileNoticeDialog)
            alertFragment.show(fragmentManager!!, alertFragment.tag)
        }
    }

    override fun showTestnetWarning() {
        if (activity != null && !activity!!.isFinishing) {
            val snack = Snackbar.make(
                activity!!.findViewById(android.R.id.content),
                R.string.testnet_warning,
                Snackbar.LENGTH_LONG
            )
            val view = snack.view
            view.setBackgroundColor(ContextCompat.getColor(activity!!, R.color.product_red_medium))
            snack.show()
        }
    }

    override fun restartAppWithVerifiedPin() {
        appUtil.restartAppWithVerifiedPin(LauncherActivity::class.java, isAfterWalletCreation)
    }

    override fun createPresenter(): PinEntryPresenter? {
        return pinEntryPresenter
    }

    override fun setupCommitHashView() {
        binding?.debugCommitHash?.apply {
            visibleIf { BuildConfig.COMMIT_HASH.isNotEmpty() }
            text = BuildConfig.COMMIT_HASH
            copyHashOnLongClick(requireContext())
        }
    }

    override fun getMvpView(): PinEntryView {
        return this
    }

    internal interface OnPinEntryFragmentInteractionListener {
        fun onSwipePressed()
    }

    companion object {
        private const val KEY_SHOW_SWIPE_HINT = "show_swipe_hint"
        private const val KEY_IS_AFTER_WALLET_CREATION = "is_after_wallet_creation"
        private val HANDLER = Handler()

        fun newInstance(
            showSwipeHint: Boolean,
            isAfterCreateWallet: Boolean
        ): PinEntryFragment {
            val args = Bundle()
            args.putBoolean(KEY_SHOW_SWIPE_HINT, showSwipeHint)
            args.putBoolean(KEY_IS_AFTER_WALLET_CREATION, isAfterCreateWallet)
            val fragment = PinEntryFragment()
            fragment.arguments = args
            return fragment
        }
    }
}

const val KEY_VALIDATING_PIN_FOR_RESULT = "validating_pin"
const val KEY_VALIDATED_PIN = "validated_pin"
const val REQUEST_CODE_VALIDATE_PIN = 88