/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.internal.javadsl.persistence.cassandra

import javax.inject.Inject
import javax.inject.Singleton

import akka.actor.ActorSystem
import com.lightbend.lagom.internal.persistence.ReadSideConfig
import com.lightbend.lagom.internal.persistence.cassandra.CassandraOffsetStore
import com.lightbend.lagom.internal.persistence.cassandra.CassandraReadSideSettings
import com.lightbend.lagom.javadsl.persistence.cassandra.CassandraSession

import scala.concurrent.ExecutionContext

/**
 * Internal API
 */
@Singleton
private[lagom] final class JavadslCassandraOffsetStore @Inject()(
    system: ActorSystem,
    session: CassandraSession,
    cassandraReadSideSettings: CassandraReadSideSettings,
    config: ReadSideConfig
)(implicit ec: ExecutionContext)
    extends CassandraOffsetStore(system, session.scalaDelegate, cassandraReadSideSettings, config)
