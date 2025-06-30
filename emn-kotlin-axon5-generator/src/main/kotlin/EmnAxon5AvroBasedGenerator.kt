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
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.KotlinCodeGenerationSpiRegistry

@OptIn(ExperimentalKotlinPoetApi::class)
open class EmnAxon5AvroBasedGenerator(
  val registry: EmnAxon5GenerationSpiRegistry,
  val properties: EmnAxon5GeneratorProperties,
  val avroRegistry: AvroCodeGenerationSpiRegistry,
  val avroProperties: AvroKotlinGeneratorProperties
) {
  companion object {
    fun create(
      registry: KotlinCodeGenerationSpiRegistry,
      properties: EmnAxon5GeneratorProperties,
      avroProperties: AvroKotlinGeneratorProperties
    ): EmnAxon5AvroBasedGenerator {
      return EmnAxon5AvroBasedGenerator(
        registry = registry,
        properties = properties,
        avroRegistry = avroRegistry,
        avroProperties = avroProperties
      )
    }
  }

  internal fun contextEmnContextFactory(declaration: ProtocolDeclaration, definitions: Definitions): EmnGenerationContext {
    val protocolDeclarationContext = ProtocolDeclarationContext.of(
      declaration = declaration,
      registry = avroRegistry,
      properties = avroProperties
    )
    return EmnGenerationContext(
      definitions = definitions,
      registry = registry,
      properties = properties,
      protocolDeclarationContext = protocolDeclarationContext
    )
  }

  fun generate(definitions: Definitions, declaration: ProtocolDeclaration): KotlinFileSpecList {

    val context = contextEmnContextFactory(declaration, definitions)

    val avroGeneratedFiles = generateFiles(input = declaration, context = context.protocolDeclarationContext)
    val emnGeneratedFiles = generateFiles(input = definitions, context = context)


    return avroGeneratedFiles + emnGeneratedFiles
  }
}
