package io.holixon.emn.generation

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnAxon5GenerationSpiRegistry
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.Definitions
import io.toolisticon.kotlin.avro.declaration.ProtocolDeclaration
import io.toolisticon.kotlin.avro.generator.AvroKotlinGeneratorProperties
import io.toolisticon.kotlin.avro.generator.spi.AvroCodeGenerationSpiRegistry
import io.toolisticon.kotlin.avro.generator.spi.ProtocolDeclarationContext
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.generateFiles
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.spi.load
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.registry.KotlinCodeGenerationSpiList

@OptIn(ExperimentalKotlinPoetApi::class)
open class EmnAxon5AvroBasedGenerator(
  val registry: EmnAxon5GenerationSpiRegistry,
  val properties: EmnAxon5GeneratorProperties,
  val avroRegistry: AvroCodeGenerationSpiRegistry,
) {
  companion object {
    val CONTEXT_UPPER_BOUND = EmnGenerationContext::class

    /**
     * Loads the default [KotlinCodeGenerationSpiList] and creates an [EmnAxon5AvroBasedGenerator] instance.
     */
    fun create(
      spiList: KotlinCodeGenerationSpiList = load(),
      properties: EmnAxon5GeneratorProperties,
    ): EmnAxon5AvroBasedGenerator {
      return EmnAxon5AvroBasedGenerator(
        registry = EmnAxon5GenerationSpiRegistry(spiList),
        properties = properties,
        avroRegistry = AvroCodeGenerationSpiRegistry(spiList)
       )
    }
  }

  internal fun contextEmnContextFactory(declaration: ProtocolDeclaration, definitions: Definitions): EmnGenerationContext {
    val emnCtx = EmnGenerationContext(
      definitions = definitions,
      registry = registry,
      properties = properties
    )

    val avprContext = ProtocolDeclarationContext.of(
      declaration = declaration,
      registry = avroRegistry,
      properties = properties
    )

    avprContext.tags[EmnGenerationContext::class] = emnCtx
    emnCtx.tags[ProtocolDeclarationContext::class] = avprContext

    return emnCtx
  }

  fun generate(definitions: Definitions, declaration: ProtocolDeclaration): KotlinFileSpecList {
    val context = contextEmnContextFactory(declaration, definitions)

    // validate references between protocol declaration and EMN definitions

    val avroGeneratedFiles = generateFiles(input = declaration, context = context.protocolDeclarationContext)

    val emnGeneratedFiles = generateFiles(input = definitions, context = context)

    return avroGeneratedFiles + emnGeneratedFiles
  }
}
