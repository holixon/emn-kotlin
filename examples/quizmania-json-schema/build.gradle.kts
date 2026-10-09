plugins {
  kotlin("jvm")
  id("io.holixon.emn.kotlin-schema") version "0.0.1-SNAPSHOT"
  id("io.holixon.emn.schema-export") version "0.0.1-SNAPSHOT"
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

emnSchemaKotlin {
  emnFiles.from(file("../../_emn/quizmania/quizmania-blueprint.emn"))
  rootPackageName.set("io.holixon.emn.example.quizmania")
  annotationEnhancers.set(listOf("axon5", "jackson"))
}

emnSchemaExport {
  emnFiles.from(file("../../_emn/quizmania/quizmania-blueprint.emn"))
  sourceRoot.set(file("../..").canonicalPath)
}

tasks.named("check") {
  dependsOn("exportEmnSchemas")
}

kotlin {
  jvmToolchain(21)
}

tasks.test {
  useJUnitPlatform()
}
