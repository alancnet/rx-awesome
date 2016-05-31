import java.util.concurrent.atomic.AtomicInteger

import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.concurrent.AsyncAssertions._
import rx.lang.scala.{Subject, Subscription, Observable}
import observables._
import scala.collection.mutable
import scala.concurrent.duration._
/**
  * Created by alco on 5/27/16.
  */
class RxDemos extends FlatSpec with Matchers {
  "Share" should "prevent projection work from being repeated" in {
    val w = new Waiter()

    val workCounter = new AtomicInteger(0)

    val list = newHotObservable.take(10)

    val listWithWork = list.map(x => {
      workCounter.incrementAndGet()
      x * 2
    }).share

    listWithWork.zip(listWithWork).doOnCompleted{
      workCounter.get shouldBe 10 // <-- Look here! It only did the work once!
      w.dismiss()
    }.subscribe()

    w.await()

  }

  "Cold Observables" should "repeat projection work if not shared" in {
    val w = new Waiter()

    val workCounter = new AtomicInteger(0)

    val list = newHotObservable.take(10)

    val listWithWork = list.map(x => {
      workCounter.incrementAndGet()
      x * 2
    })

    listWithWork.zip(listWithWork).doOnCompleted{
      workCounter.get shouldBe 20 // <-- Look here! twice the work!
      w.dismiss()
    }.subscribe()

    w.await()

  }


  "Cold Observable" should "emit the same result to each subscriber" in {
    val w = new Waiter
    val observable = newColdObservable

    observable.take(10).toList.subscribe(list => {
      list shouldBe List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      w.dismiss()
    })
    observable.take(10).toList.subscribe(list => {
      list shouldBe List(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
      w.dismiss()
    })

    w.await(dismissals(2))
  }

  "Grouping into a readable state" should "go like this" in {
    val stateStream =

      // Create a new aggregate stream
      // As v increments, the stateMap will keep track of the last value per modulus 7.
      // Example: The stream will start as Map.empty, then after 0 through 6 are emitted:
      // Map(0 -> 0, 1 -> 1, 2 -> 2, 3 -> 3, 4 -> 4, 5 -> 5, 6 -> 6)
      // Then after 0 through 13 are emitted:
      // Map(0 -> 7, 1 -> 8, 2 -> 9, 3 -> 10, 4 -> 11, 5 -> 12, 6 -> 13)
      newEndlessObservable(0 to 100)
      .scan(Map.empty[Int,Int])(
        (stateMap, v) => stateMap + ((v % 7) -> v)
      )

      // Instruct the stream to replay the last stateMap to new subscribers
      .replay(1).autoConnect

    // Connect the observable so it begins processing state
    stateStream.subscribe()

    // Simulate waiting a long time
    Thread.sleep(100)

    // Subscribe to get the latest state, and all new states in the future.
    var w = new Waiter
    stateStream.subscribe(stateMap => {
      stateMap.get(1) shouldBe Some(99) // Because 99 % 7 == 1
      w.dismiss()
    })
    w.await()

  }

  "Endless observables" should "never complete" in {
    var failed = false
    newEndlessHotObservable.sum.subscribe(sum => {
      failed = true
    })

    Thread.sleep(150)

    if (failed) fail("Should not have summed.")

  }



  "User interactions" should "be considered a stream" in {
    val w = new Waiter
    val counter = new AtomicInteger(0)

    trait LoginStatus
    case object New extends LoginStatus
    case object Submitted extends LoginStatus
    case object Success extends LoginStatus
    case object Failure  extends LoginStatus

    trait UserEvent
    case class LoginAttempt(username: String, password: String) extends UserEvent

    // Define a bus for user events
    val userEvents = Subject[UserEvent]()

    // Define the form
    val loginForm = new Object {
      var username = ""
      var password = ""
    }
    val loginButton = new Object {
      def onClick() =
        // Don't handle login, just publish an event
        if (enabled)
          userEvents onNext LoginAttempt(loginForm.username, loginForm.password)
      var enabled = true // Because UI elements are stateful without React
    }


    //Simulated login function
    def login(loginAttempt: LoginAttempt):Observable[Boolean] = {
      counter.incrementAndGet()
      Observable.just(loginAttempt.username == "bowser").delay(100.milliseconds)
    }

    // Compose streams
    val loginAttempts = userEvents
      .collect{ case x:LoginAttempt => x }

    // Stream of login status
    val loginStatus =
      // Login status starts as New
    // "New" .. Wait for login click .. "Submitted" .. Wait for API to return .. "Success" | "Failure"
      Observable.just(New) ++
        loginAttempts
          .flatMap(loginAttempt =>
            // Login status changes to Submitted when you click Login
            Observable.just(Submitted) ++
              // Login status changes to Success or Failure depending on login.
              login(loginAttempt).map{
                case true => Success
                case false => Failure
              }
          ).share // <-- Important so login attempt isn't repeated per subscriber.

    // Disable the login button during login attempts
    loginStatus
      .map{
        case New | Failure => true
        case Submitted | Success => false
      }
      .foreach(enable => loginButton.enabled = enable)

    // Navigate to the home page upon login
    loginStatus
      .filter(_ === Success)
      .foreach(_ => {
        // TODO: Navigate to home page
        w.dismiss()
      })

    // Now let's simulate user interactions
    loginStatus.filter(_ === New).take(1).delay(10.millis).foreach(_ => {
      loginForm.username = "donkey.kong"
      loginForm.password = "barrels"
      loginButton.enabled shouldBe true
      loginButton.onClick()
      loginButton.enabled shouldBe false
    })

    loginStatus.filter(_ === Failure).take(1).delay(10.millis).foreach(_ => {
      loginForm.username = "bowser"
      loginForm.password = "$qu1shM4ri0"
      loginButton.enabled shouldBe true
      loginButton.onClick()
      loginButton.enabled shouldBe false
    })

    w.await(Timeout(1000.millis))

    counter.get() shouldBe (2)

  }

}



















































