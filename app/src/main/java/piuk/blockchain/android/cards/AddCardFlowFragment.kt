package piuk.blockchain.android.cards

import piuk.blockchain.android.ui.base.FlowFragment

interface AddCardFlowFragment : FlowFragment {
    val navigator: AddCardNavigator
    val cardDetailsPersistence: CardDetailsPersistence
}