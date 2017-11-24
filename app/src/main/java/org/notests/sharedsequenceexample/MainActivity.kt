package org.notests.sharedsequenceexample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import org.notests.sharedsequence.*
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
      .asDriver(Driver.empty())
      .map { it.toString() }
      .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
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
