package piuk.blockchain.android.ui.account.chooser

import piuk.blockchain.androidcoreui.ui.base.View

interface AccountChooserView : View {

    val accountMode: AccountMode

    fun updateUi(items: List<AccountChooserItem>)
}
