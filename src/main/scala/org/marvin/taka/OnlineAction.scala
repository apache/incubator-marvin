package org.marvin.taka

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import akka.pattern.pipe

import scala.concurrent.duration._
import org.marvin.model.{EngineActionMetadata, EngineMetadata}
import org.marvin.taka.ActionHandler.{ExecuteOnline, HealthCheck, Reload}
import org.marvin.taka.ArtifactSaver.SaveToLocal
import org.marvin.taka.OnlineAction.{OnlineExecute, OnlineHealthCheck, OnlineReload}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

object OnlineAction {
  case class OnlineExecute(message: String, params: String)
  case class OnlineReload(protocol:String)
  case class OnlineHealthCheck()
}

class OnlineAction(actionName: String, metadata: EngineMetadata) extends Actor with ActorLogging{
  var onlineActionHandler: ActorRef = _
  var artifactSaver: ActorRef = _
  var engineActionMetadata: EngineActionMetadata = _
  var artifactsToLoad: String = _
  implicit val ec = context.dispatcher

  override def preStart() = {
    engineActionMetadata = metadata.actionsMap(actionName)
    artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
    onlineActionHandler = context.actorOf(Props(new OnlineActionProxy(engineActionMetadata)), name = "onlineActionHandler")
    artifactSaver = context.actorOf(Props(new ArtifactSaver(metadata)), name = "artifactSaver")
  }

  override def receive  = {
    case OnlineExecute(message, params) =>
      implicit val futureTimeout = Timeout(10 seconds)

      log.info(s"Starting to process execute to $actionName. Message: [$message] and params: [$params].")

      val originalSender = sender
      ask(onlineActionHandler, ExecuteOnline(message, params)) pipeTo originalSender


    case OnlineReload(protocol) =>
      implicit val futureTimeout = Timeout(200 seconds)

      log.info(s"Starting to process reload to $actionName. Protocol: [$protocol].")

      val futures:ListBuffer[Future[Any]] = ListBuffer[Future[Any]]()
      for(artifactName <- engineActionMetadata.artifactsToLoad) {
        futures += (artifactSaver ? SaveToLocal(artifactName, protocol))
      }

      Future.sequence(futures).onComplete { response =>
        onlineActionHandler ! Reload(protocol)
      }

    case OnlineHealthCheck =>
      implicit val futureTimeout = Timeout(10 seconds)
      log.info(s"Starting to process health to $actionName.")

      val originalSender = sender
      ask(onlineActionHandler, HealthCheck) pipeTo originalSender

    case Done =>
      log.info("Work Done!")

    case _ =>
      log.warning(s"Not valid message !!")

  }
}
