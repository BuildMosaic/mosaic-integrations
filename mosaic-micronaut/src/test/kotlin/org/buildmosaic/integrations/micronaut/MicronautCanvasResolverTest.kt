package org.buildmosaic.integrations.micronaut

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.context.exceptions.NonUniqueBeanException
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.http.HttpRequest
import io.micronaut.http.context.ServerRequestContext
import java.util.function.Supplier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.buildmosaic.core.exception.MosaicMissingKeyException
import org.buildmosaic.core.injection.CanvasKey
import org.buildmosaic.core.injection.canvas
import org.buildmosaic.core.injection.source
import org.buildmosaic.core.injection.sourceOrNull
import org.buildmosaic.core.injection.withMosaic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class MicronautCanvasResolverTest {
  interface Absent
  fun interface LateService { fun value(): String }

  @Test
  fun `Tiles resolve services and concurrent lookups retain singleton identity`() = runBlocking<Unit> {
    ApplicationContext.run().use { context ->
      canvas { dependencyResolver(MicronautCanvasResolver(context)) }.use { root ->
        assertEquals("micronaut", root.withMosaic { compose(greetingTile) })
        val bean = context.getBean(GreetingService::class.java)
        (1..20).map {
          async(Dispatchers.Default) { assertSame(bean, root.source<GreetingService>()) }
        }.awaitAll()
      }
    }
  }

  @Test
  fun `framework primary named qualifier and boxed Kotlin type selection are preserved`() = runBlocking<Unit> {
    ApplicationContext.run().use { context ->
      canvas { dependencyResolver(MicronautCanvasResolver(context)) }.use { root ->
        assertEquals("primary", root.source<TestBeans.Preferred>().value())
        assertEquals("other", root.source<TestBeans.Preferred>("other").value())
        assertEquals("other", root.source<TestBeans.Preferred>("OTHER").value())
        assertEquals(42, root.source<Int>())
        assertEquals(42, root.source<Int>("number"))
        assertNull(root.sourceOrNull<TestBeans.Preferred>(" other "))
        // Empty-name behavior belongs to Micronaut too, rather than an adapter normalization rule.
        assertSame(
          context.findBean(TestBeans.Preferred::class.java, Qualifiers.byName("")).orElse(null),
          root.sourceOrNull<TestBeans.Preferred>(""),
        )
        assertSame(root.source<TestBeans.Choice>("first"), root.source<TestBeans.Choice>("first"))
        // Micronaut matches type and qualifier together: a different type is absent.
        assertNull(root.sourceOrNull<Absent>("first"))
      }
    }
  }

  @Test
  fun `explicit ancestor and child bindings win during construction and Tile execution`() = runBlocking<Unit> {
    ApplicationContext.run().use { context ->
      canvas {
        dependencyResolver(MicronautCanvasResolver(context))
        instance<GreetingService>(GreetingService { "ancestor" })
      }.use { ancestor ->
        ancestor.withLayer { }.use { parent ->
          parent.withLayer {
            provide<String> { source<GreetingService>().greet() }
          }.use { child ->
            assertEquals("ancestor", child.source<String>())
            assertEquals("ancestor", child.withMosaic { compose(greetingTile) })
          }
          parent.withLayer {
            provide<String> { source<GreetingService>().greet() }
            instance<GreetingService>(GreetingService { "child" })
          }.use { child ->
            assertEquals("child", child.source<String>())
            assertEquals("child", child.withMosaic { compose(greetingTile) })
            assertEquals("other", child.source<TestBeans.Preferred>("other").value())
          }
        }
      }
    }
  }

  @Test
  fun `constructor lookup falls back to the framework`() = runBlocking<Unit> {
    ApplicationContext.run().use { context ->
      canvas {
        dependencyResolver(MicronautCanvasResolver(context))
        provide<String> { source<GreetingService>().greet() }
      }.use { assertEquals("micronaut", it.source<String>()) }
    }
  }

  @Test
  fun `only absence is nullable while ambiguity and creation failures propagate`() = runBlocking<Unit> {
    ApplicationContext.run().use { context ->
      canvas { dependencyResolver(MicronautCanvasResolver(context)) }.use { root ->
        assertNull(root.sourceOrNull<Absent>())
        assertNull(root.sourceOrNull<TestBeans.Choice>("missing"))
        assertEquals(CanvasKey(Absent::class), assertFailsWith<MosaicMissingKeyException> {
          root.source<Absent>()
        }.key)
        assertFailsWith<NonUniqueBeanException> { root.sourceOrNull<TestBeans.Choice>() }
        assertFailsWith<BeanInstantiationException> { root.sourceOrNull<TestBeans.Broken>() }
      }
      assertFailsWith<NonUniqueBeanException> {
        canvas {
          dependencyResolver(MicronautCanvasResolver(context))
          provide<String> { source<TestBeans.Choice>(); "unreachable" }
        }
      }
    }
  }

  @Test
  fun `a miss does not prevent later framework resolution`() = runBlocking<Unit> {
    ApplicationContext.run().use { context ->
      canvas { dependencyResolver(MicronautCanvasResolver(context)) }.use { root ->
        assertNull(root.sourceOrNull<LateService>())
        val service = LateService { "late" }
        context.registerSingleton(LateService::class.java, service)
        assertSame(service, root.source<LateService>())
      }
    }
  }

  @Test
  fun `resolver values and instance aliases remain borrowed on close and rollback`() = runBlocking<Unit> {
    val shared: TestBeans.Resource
    val prototype: TestBeans.Resource
    ApplicationContext.run().use { context ->
      canvas { dependencyResolver(MicronautCanvasResolver(context)) }.use { root ->
        shared = root.source<TestBeans.Resource>("shared")
        assertSame(shared, root.source<TestBeans.Resource>("shared"))
        prototype = root.source<TestBeans.Resource>("prototype")
        assertNotSame(prototype, root.source<TestBeans.Resource>("prototype"))
        root.withLayer { instance<AutoCloseable>(shared) }.use { alias ->
          assertSame(shared, alias.source<AutoCloseable>())
        }
        val local = TestBeans.Resource()
        assertFailsWith<IllegalStateException> {
          root.withLayer {
            instance<AutoCloseable>(shared)
            provide<TestBeans.Resource>("local") { local }
            provide<String> {
              source<TestBeans.Resource>("local")
              source<TestBeans.Resource>("shared")
              error("rollback")
            }
          }
        }
        assertEquals(1, local.closes)
      }
      assertEquals(0, shared.closes)
      assertEquals(0, prototype.closes)
    }
    assertEquals(1, shared.closes)
    // Prototype cleanup is the application's responsibility, not Canvas's.
    assertEquals(0, prototype.closes)
    prototype.close()
  }

  @Test
  fun `request proxy invocation and target identity follow framework context`() = runBlocking<Unit> {
    ApplicationContext.run().use { context ->
      canvas { dependencyResolver(MicronautCanvasResolver(context)) }.use { root ->
        val proxy = assertNotNull(root.sourceOrNull<TestBeans.Resource>("request"))
        assertFailsWith<IllegalStateException> { proxy.identity() }
        val first = ServerRequestContext.with(HttpRequest.GET<Any>("/first"), Supplier {
          val identity = proxy.identity()
          assertEquals(identity, root.source<TestBeans.Resource>("request").identity())
          identity
        })
        val second = ServerRequestContext.with(HttpRequest.GET<Any>("/second"), Supplier { proxy.identity() })
        assertNotEquals(first, second)
        assertFailsWith<IllegalStateException> { proxy.identity() }
        // The resolver does not carry request context across a coroutine dispatcher switch.
        ServerRequestContext.with(HttpRequest.GET<Any>("/switch"), Runnable {
          runBlocking {
            withContext(Dispatchers.Default) {
              assertFailsWith<IllegalStateException> { proxy.identity() }
            }
          }
        })
      }
    }
  }
}
