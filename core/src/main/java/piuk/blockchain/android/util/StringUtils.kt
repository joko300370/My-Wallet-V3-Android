package piuk.blockchain.android.util

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.Typeface
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

class StringUtils(private val context: Context) {

    @Deprecated("Don't be get getting strings in non-UI code")
    fun getString(@StringRes stringId: Int): String {
        return context.getString(stringId)
    }

    @Deprecated("Use the companion version")
    fun getStringWithMappedAnnotations(
        @StringRes stringId: Int,
        linksMap: Map<String, Uri?>,
        ctx: Context,
        onClick: () -> Unit = {}
    ): CharSequence = getStringWithMappedAnnotations(ctx, stringId, linksMap, onClick)

    companion object {
        fun getStringWithMappedAnnotations(
            context: Context,
            @StringRes stringId: Int,
            linksMap: Map<String, Uri?>,
            onClick: () -> Unit = {}
        ): CharSequence {

            val text = context.getText(stringId)
            val rawText = text as? SpannedString ?: return text
            val out = SpannableString(rawText)
            for (annotation in rawText.getSpans(0, rawText.length, android.text.Annotation::class.java)) {
                if (annotation.key == "link") {
                    out.setSpan(
                        ClickableSpanWithoutUnderline {
                            linksMap[annotation.value]?.let {
                                val intent = Intent(Intent.ACTION_VIEW, it).addFlags(FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                            onClick()
                        },
                        rawText.getSpanStart(annotation),
                        rawText.getSpanEnd(annotation),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                if (annotation.key == "font" && annotation.value == "bold") {
                    out.setSpan(
                        StyleSpan(Typeface.BOLD),
                        rawText.getSpanStart(annotation),
                        rawText.getSpanEnd(annotation),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return out
        }

        fun getResolvedStringWithAppendedMappedLearnMore(
            staticText: String,
            @StringRes textToMap: Int,
            url: String,
            context: Context,
            @ColorRes linkColour: Int,
            onClick: () -> Unit = {}
        ): SpannableStringBuilder {
            val map = mapOf("learn_more_link" to Uri.parse(url))
            val learnMoreLink = getStringWithMappedAnnotations(
                context,
                textToMap,
                map,
                onClick
            )

            val sb = SpannableStringBuilder()
                .append(staticText)
                .append(learnMoreLink)
            sb.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, linkColour)),
                staticText.length, staticText.length + learnMoreLink.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return sb
        }
    }
}

private class ClickableSpanWithoutUnderline(val onClick: () -> Unit) : ClickableSpan() {
    override fun onClick(widget: View) {
        onClick.invoke()
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.isUnderlineText = false
    }
}
