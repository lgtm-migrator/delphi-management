package actors
import akka.actor._
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import actors.PublishSocketMessageActor.{AddOutActor, PublishMessage, StopMessage}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import models.EventEnums.EventType
import models.{EventJsonSupport, RegistryEvent}
import spray.json._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Promise

object PublishSocketMessageActor {
  def props(irBasePath: String, mat: Materializer, actorSys: ActorSystem):Props = Props(new PublishSocketMessageActor(irBasePath, mat, actorSys))
  final case class AddOutActor(out: ActorRef, event: EventType)
  final case class PublishMessage(msg: RegistryEvent)
  final case class StopMessage(toStop: ActorRef)
}

class PublishSocketMessageActor(irBasePath: String, mat: Materializer, actorSys: ActorSystem) extends Actor with EventJsonSupport {
  
  val eventActorMap: mutable.HashMap[EventType, ListBuffer[ActorRef]] = new mutable.HashMap[EventType, ListBuffer[ActorRef]]()

  override def preStart() {
    println("pre start called in publisher", self)
    val flow: Flow[Message, Message, Promise[Option[Message]]] =
      Flow.fromSinkAndSourceMat(
        Sink.foreach[Message]{ msg =>
          self ! msg},
        Source(List(TextMessage("one"), TextMessage("two")))
          .concatMat(Source.maybe[Message])(Keep.right))(Keep.right)

    val (upgradeResponse, promise) =
      Http()(actorSys).singleWebSocketRequest(
        WebSocketRequest("ws://" + irBasePath + "/events"),
        flow)(mat)


  }

  override def postStop() {
    println("post stop called in publisher", self)
  }
  
  def receive: PartialFunction[Any, Unit] = {
    
    case StopMessage(toStop) =>
      println("stop received", toStop)
      for ((k, v) <- eventActorMap) v -= toStop

    case AddOutActor(out, event) =>
      println("received add out actor", out)
      if (!eventActorMap.contains(event)){
        eventActorMap += (event -> new ListBuffer[ActorRef]())
      }
      eventActorMap(event) += out


    case TextMessage.Strict(msg) =>
      println("received something ", msg)
      val registryEvent = msg.parseJson.convertTo[RegistryEvent](eventFormat)
      self ! PublishMessage(registryEvent)

    case PublishMessage(msg) =>
      println("publish message called with message", msg)
      if(eventActorMap.contains(msg.eventType)){

        val list = eventActorMap(msg.eventType)
        println("found list in actor map for msg", list)
        list.foreach((actor) => {
          println("sending message to actor", actor)
          actor ! PublishMessage(msg)
        })
      }
  }

}
