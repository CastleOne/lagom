/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.scaladsl.client.compile

import com.lightbend.lagom.scaladsl.api.Service
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.client.ServiceClient
import com.lightbend.lagom.scaladsl.client.ServiceClientConstructor
import com.lightbend.lagom.scaladsl.client.ServiceClientImplementationContext
import com.lightbend.lagom.macrotestkit.ShouldNotTypecheck
import Service._

object ServiceClientMacroErrors {

  ShouldNotTypecheck(
    "Abstract non service call check",
    "MacroErrorsServiceClient.implement[AbstractNonServiceCall]",
    "(?s).*abstract methods don't return service calls or topics.*foo.*"
  )

  ShouldNotTypecheck(
    "Abstract descriptor method check",
    "MacroErrorsServiceClient.implement[AbstractDescriptor]",
    ".*AbstractDescriptor\\.descriptor must be implemented.*"
  )

  ShouldNotTypecheck(
    "Overloaded method check",
    "MacroErrorsServiceClient.implement[OverloadedMethods]",
    ".*overloaded methods are: foo.*"
  )

}

object MacroErrorsServiceClient extends ServiceClientConstructor {
  override def construct[S <: Service](constructor: (ServiceClientImplementationContext) => S): S = null.asInstanceOf[S]
}

trait AbstractNonServiceCall extends Service {
  def foo: String

  override def descriptor = named("foo")
}

trait AbstractDescriptor extends Service {}

trait OverloadedMethods extends Service {
  def foo(arg: String): ServiceCall[String, String]
  def foo(arg1: String, arg2: String): ServiceCall[String, String]

  override def descriptor = named("foo")
}
