package org.notests.sharedsequence.utils

import io.reactivex.Notification
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

  private var recordedEvents: List<Recorded<Notification<T>>> = emptyList()

  override fun onComplete() {
    super.onComplete()
    recordedEvents += Recorded(scheduler.now(TimeUnit.MILLISECONDS), Notification.createOnComplete())
  }

  override fun onError(e: Throwable) {
    super.onError(e)
    recordedEvents += Recorded(scheduler.now(TimeUnit.MILLISECONDS), Notification.createOnError(e))
  }

  override fun onNext(t: T) {
    super.onNext(t)
    recordedEvents += Recorded(scheduler.now(TimeUnit.MILLISECONDS), Notification.createOnNext(t))
  }

  fun recordedEvents() = recordedEvents
}
