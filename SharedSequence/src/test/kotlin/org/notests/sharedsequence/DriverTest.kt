package org.notests.sharedsequence

import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.notests.sharedsequence.api.ErrorReporting
import org.notests.sharedsequence.utils.MyTestSubscriber
import org.notests.sharedsequence.utils.TestSchedulerRule
import org.notests.sharedsequence.utils.advanceTimeBy
import org.notests.sharedsequence.utils.complete
import org.notests.sharedsequence.utils.createColdObservable
import org.notests.sharedsequence.utils.createMyTestSubscriber
import org.notests.sharedsequence.utils.error
import org.notests.sharedsequence.utils.next
import org.notests.sharedsequence.utils.scheduleAt

/**
 * Created by juraj begovac on 13/9/18
 */
class DriverTest : Assert() {

  private lateinit var scheduler: TestScheduler
  private lateinit var observer: MyTestSubscriber<Int>

  @get:Rule
  val testSchedulerRule = TestSchedulerRule()

  @Before
  fun setUp() {
    scheduler = testSchedulerRule.testScheduler
    observer = scheduler.createMyTestSubscriber()
  }

  @After
  fun tearDown() {
    observer.dispose()
  }

  @Test
  fun driverCompleteOnError() {
    scheduler.scheduleAt(0) {
      observableRange()
        .map {
          if (it == 5) throw Exception()
          else it
        }
        .asDriverCompleteOnError()
        .drive(observer)
    }
    scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun driverOnErrorJustReturn() {
    val returnOnError = 7

    this.scheduler.scheduleAt(0) {
      observableRange()
        .map {
          if (it == 5) throw Exception()
          else it
        }
        .asDriver(returnOnError)
        .drive(observer)
    }
    this.scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 7),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun driverOnErrorDriveWith() {
    this.scheduler.scheduleAt(0) {
      observableRange()
        .map {
          if (it == 5) throw Exception()
          else it
        }
        .asDriver(observableRange().asDriverCompleteOnError())
        .drive(observer)
    }
    this.scheduler.advanceTimeBy(10)

    assertEquals(
      listOf(next(0, 1),
             next(0, 2),
             next(0, 3),
             next(0, 4),
             next(0, 1),
             next(0, 2),
             next(0, 3),
             next(0, 4),
             next(0, 5),
             next(0, 6),
             next(0, 7),
             next(0, 8),
             next(0, 9),
             next(0, 10),
             complete(0)),
      observer.recordedEvents())
  }

  @Test
  fun defer() {
    this.scheduler.scheduleAt(0) {
      Driver.defer { observableRange().asDriverCompleteOnError() }
        .drive(observer)
    }
    this.scheduler.advanceTimeBy(10)

    assertEquals(
      arrayListOf(
        next(0, 1),
        next(0, 2),
        next(0, 3),
        next(0, 4),
        next(0, 5),
        next(0, 6),
        next(0, 7),
        next(0, 8),
        next(0, 9),
        next(0, 10),
        complete(0)),
      observer.recordedEvents())
  }

  @Suppress("ConstantConditionIf")
  @Test
  fun deferOnErrorComplete() {
    this.scheduler.scheduleAt(0) {
      Driver.defer {
        if (true) throw Exception()
        else
          observableRange().asDriverCompleteOnError()
      }
        .drive(observer)
    }
    this.scheduler.advanceTimeBy(10)

    assertEquals(listOf(complete(0)), observer.recordedEvents())
  }

  @Suppress("ConstantConditionIf")
  @Test
  fun deferOnErrorJustReturn() {
    this.scheduler.scheduleAt(0) {
      Driver.defer {
        if (true) throw Exception()
        else
          observableRange().asDriverCompleteOnError()
      }
        .drive(observer)
    }
    this.scheduler.advanceTimeBy(10)

    assertEquals(listOf(complete(0)), observer.recordedEvents())
  }

  @Test
  fun catchErrorAndCompleteWithoutError() {
    this.scheduler.scheduleAt(0) {
      observableRange()
        .asDriverCompleteOnError()
        .drive(observer)
    }
    this.scheduler.advanceTimeBy(10)

    assertEquals(
      listOf(
        next(0, 1),
        next(0, 2),
        next(0, 3),
        next(0, 4),
        next(0, 5),
        next(0, 6),
        next(0, 7),
        next(0, 8),
        next(0, 9),
        next(0, 10),
        complete(0)),
      observer.recordedEvents())
  }

  @Test
  fun catchErrorAndComplete() {
    this.scheduler.scheduleAt(0) {
      observableRange()
        .map {
          if (it == 5)
            throw Exception()
          else it
        }
        .asDriverCompleteOnError()
        .drive(observer)
    }
    this.scheduler.advanceTimeBy(10)

    assertEquals(
      listOf(next(0, 1),
             next(0, 2),
             next(0, 3),
             next(0, 4),
             complete(0)),
      observer.recordedEvents())
  }

  @Test
  fun catchErrorAndReturn() {
    val returnOnError = 7

    observableRange()
      .map {
        if (it == 5)
          throw Exception()
        else it
      }
      .asDriver(Driver.just(returnOnError))
      .drive(observer)

    this.scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 7),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun map() {
    val exception = Exception("5 reached")

    val errorObserver = scheduler.createMyTestSubscriber<Throwable>()

    ErrorReporting.exceptions()
      .subscribe(errorObserver)

    observableRange()
      .asDriverCompleteOnError()
      .map {
        if (it == 5)
          throw exception
        else it
      }
      .drive(observer)

    this.scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 6),
                        next(0, 7),
                        next(0, 8),
                        next(0, 9),
                        next(0, 10),
                        complete(0)),
                 observer.recordedEvents())

    assertEquals(listOf(next(0, exception)),
                 errorObserver.recordedEvents())
  }

  @Test
  fun filter() {
    observableRange()
      .asDriverCompleteOnError()
      .filter {
        if (it == 5)
          throw Exception()
        else true
      }
      .drive(observer)

    this.scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 6),
                        next(0, 7),
                        next(0, 8),
                        next(0, 9),
                        next(0, 10),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun flatMap() {
    observableRange()
      .asDriverCompleteOnError()
      .flatMapDriver {
        if (it == 5)
          throw Exception()
        else Driver.just(it)
      }
      .drive(observer)

    this.scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, 3),
                        next(0, 4),
                        next(0, 6),
                        next(0, 7),
                        next(0, 8),
                        next(0, 9),
                        next(0, 10),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testDriverSharing_WhenErroring() {
    val observer1 = scheduler.createMyTestSubscriber<Int>()
    val observer2 = scheduler.createMyTestSubscriber<Int>()
    val observer3 = scheduler.createMyTestSubscriber<Int>()

    var disposable1: Disposable? = null
    var disposable2: Disposable? = null
    var disposable3: Disposable? = null

    val coldObservable = scheduler.createColdObservable<Int>(
      next(10, 0),
      next(20, 1),
      next(30, 2),
      next(40, 3),
      error(50, Error("Test")))

    val driver = coldObservable.asDriver(-1)

    scheduler.scheduleAt(200) {
      disposable1 =
        driver.asObservable()
          .subscribe({ observer1.onNext(it) },
                     { observer1.onError(it) },
                     { observer1.onComplete() })
    }

    scheduler.scheduleAt(225) {
      disposable2 =
        driver.asObservable()
          .subscribe({ observer2.onNext(it) },
                     { observer2.onError(it) },
                     { observer2.onComplete() })
    }

    scheduler.scheduleAt(235) { disposable1!!.dispose() }

    scheduler.scheduleAt(260) { disposable2!!.dispose() }

    // resubscription
    scheduler.scheduleAt(260) {
      disposable3 =
        driver.asObservable()
          .subscribe({ observer3.onNext(it) },
                     { observer3.onError(it) },
                     { observer3.onComplete() })
    }

    scheduler.scheduleAt(285) { disposable3!!.dispose() }

    scheduler.advanceTimeBy(1000)

    assertEquals(listOf(next(210, 0),
                        next(220, 1),
                        next(230, 2)),
                 observer1.recordedEvents())

    assertEquals(listOf(next(225, 1),
                        next(230, 2),
                        next(240, 3),
                        next(250, -1),
                        complete(250)),
                 observer2.recordedEvents())

    assertEquals(listOf(next(270, 0),
                        next(280, 1)),
                 observer3.recordedEvents())
  }

  @Test
  fun testDriverSharing_WhenCompleted() {
    val observer1 = scheduler.createMyTestSubscriber<Int>()
    val observer2 = scheduler.createMyTestSubscriber<Int>()
    val observer3 = scheduler.createMyTestSubscriber<Int>()

    var disposable1: Disposable? = null
    var disposable2: Disposable? = null
    var disposable3: Disposable? = null

    val coldObservable = scheduler.createColdObservable<Int>(
      next(10, 0),
      next(20, 1),
      next(30, 2),
      next(40, 3),
      complete(50))

    val driver = coldObservable.asDriver(-1)

    scheduler.scheduleAt(200) {
      disposable1 =
        driver.asObservable()
          .subscribe({ observer1.onNext(it) },
                     { observer1.onError(it) },
                     { observer1.onComplete() })
    }

    scheduler.scheduleAt(225) {
      disposable2 =
        driver.asObservable()
          .subscribe({ observer2.onNext(it) },
                     { observer2.onError(it) },
                     { observer2.onComplete() })
    }

    scheduler.scheduleAt(235) { disposable1!!.dispose() }

    scheduler.scheduleAt(260) { disposable2!!.dispose() }

    // resubscription
    scheduler.scheduleAt(260) {
      disposable3 =
        driver.asObservable()
          .subscribe({ observer3.onNext(it) },
                     { observer3.onError(it) },
                     { observer3.onComplete() })
    }

    scheduler.scheduleAt(285) { disposable3!!.dispose() }

    scheduler.advanceTimeBy(1000)

    assertEquals(listOf(next(210, 0),
                        next(220, 1),
                        next(230, 2)),
                 observer1.recordedEvents())

    assertEquals(listOf(next(225, 1),
                        next(230, 2),
                        next(240, 3),
                        complete(250)),
                 observer2.recordedEvents())

    assertEquals(listOf(next(270, 0),
                        next(280, 1)),
                 observer3.recordedEvents())
  }

  @Test
  fun testAsDriver_onErrorJustReturn() {
    val source = scheduler.createColdObservable<Int>(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(-1)
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_onErrorDriveWith() {
    val source = scheduler.createColdObservable<Int>(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(Driver.just(-1))
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_onErrorRecover() {
    val source = scheduler.createColdObservable<Int>(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(Driver.empty())
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_defer() {
    val source = scheduler.createColdObservable<Int>(next(0, 1), next(0, 2), error(0, Error("Test")))

    Driver.defer { source.asDriver(-1) }
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_map() {
    val source = scheduler.createColdObservable<Int>(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(-1)
      .map { it + 1 }
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 2),
                        next(0, 3),
                        next(0, 0),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_filter() {
    val source = scheduler.createColdObservable<Int>(next(0, 1), next(0, 2), error(0, Error("Test")))

    source.asDriver(-1)
      .filter { it % 2 == 0 }
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 2),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_switchMap() {
    val observable = scheduler.createColdObservable<Int>(next(0, 0),
                                                         next(1, 1),
                                                         error(2, Error("Test")),
                                                         complete(3))
    val observable1 = scheduler.createColdObservable<Int>(next(0, 1),
                                                          next(0, 2),
                                                          error(0, Error("Test")))
    val observable2 = scheduler.createColdObservable<Int>(next(0, 10),
                                                          next(0, 11),
                                                          error(0, Error("Test")))
    val errorObservable = scheduler.createColdObservable<Int>(complete(0))

    val drivers = arrayListOf(
      observable1.asDriver(-2),
      observable2.asDriver(-3),
      errorObservable.asDriver(-4))

    observable.asDriver(2)
      .switchMapDriver { drivers[it] }
      .drive(observer)

    scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -2),
                        next(1, 10),
                        next(1, 11),
                        next(1, -3),
                        complete(2)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_switchMap_overlapping() {
    val observable = scheduler.createColdObservable<Int>(next(0, 0),
                                                         next(1, 1),
                                                         error(2, Error("Test")),
                                                         complete(3))
    val observable1 = scheduler.createColdObservable<Int>(next(0, 1),
                                                          error(0, Error("Test")),
                                                          next(1, 2))
    val observable2 = scheduler.createColdObservable<Int>(next(0, 10),
                                                          error(0, Error("Test")),
                                                          next(1, 11))
    val errorObservable = scheduler.createColdObservable<Int>(complete(0))

    val drivers = arrayListOf(observable1.asDriver(-2),
                              observable2.asDriver(-3),
                              errorObservable.asDriver(-4))

    observable.asDriver(2)
      .switchMapDriver { drivers[it] }
      .drive(observer)

    scheduler.advanceTimeBy(10)

    assertEquals(listOf(next(0, 1),
                        next(0, -2),
                        next(1, 10),
                        next(1, -3),
                        complete(2)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_doOnNext() {
    val observable = scheduler.createColdObservable<Int>(next(0, 1),
                                                         next(0, 2),
                                                         error(0, Error("Test")))

    var events = emptyArray<Int>()

    observable.asDriver(-1)
      .doOnNext {
        events += it
      }
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())

    val expectedEvents = arrayOf(1, 2, -1)
    assertArrayEquals(expectedEvents, events)
  }

  @Test
  fun testAsDriver_distinctUntilChanged1() {
    val observable = scheduler.createColdObservable<Int>(next(0, 1),
                                                         next(0, 2),
                                                         next(0, 2),
                                                         error(0, Error("Test")))
    observable.asDriver(-1)
      .distinctUntilChanged()
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_distinctUntilChanged2() {
    val observable = scheduler.createColdObservable<Int>(next(0, 1),
                                                         next(0, 2),
                                                         next(0, 2),
                                                         error(0, Error("Test")))
    observable.asDriver(-1)
      .distinctUntilChanged { e -> e }
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_distinctUntilChanged3() {
    val observable = scheduler.createColdObservable<Int>(next(0, 1),
                                                         next(0, 2),
                                                         next(0, 2),
                                                         error(0, Error("Test")))
    observable.asDriver(-1)
      .distinctUntilChanged { e1, e2 -> e1 == e2 }
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_flatMap() {
    val observable = scheduler.createColdObservable<Int>(next(0, 1),
                                                         next(0, 2),
                                                         error(0, Error("Test")))
    observable.asDriver(-1)
      .flatMapDriver {
        Driver.just(it + 1)
      }
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 2),
                        next(0, 3),
                        next(0, 0),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_mergeSync() {
    val factories: Array<(Driver<Int>) -> Driver<Int>> = arrayOf({ driver -> Driver.merge(arrayListOf(driver)) },
                                                                 { driver -> Driver.merge(arrayListOf(driver)) },
                                                                 { driver -> Driver.merge(arrayListOf(driver)) })

    val observers = ArrayList<MyTestSubscriber<Int>>(factories.size)

    factories.forEach {
      val observable = scheduler.createColdObservable<Int>(
        next(0, 1),
        next(0, 2),
        error(0, Error("Test")))
      val driver = it(observable.asDriver(-1))

      val observer = scheduler.createMyTestSubscriber<Int>()
      driver.drive(observer)

      observers.add(observer)
    }

    scheduler.advanceTimeBy(1)

    observers.forEach {
      assertEquals(listOf(next(0, 1),
                          next(0, 2),
                          next(0, -1),
                          complete(0)),
                   it.recordedEvents())
    }
  }

  @Test
  fun testAsDriver_merge() {
    val observable: Observable<Int> = scheduler.createColdObservable(next(0, 1),
                                                                     next(0, 2),
                                                                     error(0, Error("Test")))

    Driver.merge(arrayListOf(observable.asDriver(-1).flatMapDriver { Driver.just(it + 1) }))
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 2),
                        next(0, 3),
                        next(0, 0),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_scan() {
    val observable: Observable<Int> = scheduler.createColdObservable(next(0, 1),
                                                                     next(0, 2),
                                                                     error(0, Error("Test")))

    observable.asDriver(-1)
      .scan(0) { a, n -> a + n }
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 0),
                        next(0, 1),
                        next(0, 3),
                        next(0, 2),
                        complete(0)),
                 observer.recordedEvents())
  }

  @Test
  fun testAsDriver_startWith() {
    val observable: Observable<Int> = scheduler.createColdObservable(next(0, 1),
                                                                     next(0, 2),
                                                                     error(0, Error("Test")))
    observable.asDriver(-1)
      .startWith(0)
      .drive(observer)

    scheduler.advanceTimeBy(1)

    assertEquals(listOf(next(0, 0),
                        next(0, 1),
                        next(0, 2),
                        next(0, -1),
                        complete(0)),
                 observer.recordedEvents())
  }

  private fun observableRange() = Observable.range(1, 10)

  private fun <Element> Observable<Element>.asDriverCompleteOnError(): Driver<Element> =
    asDriver(Driver.empty())

  private fun <Element> Observable<Element>.asDriver(onErrorJustReturn: Element): Driver<Element> =
    asDriver(Driver.just(onErrorJustReturn))
}
