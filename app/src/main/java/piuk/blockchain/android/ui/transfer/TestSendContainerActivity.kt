package piuk.blockchain.android.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.TransferSendFragment
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class TestSendContainerActivity : BaseAuthActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.stub_activity_test_send_container)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.container, TransferSendFragment.newInstance())
                .commit()
        }
    }

    companion object {
        fun start(ctx: Context) {
            ctx.startActivity(
                Intent(ctx, TestSendContainerActivity::class.java)
            )
        }
    }
}
