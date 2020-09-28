package piuk.blockchain.android.ui.transfer.send.activity

@Deprecated("Old send. Do not use")
object SendModel {

    /** Large TX Size limit in KB  */
    const val LARGE_TX_SIZE = 1024

    /** Large TX limit fee in USD  */
    const val LARGE_TX_FEE = 0.5

    /** Large TX limit expressed as percentage, where percentage is found by diving the fee by the
     * amount  */
    const val LARGE_TX_PERCENTAGE = 1.0
}
