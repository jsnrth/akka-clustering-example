import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.cluster.sharding.{ ClusterSharding, ClusterShardingSettings, ShardRegion }
import com.typesafe.config.ConfigFactory
import victorops.example.{ CountingActor, SimpleClusterListener }

import scala.util.hashing.{ Hashing, MurmurHash3 }

object NodeMain extends App {

  val config = ConfigFactory.load()
  val ClusterName = config.getString("victorops.example.cluster-name")
  val system = ActorSystem(ClusterName)

  val clusterListener = system.actorOf(SimpleClusterListener.props, "cluster-listener")

  (1 to 200) map { i => s"counter-thing-$i" } foreach { name =>
    ClusterSharding(system).start(
      typeName = "Counter",
      entityProps = CountingActor.props(name),
      settings = ClusterShardingSettings(system),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId
    )
  }

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ CountingActor.GetCount(id) => (id, msg)
  }

  val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case CountingActor.GetCount(id) => shardId(id, numberOfShards)
  }

  val hashing = Hashing.fromFunction(MurmurHash3.stringHash)

  def shardId(shardKey: String, memberSize: Int): String = {
    (hashing.hash(shardKey) % memberSize).toString
  }

  system.awaitTermination()

}
