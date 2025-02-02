/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.internal.scaladsl.persistence.jdbc

import java.sql.Connection

import akka.persistence.query.Offset
import akka.stream.scaladsl.Flow
import akka.Done
import akka.NotUsed
import com.lightbend.lagom.internal.persistence.jdbc.SlickOffsetDao
import com.lightbend.lagom.internal.persistence.jdbc.SlickOffsetStore
import com.lightbend.lagom.internal.persistence.jdbc.SlickProvider
import com.lightbend.lagom.scaladsl.persistence.ReadSideProcessor.ReadSideHandler
import com.lightbend.lagom.scaladsl.persistence.jdbc.JdbcReadSide
import com.lightbend.lagom.scaladsl.persistence.AggregateEvent
import com.lightbend.lagom.scaladsl.persistence.AggregateEventTag
import com.lightbend.lagom.scaladsl.persistence.EventStreamElement
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.reflect.ClassTag

/**
 * INTERNAL API
 */
private[lagom] class JdbcReadSideImpl(slick: SlickProvider, offsetStore: SlickOffsetStore)(
    implicit val ec: ExecutionContext
) extends JdbcReadSide {

  private val log = LoggerFactory.getLogger(this.getClass)

  override def builder[Event <: AggregateEvent[Event]](readSideId: String): ReadSideHandlerBuilder[Event] =
    new ReadSideHandlerBuilder[Event] {
      var globalPrepare: Connection => Unit = { _ =>
        ()
      }
      var prepare: (Connection, AggregateEventTag[Event]) => Unit = (_, _) => ()
      var eventHandlers                                           = Map.empty[Class[_ <: Event], (Connection, EventStreamElement[_ <: Event]) => Unit]

      override def setGlobalPrepare(callback: Connection => Unit): ReadSideHandlerBuilder[Event] = {
        globalPrepare = callback
        this
      }

      override def setPrepare(
          callback: (Connection, AggregateEventTag[Event]) => Unit
      ): ReadSideHandlerBuilder[Event] = {
        prepare = callback
        this
      }

      override def setEventHandler[E <: Event: ClassTag](
          handler: (Connection, EventStreamElement[E]) => Unit
      ): ReadSideHandlerBuilder[Event] = {
        val eventClass = implicitly[ClassTag[E]].runtimeClass.asInstanceOf[Class[Event]]
        eventHandlers += (eventClass -> handler.asInstanceOf[(Connection, EventStreamElement[_ <: Event]) => Unit])
        this
      }

      override def build(): ReadSideHandler[Event] =
        new JdbcReadSideHandler[Event](readSideId, globalPrepare, prepare, eventHandlers)
    }

  private class JdbcReadSideHandler[Event <: AggregateEvent[Event]](
      readSideId: String,
      globalPrepareCallback: Connection => Any,
      prepareCallback: (Connection, AggregateEventTag[Event]) => Any,
      eventHandlers: Map[Class[_ <: Event], (Connection, EventStreamElement[_ <: Event]) => Any]
  ) extends ReadSideHandler[Event] {

    import slick.profile.api._

    @volatile
    private var offsetDao: SlickOffsetDao = _

    override def globalPrepare(): Future[Done] =
      slick.ensureTablesCreated().flatMap { _ =>
        slick.db.run {
          SimpleDBIO { ctx =>
            globalPrepareCallback(ctx.connection)
            Done.getInstance()
          }
        }
      }

    override def prepare(tag: AggregateEventTag[Event]): Future[Offset] =
      for {
        _ <- slick.db.run {
          SimpleDBIO { ctx =>
            prepareCallback(ctx.connection, tag)
          }
        }
        dao <- offsetStore.prepare(readSideId, tag.tag)
      } yield {
        offsetDao = dao
        dao.loadedOffset
      }

    override def handle(): Flow[EventStreamElement[Event], Done, NotUsed] =
      Flow[EventStreamElement[Event]]
        .mapAsync(parallelism = 1) { element =>
          val dbAction = eventHandlers
            .get(element.event.getClass)
            .map { handler =>
              val castedHandler = handler.asInstanceOf[(Connection, EventStreamElement[Event]) => Unit]
              SimpleDBIO { ctx =>
                castedHandler(ctx.connection, element)
              }
            }
            .getOrElse {
              // fallback to empty action if no handler is found
              if (log.isDebugEnabled) log.debug("Unhandled event [{}]", element.event.getClass.getName)
              DBIO.successful(())
            }
            .flatMap { _ =>
              offsetDao.updateOffsetQuery(element.offset)
            }
            .map(_ => Done)

          slick.db.run(dbAction.transactionally)

        }

  }
}
