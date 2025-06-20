package io.holixon.emn.generation.spi

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.model.Definitions
import io.toolisticon.kotlin.generation.spi.context.KotlinCodeGenerationContextBase
import kotlin.reflect.KClass

/**
 * Factory to create generation context.
 */
@OptIn(ExperimentalKotlinPoetApi::class)
class EmnGenerationContext(
  private val definitions: Definitions,
  registry: EmnAxon5GenerationSpiRegistry
) : KotlinCodeGenerationContextBase<EmnGenerationContext>(registry) {
  override val contextType: KClass<EmnGenerationContext> = EmnGenerationContext::class
}
