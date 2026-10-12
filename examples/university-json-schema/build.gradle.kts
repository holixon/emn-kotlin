plugins {
  kotlin("jvm")
  id("io.holixon.emn.kotlin-schema") version "0.0.1-SNAPSHOT"
}

dependencies {
  implementation("org.axonframework:axon-modelling:5.3.2")
  implementation("org.axonframework:axon-eventsourcing:5.3.2")
  testImplementation("org.axonframework:axon-test:5.3.2")
  testRuntimeOnly("com.fasterxml.jackson.core:jackson-annotations:2.22")
  testImplementation("io.holixon.emn:emn-verify-annotations:0.0.1-SNAPSHOT")
  testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
  testImplementation("org.assertj:assertj-core:3.27.6")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.2")
}

emnSchemaKotlin {
  emnFiles.from(file("../../_emn/faculty-json-schema/faculty.emn"))
  rootPackageName.set("io.holixon.emn.example.university")
  annotationEnhancers.set(listOf("axon5"))
  specifications {
    generate = true
    fixtureProvider = "io.holixon.emn.example.university.UniversityFixtureProvider"
    errorVerifier = "io.holixon.emn.example.university.UniversityErrorVerifier"
  }
}

kotlin {
  jvmToolchain(21)
}

tasks.test {
  useJUnitPlatform()
}
