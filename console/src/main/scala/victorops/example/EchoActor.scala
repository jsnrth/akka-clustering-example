package victorops.example

import akka.actor.{ ActorRef, Actor }

case class SetWatch(ref: ActorRef)

class EchoActor extends Actor {
  def receive = {
    case SetWatch(ref) =>
      println(s"setting watch for $ref")
      context.watch(ref)

    case msg =>
      println(s"Echo: $msg")
  }
}
