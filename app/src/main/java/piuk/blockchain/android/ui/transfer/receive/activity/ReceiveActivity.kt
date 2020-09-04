package piuk.blockchain.android.ui.transfer.receive.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.blockchain.koin.scopedInject
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.NullCryptoAccount
import piuk.blockchain.android.ui.base.BlockchainActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import timber.log.Timber

class ReceiveActivity : BlockchainActivity(),
    ReceiveFragment.ReceiveFragmentHost {

    private val coincore: Coincore by scopedInject()
    private val disposables = CompositeDisposable()

    override val alwaysDisableScreenshots = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.temp_activity_receive)
        setSupportActionBar(toolbar_general)
        title = getString(R.string.common_receive)
    }

    override fun onStart() {
        super.onStart()

        disposables += coincore.findAccountByName(intent.getStringExtra(ACCOUNT_ID))
            .subscribeBy(
                onSuccess = {
                    val f = ReceiveFragment.newInstance(it as CryptoAccount)
                    supportFragmentManager.beginTransaction().replace(R.id.content, f).commit()
                },
                onError = {
                    Timber.e("Failed to find account for receive")
                }
            )
    }

    override fun onStop() {
        disposables.clear()
        super.onStop()
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        finish()
    }

    override fun actionBackPress() = finish()

    companion object {

        private const val ACCOUNT_ID = "PARAM_ACCOUNT_ID"
        fun start(ctx: Context, account: CryptoAccount) {
            require(account !is NullCryptoAccount)
            ctx.startActivity(
                Intent(ctx, ReceiveActivity::class.java).apply {
                    putExtra(ACCOUNT_ID, account.label)
                }
            )
        }
    }
}
