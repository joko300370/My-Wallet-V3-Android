package piuk.blockchain.android.util.lifecycle

import io.reactivex.subjects.PublishSubject

class LifecycleInterestedComponent {
    val appStateUpdated: PublishSubject<AppState> = PublishSubject.create()
}