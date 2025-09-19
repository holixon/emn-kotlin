package io.holixon.emn.generation.spi

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.EmnAxon5GeneratorProperties
import io.holixon.emn.model.Command
import io.holixon.emn.model.Slice
import io.toolisticon.kotlin.generation.KotlinCodeGeneration
import io.toolisticon.kotlin.generation.support.StringTransformations.WITHOUT_SPACES_TO_LOWER
import io.toolisticon.kotlin.generation.support.StringTransformations.transform

@OptIn(ExperimentalKotlinPoetApi::class)
data class CommandSlice(
  val slice: Slice,
  val command: Command,
  val properties: EmnAxon5GeneratorProperties
) {
  internal val name = slice.name ?: slice.id
  val packageSuffix = name.transform(WITHOUT_SPACES_TO_LOWER)

  val packageName = "${properties.commandSliceRootPackageName}.$packageSuffix"
  val simpleClassName = KotlinCodeGeneration.name.simpleName(name)
}

val CommandSlice.commandHandlerClassName: ClassName get() = ClassName(packageName, simpleClassName + "CommandHandler")
