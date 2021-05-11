package piuk.blockchain.android.ui.customviews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.VerifyIdentityNumericBenefitLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class NumericBenefitsAdapter : AdapterDelegate<VerifyIdentityItem> {

    override fun isForViewType(items: List<VerifyIdentityItem>, position: Int): Boolean =
        items[position] is VerifyIdentityNumericBenefitItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = VerifyIdentityNumericBenefitLayoutBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return BenefitsViewHolder(binding)
    }

    override fun onBindViewHolder(items: List<VerifyIdentityItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as? BenefitsViewHolder)?.bind(items[position], position + 1)
    }

    class BenefitsViewHolder(binding: VerifyIdentityNumericBenefitLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val titleView = binding.benefitTitle
        private val subtitleView = binding.benefitSubtitle
        private val indexView = binding.benefitIndex

        fun bind(benefit: VerifyIdentityItem, index: Int) {
            titleView.text = benefit.title
            subtitleView.text = benefit.subtitle
            indexView.text = index.toString()
        }
    }
}