[![](https://jitpack.io/v/NoTests/SharedSequence.kt.svg)](https://jitpack.io/#NoTests/SharedSequence.kt)

# SharedSequence.kt

Adaptation of RxCocoa `SharedSequence`-s (`Driver`, `Signal`) for RxJava.

This repo is written in Kotlin and should be used in Kotlin code. 

For the original documentation see: 
https://github.com/ReactiveX/RxSwift/blob/master/Documentation/Traits.md

*Definition 1.* **Shared sequence** is a safe observable with a specific sharing strategy observing 
on a specific thread. We say that the observable is **safe** if it can't error out, i.e. the 
`onError` is never called.

This library includes two default implementations of the shared sequence:
* Driver - models state propagation
* Signal - models event bus

Some shared sequence operators accept lambdas as parameters (`map`, `filter`, ...). What if the 
lambda you send as a parameter in this operators throws? Will it make the observable unsafe? The 
answer is no! These operators will catch your errors and emit them via the 
`ErrorReporting.exceptions()` observable. There you can subscribe and analyze your exceptions - 
usually, crashing your app in DEBUG mode, and log-and-ignore the exception in RELEASE mode.

Shared sequence is an abstract construct and to define a concrete implementation you need two 
things:

- a sharing strategy (e.g. `.replay(1).refCount()`)
- a thread on which a sequence is observing on (e.g. `AndroidSchedulers.mainThread()`)

This repo consists of 4 modules: 

- `SharedSequence` - Consists of most common shared sequence implementations: `Driver` and `Signal`. 
This module uses `SharedSequenceApi` and `SharedSequenceProcessor` to generate these 
implementations. In most cases, you'll only need to include this module in your app. It also serves 
as an example of how to use `SharedSequenceApi` and `SharedSequenceProcessor` to generate custom 
shared sequences. 
- `SharedSequenceApi` - Public API for generating shared sequences.
- `SharedSequenceProcessor` - An implementation of an annotation processor. 
- `app` - `Driver` example

## Driver

Let's look at a concrete example. 

*Problem.* Assume that you want to create a search input field which suggests result while you type. 
The field queries your server and displays a list of results and its size.

A typical approach would look something like this, but unfortunatelly it has some flaws.

*Attempt 1.*
```kotlin
object SuggestionsService {
  // this observable sequence can fail, e.g. network failure
  fun getSuggestions(query: String): Observable<List<String>>
}

suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { SuggestionsService.getSuggestions(it) } // 3. 

suggestions
  .subscribe { suggestions_tv.text = it.joinToString("\n") } // 4.
  
suggestions
  .subscribe { size_tv.text = it.size.toString() } // 5.
```

1. We're using the `rxbindings` to get the sequence of `TextEdit`'s text changes and map it to a 
sequence of strings.
2. We throttle the sequence to prevent spamming
3. We use `switchMap` to unsubscribe from the previous observable (i.e. canceling the old request) 
and subscribe to a new observable (i.e. making a new request).
4. We subscribe to the `suggestions` observable to get and display all the results.
5. We subscribe to the `suggestions` observable to get and display the result size.

Although the solution looks readable, it's far from correct. It will compile but the app crashes as 
soon as you open it. Of course, it crashes, we're touching the UI on a `computation` thread (the 
thread `getSuggestions` subscribes on).
  
**When working with the UI, you usually want to observe on the main thread!** Only side-effects and 
complex computations should work on different threads.

*Attempt 2.*
```kotlin
suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { SuggestionsService.getSuggestions(it) } // 3. 
      
suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { suggestions_tv.text = it.joinToString("\n") } // 4.
  
suggestions
  .observeOn(AndroidSchedulers.mainThread()) // 6.
  .subscribe { size_tv.text = it.size.toString() } // 5.
```

6. Touching the UI happens on the main thread and the app doesn't crash (or does it?)

Well, it crashes when the `getSuggestions` throws an error because we're not handling 
errors at all. Let's fix it!

*Attempt 3.*
```kotlin
suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { SuggestionsService.getSuggestions(it) } // 3. 
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
property we want. However, if you input something in the search field, you'll probably get some 
results. But, if you repeat the process several times, you'll stop getting results. That's because, 
when we get an error, we unsubscribe from the original `observable` and subscribe to a new one
(`Observable.just(listOf())`) which completes immediately. This means that we've unsubscribed from
`RxTextView.textChanges(search_et)` and no new strings are emited.
 
When you think about it, `RxTextView.textChanges(search_et)` should be safe by design. The 
problematic observable is `SuggestionsService.getSuggestions(it)`! **This is the one 
that should be safe!**

*Attempt 4.*
```kotlin
suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { // 3. 
    SuggestionsService
      .getSuggestions(it)
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

8. Now the network errors are handled and we never unsubscribe from our `textChanges`.

We're not done. You probably noticed that **the displayed result count doesn't match the actual result
count**. And that's because, when subscribing, we're creating two different execution chains and 
every time a string is emited, our function `getSuggestions` gets called twice! Usually 
returning two different result sets. We have to share our sequence.
 
*Attempt 5. (Solution)*
```kotlin
suggestions = RxTextView  // 1.
  .textChanges(search_et) // 1.
  .map { it.toString() }  // 1.
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS) // 2.
  .switchMap { // 3. 
    SuggestionsService
      .getSuggestions(it)
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
2. observes on the main thread
3. it's shared among different subscribers. 

Basically, we've built a **driver**. 

*Definition 2.* **Driver** is a shared sequence observing on a **main** thread with a sharing 
strategy `.replay(1).refCount()`.

Even though it's not hard to build a robust solution without *drivers*, by using it we guaranty 
at compile time that our observables have desired properties. Let's see how it looks like.

*Attempt 6. (Solution with drivers)*

```kotlin
suggestions = RxTextView
  .textChanges(search_et)
  .asDriver(Driver.empty()) // 1.
  .map { it.toString() }
  .throttleWithTimeout(300, TimeUnit.MILLISECONDS)
  .switchMapDriver { // 2.
    SuggestionsService
      .getSuggestions(it)
      .asDriver(Driver.just(listOf()))
  }
  
suggestions
  .drive { suggestions_tv.text = it.joinToString("\n") } // 3. 

suggestions
  .drive { size_tv.text = it.size.toString() } // 3.
```

1. We transform our observable into a `driver`. When doing so, we have to specify what happens when 
the observable errors out, thus making it safe!
2. Driver's `switchMap` lambda must return another shared sequence which means that the returning 
value is safe. To make it more explicit we call it `switchMapDriver`, meaning that its lambda 
returns a `Driver` (as opposed to `switchMapSignal` which returns a `Signal`)
3. We know that the driver observes on the main thread, no need to specify it explicitly. Note that
the `subscribe` method is renamed to `drive`. In this way, when you see an observable with a `drive`
method that compiles, you know that all the properties are satisfied. 

## Signal

*Definition 3.* **Signal** is a shared sequence observing on a **main** thread with a sharing 
strategy `.share()`.

## Custom shared sequences

To generate your own shared sequence you only need to specify a scheduler and a sharing strategy. 
Check the `SharedSequence` module (`DriverTraits` and `SignalTraits`). 

## How to include it?

Add this maven repo
```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

Include whatever you need: 
```groovy
implementation 'com.github.NoTests.SharedSequence.kt:SharedSequence:0.1.7'
```
or
```groovy
implementation 'com.github.NoTests.SharedSequence.kt:SharedSequenceApi:0.1.7'
kapt 'com.github.NoTests.SharedSequence.kt:SharedSequenceProcessor:0.1.7'
```
