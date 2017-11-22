package org.notests.sharedsequenceexample

import android.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.notests.sharedsequence.Driver
import org.notests.sharedsequence.asDriver
import org.notests.sharedsequence.just
import java.util.*

/**
 * Created by markotron on 11/21/17.
 */
object SuggestionsService {

  private fun getSuggestions(query: String): List<String> {

    val sleepMillis = Random().nextInt(1000).toLong()
    Log.d("RANDOM SLEEP (millis)", sleepMillis.toString())
    Thread.sleep(sleepMillis)

    if (Random().nextInt(5) == 2) throw RuntimeException("Simulating network errors")

    val resultsNo = Random().nextInt(10)
    Log.d("NUMBER OF RESULTS", resultsNo.toString())

    return (1..resultsNo).map { "${query}_result_$it" }
  }

  fun getSuggestionsAsDriver(query: String): Driver<List<String>> {
    return Observable
      .defer { Observable.just(getSuggestions(query)) }
      .subscribeOn(Schedulers.computation())
      .asDriver(Driver.just(listOf()))
  }

  fun getSuggestionsAsObservable(query: String): Observable<List<String>> {
    return Observable
      .defer { Observable.just(getSuggestions(query)) }
      .subscribeOn(Schedulers.computation())
  }
}