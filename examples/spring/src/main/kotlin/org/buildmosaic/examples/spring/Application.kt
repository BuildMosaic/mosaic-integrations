package org.buildmosaic.examples.spring

import kotlinx.coroutines.runBlocking
import org.buildmosaic.core.injection.Canvas
import org.buildmosaic.core.injection.canvas
import org.buildmosaic.core.injection.withMosaic
import org.buildmosaic.integrations.spring.SpringCanvasResolver
import org.springframework.beans.factory.BeanFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class ApplicationConfig {
  @Bean
  fun greetingService(): GreetingService = GreetingService { "Hello from Spring" }

  @Bean(destroyMethod = "close")
  fun mosaicCanvas(beans: BeanFactory): Canvas = runBlocking {
    canvas { dependencyResolver(SpringCanvasResolver(beans)) }
  }
}

fun main() = runBlocking {
  AnnotationConfigApplicationContext(ApplicationConfig::class.java).use { context ->
    val canvas = context.getBean(Canvas::class.java)
    val greeting = canvas.withMosaic { compose(greetingTile) }
    check(greeting == "Hello from Spring")
    println(greeting)
  }
}
