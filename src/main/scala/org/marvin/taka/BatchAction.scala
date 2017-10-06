package org.marvin.taka

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import akka.pattern.pipe

import scala.concurrent.duration._
import org.marvin.model.{EngineActionMetadata, EngineMetadata}
import org.marvin.taka.ActionHandler.{ExecuteBatch, HealthCheck, Reload}
import org.marvin.taka.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.marvin.taka.BatchAction.{BatchExecute, BatchHealthCheck, BatchReload}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future

object BatchAction {
  case class BatchExecute(protocol: String, params: String)
  case class BatchReload(protocol:String)
  case class BatchHealthCheck()
}

class BatchAction(actionName: String, metadata: EngineMetadata) extends Actor with ActorLogging{
  var batchActionHandler: ActorRef = _
  var artifactSaver: ActorRef = _
  var engineActionMetadata: EngineActionMetadata = _
  var artifactsToLoad: String = _
  implicit val ec = context.dispatcher

  override def preStart() = {
    engineActionMetadata = metadata.actionsMap(actionName)
    artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
    batchActionHandler = context.actorOf(Props(new BatchActionProxy(engineActionMetadata)), name = "batchActionHandler")
    artifactSaver = context.actorOf(Props(new ArtifactSaver(metadata)), name = "artifactSaver")
  }

  override def receive  = {
    case BatchExecute(protocol, params) =>
      implicit val futureTimeout = Timeout(200 seconds)
      val originalSender = sender

      log.info(s"Starting to process execute to $actionName. Protocol: [$protocol] and params: [$params].")

      batchActionHandler ? ExecuteBatch(protocol, params)

      for(artifactName <- engineActionMetadata.artifactsToPersist){
        artifactSaver ! SaveToRemote(artifactName, protocol)
      }


    case BatchReload(protocol) =>
      implicit val futureTimeout = Timeout(200 seconds)

      log.info(s"Starting to process reload to $actionName. Protocol: [$protocol].")

      val futures:ListBuffer[Future[Any]] = ListBuffer[Future[Any]]()
      for(artifactName <- engineActionMetadata.artifactsToLoad) {
        futures += (artifactSaver ? SaveToLocal(artifactName, protocol))
      }

      Future.sequence(futures).onComplete { response =>
        batchActionHandler ! Reload(protocol)
      }

    case BatchHealthCheck =>
      implicit val futureTimeout = Timeout(10 seconds)
      log.info(s"Starting to process health to $actionName.")

      val originalSender = sender
      ask(batchActionHandler, HealthCheck) pipeTo originalSender

    case Done =>
      log.info("Work Done!")

    case _ =>
      log.warning(s"Not valid message !!")

  }
}