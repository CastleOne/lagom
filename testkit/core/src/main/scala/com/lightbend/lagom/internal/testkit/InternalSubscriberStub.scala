/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.internal.testkit

import akka.Done
import akka.actor.ActorRef
import akka.stream.Materializer
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import scala.concurrent.Future
import scala.language.higherKinds

private[lagom] class InternalSubscriberStub[Payload, Message[_]](
    groupId: String,
    topicBuffer: ActorRef
)(implicit materializer: Materializer) {

  def mostOnceSource: Source[Message[Payload], _] = {
    Source
      .actorRef[Message[Payload]](1024, OverflowStrategy.fail)
      .prependMat(Source.empty)(subscribeToBuffer)
  }

  def leastOnce(flow: Flow[Message[Payload], Done, _]): Future[Done] = {
    mostOnceSource
      .via(flow)
      .toMat(Sink.ignore)(Keep.right[Any, Future[Done]])
      .run()
  }

  private def subscribeToBuffer[R](ref: ActorRef, t: R) = {
    topicBuffer.tell(TopicBufferActor.SubscribeToBuffer(groupId, ref), ActorRef.noSender)
    t
  }
}
