package org.buildmosaic.integrations.spring.autoconfigure

import kotlinx.coroutines.runBlocking
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.canvas
import org.buildmosaic.integrations.spring.SpringCanvasResolver
import org.springframework.beans.factory.BeanFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

/** Supplies a framework-owned Canvas unless the application already defines one. */
@AutoConfiguration
class MosaicCanvasAutoConfiguration {
  @Bean(destroyMethod = "close")
  @ConditionalOnMissingBean(Canvas::class)
  fun mosaicCanvas(beans: BeanFactory): Canvas = runBlocking {
    canvas { dependencyResolver(SpringCanvasResolver(beans)) }
  }
}
