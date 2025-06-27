package io.holixon.emn.generation

import _ktx.ResourceKtx.resourceUrl
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.AvroKotlinFixture.DEFAULT_PROPERTIES
import io.holixon.emn.generation.AvroKotlinFixture.DEFAULT_REGISTRY
import io.holixon.emn.generation.AvroKotlinFixture.PARSER
import io.holixon.emn.generation.spi.EmnAxon5GenerationSpiRegistry
import io.holixon.emn.generation.strategy.DefinitionsToCommandHandlerComponentStrategy
import io.holixon.emn.model.EmnDocumentParser
import io.toolisticon.kotlin.avro.generator.strategy.ProtocolTypesToFileStrategy
import io.toolisticon.kotlin.generation.spi.strategy.KotlinCodeGenerationStrategyList
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalKotlinPoetApi::class)
class EmnAxon5AvroBasedGeneratorTest {

  private val registry = EmnAxon5GenerationSpiRegistry(
    strategies = KotlinCodeGenerationStrategyList(
      DefinitionsToCommandHandlerComponentStrategy(),
      ProtocolTypesToFileStrategy()
    ),
  )

  private val properties = DefaultEmnAxon5GeneratorProperties("io.holixon.emn.example.faculty")

  private val generator = EmnAxon5AvroBasedGenerator(
    registry,
    properties,
    DEFAULT_REGISTRY,
    DEFAULT_PROPERTIES
  )

  private val parser = EmnDocumentParser()

  @Test
  fun `generate dummy`() {
    val file = File("src/test/resources/faculty.emn")
    val definitions = parser.parseDefinitions(file)
    val declaration = PARSER.parseProtocol(resourceUrl("faculty.avpr"))

    generator.generate(definitions, declaration).forEach {
      println(it.code)
    }

  }
}
