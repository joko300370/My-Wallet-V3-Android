package piuk.blockchain.android.ui.transactionflow.fullscreen

import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import org.koin.android.ext.android.inject
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.databinding.ActivityTransactionFlowBinding
import piuk.blockchain.android.ui.base.mvi.MviActivity
import piuk.blockchain.android.ui.transactionflow.TransactionFlowIntentMapper
import piuk.blockchain.android.ui.transactionflow.analytics.TxFlowAnalytics
import piuk.blockchain.android.ui.transactionflow.closeTransactionScope
import piuk.blockchain.android.ui.transactionflow.createTransactionScope
import piuk.blockchain.android.ui.transactionflow.engine.TransactionIntent
import piuk.blockchain.android.ui.transactionflow.engine.TransactionModel
import piuk.blockchain.android.ui.transactionflow.engine.TransactionState
import piuk.blockchain.android.ui.transactionflow.engine.TransactionStep
import piuk.blockchain.android.ui.transactionflow.transactionInject
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.getTarget
import piuk.blockchain.android.util.putAccount
import piuk.blockchain.android.util.putTarget
import timber.log.Timber

class TransactionFlowActivity :
    MviActivity<TransactionModel, TransactionIntent, TransactionState, ActivityTransactionFlowBinding>() {

    override val model: TransactionModel by transactionInject()
    private val analyticsHooks: TxFlowAnalytics by inject()

    override val alwaysDisableScreenshots: Boolean
        get() = false

    private val sourceAccount: BlockchainAccount by lazy {
        intent.extras?.getAccount(SOURCE) ?: throw IllegalStateException("No source account specified")
    }

    private val transactionTarget: TransactionTarget by lazy {
        intent.extras?.getTarget(TARGET) ?: throw IllegalStateException("No target specified")
    }

    private val action: AssetAction by lazy {
        intent.extras?.getSerializable(ACTION) as? AssetAction ?: throw IllegalStateException("No action specified")
    }

    private val compositeDisposable = CompositeDisposable()
    private var currentStep: TransactionStep = TransactionStep.ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intentMapper = TransactionFlowIntentMapper(
            sourceAccount = sourceAccount,
            target = transactionTarget,
            action = action
        )

        compositeDisposable += sourceAccount.requireSecondPassword()
            .map { intentMapper.map(it) }
            .observeOn(Schedulers.io())
            .subscribeBy(
                onSuccess = { transactionIntent ->
                    model.process(transactionIntent)
                },
                onError = {
                    Timber.e("Unable to configure transaction flow, aborting. e == $it")
                    // finishFlow()
                })
    }

    override fun initBinding(): ActivityTransactionFlowBinding = ActivityTransactionFlowBinding.inflate(layoutInflater)

    override fun render(newState: TransactionState) {
        handleStateChange(newState)
    }

    private fun handleStateChange(newState: TransactionState) {
        if (currentStep == newState.currentStep)
            return

        when (newState.currentStep) {
            TransactionStep.ZERO -> kotlin.run {
                model.process(TransactionIntent.ResetFlow)
                finish()
            }
            TransactionStep.CLOSED -> kotlin.run {
                compositeDisposable.clear()
                model.destroy()
                closeTransactionScope()
            }
            else -> kotlin.run {
                analyticsHooks.onStepChanged(newState)
            }
        }
        newState.currentStep.takeIf { it != TransactionStep.ZERO }?.let {
            showFlowStep(it)
            currentStep = it
        }
    }

    private fun showFlowStep(step: TransactionStep) {
        // TODO in next story
        Timber.e("flow step change - $step")
//        val stepSheet = when (step) {
//            TransactionStep.ZERO,
//            TransactionStep.CLOSED -> null
//            TransactionStep.ENTER_PASSWORD -> EnterSecondPasswordSheet()
//            TransactionStep.SELECT_SOURCE -> SelectSourceAccountSheet()
//            TransactionStep.ENTER_ADDRESS -> EnterTargetAddressSheet()
//            TransactionStep.ENTER_AMOUNT -> EnterAmountSheet()
//            TransactionStep.SELECT_TARGET_ACCOUNT -> SelectTargetAccountSheet()
//            TransactionStep.CONFIRM_DETAIL -> ConfirmTransactionSheet()
//            TransactionStep.IN_PROGRESS -> TransactionProgressSheet()
//        }
//        replaceBottomSheet(stepSheet)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        model.destroy()
        super.onDestroy()
        closeTransactionScope()
    }

    companion object {
        private const val SOURCE = "SOURCE_ACCOUNT"
        private const val TARGET = "TARGET_ACCOUNT"
        private const val ACTION = "ASSET_ACTION"

        fun newInstance(
            context: Context,
            sourceAccount: BlockchainAccount,
            target: TransactionTarget = NullCryptoAccount(),
            action: AssetAction
        ): Intent {
            val bundle = Bundle().apply {
                putAccount(SOURCE, sourceAccount)
                putTarget(TARGET, target)
                putSerializable(ACTION, action)
            }

            openScope()

            return Intent(context, TransactionFlowActivity::class.java).apply {
                putExtras(bundle)
            }
        }

        private fun openScope() =
            try {
                createTransactionScope()
            } catch (e: Throwable) {
                Timber.wtf("$e")
            }
    }
}