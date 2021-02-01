package piuk.blockchain.android.networking

import io.reactivex.Single
import java.util.Date

sealed class PollResult<T> {
    class FinalResult<T>(value: T) : PollResult<T>()
    class TimeOut<T>(value: T) : PollResult<T>()
    object Cancel : PollResult<Nothing>()
}

class PollService <T>{

   /* private enum ServiceError: Error {
        case conditionNotMet
        case pollCancelled
        case timeout(Value)
    }*/

   private enum class ServiceError {
       ConditionNotMet,PollCancelled,TimeOut
   }
    private lateinit var endDate: Date

    private lateinit var fetch: (() -> Single<Value>)!
}