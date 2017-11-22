package org.notests.sharedsequenceexample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.notests.sharedsequence.*
import org.notests.sharedsequence.api.debug
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

  private lateinit var suggestions: Driver<List<String>>

  private val disposableBag = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    supportActionBar?.title = "Driver example"
  }

  override fun onStart() {
    super.onStart()

    suggestions = RxTextView
      .textChanges(search_et)
      .map { it.toString() }
      .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
      .asDriver(Driver.empty())
      .switchMap(SuggestionsService::getSuggestionsAsDriver)

    disposableBag.add(
      suggestions
        .debug("BEFORE DRIVE") { Log.d("Thread [${Thread.currentThread()}]", it) }
        .drive { suggestions_tv.text = it.joinToString("\n") }
    )

    disposableBag.add(
      suggestions
        .drive { size_tv.text = it.size.toString() }
    )
  }

  override fun onStop() {
    super.onStop()
    disposableBag.clear()
  }
}
