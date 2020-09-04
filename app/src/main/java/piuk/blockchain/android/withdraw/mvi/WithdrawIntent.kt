package piuk.blockchain.android.withdraw.mvi

import com.blockchain.swap.nabu.datamanagers.LinkedBank
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.simplebuy.ErrorState
import piuk.blockchain.android.ui.base.mvi.MviIntent

sealed class WithdrawIntent : MviIntent<WithdrawState> {

    class CreateWithdrawOrder(val bankId: String, val amount: FiatValue) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState.copy(isLoading = true, withdrawRequested = true)

        override fun isValidFor(oldState: WithdrawState): Boolean {
            return !oldState.withdrawRequested
        }
    }

    data class UpdateCurrency(val currency: String) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            WithdrawState(currency = currency)
    }

    object WithdrawOrderCreated : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState.copy(isLoading = false, withdrawSucceeded = true, errorState = null)
    }

    data class BalanceUpdated(val balance: FiatValue) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState.copy(balance = balance)
    }

    data class BanksUpdated(private val linkedBanks: List<LinkedBank>) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState.copy(linkedBanks = linkedBanks, selectedBank = linkedBanks.getOrNull(0))
    }

    data class AmountUpdated(private val amount: FiatValue) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState.copy(amount = amount)
    }

    class FetchLinkedBanks(val currency: String) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState
    }

    class FetchWithdrawFee(val currency: String) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState
    }

    class FeeUpdated(private val fee: FiatValue) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState.copy(fee = fee)
    }

    class SelectedBankUpdated(private val bank: LinkedBank) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState.copy(selectedBank = bank)
    }

    class ErrorIntent(private val error: ErrorState = ErrorState.GenericError) : WithdrawIntent() {
        override fun reduce(oldState: WithdrawState): WithdrawState =
            oldState.copy(errorState = error, isLoading = false)
    }
}