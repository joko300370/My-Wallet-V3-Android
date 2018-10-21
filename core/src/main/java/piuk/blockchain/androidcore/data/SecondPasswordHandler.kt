package piuk.blockchain.androidcore.data

interface SecondPasswordHandler {

    interface ResultListener {

        fun onNoSecondPassword()

        fun onSecondPasswordValidated(validatedSecondPassword: String)
    }

    fun validate(listener: ResultListener)
}
