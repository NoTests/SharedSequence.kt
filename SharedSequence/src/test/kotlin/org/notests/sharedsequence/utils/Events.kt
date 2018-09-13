package org.notests.sharedsequence.utils

/**
 * Created by juraj begovac on 13/9/18
 */
sealed class Event {
  data class Next<T>(val value: T) : Event()
  data class Error(val error: Throwable) : Event()
  object Complete : Event()
}

data class Recorded<out T>(val delay: Long, val value: T)

fun <T> next(delay: Long, value: T): Recorded<Event> = Recorded(delay, Event.Next(value))
fun error(delay: Long, error: Throwable): Recorded<Event> = Recorded(delay, Event.Error(error))

fun complete(delay: Long): Recorded<Event> = Recorded(delay, Event.Complete)
