package org.buildmosaic.integrations.spring

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
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.beans.factory.NoUniqueBeanDefinitionException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.BeanDefinitionCustomizer
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.beans.factory.support.ScopeNotActiveException
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.GenericApplicationContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.context.support.WebApplicationContextUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class SpringCanvasResolverTest {
  interface Absent
  interface Choice
  interface Broken

  class Resource : AutoCloseable {
    var closes = 0
    override fun close() { closes++ }
  }

  @Configuration(proxyBeanMethods = false)
  class Services {
    @Bean("greeting", "alias")
    @Qualifier("metadataOnly")
    fun greeting(): GreetingService = GreetingService { "spring" }
  }

  @Test
  fun `Tiles resolve services and concurrent lookups retain singleton identity`() = runBlocking<Unit> {
    AnnotationConfigApplicationContext(Services::class.java).use { context ->
      canvas { dependencyResolver(SpringCanvasResolver(context)) }.use { root ->
        assertEquals("spring", root.withMosaic { compose(greetingTile) })
        val bean = context.getBean(GreetingService::class.java)
        (1..20).map {
          async(Dispatchers.Default) { assertSame(bean, root.source<GreetingService>()) }
        }.awaitAll()
        assertSame(bean, root.source<GreetingService>("greeting"))
        assertSame(bean, root.source<GreetingService>("alias"))
        assertNull(root.sourceOrNull<GreetingService>("metadataOnly"))
        assertNull(root.sourceOrNull<GreetingService>(" greeting "))
        assertNull(root.sourceOrNull<GreetingService>(""))
      }
    }
  }

  @Test
  fun `framework primary selection and boxed Kotlin types are preserved`() = runBlocking<Unit> {
    GenericApplicationContext().use { context ->
      context.registerBean("primary", GreetingService::class.java, Supplier { GreetingService { "primary" } },
        BeanDefinitionCustomizer { it.isPrimary = true })
      context.registerBean("other", GreetingService::class.java, Supplier { GreetingService { "other" } })
      context.beanFactory.registerSingleton("number", 42)
      context.refresh()
      canvas { dependencyResolver(SpringCanvasResolver(context)) }.use { root ->
        assertEquals("primary", root.source<GreetingService>().greet())
        assertEquals("other", root.source<GreetingService>("other").greet())
        assertEquals(42, root.source<Int>())
        assertEquals(42, root.source<Int>("number"))
      }
    }
  }

  @Test
  fun `explicit ancestor and child bindings win during construction and Tile execution`() = runBlocking<Unit> {
    AnnotationConfigApplicationContext(Services::class.java).use { context ->
      canvas {
        dependencyResolver(SpringCanvasResolver(context))
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
            // Qualifiers remain exact Canvas keys; the unqualified override does not shadow this key.
            assertEquals("spring", child.source<GreetingService>("greeting").greet())
          }
        }
      }
    }
  }

  @Test
  fun `constructor lookup falls back to the framework`() = runBlocking<Unit> {
    AnnotationConfigApplicationContext(Services::class.java).use { context ->
      canvas {
        dependencyResolver(SpringCanvasResolver(context))
        provide<String> { source<GreetingService>().greet() }
      }.use { assertEquals("spring", it.source<String>()) }
    }
  }

  @Test
  fun `only absence is nullable while ambiguity creation type and scope failures propagate`() = runBlocking<Unit> {
    GenericApplicationContext().use { context ->
      context.registerBean("first", Choice::class.java, Supplier { object : Choice {} })
      context.registerBean("second", Choice::class.java, Supplier { object : Choice {} })
      context.registerBean("broken", Broken::class.java, Supplier { error("creation failed") },
        BeanDefinitionCustomizer { it.isLazyInit = true })
      context.registerBeanDefinition("unregisteredScope", RootBeanDefinition(Resource::class.java).apply {
        scope = "unregistered"
      })
      context.refresh()
      canvas { dependencyResolver(SpringCanvasResolver(context)) }.use { root ->
        assertNull(root.sourceOrNull<Absent>())
        assertNull(root.sourceOrNull<Choice>("missing"))
        assertEquals(CanvasKey(Absent::class), assertFailsWith<MosaicMissingKeyException> {
          root.source<Absent>()
        }.key)
        assertFailsWith<NoUniqueBeanDefinitionException> { root.sourceOrNull<Choice>() }
        assertFailsWith<BeanCreationException> { root.sourceOrNull<Broken>() }
        assertFailsWith<BeanCreationException> { root.sourceOrNull<Broken>("broken") }
        assertFailsWith<BeanNotOfRequiredTypeException> { root.sourceOrNull<Absent>("first") }
        assertFailsWith<IllegalStateException> { root.sourceOrNull<Resource>("unregisteredScope") }
      }
      assertFailsWith<NoUniqueBeanDefinitionException> {
        canvas {
          dependencyResolver(SpringCanvasResolver(context))
          provide<String> { source<Choice>(); "unreachable" }
        }
      }
    }
  }

  @Test
  fun `a miss does not prevent later framework resolution`() = runBlocking<Unit> {
    GenericApplicationContext().use { context ->
      context.refresh()
      canvas { dependencyResolver(SpringCanvasResolver(context)) }.use { root ->
        assertNull(root.sourceOrNull<GreetingService>())
        assertNull(root.sourceOrNull<GreetingService>("late"))
        val service = GreetingService { "late" }
        context.beanFactory.registerSingleton("late", service)
        assertSame(service, root.source<GreetingService>())
        assertSame(service, root.source<GreetingService>("late"))
      }
    }
  }

  @Test
  fun `resolver values and instance aliases remain borrowed on close and rollback`() = runBlocking<Unit> {
    val shared = Resource()
    val prototype: Resource
    GenericApplicationContext().use { context ->
      context.registerBean("shared", Resource::class.java, Supplier { shared },
        BeanDefinitionCustomizer { it.destroyMethodName = "close" })
      context.registerBean("prototype", Resource::class.java, Supplier { Resource() },
        BeanDefinitionCustomizer { it.scope = "prototype"; it.destroyMethodName = "close" })
      context.refresh()
      canvas { dependencyResolver(SpringCanvasResolver(context)) }.use { root ->
        assertSame(shared, root.source<Resource>("shared"))
        prototype = root.source<Resource>("prototype")
        assertNotSame(prototype, root.source<Resource>("prototype"))
        root.withLayer { instance<AutoCloseable>(shared) }.use { alias ->
          assertSame(shared, alias.source<AutoCloseable>())
        }
        val local = Resource()
        assertFailsWith<IllegalStateException> {
          root.withLayer {
            instance<AutoCloseable>(shared)
            provide<Resource>("local") { local }
            provide<String> {
              source<Resource>("local")
              source<Resource>("shared")
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
    // Spring does not destroy prototypes. Applications must arrange their cleanup.
    assertEquals(0, prototype.closes)
    prototype.close()
  }

  @Test
  fun `request scope requires Spring request attributes and cleanup stays with Spring`() = runBlocking<Unit> {
    GenericApplicationContext().use { context ->
      WebApplicationContextUtils.registerWebApplicationScopes(context.beanFactory)
      context.registerBeanDefinition("request", RootBeanDefinition(Resource::class.java).apply {
        scope = "request"
        destroyMethodName = "close"
      })
      context.refresh()
      canvas { dependencyResolver(SpringCanvasResolver(context)) }.use { root ->
        assertFailsWith<ScopeNotActiveException> { root.sourceOrNull<Resource>("request") }
        val firstRequest = ServletRequestAttributes(MockHttpServletRequest())
        RequestContextHolder.setRequestAttributes(firstRequest)
        val first: Resource
        try {
          first = root.source<Resource>("request")
          assertSame(first, root.source<Resource>("request"))
          withContext(Dispatchers.Default) {
            assertFailsWith<ScopeNotActiveException> { root.sourceOrNull<Resource>("request") }
          }
          assertEquals(0, first.closes)
        } finally {
          firstRequest.requestCompleted()
          RequestContextHolder.resetRequestAttributes()
        }
        assertEquals(1, first.closes)
        val secondRequest = ServletRequestAttributes(MockHttpServletRequest())
        RequestContextHolder.setRequestAttributes(secondRequest)
        try {
          val second = root.source<Resource>("request")
          assertNotSame(first, second)
          root.close()
          assertEquals(0, second.closes)
          secondRequest.requestCompleted()
          assertEquals(1, second.closes)
        } finally {
          RequestContextHolder.resetRequestAttributes()
        }
      }
    }
  }
}
