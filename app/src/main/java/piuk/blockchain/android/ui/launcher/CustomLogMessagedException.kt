package piuk.blockchain.android.ui.launcher

class CustomLogMessagedException(logMessage: String, throwable: Throwable) :
    Exception("$logMessage -- ${throwable.message}", throwable)