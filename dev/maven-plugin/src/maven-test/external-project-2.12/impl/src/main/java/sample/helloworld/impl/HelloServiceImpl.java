/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package sample.helloworld.impl;

import akka.NotUsed;
import com.lightbend.lagom.javadsl.api.ServiceCall;
import sample.helloworld.api.HelloService;

import java.util.concurrent.CompletableFuture;

/**
 * Implementation of the HelloService.
 */
public class HelloServiceImpl implements HelloService {

  @Override
  public ServiceCall<NotUsed, String> hello(String id) {
    return request -> {
      return CompletableFuture.completedFuture("Hello, " + id  + "!");
    };
  }
}
