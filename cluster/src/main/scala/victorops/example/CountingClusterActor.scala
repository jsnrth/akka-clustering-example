package victorops.example

import akka.actor.Actor.Receive
import akka.actor.{ Actor, ActorLogging, Props }

class CountingClusterActor(counterNames: Seq[String]) extends Actor with ActorLogging {

  override def receive: Receive = {
    case _ =>
  }
}

