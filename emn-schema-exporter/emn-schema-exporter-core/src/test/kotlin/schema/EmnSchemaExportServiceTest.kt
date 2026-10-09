package io.holixon.emn.generation.schema

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EmnSchemaExportServiceTest {

  @TempDir
  lateinit var tempDirectory: File

  private val objectMapper = ObjectMapper()

  @Test
  fun `exports embedded resource and referenced schemas with manifest metadata`() {
    val modelDirectory = File(tempDirectory, "model").apply { mkdirs() }
    val resourceDirectory = File(tempDirectory, "resources").apply { mkdirs() }
    File(resourceDirectory, "CourseCreated.json").writeText("""
      {
        "type": "object",
        "properties": {
          "courseId": { "${'$'}ref": "CourseId" }
        }
      }
    """.trimIndent())
    val emnFile = File(modelDirectory, "faculty.emn").apply {
      writeText(emn(
        """
          <emn:schemaDefinition id="CourseId" name="Course ID">
            <emn:schema schemaFormat="application/schema+json"><![CDATA[
              { "type": "string", "format": "uuid" }
            ]]></emn:schema>
          </emn:schemaDefinition>
          <emn:commandType id="CreateCourse" name="Create course">
            <emn:schema schemaFormat="application/schema+json"><![CDATA[
              {
                "type": "object",
                "properties": { "courseId": { "${'$'}ref": "CourseId" } }
              }
            ]]></emn:schema>
          </emn:commandType>
          <emn:eventType id="CourseCreated" name="Course created">
            <emn:schema schemaFormat="application/schema+json" resource="../resources/CourseCreated.json" />
          </emn:eventType>
          <emn:queryType id="FindCourse" name="Find course">
            <emn:schema schemaFormat="application/schema+json" reference="CourseId" />
          </emn:queryType>
        """.trimIndent()
      ))
    }
    val outputDirectory = File(tempDirectory, "output")

    val result = EmnSchemaExportService().export(EmnSchemaExportRequest(
      emnFiles = listOf(emnFile),
      outputDirectory = outputDirectory,
      sourceRoot = tempDirectory,
      schemaResourceDirectories = listOf(resourceDirectory)
    ))

    assertThat(result.entries.map { it.typeId })
      .containsExactly("CourseCreated", "CourseId", "CreateCourse", "FindCourse")
    assertThat(result.entries.associate { it.typeId to it.kind }).containsAllEntriesOf(mapOf(
      "CourseId" to "common",
      "CreateCourse" to "command",
      "CourseCreated" to "event",
      "FindCourse" to "query"
    ))
    assertThat(result.entries.single { it.typeId == "CourseCreated" }.source.origin).isEqualTo("resource")
    assertThat(result.entries.single { it.typeId == "FindCourse" }.source.origin).isEqualTo("reference")
    assertThat(result.entries.single { it.typeId == "CreateCourse" }.references).containsExactly("CourseId")

    val commandSchema = objectMapper.readTree(File(outputDirectory, "schemas/CreateCourse.schema.json"))
    assertThat(commandSchema.at("/properties/courseId/${'$'}ref").asText())
      .isEqualTo("./CourseId.schema.json")
    val referencedSchema = objectMapper.readTree(File(outputDirectory, "schemas/FindCourse.schema.json"))
    assertThat(referencedSchema.path("${'$'}ref").asText()).isEqualTo("./CourseId.schema.json")
    assertThat(referencedSchema.path("${'$'}id").asText()).isEqualTo("FindCourse")

    val manifest = objectMapper.readTree(result.manifestFile)
    assertThat(manifest.path("format").asText()).isEqualTo(EmnSchemaExportService.MANIFEST_FORMAT)
    assertThat(manifest.path("version").asInt()).isEqualTo(1)
    assertThat(manifest.path("schemas").size()).isEqualTo(4)
    assertThat(manifest.at("/schemas/0/source/emnFile").asText()).isEqualTo("model/faculty.emn")
  }

  @Test
  fun `produces deterministic schema and manifest output`() {
    val emnFile = File(tempDirectory, "types.emn").apply {
      writeText(emn(
        """
          <emn:schemaDefinition id="Identifier" name="Identifier">
            <emn:schema schemaFormat="application/schema+json"><![CDATA[
              { "type": "string", "format": "uuid" }
            ]]></emn:schema>
          </emn:schemaDefinition>
        """.trimIndent()
      ))
    }
    val first = File(tempDirectory, "first")
    val second = File(tempDirectory, "second")
    val service = EmnSchemaExportService()

    service.export(EmnSchemaExportRequest(listOf(emnFile), first, tempDirectory))
    service.export(EmnSchemaExportRequest(listOf(emnFile), second, tempDirectory))

    assertThat(File(first, "manifest.json").readText()).isEqualTo(File(second, "manifest.json").readText())
    assertThat(File(first, "schemas/Identifier.schema.json").readText())
      .isEqualTo(File(second, "schemas/Identifier.schema.json").readText())
  }

  @Test
  fun `rejects network schema resources`() {
    val emnFile = File(tempDirectory, "remote.emn").apply {
      writeText(emn(
        """
          <emn:schemaDefinition id="Remote" name="Remote">
            <emn:schema schemaFormat="application/schema+json" resource="https://example.org/schema.json" />
          </emn:schemaDefinition>
        """.trimIndent()
      ))
    }

    assertThatIllegalArgumentException().isThrownBy {
      EmnSchemaExportService().export(EmnSchemaExportRequest(
        emnFiles = listOf(emnFile),
        outputDirectory = File(tempDirectory, "output")
      ))
    }.withMessageContaining("Only local filesystem")
  }

  @Test
  fun `rejects unresolved schema references`() {
    val emnFile = File(tempDirectory, "unresolved.emn").apply {
      writeText(emn(
        """
          <emn:commandType id="CreateCourse" name="Create course">
            <emn:schema schemaFormat="application/schema+json"><![CDATA[
              { "type": "object", "properties": { "id": { "${'$'}ref": "MissingId" } } }
            ]]></emn:schema>
          </emn:commandType>
        """.trimIndent()
      ))
    }

    assertThatIllegalArgumentException().isThrownBy {
      EmnSchemaExportService().export(EmnSchemaExportRequest(
        emnFiles = listOf(emnFile),
        outputDirectory = File(tempDirectory, "output")
      ))
    }.withMessageContaining("MissingId")
  }

  private fun emn(types: String): String = """
    <emn:definitions xmlns:emn="https://holixon.io/spec/EMN/20241231/MODEL"
                     id="test" name="Test" targetNamespace="https://example.org/test">
      <emn:types>
        $types
      </emn:types>
    </emn:definitions>
  """.trimIndent()
}
