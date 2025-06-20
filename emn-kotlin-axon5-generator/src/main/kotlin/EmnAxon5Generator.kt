package io.holixon.emn.generation

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnAxon5GenerationSpiRegistry
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.Definitions
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.generateFiles
import io.toolisticon.kotlin.generation.spec.KotlinFileSpec

@OptIn(ExperimentalKotlinPoetApi::class)
open class EmnAxon5Generator(
  val registry : EmnAxon5GenerationSpiRegistry,
  val properties : EmnAxon5GeneratorProperties = DefaultEmnAxon5GeneratorProperties
) {
  internal val context = EmnContextFactory {
    EmnGenerationContext(definitions = it, registry = registry)
  }

  fun generate(definitions: Definitions, context: EmnGenerationContext) = generateFiles(context, definitions)

  fun generate(definitions: Definitions, contextFactory: EmnContextFactory = context) = generateFiles(contextFactory, definitions)
}
