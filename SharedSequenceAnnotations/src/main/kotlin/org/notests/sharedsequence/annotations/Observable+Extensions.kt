package org.notests.sharedsequence.annotations

import io.reactivex.Notification
import io.reactivex.Observable

fun <Element> Observable<Element>.debug(id: String, logger: (String) -> Unit): Observable<Element> =
  this.doOnNext { logger("$id -> next $it") }
    .doOnError { logger("$id -> error $it") }
    .doOnComplete { logger("$id -> completed") }
    .doOnSubscribe { logger("$id -> subscribe") }
    .doOnDispose { logger("$id -> unsubscribe") }

fun <Element> Observable<Element>.doOnSubscribed(onSubscribed: () -> Unit): Observable<Element> =
  Observable.merge(this, Observable.defer { onSubscribed(); Observable.empty<Element>() })

