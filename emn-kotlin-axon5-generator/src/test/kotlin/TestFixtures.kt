package io.holixon.emn.generation

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.toolisticon.kotlin.generation.KotlinCodeGeneration

@OptIn(ExperimentalKotlinPoetApi::class)
object TestFixtures {
  // loads ALL available strategies and processors
  val SPI_REGISTRY = KotlinCodeGeneration.spi.registry()
}
