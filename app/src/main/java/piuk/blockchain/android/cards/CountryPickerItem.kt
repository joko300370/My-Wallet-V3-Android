package piuk.blockchain.android.cards

import java.util.Locale

data class CountryPickerItem(private val defCode: String = "") : PickerItem {

    override val code: String
        get() = if (defCode.isEmpty()) {
            Locale.getDefault().country.takeIf { it.isNotEmpty() } ?: DEFAULT_COUNTRY_CODE
        } else defCode

    private val locale = Locale("", code)
    override val label: String = locale.displayCountry

    override val icon: String by lazy {
        getFlagEmojiFromCountryCode(code)
    }

    companion object {
        private const val DEFAULT_COUNTRY_CODE = "US"
        private const val ASCII_OFFSET = 0x41
        private const val FLAG_OFFSET = 0x1F1E6
        private fun getFlagEmojiFromCountryCode(countryCode: String): String {
            val firstChar = Character.codePointAt(countryCode, 0) - ASCII_OFFSET + FLAG_OFFSET
            val secondChar = Character.codePointAt(countryCode, 1) - ASCII_OFFSET + FLAG_OFFSET
            return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
        }
    }
}

data class StatePickerItem(override val code: String, override val label: String) : PickerItem {
    override val icon: String? = null
}