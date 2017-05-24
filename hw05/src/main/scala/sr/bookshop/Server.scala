package sr.bookshop

import java.io.File

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory


object Server {
  def main(args: Array[String]): Unit = {

    val configFile = new File("src/bookshop.conf")
    val config = ConfigFactory.parseFile(configFile)

    val system = ActorSystem.create("bookshop", config)
    val dbManager = system.actorOf(Props(new DataStoreManager()), "db")
    val findActor = system.actorOf(Props(new FindActor(dbManager)), "find")
    val orderActor = system.actorOf(Props(new OrderActor(dbManager)), "order")
  }
}

class FindActor(val dbManager: ActorRef) extends Actor {
  def receive: PartialFunction[Any, Unit] = {
    case query: FindQuery =>
      dbManager ! Array(query, context.sender())
    case _ => Console.print("Unknown query in FindActor\n")
  }
}

class OrderActor(val dbManager: ActorRef) extends Actor {
  def receive: PartialFunction[Any, Unit] = {
    case query: OrderQuery =>
      dbManager ! Array(query, context.sender())
    case _ => Console.print("Unknown query in OrderActor\n")
  }
}


