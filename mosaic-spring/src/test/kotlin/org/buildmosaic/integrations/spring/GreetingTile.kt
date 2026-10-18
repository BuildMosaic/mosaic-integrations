package org.buildmosaic.integrations.spring

import org.buildmosaic.core.singleTile
import org.buildmosaic.core.source

fun interface GreetingService {
  fun greet(): String
}

val greetingTile = singleTile { source<GreetingService>().greet() }
