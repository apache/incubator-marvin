package org.marvin.executor.actions

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.marvin.executor.actions.OnlineAction.{OnlineExecute, OnlineHealthCheck, OnlineReload}
import org.marvin.manager.ArtifactSaver
import org.marvin.model.{EngineActionMetadata, EngineMetadata}
import org.marvin.manager.ArtifactSaver.SaveToLocal
import org.marvin.executor.proxies.EngineProxy.{ExecuteOnline, HealthCheck, Reload}
import org.marvin.executor.proxies.OnlineActionProxy

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

object OnlineAction {
  case class OnlineExecute(message: String, params: String)
  case class OnlineReload(protocol:String)
  case class OnlineHealthCheck()
}

class OnlineAction(actionName: String, metadata: EngineMetadata) extends Actor with ActorLogging{
  var onlineActionProxy: ActorRef = _
  var artifactSaver: ActorRef = _
  var engineActionMetadata: EngineActionMetadata = _
  var artifactsToLoad: String = _
  implicit val ec = context.dispatcher

  override def preStart() = {
    engineActionMetadata = metadata.actionsMap(actionName)
    artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
    onlineActionProxy = context.actorOf(Props(new OnlineActionProxy(engineActionMetadata)), name = "onlineActionProxy")
    artifactSaver = context.actorOf(Props(new ArtifactSaver(metadata)), name = "artifactSaver")
  }

  override def receive  = {
    case OnlineExecute(message, params) =>
      implicit val futureTimeout = Timeout(metadata.onlineActionTimeout milliseconds)

      log.info(s"Starting to process execute to $actionName. Message: [$message] and params: [$params].")

      val originalSender = sender
      ask(onlineActionProxy, ExecuteOnline(message, params)) pipeTo originalSender


    case OnlineReload(protocol) =>
      implicit val futureTimeout = Timeout(metadata.reloadTimeout milliseconds)

      log.info(s"Starting to process reload to $actionName. Protocol: [$protocol].")

      val futures:ListBuffer[Future[Any]] = ListBuffer[Future[Any]]()
      for(artifactName <- engineActionMetadata.artifactsToLoad) {
        futures += (artifactSaver ? SaveToLocal(artifactName, protocol))
      }

      Future.sequence(futures).onComplete { response =>
        onlineActionProxy ! Reload(protocol)
      }

    case OnlineHealthCheck =>
      implicit val futureTimeout = Timeout(metadata.healthCheckTimeout milliseconds)
      log.info(s"Starting to process health to $actionName.")

      val originalSender = sender
      ask(onlineActionProxy, HealthCheck) pipeTo originalSender

    case Done =>
      log.info("Work Done!")

    case _ =>
      log.warning(s"Not valid message !!")

  }
}
