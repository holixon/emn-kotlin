package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.Definitions
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.fileBuilder
import io.toolisticon.kotlin.generation.spec.KotlinFileSpec
import io.toolisticon.kotlin.generation.spi.strategy.KotlinFileSpecStrategy

@OptIn(ExperimentalKotlinPoetApi::class)
class DefinitionsToFilesStrategy : KotlinFileSpecStrategy<EmnGenerationContext, Definitions>(
  contextType = EmnGenerationContext::class, inputType = Definitions::class
) {
  override fun invoke(context: EmnGenerationContext, input: Definitions): KotlinFileSpec {
    val className = ClassName("io.toolisticon.kotlin.generation", "Foo")
    val fileBuilder = fileBuilder(className)

    val root = KotlinCodeGeneration.builder.interfaceBuilder(className);

    root.addKdoc("Using definitions: $input")

    return fileBuilder
      .addType(root)
      .build()
  }
}
