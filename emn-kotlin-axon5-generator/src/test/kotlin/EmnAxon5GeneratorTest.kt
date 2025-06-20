package io.holixon.emn.generation

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnAxon5GenerationSpiRegistry
import io.holixon.emn.generation.strategy.DefinitionsToFilesStrategy
import io.holixon.emn.model.EmnDocumentParser
import io.toolisticon.kotlin.generation.spi.strategy.KotlinCodeGenerationStrategyList
import org.junit.jupiter.api.Test
import java.io.File

@OptIn(ExperimentalKotlinPoetApi::class)
class EmnAxon5GeneratorTest {

  private val registry = EmnAxon5GenerationSpiRegistry(
    strategies = KotlinCodeGenerationStrategyList(DefinitionsToFilesStrategy()),
  )

  private val generator = EmnAxon5Generator(registry)

  private val parser = EmnDocumentParser()

  @Test
  fun `generate dummy`() {
    val file = File("src/test/resources/guest-register.emn")
    val definitions = parser.parseDefinitions(file)

    val fileSpec = generator.generate(definitions).single()

    println(fileSpec.code)

  }
}
