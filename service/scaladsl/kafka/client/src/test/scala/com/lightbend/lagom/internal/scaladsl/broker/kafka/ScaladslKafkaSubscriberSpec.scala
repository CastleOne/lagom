/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package com.lightbend.lagom.internal.scaladsl.broker.kafka

import org.scalatest.FlatSpec
import org.scalatest.Matchers

/**
 *
 */
class ScaladslKafkaSubscriberSpec extends FlatSpec with Matchers {

  behavior.of("ScaladslKafkaSubscriber")

  it should "create a new subscriber with updated groupId" in {
    val subscriber =
      new ScaladslKafkaSubscriber(null, null, ScaladslKafkaSubscriber.GroupId("old"), null, null, null, null)(
        null,
        null
      )
    subscriber.withGroupId("newGID") should not be subscriber
  }
}
