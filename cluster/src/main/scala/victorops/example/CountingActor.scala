package victorops.example

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.Actor.Receive
import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, Status }
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.PersistentActor
import victorops.example.CountingActor._

import scala.util.hashing.{ Hashing, MurmurHash3 }

/**
 * This is the wrapper for akka cluster sharding. It will lazily create an
 * underlying CountingActor that is persistent, and proxy all messages to it.
 *
 * This is to get around the ClusterSharding(system).start interface. That
 * requires Props up front, it seems, and I need the counterId up front for a
 * persistenceId (i.e. I need to pass in the counterId with the props).
 *
 * How do you even shard persistent actors? The examples in the akka docs
 * show using the actor path as a persistenceId, which doesn't seem right --
 * I want my persistence IDs to be application-specific, not framework specific.
 */
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

class CountingActor(counterId: String) extends PersistentActor with ActorLogging {

  private val count = new AtomicInteger(0)

  def persistenceId: String = s"counter-$counterId"

  def receiveRecover: Receive = {
    case _: GetCount =>
      log.info(s"Recover GetCount [counterId=$counterId] [count=$count]")
      count.get()
    case _: IncrementCount =>
      log.info(s"Recover IncrementCount [counterId=$counterId] [count=$count]")
      count.incrementAndGet()
    case _: ResetCount =>
      log.info(s"Recover ResetCount [counterId=$counterId] [count=$count]")
      count.getAndSet(0)
  }

  def receiveCommand: Receive = {
    case m @ GetCount(counterId) =>
      persist(m) { pm =>
        sender() ! count.get()
        log.info(s"Got GetCount [counterId=$counterId] [count=$count]")
      }

    case m @ IncrementCount(counterId) =>
      persist(m) { pm =>
        sender() ! count.incrementAndGet()
        log.info(s"Got IncrementCount [counterId=$counterId] [count=$count]")
      }

    case m @ ResetCount(counterId) =>
      persist(m) { pm =>
        sender() ! count.getAndSet(0)
        log.info(s"Got ResetCount [counterId=$counterId] [count=$count]")
      }

    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Stop)

  }
}
