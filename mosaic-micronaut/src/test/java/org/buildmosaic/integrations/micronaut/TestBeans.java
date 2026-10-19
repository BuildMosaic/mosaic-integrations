package org.buildmosaic.integrations.micronaut;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.runtime.http.scope.RequestScope;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;

@Factory
public class TestBeans {
  public interface Choice {}
  public interface Broken {}
  public interface Preferred { String value(); }

  public static class Resource implements AutoCloseable {
    private static final AtomicInteger identities = new AtomicInteger();
    private final int id = identities.incrementAndGet();
    public int closes;
    public int identity() { return id; }
    @Override public void close() { closes++; }
  }

  @Singleton
  GreetingService greeting() { return () -> "micronaut"; }

  @Singleton @Named("first")
  Choice first() { return new Choice() {}; }

  @Singleton @Named("second")
  Choice second() { return new Choice() {}; }

  @Singleton @Primary
  Preferred primary() { return () -> "primary"; }

  @Singleton @Named("other")
  Preferred other() { return () -> "other"; }

  @Singleton @Named("number")
  Integer number() { return 42; }

  @Prototype @Named("prototype") @Bean(preDestroy = "close")
  Resource prototype() { return new Resource(); }

  @Singleton @Named("shared") @Bean(preDestroy = "close")
  Resource shared() { return new Resource(); }

  @RequestScope @Named("request")
  Resource request() { return new Resource(); }

  @Singleton
  Broken broken() { throw new IllegalStateException("creation failed"); }
}
