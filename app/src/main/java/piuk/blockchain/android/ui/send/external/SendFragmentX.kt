package piuk.blockchain.android.ui.send.external

import android.support.v4.app.Fragment

interface SendFragmentX {

    interface OnSendFragmentInteractionListener {

        fun onSendFragmentClose()
    }

    val fragment: Fragment

    fun onBackPressed()

    fun onChangeFeeClicked()

    fun onSendClicked()

    companion object {

        const val Tag = "FRAG_SendFragmentX"
    }
}
