/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */
package sample.hellostream.impl;

import com.google.inject.AbstractModule;
import com.lightbend.lagom.javadsl.server.ServiceGuiceSupport;
import sample.helloworld.api.HelloService;
import sample.hellostream.api.HelloStream;

/**
 * The module that binds the HelloStream so that it can be served.
 */
public class HelloStreamModule extends AbstractModule implements ServiceGuiceSupport {
  @Override
  protected void configure() {
    // Bind the HelloStream service
    bindService(HelloStream.class, HelloStreamImpl.class);
    // Bind the HelloService client
    bindClient(HelloService.class);
  }
}
