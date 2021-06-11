package piuk.blockchain.android.ui.swipetoreceive

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.blockchain.koin.scopedInject
import info.blockchain.balance.CryptoCurrency
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.databinding.FragmentSwipeToReceiveBinding
import piuk.blockchain.android.databinding.ItemImagePagerBinding
import piuk.blockchain.android.ui.customviews.toast
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.invisible
import piuk.blockchain.android.util.setOnPageChangeListener
import piuk.blockchain.android.util.visible
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.base.UiState

@Suppress("MemberVisibilityCanPrivate")
class SwipeToReceiveFragment : BaseFragment<SwipeToReceiveView, SwipeToReceivePresenter>(),
    SwipeToReceiveView {

    private var _binding: FragmentSwipeToReceiveBinding? = null
    private val binding: FragmentSwipeToReceiveBinding
        get() = _binding!!

    private val presenter: SwipeToReceivePresenter by inject()
    private val rxBus: RxBus by inject()
    private val assetResources: AssetResources by scopedInject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwipeToReceiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding) {
            listOf(imageviewQr, textviewAddress, textviewRequestCurrency).forEach {
                it.setOnClickListener { showClipboardWarning() }
            }

            imageviewLeftArrow.setOnClickListener {
                viewpagerIcons.currentItem = viewpagerIcons.currentItem - 1
            }
            imageviewRightArrow.setOnClickListener {
                viewpagerIcons.currentItem = viewpagerIcons.currentItem + 1
            }
        }

        onViewReady()
    }

    override fun initPager(assetList: List<CryptoCurrency>) {
        val assetImageList = assetList.map { assetResources.drawableResFilled(it) }
        val adapter = ImageAdapter(
            requireContext(),
            assetImageList
        )

        binding.viewpagerIcons.run {
            offscreenPageLimit = 3
            setAdapter(adapter)
            binding.indicator.setViewPager(this)
            setOnPageChangeListener {
                onPageSelected { index ->
                    presenter.onCurrencySelected(assetList[index])
                    with(binding) {
                        when (index) {
                            0 -> imageviewLeftArrow.invisible()
                            adapter.count - 1 -> imageviewRightArrow.invisible()
                            else -> listOf(imageviewLeftArrow, imageviewRightArrow).forEach { it.visible() }
                        }
                    }
                }
            }
        }
        assetList.firstOrNull()?.let {
            presenter.onCurrencySelected(it)
        }
    }

    override fun displayReceiveAddress(address: String) {
        binding.textviewAddress.text = address
    }

    override fun displayReceiveAccount(accountName: String) {
        binding.textviewAccount.text = accountName
    }

    override fun displayAsset(cryptoCurrency: CryptoCurrency) {
        val assetName = assetResources.assetName(cryptoCurrency)
        val requestString = getString(R.string.swipe_to_receive_request, assetName)
        with(binding.textviewRequestCurrency) {
            text = requestString
            contentDescription = requestString
        }
    }

    override fun setUiState(uiState: Int) {
        when (uiState) {
            UiState.LOADING -> displayLoading()
            UiState.CONTENT -> showContent()
            UiState.FAILURE -> showNoAddressesAvailable()
            UiState.EMPTY -> showNoAddressesAvailable()
        }
    }

    override fun displayQrCode(bitmap: Bitmap) {
        binding.imageviewQr.setImageBitmap(bitmap)
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
        rxBus.unregister(ActionEvent::class.java, event)
    }

    private val compositeDisposable = CompositeDisposable()

    private val event by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    override fun onStart() {
        super.onStart()
        compositeDisposable += event.subscribe {
            // Update UI with new Address + QR
            presenter.refresh()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun createPresenter() = presenter

    override fun getMvpView() = this

    private fun showContent() {
        with(binding) {
            layoutQr.visible()
            progressBar.gone()
            imageviewQr.visible()
            textviewError.gone()
        }
    }

    private fun displayLoading() {
        with(binding) {
            layoutQr.visible()
            progressBar.visible()
            imageviewQr.invisible()
            textviewError.gone()
        }
    }

    private fun showNoAddressesAvailable() {
        with(binding) {
            layoutQr.invisible()
            textviewError.visible()
            textviewAddress.text = ""
        }
    }

    private fun showClipboardWarning() {
        val address = binding.textviewAddress.text
        activity?.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.common_yes) { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Send address", address)
                    toast(R.string.copied_to_clipboard)
                    clipboard.setPrimaryClip(clip)
                }
                .setNegativeButton(R.string.common_no, null)
                .show()
        }
    }

    companion object {
        fun newInstance(): SwipeToReceiveFragment = SwipeToReceiveFragment()
    }

    private class ImageAdapter(var context: Context, var drawables: List<Int>) : PagerAdapter() {

        override fun getCount(): Int = drawables.size

        override fun isViewFromObject(view: View, any: Any): Boolean {
            return view === any as AppCompatImageView
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val imageViewBinding = ItemImagePagerBinding.inflate(LayoutInflater.from(context), container, false)
            val imageView = imageViewBinding.imageviewCurrencyIcon
            imageView.setImageDrawable(ContextCompat.getDrawable(context, drawables[position]))
            (container as ViewPager).addView(imageView, 0)
            return imageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
            container.removeView(any as View)
        }
    }
}
