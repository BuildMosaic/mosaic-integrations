package org.buildmosaic.integrations.micronaut

import io.micronaut.context.BeanLocator
import io.micronaut.context.annotation.Bean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import kotlinx.coroutines.runBlocking
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.canvas

/** Supplies a framework-owned Canvas unless the application already defines one. */
@Factory
class MosaicCanvasFactory {
  @Singleton
  @Bean(preDestroy = "close")
  @Requires(missingBeans = [Canvas::class])
  fun mosaicCanvas(beans: BeanLocator): Canvas = runBlocking {
    canvas { dependencyResolver(MicronautCanvasResolver(beans)) }
  }
}
