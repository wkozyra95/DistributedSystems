package sr.bookshop

import java.io.{File, FileOutputStream, PrintWriter}

import akka.actor.{Actor, ActorRef, Props}
import akka.actor.Actor.Receive
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration.Duration.Infinite
import scala.concurrent.duration._
import scala.io.{BufferedSource, Source}
import scala.language.postfixOps


class DataStoreManager extends Actor {
  val ds1 = context.system.actorOf(Props(new DataStoreActor("ds1.txt")))
  val ds2 = context.system.actorOf(Props(new DataStoreActor("ds2.txt")))
  val orders = context.system.actorOf(Props(new OrderWriterActor("order.txt")))

  override def receive(): PartialFunction[Any, Unit] = {
    implicit val timeout: Timeout = 1 seconds
    val pattern: PartialFunction[Any, Unit] = {
      case Array(query: FindQuery, ref: ActorRef) =>
        val resp1 = ask(ds1,query).mapTo[Array[String]]
        val resp2 = ask(ds2,query).mapTo[Array[String]]
        val result1 = Await.result(resp1, Duration(1, "s"))
        val result2 = Await.result(resp2, Duration(1, "s"))

        ref ! FindResponse(result1 ++ result2)
      case Array(query: OrderQuery, ref: ActorRef) =>
        val resp1 = ask(ds1, FindQuery(query.name)).mapTo[Array[String]]
        val resp2 = ask(ds2, FindQuery(query.name)).mapTo[Array[String]]
        val result1 = Await.result(resp1, Duration(1, "s"))
        val result2 = Await.result(resp2, Duration(1, "s"))
        if (result1.isEmpty && result2.isEmpty) {
          ref ! OrderResponse(false)
        } else {
          orders ! Array(query, ref)
        }
      case _ => Console.print("No match DataStoreManager\n")
    }
    pattern
  }
}

class DataStoreActor(val path: String) extends Actor {
  val store = new DataStore(path)
  def receive: PartialFunction[Any, Unit] = {
    case FindQuery(name: String) =>
      context.sender() ! store.find(name)
  }
}

class DataStore(val path: String) {
  def find(name: String): Array[String] = {
    Source.fromFile(path).getLines().filter((line: String) => {
      val Array(title, price) = line.split(' ')
      name match {
        case `title` => true
        case _ => false
      }
    }).toArray
  }
}

class OrderWriterActor(val path: String) extends Actor{
  val orderWriter = new OrderWriter(path)

  def receive: Receive = {
    case Array(OrderQuery(name), ref: ActorRef) =>
      orderWriter.order(name)
      ref ! OrderResponse(true)
    case _ => Console.print("Unknown in OrderWriterActor\n")
  }
}

class OrderWriter(val path: String) {
  val orders = new PrintWriter(new FileOutputStream(new File(path),true))

  def order(name: String): Unit = {
    orders.append(name + "\n")
    orders.flush()
  }
}


