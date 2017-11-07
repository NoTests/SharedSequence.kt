package org.notests.sharedsequenceexample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import org.notests.sharedsequence.Signal
import org.notests.sharedsequence.just

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    Signal.just("Hello SharedSequence.kt! NoTests for you!").asObservable().subscribe({
      println(it)
    })
  }
}
