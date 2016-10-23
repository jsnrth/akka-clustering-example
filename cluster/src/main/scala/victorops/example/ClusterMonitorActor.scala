package victorops.example

import akka.actor.Actor.Receive
import akka.actor.{ Actor, ActorLogging }
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{ CurrentClusterState, InitialStateAsEvents, MemberEvent, MemberExited, MemberJoined, MemberRemoved, MemberUp, ReachableMember, UnreachableMember }

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ClusterMonitorActor(implicit ec: ExecutionContext) extends Actor with ActorLogging {

  private val cluster = Cluster(context.system)

  cluster.subscribe(self,
    initialStateMode = InitialStateAsEvents,
    classOf[MemberEvent],
    classOf[UnreachableMember]
  )

/*
ClusterEvent.MemberJoined - A new member has joined the cluster and its status
  has been changed to Joining.
ClusterEvent.MemberUp - A new member has joined the cluster and its status has
  been changed to Up.
ClusterEvent.MemberExited - A member is leaving the cluster and its status has
  been changed to Exiting Note that the node might already have been shutdown
  when this event is published on another node.
ClusterEvent.MemberRemoved - Member completely removed from the cluster.
ClusterEvent.UnreachableMember - A member is considered as unreachable,
  detected by the failure detector of at least one other node.
ClusterEvent.ReachableMember - A member is considered as reachable again, after
  having been unreachable. All nodes that previously detected it as unreachable
  has detected it as reachable again.
 */
  override def receive: Receive = {

    case msg @ MemberJoined(m) =>
      log.info(s"MemberJoined [$msg]")

    case msg @ MemberUp(m) =>
      log.info(s"MemberUp [$msg]")

    case msg @ MemberExited(m) =>
      log.info(s"MemberExited [$msg]")

    case msg @ MemberRemoved(m, ps) =>
      log.info(s"MemberRemoved [$msg]")

    case msg @ UnreachableMember(m) =>
      log.info(s"UnreachableMember [$msg]")

    case msg @ ReachableMember(m) =>
      log.info(s"ReachableMember [$msg]")

    case msg: MemberEvent =>
      log.info(s"MemberEvent [$msg]")

  }
}
