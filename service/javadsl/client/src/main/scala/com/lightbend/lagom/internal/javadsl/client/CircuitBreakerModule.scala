/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.internal.javadsl.client

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import akka.actor.ActorSystem
import com.lightbend.lagom.internal.client.CircuitBreakerConfig
import com.lightbend.lagom.internal.client.CircuitBreakerMetricsProviderImpl
import com.lightbend.lagom.internal.client.CircuitBreakerMetricsProviderProvider
import com.lightbend.lagom.internal.client.CircuitBreakers
import com.lightbend.lagom.internal.spi.CircuitBreakerMetricsProvider
import com.lightbend.lagom.javadsl.client.CircuitBreakersPanel
import play.api.inject.Binding
import play.api.inject.Module
import play.api.Configuration
import play.api.Environment

class CircuitBreakerModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[CircuitBreakersPanel].to[CircuitBreakersPanelImpl],
      // for backward compatibility we still need to provide it for wiring
      bind[CircuitBreakers].toProvider[CircuitBreakersProvider],
      bind[CircuitBreakerMetricsProvider].toProvider[CircuitBreakerMetricsProviderProvider],
      bind[CircuitBreakerConfig].toSelf,
      bind[CircuitBreakerMetricsProviderImpl].toSelf
    )
  }
}

@Singleton
class CircuitBreakersProvider @Inject()(
    system: ActorSystem,
    circuitBreakerConfig: CircuitBreakerConfig,
    metricsProvider: CircuitBreakerMetricsProvider
) extends Provider[CircuitBreakers] {
  lazy val get = {

    // only in case some 3rd party lib are still wiring the old one.
    system.log.warning(
      "CircuitBreakers is deprecated, use CircuitBreakersPanel instead. This warning is probably caused by your " +
        "service locator. If you are using a 3rd party service locator, upgrade your dependencies, otherwise this " +
        "service locator could become incompatible with Lagom in future versions."
    )

    new CircuitBreakers(system, circuitBreakerConfig, metricsProvider)
  }
}
