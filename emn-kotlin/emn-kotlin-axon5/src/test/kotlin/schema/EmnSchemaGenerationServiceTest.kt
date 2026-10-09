package io.holixon.emn.generation.schema

import _ktx.ResourceKtx.resourceUrl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files

class EmnSchemaGenerationServiceTest {

  private val service = EmnSchemaGenerationService()

  @Test
  fun `generates sources from emn files with configured enhancers`() {
    val outputDirectory = Files.createTempDirectory("emn-schema-service-test-").toFile()

    val result = service.generate(EmnSchemaGenerationRequest(
      emnFiles = listOf(File(resourceUrl("json-schema/faculty.emn").toURI())),
      outputDirectory = outputDirectory,
      rootPackageName = "io.holixon.emn.example.faculty",
      annotationEnhancers = listOf("axon5")
    ))

    assertThat(result.generatedFiles.map { it.name })
      .containsExactly(
        "CourseType.kt",
        "CreateCourse.kt",
        "CourseCreated.kt",
        "CourseOverviewItem.kt",
        "CourseOverview.kt"
      )
    assertThat(result.generatedFiles.first { it.name == "CreateCourse.kt" }.readText())
      .contains("package io.holixon.emn.example.faculty.command", "@TargetEntityId\nval courseId: UUID")
  }

  @Test
  fun `rejects unknown annotation enhancers`() {
    assertThatThrownBy {
      service.generate(EmnSchemaGenerationRequest(
        emnFiles = listOf(File(resourceUrl("json-schema/faculty.emn").toURI())),
        outputDirectory = Files.createTempDirectory("emn-schema-service-test-").toFile(),
        rootPackageName = "io.holixon.emn.example.faculty",
        annotationEnhancers = listOf("unknown")
      ))
    }.hasMessageContaining("Unknown JSON Schema Kotlin annotation enhancer 'unknown'")
  }

  @Test
  fun `supports the jackson annotation enhancer`() {
    val outputDirectory = Files.createTempDirectory("emn-schema-service-test-").toFile()

    val result = service.generate(EmnSchemaGenerationRequest(
      emnFiles = listOf(File(resourceUrl("quizmania/quizmania-blueprint.emn").toURI())),
      outputDirectory = outputDirectory,
      rootPackageName = "io.holixon.emn.example.quizmania",
      annotationEnhancers = listOf("jackson")
    ))

    assertThat(result.generatedFiles.first { it.name == "Question.kt" }.readText())
      .contains("package io.holixon.emn.example.quizmania.common", "@JsonTypeInfo(", "@JsonSubTypes(")
  }
}
