package io.holixon.emn.generation.strategy

import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.spi.EmnGenerationContext
import io.holixon.emn.model.FlowElementType.FlowNodeType.CommandType
import io.toolisticon.kotlin.generation.KotlinCodeGeneration.builder.dataClassBuilder
import io.toolisticon.kotlin.generation.spec.KotlinDataClassSpec
import io.toolisticon.kotlin.generation.spi.strategy.KotlinCodeGenerationStrategyBase
import java.util.*

@OptIn(ExperimentalKotlinPoetApi::class)
class NestedCommandDataClassStrategy : KotlinCodeGenerationStrategyBase<EmnGenerationContext, CommandType, KotlinDataClassSpec>(
  contextType = EmnGenerationContext::class,
  inputType = CommandType::class,
  specType = KotlinDataClassSpec::class
) {
  override fun invoke(context: EmnGenerationContext, input: CommandType): KotlinDataClassSpec {

    // FIXME -> correct type name
    val name = requireNotNull(input.name) { "Name is required" }

    val builder = dataClassBuilder(context.rootPackageName, name).apply {

      addConstructorProperty("name", String::class)
      addKdoc("Command $name")

    }

    return builder.build()
  }
}

