package io.holixon.emn.generation

import _ktx.ResourceKtx.resourceUrl
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.TestFixtures.AvroKotlinFixtures.AVRO_PARSER
import org.junit.jupiter.api.Test
import java.time.Instant

@OptIn(ExperimentalKotlinPoetApi::class)
class EmnAxon5AvroBasedGeneratorTest {

  private val properties = DefaultEmnAxon5GeneratorProperties(
    rootPackageName = "io.holixon.emn.example.faculty",
  )

  private val generator = EmnAxon5AvroBasedGenerator.create(
    TestFixtures.SPI_REGISTRY,
    properties,
  )

  private val emnParser = EmnDocumentParser()
  private val avprParser = AVRO_PARSER

  @Test
  fun `generate dummy`() {
    val definitions = emnParser.parseDefinitions(resourceUrl("faculty.emn"))
    val declaration = avprParser.parseProtocol(resourceUrl("faculty.avpr"))

    generator.generate(definitions, declaration).forEach {
      println(it.code)
    }

  }
}
