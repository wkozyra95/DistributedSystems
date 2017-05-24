package sr.bookshop

import akka.actor.ActorRef
import scala.io.StdIn

object CmdReader {
  var actorRef: Option[ActorRef] = None

  def read(ref: ActorRef): Unit = {
    actorRef = Option.apply(ref)

    while (nextOption(StdIn.readLine())) {}

    Console.print("EXIT")
  }

  def nextOption(line: String): Boolean = {
    if (actorRef.isEmpty) {
      return false
    }
    line.split(' ') match {
      case Array("find", name) =>
        actorRef.get ! FindQuery(name)
        true
      case Array("order", name) =>
        actorRef.get ! OrderQuery(name)
        true
      case Array("q" | "quit") =>
        false
      case _ =>
        Console.print("Unknown command\n")
        true
    }
  }
}
