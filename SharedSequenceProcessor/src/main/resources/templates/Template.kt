package templates

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.functions.BiFunction
import org.notests.sharedsequence.annotations.ErrorReporting
import org.notests.sharedsequence.annotations.debug
import org.notests.sharedsequence.annotations.doOnSubscribed
import java.util.concurrent.TimeUnit

open class _Template_<Element> constructor(source: Observable<Element>) {
  internal val source: Observable<Element> = share(source)
  fun asObservable() = source

  companion object {
    val scheduler: Scheduler
      get() {
        return _scheduler_
      }

    fun <Element> share(source: Observable<Element>): Observable<Element> =
      _share_(source)
  }
}

fun <Element> Observable<Element>.as_Template_(onError: _Template_<Element>) =
        _Template_(this.onErrorResumeNext(onError.asObservable()))

fun <Element> Observable<Element>.as_Template_(onError: (Throwable) -> _Template_<Element>) =
        _Template_(this.onErrorResumeNext { error: Throwable -> onError(error).asObservable() })

fun <Element> _Template_<Element>.debug(id: String,
                                        logger: (String) -> Unit): _Template_<Element> =
  _Template_(this.source.debug(id, logger))

// elementary

fun <Element> _Template_.Companion.just(element: Element): _Template_<Element> =
  _Template_(Observable.just(element).subscribeOn(scheduler))

fun <Element> _Template_.Companion.empty(): _Template_<Element> =
  _Template_(Observable.empty<Element>().subscribeOn(scheduler))

fun <Element> _Template_.Companion.never(): _Template_<Element> =
  _Template_(Observable.never<Element>().subscribeOn(scheduler))

fun <Element> _Template_.Companion.merge(sources: Iterable<_Template_<out Element>>): _Template_<Element> =
  try {
    _Template_(Observable.merge(sources.map { it.source }))
  } catch (e: Exception) {
    ErrorReporting.fatalErrorInDebugOrReportError(e)
    this.empty()
  }

fun <T1, T2> _Template_.Companion.zip(o1: _Template_<T1>, o2: _Template_<T2>): _Template_<Pair<T1, T2>> =
  _Template_(Observable.zip(o1.asObservable(), o2.asObservable(), BiFunction { t1, t2 -> Pair(t1, t2) }))

fun <T1, T2, R> _Template_.Companion.zip(o1: _Template_<T1>, o2: _Template_<T2>, zipFunction: (T1, T2) -> R): _Template_<R> =
  this.zip(o1, o2).map { zipFunction(it.first, it.second) }

// operations

fun <Element> _Template_<Element>.defer(factory: () -> _Template_<Element>) =
  _Template_(Observable.defer {
    try {
      factory().source
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
      Observable.empty<Element>()
    }
  })

fun <Element> _Template_<Element>.filter(predicate: (Element) -> Boolean): _Template_<Element> =
  _Template_<Element>(this.source.filter {
    try {
      predicate(it)
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
      false
    }
  })

fun <Element, Result> _Template_<Element>.map(func: (Element) -> Result): _Template_<Result> =
  _Template_<Result>(this.source.flatMap {
    try {
      Observable.just(func(it))
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
      Observable.empty<Result>()
    }
  })

fun <Element, Key : Comparable<Key>> _Template_<Element>.distinctUntilChanged(
  keySelector: (Element) -> Key): _Template_<Element> =
  _Template_<Element>(this.source.distinctUntilChanged { lhs, rhs ->
    try {
      keySelector(lhs).compareTo(keySelector(rhs)) == 0
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
      false
    }
  })

fun <Element> _Template_<Element>.distinctUntilChanged(comparator: (Element, Element) -> Boolean): _Template_<Element> =
  _Template_<Element>(this.source.distinctUntilChanged { lhs, rhs ->
    try {
      comparator(lhs, rhs)
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
      false
    }
  })

fun <Element> _Template_<Element>.distinctUntilChanged(): _Template_<Element> =
  this.distinctUntilChanged { lhs, rhs -> lhs == rhs }

fun <Element, Result> _Template_<Element>.scan(initialValue: Result, accumulator: (Result, Element) -> Result): _Template_<Result> =
  _Template_<Result>(this.source.scan(initialValue, { result, element ->
    try {
      accumulator(result, element)
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
      result
    }
  }))

fun <Element> _Template_<Element>.doOnNext(onNext: (Element) -> Unit): _Template_<Element> =
  _Template_<Element>(this.source.doOnNext {
    try {
      onNext(it)
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
    }
  })

fun <Element> _Template_<Element>.doOnCompleted(onCompleted: () -> Unit): _Template_<Element> =
  _Template_<Element>(this.source.doOnComplete {
    try {
      onCompleted()
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
    }
  })

fun <Element> _Template_<Element>.doOnSubscribe(onSubscribe: () -> Unit): _Template_<Element> =
  _Template_<Element>(this.source.doOnSubscribe {
    try {
      onSubscribe()
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
    }
  })

fun <Element> _Template_<Element>.doOnSubscribed(onSubscribed: () -> Unit): _Template_<Element> =
  _Template_<Element>(this.source.doOnSubscribed {
    try {
      onSubscribed()
    } catch (e: Exception) {
      ErrorReporting.fatalErrorInDebugOrReportError(e)
    }
  })

fun <Element> _Template_<Element>.startWith(element: Element): _Template_<Element> =
  _Template_<Element>(this.source.startWith(element))

fun <Element> _Template_<Element>.debounce(timeout: Long, unit: TimeUnit): _Template_<Element> =
  _Template_<Element>(this.source.debounce(timeout, unit))

fun <Element> _Template_<Element>.delay(timeout: Long, unit: TimeUnit): _Template_<Element> =
  _Template_<Element>(this.source.delay(timeout, unit))