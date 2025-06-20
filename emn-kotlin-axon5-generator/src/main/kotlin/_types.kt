package io.holixon.emn.generation

import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.Definitions
import io.toolisticon.kotlin.generation.spi.KotlinCodeGenerationContextFactory

typealias EmnContextFactory = KotlinCodeGenerationContextFactory<EmnGenerationContext, Definitions>
