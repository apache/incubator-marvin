package org.marvin.executor.actions

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import org.marvin.executor.actions.BatchAction.{BatchExecute, BatchHealthCheck, BatchReload}
import org.marvin.executor.proxies.BatchActionProxy
import org.marvin.manager.ArtifactSaver
import org.marvin.model.{EngineActionMetadata, EngineMetadata}
import org.marvin.manager.ArtifactSaver.{SaveToLocal, SaveToRemote}
import org.marvin.executor.proxies.EngineProxy.{ExecuteBatch, HealthCheck, Reload}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.concurrent.duration._

object BatchAction {
  case class BatchExecute(protocol: String, params: String)
  case class BatchReload(protocol:String)
  case class BatchHealthCheck()
}

class BatchAction(actionName: String, metadata: EngineMetadata) extends Actor with ActorLogging{
  var batchActionProxy: ActorRef = _
  var artifactSaver: ActorRef = _
  var engineActionMetadata: EngineActionMetadata = _
  var artifactsToLoad: String = _
  implicit val ec = context.dispatcher

  override def preStart() = {
    engineActionMetadata = metadata.actionsMap(actionName)
    artifactsToLoad = engineActionMetadata.artifactsToLoad.mkString(",")
    batchActionProxy = context.actorOf(Props(new BatchActionProxy(engineActionMetadata)), name = "batchActionProxy")
    artifactSaver = context.actorOf(Props(new ArtifactSaver(metadata)), name = "artifactSaver")
  }

  override def receive  = {
    case BatchExecute(protocol, params) =>
      implicit val futureTimeout = Timeout(metadata.batchActionTimeout milliseconds)

      log.info(s"Starting to process execute to $actionName. Protocol: [$protocol] and params: [$params].")

      batchActionProxy ? ExecuteBatch(protocol, params)

      for(artifactName <- engineActionMetadata.artifactsToPersist){
        artifactSaver ! SaveToRemote(artifactName, protocol)
      }


    case BatchReload(protocol) =>
      implicit val futureTimeout = Timeout(metadata.reloadTimeout milliseconds)

      log.info(s"Starting to process reload to $actionName. Protocol: [$protocol].")

      val futures:ListBuffer[Future[Any]] = ListBuffer[Future[Any]]()
      for(artifactName <- engineActionMetadata.artifactsToLoad) {
        futures += (artifactSaver ? SaveToLocal(artifactName, protocol))
      }

      Future.sequence(futures).onComplete { response =>
        batchActionProxy ! Reload(protocol)
      }

    case BatchHealthCheck =>
      implicit val futureTimeout = Timeout(metadata.healthCheckTimeout milliseconds)
      log.info(s"Starting to process health to $actionName.")

      val originalSender = sender
      ask(batchActionProxy, HealthCheck) pipeTo originalSender

    case Done =>
      log.info("Work Done!")

    case _ =>
      log.warning(s"Not valid message !!")

  }
}