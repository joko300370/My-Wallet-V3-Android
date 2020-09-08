package piuk.blockchain.android.ui.transfer.send.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.coincore.SendTarget
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.android.util.getAccount
import piuk.blockchain.android.util.getSendTarget
import piuk.blockchain.android.util.putAccount
import piuk.blockchain.android.util.putSendTarget
import piuk.blockchain.androidcore.utils.helperfunctions.consume

class SendActivity : BlockchainActivity(), SendFragment.SendFragmentHost {

    override val alwaysDisableScreenshots = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.temp_activity_send)
        setSupportActionBar(toolbar_general)
        title = getString(R.string.common_send)

        intent.getAccount(TX_SOURCE)?.let {
            val f = SendFragment.newInstance(
                sourceAccount = it as CryptoAccount,
                target = intent.getSendTarget(TX_TARGET)
            )
            supportFragmentManager.beginTransaction().replace(R.id.content, f).commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        finish()
    }

    override fun actionBackPress() = finish()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val TX_SOURCE = "SOURCE"
        private const val TX_TARGET = "TARGET"

        fun start(ctx: Context, source: CryptoAccount, target: SendTarget? = null) {
            require(source !is NullCryptoAccount)
            ctx.startActivity(
                Intent(ctx, SendActivity::class.java).apply {
                    putAccount(TX_SOURCE, source)
                    target?.let {
                        putSendTarget(TX_TARGET, it)
                    }
                }
            )
        }
    }
}
