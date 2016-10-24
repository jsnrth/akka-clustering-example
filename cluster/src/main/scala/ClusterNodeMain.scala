import akka.actor.{ ActorSystem, Props }
import akka.cluster.Cluster
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import victorops.example.{ ClusterMonitorActor, CountingActor, CountingEntityActor }

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

object ClusterNodeMain {

  def main(args: Array[String]): Unit = {

    implicit val ec = scala.concurrent.ExecutionContext.global

    val log = LoggerFactory.getLogger("NodeMain")

    val config = ConfigFactory.load()

    val ClusterName = config.getString("victorops.example.cluster-name")

    val ClusterShards = config.getInt("victorops.example.cluster-shards")

    val system = ActorSystem(ClusterName)

    val cluster = Cluster(system)

    sys.addShutdownHook {
      log.info(s"Leaving cluster now on shutdown! [$cluster]")
      shutOffTheLights(waitForIt = true, cluster, system)
    }

    cluster.registerOnMemberRemoved {
      log.warn(s"Member removed, shutting down [$cluster]")
      shutOffTheLights(waitForIt = false, cluster, system)
    }

    system.actorOf(Props(classOf[ClusterMonitorActor], ec), "cluster-monitor")

    val counterShardRegion = ClusterSharding(system).start(
      typeName = "Counter",
      entityProps = Props[CountingEntityActor],
      settings = ClusterShardingSettings(system),
      extractEntityId = CountingActor.extractEntityId,
      extractShardId = CountingActor.extractShardId(ClusterShards)
    )

    system.awaitTermination()
  }

  private def shutOffTheLights(waitForIt: Boolean, cluster: Cluster, system: ActorSystem)
    (implicit ec: ExecutionContext): Unit = {

    cluster.leave(cluster.selfAddress)
    if (waitForIt) Thread.sleep(5000) // let the cluster leave

    system.terminate() onComplete {
      case Success(_) => sys.exit()
      case Failure(e) => sys.exit()
    }
  }
}
