package org.apache.marvin.executor.manager

import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.actor.{Actor, ActorLogging, Address}

import scala.collection.immutable

class ExecutorClusterListener(seedNodes: immutable.Seq[Address]) extends Actor with ActorLogging {

  var cluster: Cluster = _

  override def preStart(): Unit = {
    cluster = Cluster(context.system)

    log.info(s"Joining to the cluster ${context.system.name} ...")
    cluster.joinSeedNodes(seedNodes)

    log.info(s"Subscribing to the cluster ${context.system.name} ...")
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberUp], classOf[MemberEvent], classOf[UnreachableMember])

    log.info(s"Cluster configuration done! :-P")
    log.info(s"Cluster Node Address is ${cluster.selfAddress}")
  }

  override def postStop(): Unit = {
    log.info(s"Leaving cluster ${context.system.name} :-( ...")
    cluster.unsubscribe(self)
    cluster.leave(cluster.selfAddress)
    log.info("Left cluster with success!")
  }

  def receive = {
    case MemberUp(member) =>
      log.info("Member is Up: {}", member.address)

    case UnreachableMember(member) =>
      log.info("Member detected as unreachable: {}", member)

    case MemberRemoved(member, previousStatus) =>
      log.info("Member is Removed: {} after {}", member.address, previousStatus)

    case _:MemberEvent =>
      log.info("Unknow Message received ...")
  }
}
