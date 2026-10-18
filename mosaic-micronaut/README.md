# Micronaut Canvas dependencies

Use Micronaut services from unchanged Mosaic Tiles through Canvas. Install one
`MicronautCanvasResolver`; services keep their normal Micronaut definitions.

The tested combination is Mosaic Runtime **1.1.0**, Micronaut **4.9.3**, and
Kotlin **2.4.20**, built with JDK 21 for JVM target 17. Tests run on JDK 17 and 21.
Other framework versions are not verified. The adapter uses `micronaut-context` and requires no
annotation processing itself; your application's bean definitions use your
normal Micronaut annotation processing. No adapter artifact is published.

## Wire one Canvas

In a Kotlin application with Micronaut annotation processing:

```kotlin
import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.canvas
import org.buildmosaic.integrations.micronaut.MicronautCanvasResolver

@Factory
class MosaicFactory {
  @Singleton
  @Bean(preDestroy = "close")
  fun mosaicCanvas(beans: BeanContext): Canvas = runBlocking {
    canvas { dependencyResolver(MicronautCanvasResolver(beans)) }
  }
}
```

Inject this Canvas into a suspending handler and execute existing Tiles:

```kotlin
return mosaicCanvas.withMosaic { compose(greetingTile) }
```

The handler imports `org.buildmosaic.core.injection.withMosaic`. The Tile still
uses ordinary `source<GreetingService>()`; it needs no Micronaut annotation or
import. `runBlocking` bridges synchronous bean creation once, rather than each
request. Micronaut closes the Canvas bean at shutdown. Finish Mosaic work before
that shutdown. Avoid eager Canvas providers that introduce bean-creation cycles.

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
mapped. There is no auto-configuration or request-context propagation. Optional
Mosaic analysis cannot infer dynamic framework bindings.

## Verify

From the repository root with JDK 21:

```sh
./gradlew clean build --no-daemon
```

Real Micronaut contexts test unchanged Tiles, primary/named selection,
errors, Canvas precedence, repeated lookups, ownership/rollback, and request-proxy
boundaries. See the [Mosaic core guide](https://github.com/BuildMosaic/Mosaic/blob/main/mosaic-core/README.md)
for composition and Canvas APIs and [Micronaut's lookup contract](https://docs.micronaut.io/4.9.3/api/io/micronaut/context/BeanLocator.html)
for candidate selection.
