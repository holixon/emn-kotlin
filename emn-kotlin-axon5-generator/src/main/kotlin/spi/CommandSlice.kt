package io.holixon.emn.generation.spi

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ExperimentalKotlinPoetApi
import io.holixon.emn.generation.EmnAxon5GeneratorProperties
import io.holixon.emn.generation.ext.StringTransformations.TO_UPPER_CAMEL_CASE
import io.holixon.emn.generation.ext.StringTransformations.WITHOUT_SPACES_TO_LOWER
import io.holixon.emn.generation.ext.StringTransformations.transform
import io.holixon.emn.model.Command
import io.holixon.emn.model.Slice

@OptIn(ExperimentalKotlinPoetApi::class)
data class CommandSlice(
  val slice: Slice,
  val command: Command,
  val properties: EmnAxon5GeneratorProperties
) {
  internal val name = slice.name ?: slice.id
  val packageSuffix = name.transform(WITHOUT_SPACES_TO_LOWER)

  val packageName = "${properties.commandSliceRootPackageName}.$packageSuffix"
  val simpleClassName = name.transform(TO_UPPER_CAMEL_CASE)

}

val CommandSlice.commandHandlerClassName: ClassName get() = ClassName(packageName, simpleClassName + "CommandHandler")
