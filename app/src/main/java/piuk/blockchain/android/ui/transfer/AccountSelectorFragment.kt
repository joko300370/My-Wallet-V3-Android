package piuk.blockchain.android.ui.transfer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.blockchain.koin.scopedInject
import io.reactivex.Single
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.databinding.FragmentTransferAccountSelectorBinding
import piuk.blockchain.android.ui.customviews.IntroHeaderView
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.account.StatusDecorator
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.visible

abstract class AccountSelectorFragment : Fragment() {

    private var _binding: FragmentTransferAccountSelectorBinding? = null
    private val binding: FragmentTransferAccountSelectorBinding
        get() = _binding!!

    private val coincore: Coincore by scopedInject()
    private val accountsSorting: AccountsSorting by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransferAccountSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.accountSelectorAccountList) {
            onLoadError = ::doOnLoadError
            onListLoaded = ::doOnListLoaded
        }
    }

    fun initialiseAccountSelectorWithHeader(
        statusDecorator: StatusDecorator,
        onAccountSelected: (BlockchainAccount) -> Unit,
        @StringRes title: Int,
        @StringRes label: Int,
        @DrawableRes icon: Int
    ) {
        val introHeaderView = IntroHeaderView(requireContext())
        introHeaderView.setDetails(title, label, icon)

        with(binding.accountSelectorAccountList) {
            this.onAccountSelected = onAccountSelected
            initialise(
                accounts(),
                statusDecorator,
                introHeaderView
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun refreshItems() {
        binding.accountSelectorAccountList.loadItems(
            accounts()
        )
    }

    private fun accounts(): Single<List<BlockchainAccount>> =
        coincore.allWalletsWithActions(setOf(fragmentAction), accountsSorting.sorter()).map {
            it.map { account -> account }
        }

    protected abstract val fragmentAction: AssetAction

    protected fun setEmptyStateDetails(
        @StringRes title: Int,
        @StringRes label: Int,
        @StringRes ctaText: Int,
        action: () -> Unit
    ) {
        binding.accountSelectorEmptyView.setDetails(
            title = title, description = label, ctaText = ctaText
        ) {
            action()
        }
    }

    @CallSuper
    protected open fun doOnEmptyList() {
        with(binding) {
            accountSelectorAccountList.gone()
            accountSelectorEmptyView.visible()
        }
    }

    @CallSuper
    protected open fun doOnListLoaded() {
        with(binding) {
            accountSelectorAccountList.visible()
            accountSelectorEmptyView.gone()
        }
    }

    private fun doOnLoadError(t: Throwable) {
        ToastCustom.makeText(
            requireContext(),
            getString(R.string.transfer_wallets_load_error),
            ToastCustom.LENGTH_SHORT,
            ToastCustom.TYPE_ERROR
        )
        doOnEmptyList()
    }

    private fun doOnListLoaded(isEmpty: Boolean) {
        if (isEmpty) doOnEmptyList() else doOnListLoaded()
    }
}