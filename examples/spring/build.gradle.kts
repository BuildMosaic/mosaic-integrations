import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  application
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  implementation(project(":mosaic-spring"))
  implementation(libs.spring.context)
  implementation(libs.coroutines.core)
}

kotlin {
  jvmToolchain(21)
  compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
application { mainClass.set("org.buildmosaic.examples.spring.ApplicationKt") }
// The executable checks its result, so the normal build also exercises the wiring.
tasks.check { dependsOn(tasks.run) }
