package victorops.example

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.Cluster
import akka.cluster.sharding.ClusterSharding
import akka.cluster.sharding.ShardRegion.{ CurrentShardRegionState, GetClusterShardingStats }
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.{ Config, ConfigFactory }
import org.slf4j.LoggerFactory
import victorops.example.CountingActor.{ GetCount, IncrementCount, ResetCount }

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.{ Failure, Success }

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
  def counterRegion: ActorRef
}

trait FutureHelpers {
  def awaitResult[T](f: Future[T]) = Await.result(f, Duration.Inf)

  implicit class RichFuture[T](f: Future[T]) {
    def get[T] = awaitResult(f)
  }
}

class ConsoleImports(val config: Config, val system: ActorSystem, val cluster: Cluster)

class Console(config: Config, system: ActorSystem, cluster: Cluster) {

  object imports extends ConsoleImports(config, system, cluster)
    with CountingTasks with ClusterTasks with FutureHelpers {

    val log = LoggerFactory.getLogger("ConsoleLog")

    lazy implicit val timeout = Timeout(1 second)

    lazy implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    lazy val ClusterShards = config.getInt("victorops.example.cluster-shards")

    lazy val counterRegion = ClusterSharding(system).startProxy(
      typeName = "Counter",
      role = None,
      extractEntityId = CountingActor.extractEntityId,
      extractShardId = CountingActor.extractShardId(ClusterShards)
    )

    lazy val echo = system.actorOf(Props[EchoActor], "echo-actor")

    def initialize(): Unit = {

    }
  }
}

