package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.Definitions
import io.toolisticon.kotlin.generation.spec.KotlinFileSpec
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy

/** Generates specification tests independently from command handler generation. */
@OptIn(ExperimentalKotlinPoetApi::class)
class DefinitionsToCommandHandlerComponentTestFixtureStrategy :
  KotlinFileSpecListStrategy<EmnGenerationContext, Definitions>(
    contextType = EmnGenerationContext::class,
    inputType = Definitions::class
  ) {

  override fun invoke(context: EmnGenerationContext, input: Definitions): KotlinFileSpecList {
    val testFiles: List<KotlinFileSpec> = context.commandSlices.flatMap {
      CommandHandlingComponentTestFixtureStrategy().execute(context, it)
    }
    return KotlinFileSpecList(testFiles)
  }
}
