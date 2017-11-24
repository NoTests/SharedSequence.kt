# SharedSequence.kt

Adaptation of RxCocoa `SharedSequence`-s (`Driver`, `Signal`) for RxJava.

This repo is written in Kotlin and should be used in Kotlin code. 

For the original documentation see: 
https://github.com/ReactiveX/RxSwift/blob/master/Documentation/Traits.md

*Definition 1.* **Shared sequence** is a safe observable with a specific sharing strategy observing 
on a specific thread. We say that the observable is **safe** if it can't error out, i.e. the 
`onError` is never called.

Shared sequence is an abstract construct and to define a concrete implementation you need two 
things:

- a sharing strategy (e.g. `.replay(1).refCount()`)
- a thread on which a sequence is observing on (e.g. `AndroidSchedulers.mainThread()`)

This repo consists of 4 modules: 

- `SharedSequence` - Consists of most common shared sequence implementations: `Driver` and `Signal`. 
This module uses `SharedSequenceApi` and `SharedSequenceProcessor` to generate these 
implementations. In most cases you'll only need to include this module in your app. It also serves 
as an example of how to use `SharedSequenceApi` and `SharedSequenceProcessor` to generate custom 
shared sequences. 
- `SharedSequenceApi` - Public API for generating shared sequences.
- `SharedSequenceProcessor` - An implementation of an annotation processor. 
- `app` - `Driver` example

## Driver

*Definition 2.* **Driver** is a shared sequence observing on a **main** thread with a sharing 
strategy `.replay(1).refCount()`.

To clarify this definition we will look at a concrete example. 

*Problem.* Assume that you want to create a search input field which suggests result while you type. 
The field queries your server and displays a list of results and its size.

While solving this problem you should take care of

- error handling (1) 
- not spamming your server with unnecessary requests (2)

To simplify the code we will mock server-side requests like this:

```kotlin
object SuggestionsService {

  private fun getSuggestions(query: String): List<String> {
    // Random response time
    val sleepMillis = Random().nextInt(1000).toLong()
    Thread.sleep(sleepMillis)
    
    // Sometimes the server will return an error
    if (Random().nextInt(5) == 2) throw RuntimeException("Simulating network errors")
    
    // If no error, return a random number of results
    val resultsNo = Random().nextInt(10)
    return (1..resultsNo).map { "${query}_result_$it" }
  }

  fun getSuggestionsAsObservable(query: String): Observable<List<String>> =
    Observable
      .defer { Observable.just(getSuggestions(query)) }
      .subscribeOn(Schedulers.computation())
}
```

*Attempt 1.*
```kotlin
suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { SuggestionsService.getSuggestionsAsObservable(it) } // 3. 
      

suggestions
  .subscribe { suggestions_tv.text = it.joinToString("\n") } // 4.
  
suggestions
  .subscribe { size_tv.text = it.size.toString() } // 5.
```

1. We're using the `rxbindings` to get the sequence of `TextEdit`'s text changes and map it to a 
sequence of strings.
2. We throttle the sequence to prevent spamming, thus solving (2)
3. We use `switchMap` to unsubscribe from the previous observable (i.e. canceling the old request) 
and subscribe to a new observable (i.e. making a new request).
4. We subscribe to the `suggestions` observable to get and display all the results.
5. We subscribe to the `suggestions` observable to get and display the result size.

Although, the solution looks readable, it's far from correct. It will compile but the app crashes as 
soon as you open it. Of course it crashes, we're touching the UI on a `computation` thread (the 
thread `getSuggestionsAsObservable` subscribes on).
  
**When working with the UI, you usually want to observe on the main thread!** Only side-effects and 
complex computations should work on different threads.

*Attempt 2.*
```kotlin
suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { SuggestionsService.getSuggestionsAsObservable(it) } // 3. 
      
suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { suggestions_tv.text = it.joinToString("\n") } // 4.
  
suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { size_tv.text = it.size.toString() } // 5.
```

6. Touching the UI happens on the main thread and the app doesn't crash (or does it?)

Well, it crashes when the `getSuggestionsAsObservable` throws an error because we're not handling 
errors at all. 

