package piuk.blockchain.android.ui.base

interface FlowFragment {
    // methods for handling back press and navigation back arrow
    // return true if we want the screen to get popped or handle the back press itself
    fun onBackPressed(): Boolean

    // return true if we want the screen to handle the back press and not get popped
    fun backPressedHandled(): Boolean = false
}