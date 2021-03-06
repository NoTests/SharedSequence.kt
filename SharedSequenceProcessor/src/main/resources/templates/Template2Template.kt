package templates

/**
 * Created by markotron on 11/6/17.
 */
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import org.notests.sharedsequence.api.ErrorReporting

fun <Element, SequenceOfElements: _Template2_<Element>> _Template_<SequenceOfElements>.switchOnNext(): _Template2_<Element> =
  _Template2_<Element>(Observable.switchOnNext(this.source.map { it.asObservable() }))

fun <Element, T1> _Template_<Element>.withLatestFrom_Template2_(other: _Template2_<T1>): _Template_<Pair<Element, T1>> =
  _Template_<Pair<Element, T1>>(this.source.withLatestFrom(other.asObservable(), BiFunction { t1, t2 -> Pair(t1, t2) }))

fun <Element, Result> _Template_<Element>.flatMap_Template2_(func: (Element) -> _Template2_<Result>): _Template2_<Result> =
  _Template2_<Result>(this.source.flatMap {
    try {
      func(it).source
    } catch (e: Exception) {
      ErrorReporting.report(e)
      Observable.empty<Result>()
    }
  })

fun <Element, Result> _Template_<Element>.switchMap_Template2_(func: (Element) -> _Template2_<Result>): _Template2_<Result> =
  _Template2_<Result>(this.source.switchMap {
    try {
      func(it).source
    } catch (e: Exception) {
      ErrorReporting.report(e)
      Observable.empty<Result>()
    }
  })