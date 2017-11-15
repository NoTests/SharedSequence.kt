package org.notests.sharedsequence

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import org.notests.sharedsequence.annotations.SharedSequence

/**
 * Created by markotron on 11/6/17.
 */
@SharedSequence("Signal")
object SignalTraits : SharingTrait {
    override val scheduler: Scheduler
        get() = AndroidSchedulers.mainThread()

    override fun <Element> share(source: Observable<Element>): Observable<Element> =
            source.share()
}