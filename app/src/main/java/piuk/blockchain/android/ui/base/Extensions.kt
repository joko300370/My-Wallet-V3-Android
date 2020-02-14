package piuk.blockchain.android.ui.base

import androidx.fragment.app.FragmentActivity

fun FragmentActivity.setupToolbar(resource: Int, homeAsUpEnabled: Boolean = true) {
    (this as? BlockchainActivity)?.let {
        it.setupToolbar(
            it.supportActionBar ?: return,
            resource
        )
        it.supportActionBar?.setDisplayHomeAsUpEnabled(homeAsUpEnabled)
    }
}

fun FragmentActivity.setupToolbar(resource: String, homeAsUpEnabled: Boolean = true) {
    (this as? BlockchainActivity)?.let {
        it.setupToolbar(
            it.supportActionBar ?: return,
            resource
        )
        it.supportActionBar?.setDisplayHomeAsUpEnabled(homeAsUpEnabled)
    }
}