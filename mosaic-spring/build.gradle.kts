import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  `java-library`
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  api(libs.mosaic.core)
  api(libs.spring.beans)
  testImplementation(libs.spring.context)
  testImplementation(libs.spring.web)
  testImplementation(libs.spring.test)
  testImplementation(libs.jakarta.servlet)
  testImplementation(libs.coroutines.core)
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
