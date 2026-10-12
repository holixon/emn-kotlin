package io.holixon.emn.generation

import _ktx.ResourceKtx.resourceUrl
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.TestFixtures.AvroKotlinFixtures.AVRO_PARSER
import io.holixon.emn.generation.TestFixtures.logger
import io.holixon.emn.generation.TestFixtures.writeTo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ExperimentalKotlinPoetApi::class)
class EmnAxon5AvroBasedGeneratorTest {

  private val properties = DefaultEmnAxon5GeneratorProperties(
    emnName = "faculty",
    rootPackageName = "io.holixon.emn.example.faculty",
    instanceCreator = "instancio"
  )

  private val generator = EmnAxon5AvroBasedGenerator.create(
    TestFixtures.SPI_REGISTRY,
    properties,
  )

  private val emnParser = EmnDocumentParser()
  private val avprParser = AVRO_PARSER

  private val targetDir = TestFixtures.createGeneratedSourcesDir()

  @Test
  fun `generate dummy`() {
    val definitions = emnParser.parseDefinitions(resourceUrl("faculty/faculty.emn"))
    val declaration = avprParser.parseProtocol(resourceUrl("faculty/faculty.avpr"))

    val files = generator.generate(definitions, declaration)

    files.filter { it -> it. className.simpleName.startsWith("CreateCourse") }
      .forEach { logger.info { it.code } }

  }

  @Test
  fun `command handlers and specification tests can be generated independently`() {
    val definitions = emnParser.parseDefinitions(resourceUrl("faculty/faculty.emn"))
    val declaration = avprParser.parseProtocol(resourceUrl("faculty/faculty.avpr"))

    val testsOnly = EmnAxon5AvroBasedGenerator.create(
      TestFixtures.SPI_REGISTRY,
      properties.copy(generateCommandSlices = false, generateCommandSliceTests = true)
    ).generate(definitions, declaration)
    assertThat(testsOnly.map { it.className.simpleName })
      .contains("CreateCourseCommandHandlerTest")
      .doesNotContain("CreateCourseCommandHandler", "CreateCourseState")

    val handlersOnly = EmnAxon5AvroBasedGenerator.create(
      TestFixtures.SPI_REGISTRY,
      properties.copy(generateCommandSlices = true, generateCommandSliceTests = false)
    ).generate(definitions, declaration)
    assertThat(handlersOnly.map { it.className.simpleName })
      .contains("CreateCourseCommandHandler", "CreateCourseState")
      .doesNotContain("CreateCourseCommandHandlerTest")
  }

  @Test
  fun `generated handlers support missing event sourced state`() {
    val definitions = emnParser.parseDefinitions(resourceUrl("faculty/faculty.emn"))
    val declaration = avprParser.parseProtocol(resourceUrl("faculty/faculty.avpr"))

    val handler = generator.generate(definitions, declaration)
      .single { it.className.simpleName == "CreateCourseCommandHandler" }
      .code

    assertThat(handler)
      .contains("@InjectEntity state: CreateCourseState?")
      .contains("(state ?: CreateCourseState()).decide(command)")
      .doesNotContain("@Nullable")
  }
}
