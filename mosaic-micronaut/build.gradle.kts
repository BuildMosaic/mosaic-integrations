import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  `java-library`
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.kotlin.kapt)
}

dependencies {
  api(libs.mosaic.core)
  api(libs.micronaut.context)
  testImplementation(libs.micronaut.http.server)
  kapt(libs.micronaut.inject.java)
  implementation(libs.coroutines.core)
  testImplementation(libs.kotlin.test.junit5)
  testImplementation(platform(libs.junit.bom))
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.platform.launcher)
}

kotlin {
  jvmToolchain(21)
  compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}
java {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
}
tasks.test { useJUnitPlatform() }
