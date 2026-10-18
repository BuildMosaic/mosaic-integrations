package org.buildmosaic.integrations.micronaut

import io.micronaut.context.BeanLocator
import io.micronaut.inject.qualifiers.Qualifiers
import org.buildmosaic.core.injection.CanvasDependencyResolver
import org.buildmosaic.core.injection.CanvasKey

/**
 * Borrows services from [beans] for keys absent from explicit Canvas bindings.
 * A null qualifier uses Micronaut type selection, including primary candidates. Strings use
 * [Qualifiers.byName] with Micronaut's named-qualifier matching rules. Only absence returns null;
 * framework errors propagate. Every call delegates to Micronaut; scopes and resource lifetimes
 * remain application-owned. Neither this resolver nor Canvas closes the locator or resolver values.
 */
class MicronautCanvasResolver(private val beans: BeanLocator) : CanvasDependencyResolver {
  override fun <T : Any> resolve(key: CanvasKey<T>): T? =
    beans.findBean(key.type.javaObjectType, key.qualifier?.let { Qualifiers.byName<T>(it) }).orElse(null)
}
