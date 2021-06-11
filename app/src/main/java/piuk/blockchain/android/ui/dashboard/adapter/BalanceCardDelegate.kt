package piuk.blockchain.android.ui.dashboard.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import piuk.blockchain.android.R
import piuk.blockchain.android.coincore.AssetResources
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.databinding.ItemDashboardBalanceCardBinding
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.BalanceState
import piuk.blockchain.android.ui.dashboard.asDeltaPercent
import piuk.blockchain.android.ui.dashboard.setDeltaColour
import piuk.blockchain.android.util.context
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor

class BalanceCardDelegate<in T>(
    private val selectedFiat: String,
    private val coincore: Coincore,
    private val assetResources: AssetResources
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is BalanceState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BalanceCardViewHolder(
            ItemDashboardBalanceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            selectedFiat,
            coincore,
            assetResources
        )

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder
    ) = (holder as BalanceCardViewHolder).bind(items[position] as BalanceState)
}

private class BalanceCardViewHolder(
    private val binding: ItemDashboardBalanceCardBinding,
    private val selectedFiat: String,
    private val coincore: Coincore,
    private val assetResources: AssetResources
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(state: BalanceState) {
        configurePieChart()

        if (state.isLoading) {
            renderLoading()
        } else {
            renderLoaded(state)
        }
    }

    private fun renderLoading() {
        with(binding) {
            totalBalance.resetLoader()
            balanceDeltaValue.resetLoader()
            balanceDeltaPercent.resetLoader()
        }
        populateEmptyPieChart()
    }

    @SuppressLint("SetTextI18n")
    private fun renderLoaded(state: BalanceState) {

        with(binding) {
            totalBalance.text = state.fiatBalance?.toStringWithSymbol() ?: ""

            if (state.delta == null) {
                balanceDeltaValue.text = ""
                balanceDeltaPercent.text = ""
            } else {
                val (deltaVal, deltaPercent) = state.delta!!

                balanceDeltaValue.text = deltaVal.toStringWithSymbol()
                balanceDeltaValue.setDeltaColour(deltaPercent)
                balanceDeltaPercent.asDeltaPercent(deltaPercent, "(", ")")
            }

            populatePieChart(state)
        }
    }

    private fun populateEmptyPieChart() {
        with(binding) {
            val entries = listOf(PieEntry(100f))

            val sliceColours = listOf(context.getResolvedColor(R.color.grey_100))

            pieChart.data = PieData(
                PieDataSet(entries, null).apply {
                    sliceSpace = 5f
                    setDrawIcons(false)
                    setDrawValues(false)
                    colors = sliceColours
                })
            pieChart.invalidate()
        }
    }

    private fun populatePieChart(state: BalanceState) {
        with(binding) {
            val entries = ArrayList<PieEntry>().apply {
                coincore.cryptoAssets.forEach {
                    val asset = state[(it as CryptoAsset).asset]
                    val point = asset.fiatBalance?.toFloat() ?: 0f
                    add(PieEntry(point))
                }

                // Add all fiat from Funds
                add(PieEntry(state.getFundsFiat(selectedFiat).toFloat()))
            }

            if (entries.all { it.value == 0.0f }) {
                populateEmptyPieChart()
            } else {
                val sliceColours = coincore.cryptoAssets.map {
                    ContextCompat.getColor(itemView.context, assetResources.colorRes((it as CryptoAsset).asset))
                }.toMutableList()

                // Add colour for Funds
                sliceColours.add(ContextCompat.getColor(itemView.context, R.color.green_500))

                pieChart.data = PieData(
                    PieDataSet(entries, null).apply {
                        sliceSpace = SLICE_SPACE_DP
                        setDrawIcons(false)
                        setDrawValues(false)
                        colors = sliceColours
                    })
                pieChart.invalidate()
            }
        }
    }

    private fun configurePieChart() {
        with(binding.pieChart) {
            setDrawCenterText(false)

            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = PIE_HOLE_RADIUS

            setDrawEntryLabels(false)
            legend.isEnabled = false
            description.isEnabled = false

            setTouchEnabled(false)
            setNoDataText(null)
        }
    }

    companion object {
        private const val SLICE_SPACE_DP = 2f
        private const val PIE_HOLE_RADIUS = 85f
    }
}
