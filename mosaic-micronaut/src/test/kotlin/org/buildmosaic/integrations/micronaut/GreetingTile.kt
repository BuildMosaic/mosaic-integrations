package org.buildmosaic.integrations.micronaut

import org.buildmosaic.core.singleTile
import org.buildmosaic.core.source

fun interface GreetingService {
  fun greet(): String
}

val greetingTile = singleTile { source<GreetingService>().greet() }
