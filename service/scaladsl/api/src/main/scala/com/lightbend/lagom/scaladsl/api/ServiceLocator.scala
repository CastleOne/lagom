/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.scaladsl.api

import java.net.URI

import com.lightbend.lagom.scaladsl.api.Descriptor.Call

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Locates services.
 *
 * The service locator is responsible for two things, one is locating services according to the passed in name and
 * service call information, the other is to implement circuit breaking functionality when
 * [[#doWithService]] is invoked.
 *
 * The reason circuit breaking is a service locator concern is that generally, the service locator will want to be aware
 * of when a circuit breaker is open, and respond accordingly.  For example, it may decide to pull that node from its
 * routing pool, or it may decide to notify some up stream service registry that that node is no longer responding.
 */
trait ServiceLocator {

  /**
   * Locate a service's URI for the given name.
   *
   * @param name The name of the service.
   * @return The URI for that service, if it exists.
   */
  def locate(name: String): Future[Option[URI]] = locate(name, Descriptor.NoCall)

  /**
   * Locate the service's URIs for the given name.
   *
   * @param name The name of the service.
   * @return One or more URIs for that service, otherwise an empty List ([[Nil]]) if none is found.
   * @since 1.4
   */
  def locateAll(name: String): Future[List[URI]] = locateAll(name, Descriptor.NoCall)

  /**
   * Locate a service's URI for the given name.
   *
   * @param name        The name of the service.
   * @param serviceCall The service call descriptor that this lookup is for.
   * @return The URI for that service, if it exists.
   */
  def locate(name: String, serviceCall: Descriptor.Call[_, _]): Future[Option[URI]]

  /**
   * Locate the service's URIs for the given name.
   *
   * @param name        The name of the service.
   * @param serviceCall The service call descriptor that this lookup is for.
   * @return One or more URIs for that service, otherwise an empty List ([[Nil]]) if none is found.
   * @since 1.4
   */
  def locateAll(name: String, serviceCall: Descriptor.Call[_, _]): Future[List[URI]] =
    locate(name, serviceCall).map(_.toList)(ExecutionContext.global)

  /**
   * Do the given action with the given service.
   *
   * This should be used in preference to [[#locate]] when possible as it will allow the
   * service locator to add in things like circuit breakers.
   *
   * It is required that the service locator will, based on the service call circuit breaker configuration, wrap the
   * invocation of the passed in block with a circuit breaker.
   *
   * @param name        The name of the service.
   * @param serviceCall The service call descriptor that this lookup is for.
   * @param block       A block of code that takes the URI for the service, and returns a future of some work done on the
   *              service. This will only be executed if the service was found.
   * @return The result of the executed block, if the service was found.
   */
  def doWithService[T](name: String, serviceCall: Descriptor.Call[_, _])(block: URI => Future[T])(
      implicit ec: ExecutionContext
  ): Future[Option[T]]
}

object ServiceLocator {

  /**
   * A service locator that doesn't resolve any services.
   */
  object NoServiceLocator extends ServiceLocator {
    override def locate(name: String, serviceCall: Call[_, _]): Future[Option[URI]] = Future.successful(None)
    override def doWithService[T](name: String, serviceCall: Call[_, _])(block: (URI) => Future[T])(
        implicit ec: ExecutionContext
    ): Future[Option[T]] = Future.successful(None)
  }
}
