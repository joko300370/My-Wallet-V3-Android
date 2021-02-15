package piuk.blockchain.android.ui.airdrops

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.blockchain.extensions.exhaustive
import com.blockchain.koin.scopedInject
import com.blockchain.ui.urllinks.STX_STACKS_LEARN_MORE
import piuk.blockchain.android.R
import piuk.blockchain.android.campaign.blockstackCampaignName
import piuk.blockchain.android.campaign.sunriverCampaignName
import piuk.blockchain.android.databinding.DialogAirdropStatusBinding
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.goneIf
import piuk.blockchain.android.util.visible
import java.lang.IllegalStateException
import java.text.DateFormat

class AirdropStatusSheet : SlidingModalBottomDialog<DialogAirdropStatusBinding>(), AirdropCentreView {

    private val presenter: AirdropCentrePresenter by scopedInject()

    private val airdropName: String by unsafeLazy {
        arguments?.getString(ARG_AIRDROP_NAME) ?: blockstackCampaignName
    }

    override fun initBinding(inflater: LayoutInflater, container: ViewGroup?): DialogAirdropStatusBinding =
        DialogAirdropStatusBinding.inflate(inflater, container, false)

    override fun initControls(binding: DialogAirdropStatusBinding) {
        binding.ctaButton.setOnClickListener { onCtaClick() }
        presenter.attachView(this)
    }

    @SuppressLint("SetTextI18n")
    override fun renderList(statusList: List<Airdrop>) {

        val airdrop = statusList.find { it.name == airdropName }
            ?: throw IllegalStateException("No $airdropName airdrop found")

        when (airdropName) {
            sunriverCampaignName -> {
                renderSunriver(airdrop)
            }
            blockstackCampaignName -> {
                renderBlockstacks(airdrop)
            }
        }
    }

    private fun renderBlockstacks(airdrop: Airdrop) {
        with(binding) {
            title.setText(R.string.airdrop_sheet_stx_title)
            body.setText(R.string.airdrop_sheet_stx_body)
            iconCrypto.setImageResource(R.drawable.ic_logo_stx)
        }

        renderStatus(airdrop)
        renderDate(airdrop)
        renderAmount(airdrop)

        if (airdrop.status == AirdropState.RECEIVED) {
            showSupportInfo(
                R.string.airdrop_sheet_stx_where_are_my_stacks_title,
                R.string.airdrop_sheet_stx_where_are_my_stacks,
                Uri.parse(STX_STACKS_LEARN_MORE)
            )
        }
    }

    private fun renderSunriver(airdrop: Airdrop) {
        with(binding) {
            title.setText(R.string.airdrop_sheet_xlm_title)
            body.gone()
            iconCrypto.setImageResource(R.drawable.vector_xlm_colored)
        }
        renderStatus(airdrop)
        renderDate(airdrop)
        renderAmount(airdrop)
    }

    private fun renderStatus(airdrop: Airdrop) {
        when (airdrop.status) {
            AirdropState.UNKNOWN ->
                setStatusView(
                    R.string.airdrop_status_unknown,
                    R.color.black,
                    R.drawable.bkgd_status_unknown
                )
            AirdropState.EXPIRED ->
                setStatusView(
                    R.string.airdrop_status_expired,
                    R.color.grey_600,
                    R.drawable.bkgd_grey_100_rounded
                )
            AirdropState.PENDING ->
                setStatusView(
                    R.string.airdrop_status_pending,
                    R.color.blue_600,
                    R.drawable.bkgd_status_pending
                )
            AirdropState.RECEIVED ->
                setStatusView(
                    R.string.airdrop_status_received,
                    R.color.green_600,
                    R.drawable.bkgd_green_100_rounded
                )
            AirdropState.REGISTERED -> throw NotImplementedError("AirdropState.REGISTERED")
        }.exhaustive
    }

    private fun setStatusView(
        @StringRes message: Int,
        @ColorRes textColour: Int,
        @DrawableRes background: Int
    ) {
        with(binding.statusValue) {
            setText(message)
            setTextColor(ContextCompat.getColor(context, textColour))
            setBackground(ContextCompat.getDrawable(context, background))
        }
    }

    private fun renderDate(airdrop: Airdrop) {
        airdrop.date?.let {
            val formatted = DateFormat.getDateInstance(DateFormat.SHORT).format(it)
            binding.dateValue.text = formatted
        }
    }

    @SuppressLint("SetTextI18n")
    private fun renderAmount(airdrop: Airdrop) {

        val amount = if (airdrop.amountCrypto != null) {
            "${airdrop.amountCrypto.toStringWithSymbol()} (${airdrop.amountFiat?.toStringWithSymbol()})"
        } else {
            ""
        }

        with(binding) {
            amountValue.text = amount
            amountLabel.goneIf(amount.isEmpty())
            amountValue.goneIf(amount.isEmpty())
            dividerAmount.goneIf(amount.isEmpty())
        }
    }

    @Suppress("SameParameterValue")
    private fun showSupportInfo(@StringRes title: Int, @StringRes message: Int, link: Uri) {
        with(binding) {
            supportHeading.setText(title)
            supportHeading.visible()

            supportMessage.setText(message)
            supportMessage.visible()

            supportLink.setOnClickListener {
                context?.startActivity(Intent(Intent.ACTION_VIEW, link))
            }
            supportLink.visible()
        }
    }

    override fun onSheetHidden() {
        super.onSheetHidden()
        presenter.detachView(this)
        dialog?.let {
            onCancel(it)
        }
    }

    private fun onCtaClick() = dismiss()

    companion object {
        private const val ARG_AIRDROP_NAME = "AIRDROP_NAME"

        fun newInstance(airdropName: String): AirdropStatusSheet {
            return AirdropStatusSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_AIRDROP_NAME, airdropName)
                }
            }
        }
    }

    override fun renderListUnavailable() {
        dismiss()
    }

    override fun showProgressDialog(messageId: Int, onCancel: (() -> Unit)?) {}
    override fun dismissProgressDialog() {}
}
