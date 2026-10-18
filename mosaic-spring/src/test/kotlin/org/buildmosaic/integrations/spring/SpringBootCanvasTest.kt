package org.buildmosaic.integrations.spring

import kotlinx.coroutines.runBlocking
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.canvas
import org.buildmosaic.core.injection.source
import org.buildmosaic.core.injection.withMosaic
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class SpringBootCanvasTest {
  class Greeting : GreetingService, AutoCloseable {
    var closes = 0
    override fun greet(): String = "boot"
    override fun close() { closes++ }
  }

  class Consumer(val canvas: Canvas) {
    suspend fun greet(): String = canvas.withMosaic { compose(greetingTile) }
  }

  class OwnedResource : AutoCloseable {
    var closes = 0
    override fun close() { closes++ }
  }

  // Ordinary application beans: no Mosaic configuration, imports, or component scanning of the adapter.
  @Configuration(proxyBeanMethods = false)
  @EnableAutoConfiguration
  class ApplicationBeans {
    @Bean
    fun greeting(): Greeting = Greeting()

    @Bean
    fun consumer(canvas: Canvas): Consumer = Consumer(canvas)
  }

  @Configuration(proxyBeanMethods = false)
  class CustomCanvas {
    @Bean(destroyMethod = "close")
    fun applicationCanvas(): Canvas = runBlocking {
      canvas {
        instance<GreetingService>(GreetingService { "custom" })
        provide<OwnedResource> { OwnedResource() }
      }
    }
  }

  @Test
  fun `Boot discovers one Canvas and injects it into application services`() = runBlocking<Unit> {
    val greeting: Greeting
    SpringApplicationBuilder(ApplicationBeans::class.java).web(WebApplicationType.NONE).run().use { context ->
      val root = context.getBean(Canvas::class.java)
      val consumer = context.getBean(Consumer::class.java)
      greeting = context.getBean(Greeting::class.java)
      assertEquals(1, context.getBeansOfType(Canvas::class.java).size)
      assertSame(root, consumer.canvas)
      assertSame(greeting, root.source<GreetingService>())
      assertEquals("boot", consumer.greet())
      root.close()
      assertEquals(0, greeting.closes)
    }
    assertEquals(1, greeting.closes)
  }

  @Test
  fun `application Canvas wins and remains managed by Spring`() = runBlocking<Unit> {
    val owned: OwnedResource
    SpringApplicationBuilder(ApplicationBeans::class.java, CustomCanvas::class.java)
      .web(WebApplicationType.NONE).run().use { context ->
        val root = context.getBean(Canvas::class.java)
        assertEquals(1, context.getBeansOfType(Canvas::class.java).size)
        assertSame(context.getBean("applicationCanvas"), root)
        assertSame(root, context.getBean(Consumer::class.java).canvas)
        assertEquals("custom", context.getBean(Consumer::class.java).greet())
        owned = root.source<OwnedResource>()
        assertEquals(0, owned.closes)
      }
    assertEquals(1, owned.closes)
  }
}
