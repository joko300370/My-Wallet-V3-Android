package piuk.blockchain.android.ui.send.external

import piuk.blockchain.android.ui.send.SendFragment

class SendFragmentXFactory {

    fun newInstance(newInstanceArguments: NewInstanceArguments): SendFragmentX {
        return SendFragment.newInstance(newInstanceArguments)
    }
}

sealed class NewInstanceArguments(val uri: String?) {

    class AccountPosition(
        uri: String?,
        val selectedAccountPosition: Int
    ) : NewInstanceArguments(uri)

    class Contact(
        val nonNullUri: String,
        val contactId: String,
        val contactMdid: String,
        val fctxId: String
    ) : NewInstanceArguments(nonNullUri)
}
