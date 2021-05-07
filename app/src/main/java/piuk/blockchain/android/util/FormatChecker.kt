package piuk.blockchain.android.util

import java.util.regex.Pattern

class FormatChecker {
    private val phonePattern =
        Pattern.compile("(\\+[1-9]{1}[0-9]{1,2}+|00[1-9]{1}[0-9]{1,2}+)[\\(\\)\\.\\-\\s\\d]{6,16}")

    fun isValidEmailAddress(address: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(address).matches()
    }

    fun isValidMobileNumber(mobile: String): Boolean {
        return phonePattern.matcher(mobile).matches()
    }
}