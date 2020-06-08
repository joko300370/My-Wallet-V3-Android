package piuk.blockchain.android.ui.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.transfer.send.TransferSendFragment

class TestSendContainerActivity : AppCompatActivity() {

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
