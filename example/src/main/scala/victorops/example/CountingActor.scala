package victorops.example

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.Actor

case object GetCount
case object IncrementCount
case object DecrementCount
case object ResetCount

class CountingActor extends Actor {
  private val count = new AtomicInteger(0)

  def receive: Receive = {
    case GetCount =>
      sender() ! count.get()

    case IncrementCount =>
      count.incrementAndGet()

    case DecrementCount =>
      count.decrementAndGet()

    case ResetCount =>
      count.set(0)
  }
}
