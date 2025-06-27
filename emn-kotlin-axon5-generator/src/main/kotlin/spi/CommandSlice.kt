package io.holixon.emn.generation.spi

import _ktx.StringKtx.firstUppercase
import io.holixon.emn.generation.removeSpaces
import io.holixon.emn.model.FlowElement.FlowNode.Command
import io.holixon.emn.model.Slice
import io.toolisticon.kotlin.generation.PackageName

data class CommandSlice(
  val slice: Slice,
  val command: Command
) {
  internal fun name() = slice.name ?: slice.id
  fun packageName(rootPackage: PackageName): PackageName = rootPackage + name().removeSpaces().lowercase()
  fun simpleClassName(): String = name().removeSpaces().firstUppercase()
}
