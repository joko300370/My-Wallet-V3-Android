package piuk.blockchain.android.cards

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.blockchain.koin.scopedInject
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import com.blockchain.preferences.SimpleBuyPrefs
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.PaymentMethod
import com.blockchain.nabu.datamanagers.custodialwalletimpl.CardStatus
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.databinding.FragmentAddNewCardBinding
import piuk.blockchain.android.ui.base.mvi.MviFragment
import piuk.blockchain.android.ui.base.setupToolbar
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.inflate
import piuk.blockchain.android.util.visible
import piuk.blockchain.android.util.AfterTextChangedWatcher
import java.util.Calendar
import java.util.Date

class AddNewCardFragment :
    MviFragment<CardModel, CardIntent, CardState, FragmentAddNewCardBinding>(), AddCardFlowFragment {

    override val model: CardModel by scopedInject()

    private var availableCards: List<PaymentMethod.Card> = emptyList()
    private val compositeDisposable = CompositeDisposable()
    private val custodialWalletManager: CustodialWalletManager by scopedInject()
    private val simpleBuyPrefs: SimpleBuyPrefs by inject()

    override val navigator: AddCardNavigator
        get() = (activity as? AddCardNavigator)
            ?: throw IllegalStateException("Parent must implement AddCardNavigator")

    override val cardDetailsPersistence: CardDetailsPersistence
        get() = (activity as? CardDetailsPersistence)
            ?: throw IllegalStateException("Parent must implement CardDetailsPersistence")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_add_new_card)

    private val textWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(s: Editable?) {
            with(binding) {
                btnNext.isEnabled = cardName.isValid && cardNumber.isValid && cvv.isValid && expiryDate.isValid
            }
            hideError()
        }
    }

    private fun hideError() {
        binding.sameCardError.gone()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)

        with(binding) {
            cardName.addTextChangedListener(textWatcher)
            cardNumber.addTextChangedListener(textWatcher)
            cvv.addTextChangedListener(textWatcher)
            expiryDate.addTextChangedListener(textWatcher)
            btnNext.apply {
                isEnabled = false
                setOnClickListener {
                    if (cardHasAlreadyBeenAdded()) {
                        showError()
                    } else {
                        cardDetailsPersistence.setCardData(
                            CardData(
                                fullName = cardName.text.toString(),
                                number = cardNumber.text.toString().replace(" ", ""),
                                month = expiryDate.month.toInt(),
                                year = expiryDate.year.toInt(),
                                cvv = cvv.text.toString()
                            )
                        )
                        activity.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

                        navigator.navigateToBillingDetails()
                        analytics.logEvent(SimpleBuyAnalytics.CARD_INFO_SET)
                    }
                }
            }

            compositeDisposable += custodialWalletManager.fetchUnawareLimitsCards(
                listOf(
                    CardStatus.PENDING,
                    CardStatus.ACTIVE
                )
            ).subscribeBy(onSuccess = {
                availableCards = it
            })
            cardNumber.displayCardTypeIcon(false)
        }
        activity.setupToolbar(R.string.add_card_title)
        analytics.logEvent(SimpleBuyAnalytics.ADD_CARD)

        setupCardInfo()
    }

    private fun setupCardInfo() {
        if (simpleBuyPrefs.addCardInfoDismissed) {
            binding.cardInfoGroup.gone()
        } else {
            binding.cardInfoClose.setOnClickListener {
                simpleBuyPrefs.addCardInfoDismissed = true
                binding.cardInfoGroup.gone()
            }
        }
    }

    private fun cardHasAlreadyBeenAdded(): Boolean {
        with(binding) {
            availableCards.forEach {
                if (it.expireDate.hasSameMonthAndYear(
                        month = expiryDate.month.toInt(),
                        year = expiryDate.year.toInt().asCalendarYear()
                    ) &&
                    cardNumber.text?.toString()?.takeLast(4) == it.endDigits &&
                    cardNumber.cardType == it.cardType
                )
                    return true
            }
            return false
        }
    }

    private fun showError() {
        binding.sameCardError.visible()
    }

    override fun render(newState: CardState) {}

    override fun onBackPressed(): Boolean = true

    private fun Date.hasSameMonthAndYear(year: Int, month: Int): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = this
        // calendar api returns months 0-11
        return calendar.get(Calendar.YEAR) == year && calendar.get(Calendar.MONTH) == month - 1
    }

    override fun onPause() {
        compositeDisposable.clear()
        super.onPause()
    }

    private fun Int.asCalendarYear(): Int =
        if (this < 100) 2000 + this else this

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentAddNewCardBinding =
        FragmentAddNewCardBinding.inflate(inflater, container, false)
}