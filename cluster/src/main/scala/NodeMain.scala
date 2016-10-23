import akka.actor.{ ActorSystem, Props }
import akka.cluster.Cluster
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings }
import com.typesafe.config.ConfigFactory
import org.slf4j.LoggerFactory
import victorops.example.{ ClusterMonitorActor, CountingActor, CountingEntityActor }

import scala.util.{ Failure, Success }

object NodeMain {

  def main(args: Array[String]): Unit = {
    val log = LoggerFactory.getLogger("NodeMain")

    implicit val ec = scala.concurrent.ExecutionContext.global

    val config = ConfigFactory.load()
    val ClusterName = config.getString("victorops.example.cluster-name")
    val ClusterShards = config.getInt("victorops.example.cluster-shards")

    val system = ActorSystem(ClusterName)
    val cluster = Cluster(system)

    sys.addShutdownHook {
      log.info(s"Leaving cluster now on shutdown! [$cluster]")
      cluster.leave(cluster.selfAddress)
      Thread.sleep(5000) // sleep for 5 seconds to let the cluster leave
    }

    cluster.registerOnMemberRemoved {
      log.warn(s"Member removed, shutting down")
      cluster.leave(cluster.selfAddress)
      system.terminate()

      system.whenTerminated onComplete  {
        case Success(_) => sys.exit()
        case Failure(e) => sys.exit()
      }
    }

    system.actorOf(Props(classOf[ClusterMonitorActor], ec), "cluster-monitor")

    //val clusterListener = system.actorOf(SimpleClusterListener.props, "cluster-listener")

    val counterShardRegion = ClusterSharding(system).start(
      typeName = "Counter",
      entityProps = Props[CountingEntityActor],
      settings = ClusterShardingSettings(system),
      extractEntityId = CountingActor.extractEntityId,
      extractShardId = CountingActor.extractShardId(ClusterShards)
    )

    system.awaitTermination()

  }
}
