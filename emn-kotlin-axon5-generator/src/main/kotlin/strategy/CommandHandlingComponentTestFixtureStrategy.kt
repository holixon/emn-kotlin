package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.CommandSlice
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.generation.spi.commandHandlerFixtureTestClassName
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.classBuilder
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy

@OptIn(ExperimentalKotlinPoetApi::class)
class CommandHandlingComponentTestFixtureStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, CommandSlice>(
  contextType = EmnGenerationContext::class, inputType = CommandSlice::class
) {
  override fun invoke(
    context: EmnGenerationContext,
    input: CommandSlice
  ): KotlinFileSpecList {
    val fileBuilder = fileBuilder(input.commandHandlerFixtureTestClassName)

    val commandHandlerTypeBuilder = classBuilder(input.commandHandlerFixtureTestClassName).apply {


      context.definitions.specifications
        .filter { spec -> spec.slice != null && spec.slice!!.id == input.slice.id }
        .forEach { spec ->

          this.addKdoc("\n${spec.name}\n\n")
        }
    }

    fileBuilder.addType(commandHandlerTypeBuilder.build())
    return KotlinFileSpecList(fileBuilder.build())
  }
}

