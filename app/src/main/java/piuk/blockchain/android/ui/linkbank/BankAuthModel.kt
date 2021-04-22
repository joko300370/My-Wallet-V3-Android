package piuk.blockchain.android.ui.linkbank

import com.blockchain.extensions.exhaustive
import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.models.data.BankPartner
import com.blockchain.nabu.models.data.LinkedBank
import com.blockchain.nabu.models.data.LinkedBankErrorState
import com.blockchain.nabu.models.data.LinkedBankState
import io.reactivex.Completable
import io.reactivex.Scheduler
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.simplebuy.SimpleBuyInteractor
import piuk.blockchain.android.ui.base.mvi.MviModel
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class BankAuthModel(
    private val interactor: SimpleBuyInteractor,
    initialState: BankAuthState,
    scheduler: Scheduler,
    environmentConfig: EnvironmentConfig,
    crashLogger: CrashLogger
) : MviModel<BankAuthState, BankAuthIntent>(
    initialState,
    scheduler,
    environmentConfig,
    crashLogger
) {
    override fun performAction(previousState: BankAuthState, intent: BankAuthIntent): Disposable? =
        when (intent) {
            is BankAuthIntent.CancelOrder,
            is BankAuthIntent.CancelOrderAndResetAuthorisation -> (previousState.id?.let {
                interactor.cancelOrder(it)
            } ?: Completable.complete())
                .subscribeBy(
                    onComplete = {
                        process(BankAuthIntent.OrderCanceled)
                    },
                    onError = {
                        process(BankAuthIntent.ErrorIntent())
                    }
                )
            is BankAuthIntent.UpdateAccountProvider -> processBankLinkingUpdate(intent)
            is BankAuthIntent.GetLinkedBankState -> processBankLinkStateUpdate(intent)
            is BankAuthIntent.StartPollingForLinkStatus -> processLinkStatusPolling(intent, previousState)
            is BankAuthIntent.StartBankApproval -> {
                interactor.updateApprovalStatus()
                null
            }
            else -> null
        }

    private fun processBankLinkingUpdate(intent: BankAuthIntent.UpdateAccountProvider) =
        interactor.updateSelectedBankAccountId(
            linkingId = intent.linkingBankId,
            providerAccountId = intent.accountProviderId,
            accountId = intent.accountId,
            partner = intent.linkBankTransfer.partner,
            source = intent.authSource
        ).subscribeBy(
            onComplete = {
                process(BankAuthIntent.StartPollingForLinkStatus(intent.linkingBankId))
            },
            onError = {
                process(BankAuthIntent.ProviderAccountIdUpdateError)
            }
        )

    private fun processBankLinkStateUpdate(intent: BankAuthIntent.GetLinkedBankState) =
        interactor.pollForBankLinkingCompleted(
            intent.linkingBankId
        ).subscribeBy(
            onSuccess = {
                when (it.state) {
                    LinkedBankState.ACTIVE -> process(BankAuthIntent.LinkedBankStateSuccess(it))
                    LinkedBankState.BLOCKED,
                    LinkedBankState.UNKNOWN -> handleBankLinkingError(it)
                    LinkedBankState.PENDING,
                    LinkedBankState.CREATED -> process(
                        BankAuthIntent.BankAuthErrorState(ErrorState.BankLinkingTimeout)
                    )
                }
            },
            onError = {
                process(BankAuthIntent.BankAuthErrorState(ErrorState.BankLinkingFailed))
            }
        )

    private fun processLinkStatusPolling(
        intent: BankAuthIntent.StartPollingForLinkStatus,
        previousState: BankAuthState
    ) = interactor.pollForLinkedBankState(
        intent.bankId, if (previousState.linkBankTransfer?.partner == BankPartner.YAPILY) {
            BankPartner.YAPILY
        } else {
            null
        }
    ).subscribeBy(
        onSuccess = {
            when (it.state) {
                LinkedBankState.ACTIVE -> {
                    process(BankAuthIntent.LinkedBankStateSuccess(it))
                }
                LinkedBankState.BLOCKED -> {
                    handleBankLinkingError(it)
                }
                LinkedBankState.PENDING,
                LinkedBankState.CREATED -> {
                    when (previousState.linkBankTransfer?.partner) {
                        BankPartner.YODLEE -> process(BankAuthIntent.BankAuthErrorState(ErrorState.BankLinkingTimeout))
                        BankPartner.YAPILY -> process(
                            BankAuthIntent.UpdateLinkingUrl(it.authorisationUrl)
                        )
                    }
                }
                LinkedBankState.UNKNOWN -> process(BankAuthIntent.BankAuthErrorState(ErrorState.BankLinkingFailed))
            }
        },
        onError = {
            process(BankAuthIntent.BankAuthErrorState(ErrorState.BankLinkingFailed))
        }
    )

    @Suppress("IMPLICIT_CAST_TO_ANY")
    private fun handleBankLinkingError(it: LinkedBank) {
        when (it.errorStatus) {
            LinkedBankErrorState.ACCOUNT_ALREADY_LINKED -> process(
                BankAuthIntent.BankAuthErrorState(ErrorState.LinkedBankAlreadyLinked)
            )
            LinkedBankErrorState.UNKNOWN -> process(BankAuthIntent.BankAuthErrorState(ErrorState.BankLinkingFailed))
            LinkedBankErrorState.ACCOUNT_TYPE_UNSUPPORTED -> process(
                BankAuthIntent.BankAuthErrorState(ErrorState.LinkedBankAccountUnsupported)
            )
            LinkedBankErrorState.NAMES_MISMATCHED -> process(
                BankAuthIntent.BankAuthErrorState(ErrorState.LinkedBankNamesMismatched)
            )
            LinkedBankErrorState.REJECTED -> process(
                BankAuthIntent.BankAuthErrorState(ErrorState.LinkedBankRejected)
            )
            LinkedBankErrorState.EXPIRED -> process(
                BankAuthIntent.BankAuthErrorState(ErrorState.LinkedBankExpired)
            )
            LinkedBankErrorState.FAILURE -> process(
                BankAuthIntent.BankAuthErrorState(ErrorState.LinkedBankFailure)
            )
            LinkedBankErrorState.INVALID -> process(
                BankAuthIntent.BankAuthErrorState(ErrorState.LinkedBankInvalid)
            )
            LinkedBankErrorState.NONE -> {
                // check the state is not a linking final state
                if (it.state == LinkedBankState.BLOCKED) {
                    process(BankAuthIntent.BankAuthErrorState(ErrorState.BankLinkingFailed))
                } else {
                    // do nothing
                }
            }
        }.exhaustive
    }
}