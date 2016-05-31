import rx.lang.scala.{Subject, Observable}
import scala.annotation.tailrec
import scala.concurrent.duration._

object observables {
  def newColdObservable = Observable.from(0 until 100)
  def newRepeatingObservable = newColdObservable.repeat
  // Not to be confused with Infinite! It emits data, and then pauses forever.
  def newEndlessObservable[T](data: Iterable[T]) = Observable.from(data) ++ Subject()
  def newSlowObservable = newColdObservable.zip(Observable.timer(10.milliseconds)).map(_._1)
  def newAsyncObservable = newColdObservable.delay(10.milliseconds)
  def newHotObservable = {
    val subject = Subject[Int]()
    var abort: Option[() => Unit] = None
    val thread = new Thread(new Runnable {
      override def run(): Unit = {
        var running = true
        abort = Some(() => running = false)
        @tailrec
        def submit(i: Int):Unit = {
          subject.onNext(i)
          Thread.sleep(10)
          if (running) submit(i + 1)
        }
        submit(0)
      }
    })

    subject
      .doOnSubscribe(thread.start())
      .doOnUnsubscribe{
        if (abort.isDefined) {
          val cb = abort.get()
          abort = None
          cb
        }
      }
      .share
  }
  def newEndlessHotObservable = newHotObservable.repeat
}
