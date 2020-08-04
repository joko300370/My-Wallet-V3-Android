package piuk.blockchain.android.coincore.erc20

import com.blockchain.logging.CrashLogger
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.swap.nabu.datamanagers.CustodialWalletManager
import com.blockchain.swap.nabu.service.TierService
import com.blockchain.wallet.DefaultLabels
import io.reactivex.Completable
import piuk.blockchain.android.coincore.impl.CryptoAssetBase
import piuk.blockchain.android.thepit.PitLinking
import piuk.blockchain.androidcore.data.charts.ChartsDataManager
import piuk.blockchain.androidcore.data.erc20.Erc20Account
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager

internal abstract class Erc20TokensBase(
    protected val erc20Account: Erc20Account,
    protected val feeDataManager: FeeDataManager,
    custodialManager: CustodialWalletManager,
    exchangeRates: ExchangeRateDataManager,
    historicRates: ChartsDataManager,
    currencyPrefs: CurrencyPrefs,
    labels: DefaultLabels,
    pitLinking: PitLinking,
    crashLogger: CrashLogger,
    tiersService: TierService
) : CryptoAssetBase(
    exchangeRates,
    historicRates,
    currencyPrefs,
    labels,
    custodialManager,
    pitLinking,
    crashLogger,
    tiersService
) {
    final override fun initToken(): Completable = erc20Account.fetchErc20Address().ignoreElements()
}
