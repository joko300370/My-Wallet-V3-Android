package piuk.blockchain.android.simplebuy

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.blockchain.swap.nabu.datamanagers.BankDetail
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.R
import piuk.blockchain.android.util.CopyableTextFormItem

class BankDetailsContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    fun initWithBankDetailsAndAmount(
        bankDetails: List<BankDetail>,
        amount: FiatValue,
        copyFieldListener: CopyFieldListener? = null
    ) {
        if (childCount == 0) {
            bankDetails.forEach {
                addView(
                    CopyableTextFormItem(
                        context = context,
                        title = it.title,
                        value = it.value,
                        isCopyable = it.isCopyable,
                        onCopy = { field -> copyFieldListener?.onFieldCopied(field) }
                    ),
                    LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                    )
                )
            }
            addView(
                CopyableTextFormItem(
                    context = context,
                    title = context.getString(R.string.simple_buy_amount_to_send),
                    value = amount.toStringWithSymbol()
                ),
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                )
            )
        }
    }
}

interface CopyFieldListener {
    fun onFieldCopied(field: String)
}