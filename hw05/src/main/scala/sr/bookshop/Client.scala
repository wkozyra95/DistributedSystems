package sr.bookshop

import java.io.File

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory


object Client {
  def main(args: Array[String]): Unit = {
    val configFile = new File("src/client.conf")
    val config = ConfigFactory.parseFile(configFile)

    val system = ActorSystem.create("client", config)
    val ref = system.actorOf(Props[ClientActor], "client")
    CmdReader.read(ref)
  }
}


class ClientActor extends Actor {
  val BOOKSTORE_PATH = "akka.tcp://bookshop@127.0.0.1:2552/user/"

  def receive: PartialFunction[Any, Unit] = {
    case query: FindQuery =>
      context.actorSelection(BOOKSTORE_PATH + "find").tell(query, context.self)
    case query: OrderQuery => context.actorSelection(BOOKSTORE_PATH + "order").tell(query, context.self)
    case query: StreamContentQuery => context.actorSelection(BOOKSTORE_PATH + "content").tell(query, context.self)
    case FindResponse(list: Array[String]) =>
      Console.print("find result: " + list.deep.mkString(", "))
    case OrderResponse(r: Boolean) => Console.print(r)
    case _ => print("Unknown message\n")
  }
}
