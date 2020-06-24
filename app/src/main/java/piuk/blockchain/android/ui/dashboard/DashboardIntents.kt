package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import piuk.blockchain.android.ui.base.mvi.MviIntent
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementCard
import piuk.blockchain.androidcore.data.charts.PriceSeries
import java.math.BigInteger

sealed class DashboardIntent : MviIntent<DashboardState>

class FiatBalanceUpdate(
    val balanceList: List<FiatValue>
): DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(
            fundsFiatBalances = FundsBalanceState(balanceList)
        )
    }
}

object RefreshAllIntent : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(assets = oldState.assets.reset())
    }
}

class BalanceUpdate(
    val cryptoCurrency: CryptoCurrency,
    private val newBalance: CryptoValue
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {

        require(cryptoCurrency == newBalance.currency) { throw IllegalStateException("CryptoCurrency mismatch") }

        val oldAsset = oldState[cryptoCurrency]
        val newAsset = oldAsset.copy(cryptoBalance = newBalance, hasBalanceError = false)
        val newAssets = oldState.assets.copy(patchAsset = newAsset)

        return oldState.copy(assets = newAssets)
    }
}

class BalanceUpdateError(
    val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState[cryptoCurrency]
        val newAsset = oldAsset.copy(
            cryptoBalance = CryptoValue(cryptoCurrency, BigInteger.ZERO),
            hasBalanceError = true
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)

        return oldState.copy(assets = newAssets)
    }
}

class CheckForCustodialBalanceIntent(
    val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState[cryptoCurrency]
        val newAsset = oldAsset.copy(
            hasCustodialBalance = false
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)
        return oldState.copy(assets = newAssets)
    }
}

class UpdateHasCustodialBalanceIntent(
    val cryptoCurrency: CryptoCurrency,
    private val hasCustodial: Boolean
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState[cryptoCurrency]
        val newAsset = oldAsset.copy(
            hasCustodialBalance = hasCustodial
        )
        val newAssets = oldState.assets.copy(patchAsset = newAsset)
        return oldState.copy(assets = newAssets)
    }
}

class RefreshPrices(
    val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState = oldState
}

class PriceUpdate(
    val cryptoCurrency: CryptoCurrency,
    private val latestPrice: FiatValue,
    private val oldPrice: FiatValue
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState.assets[cryptoCurrency]
        val newAsset = updateAsset(oldAsset, latestPrice, oldPrice)

        return oldState.copy(assets = oldState.assets.copy(patchAsset = newAsset))
    }

    private fun updateAsset(
        old: AssetState,
        latestPrice: FiatValue,
        oldPrice: FiatValue
    ): AssetState {
        return old.copy(
            price = latestPrice,
            price24h = oldPrice
        )
    }
}

class PriceHistoryUpdate(
    val cryptoCurrency: CryptoCurrency,
    private val historicPrices: PriceSeries
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        val oldAsset = oldState.assets[cryptoCurrency]
        val newAsset = updateAsset(oldAsset, historicPrices)

        return oldState.copy(assets = oldState.assets.copy(patchAsset = newAsset))
    }

    private fun updateAsset(
        old: AssetState,
        historicPrices: PriceSeries
    ): AssetState {
        val trend = historicPrices.filter { it.price != null }.map { it.price!!.toFloat() }

        return old.copy(priceTrend = trend)
    }
}

class ShowAnnouncement(private val card: AnnouncementCard) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(announcement = card)
    }
}

object ClearAnnouncement : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState {
        return oldState.copy(announcement = null)
    }
}

class ShowAssetDetails(
    private val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        when {
            oldState.showAssetSheetFor != null -> oldState
            oldState.shouldShowCustodialIntro(cryptoCurrency) ->
                oldState.copy(
                    showDashboardSheet = DashboardSheet.CUSTODY_INTRO,
                    pendingAssetSheetFor = cryptoCurrency,
                    showAssetSheetFor = null,
                    custodyIntroSeen = true
                )
            else -> oldState.copy(
                showAssetSheetFor = cryptoCurrency,
                pendingAssetSheetFor = null,
                showDashboardSheet = null
            )
        }
}

class ShowDashboardSheet(
    private val dashboardSheet: DashboardSheet
) : DashboardIntent() {
    override fun isValidFor(oldState: DashboardState): Boolean =
        dashboardSheet != DashboardSheet.CUSTODY_INTRO

    override fun reduce(oldState: DashboardState): DashboardState =
        // Custody sheet isn't displayed via this intent, so filter it out
        oldState.copy(
            showDashboardSheet = dashboardSheet,
            showAssetSheetFor = null
        )
}

class CancelSimpleBuyOrder(
    val orderId: String
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState = oldState
}

object ClearBottomSheet : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            showDashboardSheet = null,
            showAssetSheetFor = oldState.pendingAssetSheetFor,
            pendingAssetSheetFor = null
        )
}

class StartCustodialTransfer(
    private val cryptoCurrency: CryptoCurrency
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            showDashboardSheet = null,
            showAssetSheetFor = null,
            pendingAssetSheetFor = null,
            transferFundsCurrency = cryptoCurrency
        )
}

object AbortFundsTransfer : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(
            showDashboardSheet = null,
            transferFundsCurrency = null
            )
        }

object CheckBackupStatus : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        oldState
}

class BackupStatusUpdate(
    private val isBackedUp: Boolean
) : DashboardIntent() {
    override fun reduce(oldState: DashboardState): DashboardState =
        if (isBackedUp) {
            oldState.copy(showDashboardSheet = DashboardSheet.BASIC_WALLET_TRANSFER)
        } else {
            oldState.copy(showDashboardSheet = DashboardSheet.BACKUP_BEFORE_SEND)
        }
}

object TransferFunds : DashboardIntent() {
    override fun isValidFor(oldState: DashboardState): Boolean =
        oldState.transferFundsCurrency != null

    override fun reduce(oldState: DashboardState): DashboardState =
        oldState.copy(showDashboardSheet = DashboardSheet.BASIC_WALLET_TRANSFER)
    }
