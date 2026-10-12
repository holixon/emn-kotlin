plugins {
  kotlin("jvm")
  id("io.holixon.emn") version "0.0.1-SNAPSHOT"
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("com.fasterxml.jackson.core:jackson-annotations:2.19.2")
  implementation("org.axonframework:axon-modelling:5.3.2")
  implementation("org.axonframework:axon-eventsourcing:5.3.2")
  testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
  testImplementation("org.assertj:assertj-core:3.27.6")
  testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

emn {
  emnFiles.from(file("../../_emn/quizmania/quizmania-blueprint.emn"))
  rootPackageName.set("io.holixon.emn.example.quizmania")

  kotlin {
    enabled.set(true)
    annotationEnhancers.set(listOf("axon5", "jackson"))
  }

  schemaExport {
    enabled.set(true)
    sourceRoot.set(file("../..").canonicalPath)
  }
}

kotlin {
  jvmToolchain(21)
}

tasks.test {
  useJUnitPlatform()
}
