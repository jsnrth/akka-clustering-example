package victorops.example

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ Actor, ActorLogging, Props, ReceiveTimeout }
import akka.cluster.sharding.ShardRegion.Passivate
import victorops.example.CountingActor._

object CountingActor {

  def props(name: String): Props = {
    Props(classOf[CountingActor], name)
  }

  case class GetCount(counterId: String)
  case object IncrementCount
  case object ResetCount
  case object StopCounter
}

class CountingActor(name: String) extends Actor with ActorLogging {

  private val count = new AtomicInteger(0)

  override def receive: Receive = {
    case GetCount(_) =>
      sender() ! count.get()
      log.info(s"Got GetCount [count=$count]")

    case IncrementCount =>
      sender() ! count.incrementAndGet()
      log.info(s"Got IncrementCount [count=$count]")

    case ResetCount =>
      sender() ! count.getAndSet(0)
      log.info(s"Got ResetCount [count=$count]")

    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Stop)

    case StopCounter =>
      context.stop(self)
  }
}
