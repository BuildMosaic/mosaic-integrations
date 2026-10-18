# Spring Canvas dependencies

Use Spring services from unchanged Mosaic Tiles through Canvas. Install one
`SpringCanvasResolver`; services keep their normal Spring definitions.

The tested combination is Mosaic Runtime **1.1.0**, Spring Framework **6.2.19**,
and Kotlin **2.4.20**, built with JDK 21 for JVM target 17. Tests run on JDK 17
and 21. Other framework versions and Spring Boot combinations are not verified. The adapter
uses `spring-beans`; application configuration uses your existing Spring context.
No adapter artifact is published.

## Wire one Canvas

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

Inject this Canvas into a suspending handler and execute existing Tiles:

```kotlin
return mosaicCanvas.withMosaic { compose(greetingTile) }
```

The handler imports `org.buildmosaic.core.injection.withMosaic`. The Tile still
uses ordinary `source<GreetingService>()`; it needs no Spring annotation or import.
`runBlocking` bridges synchronous bean creation once, rather than each request.
Spring closes the Canvas bean at shutdown. Finish Mosaic work before that shutdown.
Avoid eager Canvas providers that introduce cycles during Spring bean creation.

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
mapped. There is no auto-configuration or request-context propagation. Optional
Mosaic analysis cannot infer dynamic framework bindings.

## Verify

From the repository root with JDK 21:

```sh
./gradlew clean build --no-daemon
```

Real Spring contexts test unchanged Tiles, primary/name/alias selection,
errors, Canvas precedence, repeated lookups, ownership/rollback, and servlet
request boundaries. See the [Mosaic core guide](https://github.com/BuildMosaic/Mosaic/blob/main/mosaic-core/README.md)
for composition and Canvas APIs and [Spring's lookup contract](https://docs.spring.io/spring-framework/docs/6.2.x/javadoc-api/org/springframework/beans/factory/ObjectProvider.html)
for candidate selection.
