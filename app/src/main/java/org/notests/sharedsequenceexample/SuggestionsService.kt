package org.notests.sharedsequenceexample

import android.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 * Created by markotron on 11/21/17.
 */
object SuggestionsService {
  fun getSuggestions(query: String): Observable<List<String>> = Observable
    .defer {
      // random request length
      val sleepMillis = Random().nextInt(1000).toLong()
      Log.d("RANDOM SLEEP (millis)", sleepMillis.toString())
      Thread.sleep(sleepMillis)

      // sometimes the request can fail
      if (Random().nextInt(5) == 2) throw RuntimeException("Simulating network errors")

      // if it hasn't failed, return random results
      val resultsNo = Random().nextInt(10)
      Log.d("NUMBER OF RESULTS", resultsNo.toString())

      Observable.just((1..resultsNo).map { "${query}_result_$it" })
    }
    // requests are not running on the main thread
    .subscribeOn(Schedulers.computation())
}