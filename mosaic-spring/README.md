# Spring Canvas dependencies

Add `mosaic-spring` to a Spring Boot application and inject `Canvas`. Boot
supplies one singleton Canvas connected to the application's bean factory; existing
Tiles resolve Spring services without per-service registration or Mosaic configuration.
Spring closes the Canvas at application shutdown.

The tested combination is Mosaic Runtime **1.1.0**, Spring Boot **3.5.16** / Spring
Framework **6.2.19**, and Kotlin **2.2.21**. Kotlin consumers use the same language/API
2.2 baseline as Mosaic Core. Build JDK 21, JVM target 17; tests run
on JDK 17 and 21. Other framework versions are not verified. No adapter artifact is published;
in this checkout the module is consumed with `implementation(project(":mosaic-spring"))`.
Boot is a compile-only dependency of the adapter; a Boot application supplies its
usual Boot dependencies and enables auto-configuration through `@SpringBootApplication`.

## Use the injected Canvas

```kotlin
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.withMosaic
import org.springframework.stereotype.Service

@Service
class GreetingHandler(private val canvas: Canvas) {
  suspend fun greet(): String = canvas.withMosaic { compose(greetingTile) }
}
```

The existing Tile uses ordinary `source<GreetingService>()` and needs no Spring
annotations or imports. Finish Mosaic work before application shutdown.

## Supply your own Canvas or use plain Spring

Boot backs off when any application Canvas bean is defined. Your Canvas is used
as supplied; the adapter does not install a resolver into it. The low-level
`SpringCanvasResolver(BeanFactory)` remains available for custom wiring and plain
Spring Framework applications, where Boot auto-configuration does not run:

```kotlin
import kotlinx.coroutines.runBlocking
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.canvas
import org.buildmosaic.integrations.spring.SpringCanvasResolver
import org.springframework.beans.factory.BeanFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MosaicConfig {
  @Bean(destroyMethod = "close")
  fun mosaicCanvas(beans: BeanFactory): Canvas = runBlocking {
    canvas { dependencyResolver(SpringCanvasResolver(beans)) }
  }
}
```

`runBlocking` bridges synchronous bean construction to the suspend Canvas builder
at startup. It is not part of request execution. Custom factories must avoid eager
provider lookups that create bean-construction cycles.

## Resolution and qualifiers

| Canvas key qualifier | Spring lookup |
| --- | --- |
| `null` | `getBeanProvider(type).getObject()`, including primary selection |
| String | Exact bean name or alias, checked with `containsBean` then typed `getBean` |

For example, `source<Service>("billing")` means the bean named or aliased
`billing`. It does **not** select arbitrary `@Qualifier("billing")` metadata.
Strings, including empty strings and whitespace, are passed unchanged. Existing
names with an incompatible type throw Spring's type error. Only absence returns
null from `sourceOrNull`; ambiguity, creation, and scope errors propagate.
`source` reports absence with Mosaic's `MosaicMissingKeyException`. Inactive
request scope throws `ScopeNotActiveException` for named and unqualified lookups,
including `sourceOrNull`.

Exact explicit Canvas bindings win, from the nearest layer through its ancestors,
before the resolver runs. This also applies to eager provider construction. One
resolver is installed per Canvas lineage and inherited by children. Child
bindings do not rewire Spring-created services or already constructed parent
services. Each external lookup delegates again, without caching values or misses.

## Scopes and ownership

- **Singletons:** Spring supplies identity and owns destruction.
- **Prototypes:** each lookup asks Spring for an instance. Neither Canvas nor
  Spring context shutdown destroys these instances; the application must arrange
  cleanup for disposable prototypes. Canvas providers or Tile result caching can
  retain a prototype, so lookup frequency is not an execution-scope guarantee.
- **Request scope:** usable only where Spring's scope is registered and its
  request context is active. Missing context remains a Spring scope failure.
  Servlet request attributes are thread-bound; switching coroutine dispatchers
  without application-provided propagation loses them. Tests cover direct lookup
  under servlet request attributes, not end-to-end HTTP coroutine propagation.
- Other custom scopes rely on the application's Spring configuration and are
  not verified by this adapter.

Resolver lookup borrows beans. Closing a Canvas or rolling back its construction
never closes beans merely obtained through the resolver, or the resolver/factory
itself. Spring request completion destroys request beans independently of Canvas.
**`provide` owns its returned value**, even when it came from Spring. For an
external value or alias, use `instance<Interface>(bean)` or
`instance<Interface>("alias", bean)` to keep it borrowed. A Canvas layer does not
create a Spring request scope. Complete all request work before request completion.

Canvas keys carry a class and optional string: annotation-valued qualifiers,
generic type tokens, collection injection, and injection-point metadata are not
mapped. Request-context propagation remains the application's responsibility.
Optional Mosaic analysis cannot infer dynamic framework bindings.

## Verify

From the repository root with JDK 21:

```sh
./gradlew clean build --no-daemon
```

Real Boot contexts test discovery through auto-configuration imports, constructor
injection, custom Canvas backoff, Tile execution, and shutdown ownership.
Real Spring contexts also test primary/name/alias selection,
errors, Canvas precedence, repeated lookups, ownership/rollback, and servlet
request boundaries. See the [Mosaic core guide](https://github.com/BuildMosaic/Mosaic/blob/main/mosaic-core/README.md)
for composition and Canvas APIs and [Spring's lookup contract](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/beans/factory/ObjectProvider.html)
for candidate selection.
