package piuk.blockchain.android.ui.customviews

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import piuk.blockchain.android.databinding.VerifyIdentityIconedBenefitLayoutBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate

class IconedBenefitsAdapter : AdapterDelegate<VerifyIdentityItem> {

    override fun isForViewType(items: List<VerifyIdentityItem>, position: Int): Boolean =
        items[position] is VerifyIdentityIconedBenefitItem

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = VerifyIdentityIconedBenefitLayoutBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return BenefitsViewHolder(binding)
    }

    override fun onBindViewHolder(items: List<VerifyIdentityItem>, position: Int, holder: RecyclerView.ViewHolder) {
        (holder as? BenefitsViewHolder)?.bind(items[position])
    }

    class BenefitsViewHolder(binding: VerifyIdentityIconedBenefitLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val titleView = binding.benefitTitle
        private val subtitleView = binding.benefitSubtitle
        private val icon = binding.icon

        fun bind(benefit: VerifyIdentityItem) {
            titleView.text = benefit.title
            subtitleView.text = benefit.subtitle
            icon.setImageResource((benefit as VerifyIdentityIconedBenefitItem).icon)
        }
    }
}