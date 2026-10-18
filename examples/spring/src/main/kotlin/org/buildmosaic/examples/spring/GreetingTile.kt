package org.buildmosaic.examples.spring

import org.buildmosaic.core.singleTile
import org.buildmosaic.core.source

fun interface GreetingService {
  fun greet(): String
}

// Application logic has no Spring dependency or per-service Canvas registration.
val greetingTile = singleTile { source<GreetingService>().greet() }
