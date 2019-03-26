package actors
import akka.actor._
import akka.http.scaladsl.model.ws.{Message, TextMessage, WebSocketRequest}
import actors.PublishSocketMessageActor.{ PublishMessage, StopMessage}
import akka.http.scaladsl.Http
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import models.EventEnums.EventType
import models.{EventJsonSupport, RegistryEvent}
import play.api.Logger
import spray.json._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Promise

object PublishLogsActor {
  def props(irBasePath: String, mat: Materializer, actorSys: ActorSystem):Props = Props(new PublishLogsActor(irBasePath, mat, actorSys))
  final case class AddOutActor(out: ActorRef, event: EventType)
  final case class PublishMessage(msg: RegistryEvent)
  final case class StopMessage(toStop: ActorRef)
}

class PublishLogsActor(irBasePath: String, mat: Materializer, actorSys: ActorSystem) extends Actor with EventJsonSupport {

  val eventActorMap: mutable.HashMap[EventType, ListBuffer[ActorRef]] = new mutable.HashMap[EventType, ListBuffer[ActorRef]]()

  override def preStart() {

    Logger.debug("pre start called in publisher" + self)
    val flow: Flow[Message, Message, Promise[Option[Message]]] =
      Flow.fromSinkAndSourceMat(
        Sink.foreach[Message]{ msg =>
          self ! msg},
        Source(List(TextMessage("Logs")))
          .concatMat(Source.maybe[Message])(Keep.right))(Keep.right)


    Http()(actorSys).singleWebSocketRequest(
      WebSocketRequest("ws://" + irBasePath + "/logs"),
      flow)(mat)
  }

  override def postStop() {
    Logger.debug("post stop called in publisher" + self)
  }

  def receive: PartialFunction[Any, Unit] = {

    case StopMessage(toStop) =>
      Logger.debug("stop received" + toStop)
      for ((k, v) <- eventActorMap) v -= toStop

    case TextMessage.Strict(msg) =>
      Logger.debug("received something " + msg)
      val registryEvent = msg.parseJson.convertTo[RegistryEvent](eventFormat)
      self ! PublishMessage(registryEvent)

    case PublishMessage(msg) =>
      Logger.debug("publish message called with message" + msg)
      if(eventActorMap.contains(msg.eventType)){

        val list = eventActorMap(msg.eventType)
        list.foreach(actor => {
          Logger.debug("sending message to actor" + actor)
          actor ! PublishMessage(msg)
        })
      }
  }
}