package org.notests.sharedsequenceexample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_no_driver.*
import java.util.concurrent.TimeUnit

class NoDriverActivity : AppCompatActivity() {

  private lateinit var suggestions: Observable<List<String>>
  private val disposableBag = CompositeDisposable()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_no_driver)

    supportActionBar?.title = "Observable example"
  }

  override fun onStart() {
    super.onStart()

    suggestions = RxTextView
      .textChanges(search_et)
      .map { it.toString() }
      .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
      .switchMap { SuggestionsService.getSuggestionsAsObservable(it).onErrorResumeNext(Observable.just(listOf())) } // 2. ovdje ide onErrorResumeNext
      .replay(1).refCount()                                                                 // 3. Ako ne zelimo da se dva puta poziva
//      .onErrorResumeNext(Observable.just(listOf()))                                                 // 2. Ovo nije dobro jer se stream dispose-a cim se error desi. Daljnja tipkanja nista ne rade.

    disposableBag.add(
      suggestions
        .observeOn(AndroidSchedulers.mainThread())                                                    // 1. Na ovo treba paziti
        .subscribe { suggestions_tv.text = it.joinToString("\n") }
    )

    disposableBag.add(
      suggestions
        .observeOn(AndroidSchedulers.mainThread())                                                    // 1. Na ovo treba paziti
        .subscribe { size_tv.text = it.size.toString() }
    )
  }

  override fun onStop() {
    super.onStop()
    disposableBag.clear()
  }

}
