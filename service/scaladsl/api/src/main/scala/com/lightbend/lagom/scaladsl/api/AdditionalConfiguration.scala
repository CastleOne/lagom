/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.scaladsl.api

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import play.api.Configuration

/**
 * This trait allows Lagom integrations to define additional configuration that gets mixed into the application trait.
 *
 * By extending this, and overriding `additionalConfiguration`, an integration can inject configuration, and the user
 * can control which order this configuration gets applied by changing the order in which traits are mixed together.
 */
trait ProvidesAdditionalConfiguration {

  /**
   * Define the additional configuration to add to the application.
   *
   * Classes that override this must combine the configuration they add with the configuration from the super
   * implementation. Failure to do this will prevent different integrations from working with each other.
   *
   * When overriding, the overridden file should be a def, so as to ensure multiple components can all override it.
   * Lagom will only invoke this method once from a lazy val, so it will effectively be calculated once.
   */
  def additionalConfiguration: AdditionalConfiguration = AdditionalConfiguration.empty
}

/**
 * Additional configuration that will be added to the main system configuration.
 */
final class AdditionalConfiguration private (private[lagom] val configuration: Config) {

  @deprecated(message = "prefer constructor using typesafe Config instead", since = "1.4.0")
  def this(configuration: Configuration) = this(configuration.underlying)

  /**
   * Add configuration to the additional configuration.
   */
  def ++(configurationToAdd: Config): AdditionalConfiguration = {
    new AdditionalConfiguration(configuration.withFallback(configurationToAdd))
  }

  @deprecated(message = "prefer method using typesafe Config instead", since = "1.4.0")
  def ++(configurationToAdd: Configuration): AdditionalConfiguration = {
    this.++(configurationToAdd.underlying)
  }

}

object AdditionalConfiguration {
  private[lagom] val empty = new AdditionalConfiguration(ConfigFactory.empty)
}
