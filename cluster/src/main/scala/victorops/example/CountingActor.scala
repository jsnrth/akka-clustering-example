package victorops.example

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.Actor.Receive
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, Status }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import victorops.example.CountingActor._

import scala.util.hashing.{ Hashing, MurmurHash3 }

class CountingEntityActor extends Actor with ActorLogging {

  override def receive: Receive = {
    case msg @ Msg(counterId) =>
      countingActor(counterId) forward msg
  }

  private def countingActor(counterId: String): ActorRef = {
    val actorName = s"$counterId-counter"
    context.child(actorName) getOrElse {
      context.actorOf(CountingActor.props(counterId), actorName)
    }
  }
}

object CountingActor {

  def props(clusterId: String): Props = {
    Props(classOf[CountingActor], clusterId)
  }

  sealed trait Msg {
    def counterId: String
  }

  object Msg {
    def unapply(msg: Msg): Option[String] = Option(msg.counterId)
  }

  case class Initialize(counterId: String) extends Msg
  case class GetCount(counterId: String) extends Msg
  case class IncrementCount(counterId: String) extends Msg
  case class ResetCount(counterId: String) extends Msg
  //case class StopCounter(counterId: String) extends Msg

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ CountingActor.Msg(id) => (id, msg)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case CountingActor.Msg(id) => shardId(id, numberOfShards)
  }

  private val hashing = Hashing.fromFunction(MurmurHash3.stringHash)

  private def shardId(shardKey: String, memberSize: Int): String = {
    (hashing.hash(shardKey) % memberSize).toString
  }
}

class CountingActor(counterId: String) extends Actor with ActorLogging {

  private val count = new AtomicInteger(0)

  override def receive: Receive = {
    case GetCount(counterId) =>
      sender() ! count.get()
      log.info(s"Got GetCount [counterId=$counterId] [count=$count]")

    case IncrementCount(counterId) =>
      sender() ! count.incrementAndGet()
      log.info(s"Got IncrementCount [counterId=$counterId] [count=$count]")

    case ResetCount(counterId) =>
      sender() ! count.getAndSet(0)
      log.info(s"Got ResetCount [counterId=$counterId] [count=$count]")

    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Stop)

  }
}
