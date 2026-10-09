package io.holixon.emn.generation

import _ktx.ResourceKtx.resourceUrl
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.TestFixtures.AvroKotlinFixtures.AVRO_PARSER
import io.holixon.emn.generation.TestFixtures.logger
import io.holixon.emn.generation.TestFixtures.writeTo
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
}
