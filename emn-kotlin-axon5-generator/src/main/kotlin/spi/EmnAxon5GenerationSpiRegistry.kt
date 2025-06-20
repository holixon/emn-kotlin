package io.holixon.emn.generation.spi

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.toolisticon.kotlin.generation.spi.KotlinCodeGenerationSpiRegistry
import io.toolisticon.kotlin.generation.spi.processor.KotlinCodeGenerationProcessorList
import io.toolisticon.kotlin.generation.spi.registry.KotlinCodeGenerationServiceRepository
import io.toolisticon.kotlin.generation.spi.strategy.KotlinCodeGenerationStrategyList

@OptIn(ExperimentalKotlinPoetApi::class)
class EmnAxon5GenerationSpiRegistry(
  private val registry: KotlinCodeGenerationSpiRegistry,
) : KotlinCodeGenerationSpiRegistry by registry {
  companion object {
    private val CONTEXT_UPPER_BOUND = EmnGenerationContext::class
  }


  constructor(strategies: KotlinCodeGenerationStrategyList, processors: KotlinCodeGenerationProcessorList = KotlinCodeGenerationProcessorList()) : this(
    registry = KotlinCodeGenerationServiceRepository(CONTEXT_UPPER_BOUND, processors, strategies)
  )

}
