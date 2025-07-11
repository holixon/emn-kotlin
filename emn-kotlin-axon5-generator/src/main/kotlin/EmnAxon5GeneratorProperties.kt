package io.holixon.emn.generation

import io.toolisticon.kotlin.generation.PackageName

interface EmnAxon5GeneratorProperties {
  val emnName : String
  val rootPackageName: PackageName
}

data class DefaultEmnAxon5GeneratorProperties(
  override val emnName: String,
  override val rootPackageName: PackageName
) : EmnAxon5GeneratorProperties
