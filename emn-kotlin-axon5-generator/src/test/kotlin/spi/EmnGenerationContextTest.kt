package io.holixon.emn.generation.spi

import _ktx.ResourceKtx.resourceUrl
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.DefaultEmnAxon5GeneratorProperties
import io.holixon.emn.generation.TestFixtures
import io.holixon.emn.generation.TestFixtures.AvroKotlinFixtures.AVRO_PARSER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@OptIn(ExperimentalKotlinPoetApi::class)
class EmnGenerationContextTest {

  private val properties = DefaultEmnAxon5GeneratorProperties(
    emnName = "faculty",
    rootPackageName = "io.holixon.emn.example.faculty",
    instanceCreator = "instancio"
  )


  private val emnParser = EmnDocumentParser()
  private val avprParser = AVRO_PARSER


  @Test
  fun `create valid context`() {
    val definitions = emnParser.parseDefinitions(resourceUrl("faculty/faculty.emn"))
    val declaration = avprParser.parseProtocol(resourceUrl("faculty/faculty.avpr"))

    val context = EmnGenerationContext.create(
      declaration,
      definitions,
      TestFixtures.SPI_REGISTRY,
      properties,
    )

    val validation = EmnGenerationContext.validateContext(context)
    assertThat(validation.isValid).isTrue
  }
}