*Attempt 3.*
```kotlin
suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { SuggestionsService.getSuggestionsAsObservable(it) } // 3. 
  .onErrorResumeNext(Observable.just(listOf())) // 7.
      

suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { suggestions_tv.text = it.joinToString("\n") } // 4.
  
suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { size_tv.text = it.size.toString() } // 5.
```

7. When the error happens just continue with the empty list. 

Now the app doesn't crash! That's because our `suggestions` observable is *safe*! That's the 
property we want. However, if you input something in the search field, you'll probably get something. 
But, if you repeat the process several times, you'll stop getting results. That's because, when
we get an error, we unsubscribe from the original `observable` and subscribe to a new one
(`Observable.just(listOf())`) which completes immediately. This means that we've unsubscribed from
`RxTextView.textChanges(search_et)` and no new strings are emmited.
 
When you think about it, `RxTextView.textChanges(search_et)` should be safe by design. The 
problematic observable is `SuggestionsService.getSuggestionsAsObservable(it)`! **This is the one 
that should be safe!**

*Attempt 4.*
```kotlin
suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { // 3. 
    SuggestionsService
      .getSuggestionsAsObservable(it)
      .onErrorResumeNext(Observable.just(listOf())) // 8. 
   }  
  // .onErrorResumeNext(Observable.just(listOf())) // 7.
      

suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { suggestions_tv.text = it.joinToString("\n") } // 4.
  
suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { size_tv.text = it.size.toString() } // 5.
```

8. No the network errors are handled and we never unsubscribe from our `textChanges`, thus solving 
(1).

We're not done. You probably noticed that the displayed result count don't match the actual result
count. And that's because, when subscribing, we're creating two different execution chains and every
time a string is emmited, our function `getSuggestionsAsObservable` gets called twice! Usually 
returning two different result sets. We have to share our sequence.
 
*Attempt 5. (Solution)*
```kotlin
suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { // 3. 
    SuggestionsService
      .getSuggestionsAsObservable(it)
      .onErrorResumeNext(Observable.just(listOf())) // 8. 
   }  
  // .onErrorResumeNext(Observable.just(listOf())) // 7.
  .replay(1)  // 9.
  .refCount() // 9.
      

suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { suggestions_tv.text = it.joinToString("\n") } // 4.
  
suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { size_tv.text = it.size.toString() } // 5.
```

9. Now, our observable is shared! Our mock method gets called once per text change, which is what we
usually want.
 
Finally, we have a robust solution. Let's think about what we have built and how can we reuse it.

We built an observable with the following properties: 

1. it can't error out, i.e. it's safe
2. observes on a main thread
3. it's shared between different subscribers. 

Basically, we've built a **driver**. 

Even though it's not hard to build a robust solution without *drevers*, by using it we guaranty 
at compile time that our observables have desired properties. Let's see how it looks like.

*Attempt 6. (Solution with drivers)*

```kotlin
val mapper = { s: String -> 
  SuggestionsService
    .getSuggestionsAsObservable(s)
    .asDriver(Driver.just(listOf())) // 2.
}

suggestions = RxTextView
  .textChanges(search_et)
  .asDriver(Driver.empty()) // 1.
  .map { it.toString() }
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
  .switchMap(mapper) // 2.
  
suggestions
  .drive { suggestions_tv.text = it.joinToString("\n") } // 3. 

suggestions
  .drive { size_tv.text = it.size.toString() } // 3.
```

1. We transform our observable into a `driver`. When doing so, we have to specify what happens when 
the observable errors out, thus making it safe!
2. Driver's `switchMap` lambda must return another shared sequence which means that the returning 
value is safe. 
3. We know that the driver observes on a main thread, no need to specify it explicitly. Note that
the `subscribe` method is renamed to `drive`. In this way, when you see an observable with a `drive`
method that compiles, you know that all the properties are satisfied. 

## Signal

*Definition 3.* **Signal** is a shared sequence observing on a **main** thread with a sharing 
strategy `.share()`.

## Custom shared sequences

To generate your own shared sequence you only need to specify a scheduler and a sharing strategy. 
Check the `SharedSequence` module and copy the `Driver` and `Signal`. 
