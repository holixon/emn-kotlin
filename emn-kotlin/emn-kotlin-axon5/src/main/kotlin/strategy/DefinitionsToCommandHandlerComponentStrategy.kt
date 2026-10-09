package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.Definitions
import io.toolisticon.kotlin.generation.spec.KotlinFileSpec
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy

@OptIn(ExperimentalKotlinPoetApi::class)
class DefinitionsToCommandHandlerComponentStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, Definitions>(
  contextType = EmnGenerationContext::class, inputType = Definitions::class
) {
  override fun invoke(context: EmnGenerationContext, input: Definitions): KotlinFileSpecList {

    // empty if disabled in properties.
    val commandHandlerComponentFiles: List<KotlinFileSpec> = context.commandSlices.flatMap {
      CommandHandlingComponentStrategy().execute(context, it)
    }

    // empty if disabled in properties.
    val commandHandlerComponentTestFixtureFiles: List<KotlinFileSpec> = context.commandSlices.flatMap {
      CommandHandlingComponentTestFixtureStrategy().execute(context, it)
    }

    return KotlinFileSpecList(commandHandlerComponentFiles + commandHandlerComponentTestFixtureFiles)
  }
}
