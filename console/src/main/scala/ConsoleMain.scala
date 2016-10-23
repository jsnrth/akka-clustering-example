import akka.actor.ActorSystem
import akka.cluster.Cluster
import com.typesafe.config.ConfigFactory
import victorops.example.Console

import scala.concurrent.ExecutionContext
import scala.util.{ Failure, Success }

object ConsoleMain {
  def main(args: Array[String]): Unit = {

    val config = ConfigFactory.load()
    val ClusterName = config.getString("victorops.example.cluster-name")
    val system = ActorSystem(ClusterName)
    val cluster = Cluster(system)
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    val amm = ammonite.Main(
      predef =
        """
          |println("Loading the VictorOps Example Console!")
          |import akka.actor._
          |import akka.pattern._
          |import console.imports._
        """.stripMargin
    ).run(
      "config" -> config,
      "system" -> system,
      "cluster" -> cluster,
      "console" -> new Console(config, system, cluster)
    )

    shutOffTheLights(waitForIt = true, cluster, system)
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
