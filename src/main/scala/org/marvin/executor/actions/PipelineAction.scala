package org.marvin.executor.actions

import akka.Done
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import org.marvin.executor.actions.PipelineAction.PipelineExecute
import org.marvin.executor.proxies.BatchActionProxy
import org.marvin.manager.ArtifactSaver
import org.marvin.model.EngineMetadata
import org.marvin.manager.ArtifactSaver.SaveToRemote
import org.marvin.executor.proxies.EngineProxy.{ExecuteBatch, Reload}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object PipelineAction {
  case class PipelineExecute(protocol:String, params:String)
}

class PipelineAction(metadata: EngineMetadata) extends Actor with ActorLogging{
  implicit val ec = context.dispatcher

  var artifactSaver: ActorRef = _

  override def preStart() = {
    artifactSaver = context.actorOf(Props(new ArtifactSaver(metadata)), name = "artifactSaver")
  }

  override def receive  = {
    case PipelineExecute(protocol, params) =>
      implicit val futureTimeout = Timeout(200 seconds)

      log.info(s"Starting to process pipeline process with. Protocol: [$protocol] and Params: [$params].")

      for(actionName <- metadata.pipelineActions){
        val engineActionMetadata = metadata.actionsMap(actionName)
        val _actor: ActorRef = context.actorOf(Props(new BatchActionProxy(engineActionMetadata)), name = actionName.concat("Actor"))
        Await.result((_actor ? Reload(protocol)), futureTimeout.duration)
        Await.result((_actor ? ExecuteBatch(protocol, params)), futureTimeout.duration)
        context stop _actor

        val futures:ListBuffer[Future[Done]] = ListBuffer[Future[Done]]()

        for(artifactName <- engineActionMetadata.artifactsToLoad) {
          futures += (artifactSaver ? SaveToRemote(artifactName, protocol)).mapTo[Done]
        }

        if (!futures.isEmpty) Future.sequence(futures).onComplete{
          case Success(response) =>
            log.info(s"All artifacts from [$actionName] where saved with successful!! [$response]")
          case Failure(failure) =>
            failure.printStackTrace()
        }
      }

    case Done =>
      log.info("Work Done!")

    case _ =>
      log.warning(s"Not valid message !!")

  }
}