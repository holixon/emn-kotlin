plugins {
  kotlin("jvm")
  id("io.holixon.emn.kotlin-schema") version "0.0.1-SNAPSHOT"
}

dependencies {
  implementation("org.axonframework:axon-modelling:5.3.2")
  implementation("org.axonframework:axon-eventsourcing:5.3.2")
  testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
  testImplementation("org.assertj:assertj-core:3.27.6")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")
}

emnSchemaKotlin {
  emnFiles.from(file("../../_emn/json-schema/faculty.emn"))
  rootPackageName.set("io.holixon.emn.example.university")
  annotationEnhancers.set(listOf("axon5"))
}

kotlin {
  jvmToolchain(21)
}

tasks.test {
  useJUnitPlatform()
}
