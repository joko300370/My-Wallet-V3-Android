package piuk.blockchain.android.cards

interface CardDetailsPersistence {
    fun setCardData(cardData: CardData)
    fun getCardData(): CardData?
}