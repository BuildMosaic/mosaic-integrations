# Micronaut Canvas dependencies

Add `mosaic-micronaut` to a Micronaut application and inject `Canvas`. Micronaut
discovers the adapter's bean definitions and supplies one singleton Canvas connected
to its DI container. Existing Tiles need no changes, per-service registration, or
application-specific Mosaic factory. Micronaut closes the Canvas at shutdown.

The tested combination is Mosaic Runtime **1.1.0**, Micronaut **4.9.3**, and Kotlin
**2.4.20**. Build JDK 21, JVM target 17; tests run on JDK 17 and 21. Other versions
are not verified. No adapter artifact is published; in this checkout the module
is consumed with `implementation(project(":mosaic-micronaut"))`. The adapter's
bean definitions ship in its JAR; no adapter-specific annotation-processing or
package-scanning configuration is needed in the application.

## Use the injected Canvas

```kotlin
import jakarta.inject.Singleton
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.withMosaic

@Singleton
class GreetingHandler(private val canvas: Canvas) {
  suspend fun greet(): String = canvas.withMosaic { compose(greetingTile) }
}
```

The existing Tile uses ordinary `source<GreetingService>()` and needs no Micronaut
annotations or imports. Finish Mosaic work before application shutdown.

## Supply your own Canvas

The default factory backs off when the application defines a Canvas. Your Canvas
is used as supplied; the adapter does not install a resolver into it. For example,
custom wiring can still use the low-level `MicronautCanvasResolver(BeanLocator)`:

```kotlin
import io.micronaut.context.BeanLocator
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.canvas
import org.buildmosaic.integrations.micronaut.MicronautCanvasResolver

@Factory
class ApplicationCanvasFactory {
  @Singleton
  @Bean(preDestroy = "close")
  fun applicationCanvas(beans: BeanLocator): Canvas = runBlocking {
    canvas {
      dependencyResolver(MicronautCanvasResolver(beans))
      // Add application-specific bindings here.
    }
  }
}
```

`runBlocking` bridges synchronous bean construction to the suspend Canvas builder
at startup. It is not part of request execution. Custom factories must avoid eager
provider lookups that create bean-construction cycles.

## Resolution and qualifiers

| Canvas key qualifier | Micronaut lookup |
| --- | --- |
| `null` | `findBean(type, null)`, including primary selection |
| String | `findBean(type, Qualifiers.byName(value))` |

For example, `source<Service>("billing")` uses Micronaut's named qualifier,
including `@Named("billing")`. This follows Micronaut's matching rules, including
case-insensitive name matching and bean-type-name conventions, rather than a
portable exact bean-name contract. Strings, including empty strings and whitespace,
are passed unchanged; the framework determines what they match. Type and name
are matched together, so an incompatible type/name pair is absent.

Only absence returns null from `sourceOrNull`; ambiguity, creation, and scope
errors propagate. `source` reports absence with Mosaic's `MosaicMissingKeyException`.
Exact explicit Canvas bindings win, from the nearest layer through its ancestors,
before the resolver runs. This also applies to eager provider construction. One
resolver is installed per Canvas lineage and inherited by children. Child bindings
do not rewire Micronaut-created services or already constructed parent services.
Each external lookup delegates again, without caching values or misses.

## Scopes and ownership

- **Singletons:** Micronaut supplies identity and owns destruction.
- **Prototypes:** each lookup asks Micronaut for an instance. Canvas does not
  destroy it, and context shutdown does not clean up these prototype instances.
  The application must arrange cleanup for disposable prototypes. Canvas providers
  or Tile result caching can retain a prototype, so lookup frequency is not an
  execution-scope guarantee.
- **Request scope:** requires the application's Micronaut HTTP server scope
  infrastructure. Micronaut can return a scoped proxy without an active request;
  invoking that proxy then fails. Under an active `ServerRequestContext`, the
  proxy uses one target per request. Tests cover direct proxy invocation under
  framework request contexts, not end-to-end HTTP coroutine propagation or server
  request termination. Switching coroutine dispatchers without application-provided
  propagation loses the request context.
- Other custom scopes rely on the application's Micronaut configuration and are
  not verified by this adapter.

Resolver lookup borrows beans. Closing a Canvas or rolling back its construction
never closes beans merely obtained through the resolver, or the resolver/locator
itself. **`provide` owns its returned value**, even when it came from Micronaut.
For an external value or alias, use `instance<Interface>(bean)` or
`instance<Interface>("alias", bean)` to keep it borrowed. A Canvas layer does not
create a Micronaut request scope. Complete all request work within the framework's
request lifetime.

Canvas keys carry a class and optional string: annotation-valued qualifiers,
generic type tokens, collection injection, and injection-point metadata are not
mapped. Request-context propagation remains the application's responsibility.
Optional Mosaic analysis cannot infer dynamic framework bindings.

## Verify

From the repository root with JDK 21:

```sh
./gradlew clean build --no-daemon
```

Real Micronaut contexts test library bean discovery, constructor injection, custom
Canvas backoff, Tile execution, and shutdown ownership. They also test primary/named selection,
errors, Canvas precedence, repeated lookups, ownership/rollback, and request-proxy
boundaries. See the [Mosaic core guide](https://github.com/BuildMosaic/Mosaic/blob/main/mosaic-core/README.md)
for composition and Canvas APIs and [Micronaut's lookup contract](https://docs.micronaut.io/4.9.3/api/io/micronaut/context/BeanLocator.html)
for candidate selection.
