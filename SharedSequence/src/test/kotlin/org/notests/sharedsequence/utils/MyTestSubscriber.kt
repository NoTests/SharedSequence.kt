package org.notests.sharedsequence.utils

import io.reactivex.disposables.Disposable
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit

/**
 * Created by juraj begovac on 13/9/18
 */
class MyTestSubscriber<T>(private val scheduler: TestScheduler) : TestObserver<T>() {
  override fun onSubscribe(d: Disposable) {
  }

  private var recordedEvents: List<Recorded<Event>> = emptyList()

  override fun onComplete() {
    super.onComplete()
    recordedEvents += Recorded(scheduler.now(TimeUnit.MILLISECONDS), Event.Complete as Event)
  }

  override fun onError(e: Throwable) {
    super.onError(e)
    recordedEvents += Recorded(scheduler.now(TimeUnit.MILLISECONDS), Event.Error(e))
  }

  override fun onNext(t: T) {
    super.onNext(t)
    recordedEvents += Recorded(scheduler.now(TimeUnit.MILLISECONDS), Event.Next(t))
  }

  fun recordedEvents() = recordedEvents
}
