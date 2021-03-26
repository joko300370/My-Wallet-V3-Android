package piuk.blockchain.android.ui.dashboard.announcements

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.gone
import piuk.blockchain.android.util.isVisible
import piuk.blockchain.android.util.visible
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import com.blockchain.notifications.analytics.Analytics
import piuk.blockchain.android.databinding.ItemAnnouncementMiniBinding
import piuk.blockchain.android.databinding.ItemAnnouncementStandardBinding

class StdAnnouncementDelegate<in T>(private val analytics: Analytics) :
    AdapterDelegate<T> {

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) {
        val announcement = items[position] as StandardAnnouncementCard

        (holder as AnnouncementViewHolder).apply {

            if (announcement.titleText != 0) {
                title.setText(announcement.titleText)
                title.visible()
            } else {
                title.gone()
            }

            if (announcement.background != 0) {
                container.setBackgroundResource(announcement.background)
            } else {
                container.setBackgroundColor(Color.WHITE)
            }

            if (announcement.bodyText != 0) {
                body.setText(announcement.bodyText)
                body.visible()
            } else {
                body.gone()
            }

            if (announcement.iconImage != 0) {
                icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, announcement.iconImage))
                if (announcement.shouldWrapIconWidth) {
                    // This is only used to display the vector_aave_yfi_dot_announcement icon in the correct size
                    icon.layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT
                }
                icon.visible()
            } else {
                icon.gone()
            }

            if (announcement.ctaText != 0) {
                ctaBtn.setText(announcement.ctaText)
                ctaBtn.setOnClickListener {
                    analytics.logEvent(AnnouncementAnalyticsEvent.CardActioned(announcement.name))
                    announcement.ctaClicked()
                }
                ctaBtn.visible()
            } else {
                ctaBtn.gone()
            }

            if (announcement.dismissText != 0) {
                dismissBtn.setText(announcement.dismissText)
                dismissBtn.setOnClickListener {
                    analytics.logEvent(AnnouncementAnalyticsEvent.CardDismissed(announcement.name))
                    announcement.dismissClicked()
                }
                dismissBtn.visible()
                closeBtn.gone()
            } else {
                dismissBtn.gone()
            }

            if (announcement.dismissRule != DismissRule.CardPersistent) {
                closeBtn.setOnClickListener {
                    analytics.logEvent(AnnouncementAnalyticsEvent.CardDismissed(announcement.name))
                    announcement.dismissClicked()
                }
                closeBtn.visible()
            } else {
                closeBtn.gone()
                dismissBtn.gone()
            }

            paintButtons(announcement.buttonColor)
        }
        analytics.logEvent(AnnouncementAnalyticsEvent.CardShown(announcement.name))
    }

    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position]
        return item is StandardAnnouncementCard
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {

        val binding = ItemAnnouncementStandardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnnouncementViewHolder(binding)
    }

    private class AnnouncementViewHolder constructor(
        binding: ItemAnnouncementStandardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val container: View = binding.cardContainer
        val icon: ImageView = binding.icon
        val title: TextView = binding.msgTitle
        val body: TextView = binding.msgBody
        val closeBtn: ImageView = binding.btnClose
        val ctaBtn: TextView = binding.btnCta1
        val dismissBtn: TextView = binding.btnDismiss

        fun paintButtons(@ColorRes btnColour: Int) {
            val colour = ContextCompat.getColor(ctaBtn.context, btnColour)
            if (ctaBtn.isVisible()) {
                ctaBtn.setBackgroundColor(colour)
            }

            if (dismissBtn.isVisible()) {
                val bgColour = ContextCompat.getColor(ctaBtn.context, R.color.announce_background)
                val gd = GradientDrawable()
                gd.setColor(bgColour)
                gd.setStroke(2, colour)
                dismissBtn.background = gd
                dismissBtn.setTextColor(colour)
            }
        }
    }
}

class MiniAnnouncementDelegate<in T>(private val analytics: Analytics) :
    AdapterDelegate<T> {
    override fun isForViewType(items: List<T>, position: Int): Boolean {
        val item = items[position]
        return item is MiniAnnouncementCard
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemAnnouncementMiniBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AnnouncementViewHolder(binding)
    }

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) {
        val announcement = items[position] as MiniAnnouncementCard
        (holder as AnnouncementViewHolder).apply {
            if (announcement.titleText != 0) {
                title.setText(announcement.titleText)
                title.visible()
            } else {
                title.gone()
            }
            if (announcement.bodyText != 0) {
                body.setText(announcement.bodyText)
                body.visible()
            } else {
                body.gone()
            }
            if (announcement.iconImage != 0) {
                icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, announcement.iconImage))
                icon.visible()
            } else {
                icon.gone()
            }

            if (announcement.background != 0) {
                cardContainer.setBackgroundResource(announcement.background)
            } else {
                cardContainer.setBackgroundColor(Color.WHITE)
            }

            if (announcement.hasCta) {
                cardContainer.setOnClickListener {
                    analytics.logEvent(AnnouncementAnalyticsEvent.CardActioned(announcement.name))
                    announcement.ctaClicked()
                }
                actionIcon.visible()
            } else {
                actionIcon.gone()
            }
        }
        analytics.logEvent(AnnouncementAnalyticsEvent.CardShown(announcement.name))
    }

    private class AnnouncementViewHolder constructor(
        binding: ItemAnnouncementMiniBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        val icon: ImageView = binding.icon
        val title: TextView = binding.msgTitle
        val body: TextView = binding.msgBody
        val cardContainer: View = binding.miniCardContainer
        val actionIcon: ImageView = binding.actionIcon
    }
}