package io.holixon.emn.generation.specification

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EmnSpecificationTestGenerationServiceTest {

  @TempDir
  lateinit var outputDirectory: File

  @Test
  fun `generates annotated Axon fixture tests from JSON Schema values`() {
    val emnFile = File(requireNotNull(javaClass.getResource("/specification/json-schema-specification.emn")).toURI())

    val result = EmnSpecificationTestGenerationService().generate(
      EmnSpecificationTestGenerationRequest(
        emnFiles = listOf(emnFile),
        outputDirectory = outputDirectory,
        rootPackageName = "io.holixon.example",
        fixtureProviderClassName = "io.holixon.example.QuizmaniaFixtureProvider"
      )
    )

    assertThat(result.generatedFiles).hasSize(1)
    assertThat(result.generatedFiles.single().readText()).contains(
      "@EmnSpecification(id = \"create-course-success\")",
      "@ExtendWith(AxonFrameworkExtension::class)",
      "@ProvidedAxonTestFixture(QuizmaniaFixtureProvider::class)",
      "fun `create-course-success`(fixture: AxonTestFixture)",
      "CreateCourse(",
      "courseId = UUID.nameUUIDFromBytes(\"course-4711\".toByteArray())",
      "capacity = 25",
      "CourseCreated(",
      "scores = mapOf(UUID.fromString(\"123e4567-e89b-12d3-a456-426614174000\") to 10)",
      "@EmnSpecification(id = \"create-course-duplicate\")",
      "fun `create-course-duplicate`(fixture: AxonTestFixture)",
      ".exceptionSatisfies { exception ->",
      "verifyEmnError(\"DuplicateCourse\"",
      "\"type\" to \"urn:university:course:duplicate\"",
      "\"category\" to \"BUSINESS_INVALID_COMMAND\"",
      "\"courseId\" to UUID.nameUUIDFromBytes(\"course-4711\".toByteArray())",
      "exception as? CommandExecutionException",
      "commandException.getDetails(Map::class.java)",
      "assertEmnErrorDetails(expectedDetails, actualDetails, errorTypeId)"
    )
    assertThat(result.generatedFiles.single().readText()).doesNotContain("constructor(fixture")
  }

  @Test
  fun `delegates error assertions to a configured verifier`() {
    val emnFile = File(requireNotNull(javaClass.getResource("/specification/json-schema-specification.emn")).toURI())

    val result = EmnSpecificationTestGenerationService().generate(
      EmnSpecificationTestGenerationRequest(
        emnFiles = listOf(emnFile),
        outputDirectory = outputDirectory,
        rootPackageName = "io.holixon.example",
        fixtureProviderClassName = "io.holixon.example.QuizmaniaFixtureProvider",
        errorVerifierClassName = "io.holixon.example.QuizmaniaErrorVerifier"
      )
    )

    assertThat(result.generatedFiles.single().readText()).contains(
      "QuizmaniaErrorVerifier.verify(\"DuplicateCourse\"",
      "\"type\" to \"urn:university:course:duplicate\"",
      "exception)",
    ).doesNotContain(
      "fun verifyEmnError(",
      "CommandExecutionException"
    )
  }
}
