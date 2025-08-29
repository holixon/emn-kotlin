package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.Definitions
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.spec.KotlinFileSpec
import io.toolisticon.kotlin.generation.spec.KotlinFileSpecList
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecListStrategy

@OptIn(ExperimentalKotlinPoetApi::class)
class DefinitionsToCommandHandlerComponentStrategy : KotlinFileSpecListStrategy<EmnGenerationContext, Definitions>(
  contextType = EmnGenerationContext::class, inputType = Definitions::class
) {
  override fun invoke(context: EmnGenerationContext, input: Definitions): KotlinFileSpecList {

    val commandHandlerComponentFiles: List<KotlinFileSpec> = context.commandSlices.flatMap { commandSlice ->
      CommandHandlingComponentStrategy().invoke(context, commandSlice)
    }

    return KotlinFileSpecList(commandHandlerComponentFiles)
  }

  private fun buildSlice(input: Definitions): KotlinFileSpec {
    val timeline = input.timelines[0]
    val slice = timeline.sliceSet.first()

    val sliceName = slice.name?.replace(" ", "") ?: "slice1"
    val packageName = "io.holixon.emn.example.faculty." + sliceName.lowercase()
    val commandHandlerClassName = sliceName.replaceFirstChar { if (it.isLowerCase()) it.uppercase() else it.toString() } + "CommandHandler"
    val sliceClassName = ClassName(packageName, commandHandlerClassName)
    val sliceFileBuilder = fileBuilder(sliceClassName)
    val sliceRoot = KotlinCodeGeneration.builder.interfaceBuilder(sliceClassName)

    sliceRoot.addKdoc(
      """
        Commands: ${slice.commands()}

        Events: ${slice.events()}

        Aggregates: ${timeline.aggregatesForSlice(slice)}

      """.trimIndent()
    )

    return sliceFileBuilder
      .addType(sliceRoot)
      .build()
  }

  private fun buildFoo(input: Definitions): KotlinFileSpec {
    val classNameFoo = ClassName("io.toolisticon.kotlin.generation", "Foo")
    val fileBuilder = fileBuilder(classNameFoo)

    val root = KotlinCodeGeneration.builder.interfaceBuilder(classNameFoo);
    root.addKdoc("Using definitions: $input")

    return fileBuilder
      .addType(root)
      .build()
  }
}
