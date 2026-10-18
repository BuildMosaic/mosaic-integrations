plugins {
  base
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.kapt) apply false
}

group = "org.buildmosaic"
