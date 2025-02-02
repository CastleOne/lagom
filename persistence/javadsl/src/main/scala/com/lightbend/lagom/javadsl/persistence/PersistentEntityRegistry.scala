/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.javadsl.persistence

import java.util.concurrent.CompletionStage
import java.util.Optional
import java.util.UUID

import akka.japi.Pair
import akka.japi.function.Creator
import akka.stream.javadsl
import akka.Done
import akka.NotUsed
import com.lightbend.lagom.javadsl.persistence.Offset.Sequence
import com.lightbend.lagom.javadsl.persistence.Offset.TimeBasedUUID

import scala.concurrent.duration._

/**
 * At system startup all [[PersistentEntity]] classes must be registered here
 * with [[PersistentEntityRegistry#register]].
 *
 * Later, [[PersistentEntityRef]] can be retrieved with [[PersistentEntityRegistry#refFor]].
 * Commands are sent to a [[PersistentEntity]] using a `PersistentEntityRef`.
 */
trait PersistentEntityRegistry {

  /**
   * At system startup all [[PersistentEntity]] classes must be registered
   * with this method.
   */
  def register[C, E, S](entityClass: Class[_ <: PersistentEntity[C, E, S]]): Unit

  /**
   * Retrieve a [[PersistentEntityRef]] for a given [[PersistentEntity]] class
   * and identifier. Commands are sent to a `PersistentEntity` using a `PersistentEntityRef`.
   */
  def refFor[C](entityClass: Class[_ <: PersistentEntity[C, _, _]], entityId: String): PersistentEntityRef[C]

  /**
   * A stream of the persistent events that have the given `aggregateTag`, e.g.
   * all persistent events of all `Order` entities.
   *
   * The type of the offset is journal dependent, some journals use time-based
   * UUID offsets, while others use sequence numbers. The passed in `fromOffset`
   * must either be [[Offset#NONE]], or an offset that has previously been produced
   * by this journal.
   *
   * The stream will begin with events starting ''after'' `fromOffset`.
   * To resume an event stream, store the `Offset` corresponding to the most
   * recently processed `Event`, and pass that back as the value for
   * `fromOffset` to start the stream from events following that one.
   *
   * @throws IllegalArgumentException If the `fromOffset` type is not supported
   *   by this journal.
   */
  def eventStream[Event <: AggregateEvent[Event]](
      aggregateTag: AggregateEventTag[Event],
      fromOffset: Offset
  ): javadsl.Source[Pair[Event, Offset], NotUsed]

  /**
   * A stream of the persistent events that have the given `aggregateTag`, e.g.
   * all persistent events of all `Order` entities.
   *
   * This method will only work with journals that support UUID offsets. Journals that
   * produce sequence offsets will fail during stream handling.
   */
  @deprecated("Use eventStream(AggregateEventTag, Offset) instead", "1.2.0")
  def eventStream[Event <: AggregateEvent[Event]](
      aggregateTag: AggregateEventTag[Event],
      fromOffset: Optional[UUID]
  ): javadsl.Source[Pair[Event, UUID], NotUsed] = {
    val offset = if (fromOffset.isPresent) {
      Offset.timeBasedUUID(fromOffset.get())
    } else Offset.NONE
    eventStream(aggregateTag, offset).asScala.map { pair =>
      val uuid = pair.second match {
        case timeBased: TimeBasedUUID => timeBased.value()
        case sequence: Sequence       =>
          // While we *could* translate the sequence number to a time-based UUID, this would be very bad, since the UUID
          // would either be non unique (violating the fundamental aim of UUIDs), or it would change every time the
          // event was loaded. Also, a sequence number is not a timestamp.
          throw new IllegalStateException("Sequence based offset is not supported in a UUID event stream")
      }
      Pair(pair.first, uuid)
    }.asJava
  }

  /**
   * No-op method that exists only for backward-compatibility reasons.
   * Lagom now uses Akka's CoordinatedShutdown to gracefully shut down all sharded entities,
   * including Persistent Entities.
   *
   * @return a completed `CompletionStage`
   * @deprecated As of Lagom 1.4, this method has no effect and no longer needs to be called
   *
   */
  @deprecated("This method has no effect and no longer needs to be called", "1.4.0")
  def gracefulShutdown(timeout: FiniteDuration): CompletionStage[Done]

}
