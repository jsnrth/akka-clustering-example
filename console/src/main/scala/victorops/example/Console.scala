package victorops.example

import akka.actor.{ ActorSystem, Props }
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ShardRegion.{ CurrentShardRegionState, GetClusterShardingStats }
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.{ Config, ConfigFactory }
import victorops.example.CountingActor.{ GetCount, IncrementCount, ResetCount }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }

trait CountingTasks { self: ClusterTasks with FutureHelpers =>

  implicit def timeout: Timeout

  def system: ActorSystem

  def getCount(name: String): Int = awaitResult {
    (counterRegion ? GetCount(name)).mapTo[Int]
  }

  def incrementCount(name: String): Int = awaitResult {
    (counterRegion ? IncrementCount(name)).mapTo[Int]
  }

  def resetCount(name: String): Int = awaitResult {
    (counterRegion ? ResetCount(name)).mapTo[Int]
  }
}

trait ClusterTasks {
  def system: ActorSystem
  def config: Config

  lazy val ClusterShards = config.getInt("victorops.example.cluster-shards")

  lazy val counterRegion = ClusterSharding(system).startProxy(
    typeName = "Counter",
    role = None,
    extractEntityId = CountingActor.extractEntityId,
    extractShardId = CountingActor.extractShardId(ClusterShards)
  )
}

trait FutureHelpers {
  def awaitResult[T](f: Future[T]) = Await.result(f, Duration.Inf)

  implicit class RichFuture[T](f: Future[T]) {
    def get[T] = awaitResult(f)
  }
}

object console extends CountingTasks with ClusterTasks with FutureHelpers {

  lazy implicit val timeout = Timeout(1 second)

  lazy val config = ConfigFactory.load()

  lazy val ClusterName = config.getString("victorops.example.cluster-name")

  lazy val system: ActorSystem = ActorSystem(ClusterName)

  lazy val clusterListener = system.actorOf(SimpleClusterListener.props, "cluster-listener")

  lazy val echo = system.actorOf(Props[EchoActor], "echo-actor")

  def initializeConsole(): Unit = {
    system
    //clusterListener
    counterRegion
    ()
  }
}
