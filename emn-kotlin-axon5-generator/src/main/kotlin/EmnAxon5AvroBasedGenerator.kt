package io.holixon.emn.generation

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnAxon5GenerationSpiRegistry
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.Definitions
import io.toolisticon.kotlin.avro.declaration.ProtocolDeclaration
import io.toolisticon.kotlin.avro.generator.spi.AvroCodeGenerationSpiRegistry
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.generateFiles
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.spi.load
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.registry.KotlinCodeGenerationSpiList

@OptIn(ExperimentalKotlinPoetApi::class)
open class EmnAxon5AvroBasedGenerator(
  val registry: EmnAxon5GenerationSpiRegistry,
  val avroRegistry: AvroCodeGenerationSpiRegistry,
  val properties: EmnAxon5GeneratorProperties
) {

  object Tags {
    data object TestFileSpec
  }

  companion object {
    val CONTEXT_UPPER_BOUND = EmnGenerationContext::class

    /**
     * Loads the default [KotlinCodeGenerationSpiList] and creates an [EmnAxon5AvroBasedGenerator] instance.
     */
    fun create(
      spiList: KotlinCodeGenerationSpiList = load(),
      properties: EmnAxon5GeneratorProperties,
    ) = EmnAxon5AvroBasedGenerator(
      avroRegistry = AvroCodeGenerationSpiRegistry(spiList),
      registry = EmnAxon5GenerationSpiRegistry(spiList),
      properties = properties,
    )
  }

  internal fun contextEmnContextFactory(declaration: ProtocolDeclaration, definitions: Definitions): EmnGenerationContext {
    val emnCtx = EmnGenerationContext.create(
      declaration = declaration,
      definitions = definitions,
      registry = registry,
      avroRegistry = avroRegistry,
      properties = properties,
    )

    val validation = EmnGenerationContext.validateContext(emnCtx)
    require(validation.isValid) {
      "EMN Generation Context is not valid: ${validation.errors.joinToString { "${it.dataPath} ${it.message}" }}"
    }

    return emnCtx
  }

  fun generate(definitions: Definitions, declaration: ProtocolDeclaration): KotlinFileSpecList {
    val context = contextEmnContextFactory(declaration, definitions)

    val avroGeneratedFiles = generateFiles(input = declaration, context = context.protocolDeclarationContext)

    val emnGeneratedFiles = generateFiles(input = definitions, context = context)

    return avroGeneratedFiles + emnGeneratedFiles
  }
}
