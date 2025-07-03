package io.holixon.emn.generation.spi

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.EmnAxon5AvroBasedGenerator.Companion.CONTEXT_UPPER_BOUND
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.spi.filter.hasContextType
import io.toolisticon.kotlin.generation.spi.KotlinCodeGenerationSpiRegistry
import io.toolisticon.kotlin.generation.spi.registry.KotlinCodeGenerationSpiList

@OptIn(ExperimentalKotlinPoetApi::class)
class EmnAxon5GenerationSpiRegistry(
  private val registry: KotlinCodeGenerationSpiRegistry,
) : KotlinCodeGenerationSpiRegistry by registry {
  companion object {
    val hasContextType = hasContextType(CONTEXT_UPPER_BOUND)
  }

  constructor(spiList: KotlinCodeGenerationSpiList) : this(registry = spiList.filter(hasContextType).registry())

}
