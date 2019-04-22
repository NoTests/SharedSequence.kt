package org.notests.sharedsequence.api

import io.reactivex.subjects.PublishSubject

/**
 * Created by markotron on 11/6/17.
 */
object ErrorReporting {
  private val reportingSubject = PublishSubject.create<Throwable>()

  fun report(throwable: Throwable) {
    reportingSubject.onNext(throwable)
  }

  fun exceptions() = reportingSubject
}

