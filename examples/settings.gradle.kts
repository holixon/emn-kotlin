pluginManagement {
  repositories {
    mavenLocal()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}

rootProject.name = "emn-examples"

include("university-json-schema")
include("quizmania-json-schema")
