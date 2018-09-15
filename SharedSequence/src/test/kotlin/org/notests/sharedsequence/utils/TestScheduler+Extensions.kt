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

data class Recorded<out T>(val delay: Long, val value: T)

fun <T> next(delay: Long, value: T): Recorded<Notification<T>> = Recorded(delay, Notification.createOnNext(value))
fun <T> error(delay: Long, error: Throwable): Recorded<Notification<T>> = Recorded(delay, Notification.createOnError
(error))

fun <T> complete(delay: Long): Recorded<Notification<T>> = Recorded(delay, Notification.createOnComplete())

@Suppress("UNCHECKED_CAST")
fun <T> TestScheduler.createColdObservable(vararg events: Recorded<Notification<T>>): Observable<T> =
  events.map { (delay, value) ->
    Observable.timer(delay, TimeUnit.MILLISECONDS, this)
      .map {
        when (value.notificationType()) {
          is NotificationType.Next<*> -> Notification.createOnNext(value.value!!)
          is NotificationType.Error   -> Notification.createOnError(value.error!!)
          NotificationType.Complete   -> Notification.createOnComplete()
        }
      }
  }.merge().dematerialize()

fun TestScheduler.scheduleAt(delay: Long, action: () -> Unit): Disposable =
  this.createWorker().schedule(action, delay, TimeUnit.MILLISECONDS)

fun TestScheduler.advanceTimeBy(delay: Long) =
  this.advanceTimeBy(delay, TimeUnit.MILLISECONDS)

fun <T> TestScheduler.createMyTestSubscriber() = MyTestSubscriber<T>(this)

private sealed class NotificationType {
  data class Next<T>(val value: T) : NotificationType()
  data class Error(val error: Throwable) : NotificationType()
  object Complete : NotificationType()
}

private fun <T> Notification<T>.notificationType(): NotificationType {
  if (isOnNext)
    return NotificationType.Next(this.value!!)
  if (isOnError)
    return NotificationType.Error(this.error!!)
  return NotificationType.Complete
}
