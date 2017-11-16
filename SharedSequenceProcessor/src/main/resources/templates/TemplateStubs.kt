package templates

/**
 * Created by markotron on 11/6/17.
 */
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

val _Template_.Companion._scheduler_ get() = Schedulers.io()
val _Template2_.Companion._scheduler_ get() = Schedulers.io()

fun <T> _share_(source: Observable<T>) = source

open class _Template2_<Element> constructor(source: Observable<Element>) {
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