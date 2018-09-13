package org.notests.sharedsequence.utils

import io.reactivex.Notification
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.merge
import io.reactivex.schedulers.TestScheduler
import java.util.concurrent.TimeUnit

/**
 * Created by juraj begovac on 13/9/18
 */

@Suppress("UNCHECKED_CAST")
fun <T> TestScheduler.createColdObservable(vararg events: Recorded<Event>): Observable<T> =
  events.map { (delay, value) ->
    Observable.timer(delay, TimeUnit.MILLISECONDS, this)
      .map {
        when (value) {
          is Event.Next<*>  -> Notification.createOnNext(value.value as T)
          is Event.Complete -> Notification.createOnComplete<Long>()
          is Event.Error    -> Notification.createOnError(value.error)
        }
      }
  }.merge().dematerialize()

fun TestScheduler.scheduleAt(delay: Long, action: () -> Unit): Disposable =
  this.createWorker().schedule(action, delay, TimeUnit.MILLISECONDS)

fun TestScheduler.advanceTimeBy(delay: Long) =
  this.advanceTimeBy(delay, TimeUnit.MILLISECONDS)

fun <T> TestScheduler.createMyTestSubscriber() = MyTestSubscriber<T>(this)
