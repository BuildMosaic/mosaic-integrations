package org.buildmosaic.integrations.micronaut

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.canvas
import org.buildmosaic.core.injection.source
import org.buildmosaic.core.injection.withMosaic
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

// Ordinary application service: constructor injection finds the library's generated bean definition.
@Singleton
class CanvasConsumer(val canvas: Canvas) {
  suspend fun greet(): String = canvas.withMosaic { compose(greetingTile) }
}

@Factory
@Requires(property = "mosaic.test.custom-canvas", value = "true")
class CustomCanvasFactory {
  @Singleton
  @Bean(preDestroy = "close")
  fun applicationCanvas(): Canvas = runBlocking {
    canvas {
      instance<GreetingService>(GreetingService { "custom" })
      provide<MicronautCanvasFactoryTest.OwnedResource> { MicronautCanvasFactoryTest.OwnedResource() }
    }
  }
}

class MicronautCanvasFactoryTest {
  class OwnedResource : AutoCloseable {
    var closes = 0
    override fun close() { closes++ }
  }

  @Test
  fun `Micronaut discovers one Canvas and injects it into application services`() = runBlocking<Unit> {
    val borrowed: TestBeans.Resource
    ApplicationContext.run().use { context ->
      val root = context.getBean(Canvas::class.java)
      val consumer = context.getBean(CanvasConsumer::class.java)
      assertEquals(1, context.getBeansOfType(Canvas::class.java).size)
      assertSame(root, consumer.canvas)
      assertEquals("micronaut", consumer.greet())
      borrowed = root.source<TestBeans.Resource>("shared")
      root.close()
      assertEquals(0, borrowed.closes)
    }
    assertEquals(1, borrowed.closes)
  }

  @Test
  fun `application Canvas wins and remains managed by Micronaut`() = runBlocking<Unit> {
    val owned: OwnedResource
    ApplicationContext.run(mapOf("mosaic.test.custom-canvas" to "true")).use { context ->
      val root = context.getBean(Canvas::class.java)
      assertEquals(1, context.getBeansOfType(Canvas::class.java).size)
      assertSame(root, context.getBean(CanvasConsumer::class.java).canvas)
      assertEquals("custom", context.getBean(CanvasConsumer::class.java).greet())
      owned = root.source<OwnedResource>()
      assertEquals(0, owned.closes)
    }
    assertEquals(1, owned.closes)
  }
}
