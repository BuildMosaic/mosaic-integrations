package org.buildmosaic.integrations.spring

import org.buildmosaic.core.injection.CanvasDependencyResolver
import org.buildmosaic.core.injection.CanvasKey
import org.springframework.beans.factory.BeanFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.NoUniqueBeanDefinitionException

/**
 * Borrows services from [beans] for keys absent from explicit Canvas bindings.
 * A null qualifier uses Spring type selection, including primary candidates. A string is an exact
 * bean name or alias, not arbitrary `@Qualifier` metadata. Only absence returns null; Spring errors
 * propagate. Every call delegates to Spring; scopes and resource lifetimes remain application-owned.
 * Neither this resolver nor Canvas closes the bean factory or values obtained by resolver lookup.
 */
class SpringCanvasResolver(private val beans: BeanFactory) : CanvasDependencyResolver {
  override fun <T : Any> resolve(key: CanvasKey<T>): T? {
    val name = key.qualifier
    if (name == null) {
      // getIfAvailable suppresses inactive-scope failures, which Canvas must propagate.
      return try {
        beans.getBeanProvider(key.type.javaObjectType).getObject()
      } catch (failure: NoSuchBeanDefinitionException) {
        if (failure is NoUniqueBeanDefinitionException) throw failure
        null
      }
    }
    if (!beans.containsBean(name)) return null
    return beans.getBean(name, key.type.javaObjectType)
  }
}
