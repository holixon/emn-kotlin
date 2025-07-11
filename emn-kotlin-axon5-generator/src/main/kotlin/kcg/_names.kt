package io.holixon.emn.generation.kcg

@Suppress("ClassName")
object name {
  fun String.transformUpperCamelcase() = split(Regex("_+|(?<=[a-z])(?=[A-Z])"))
    .filter { it.isNotEmpty() }
    .joinToString("") { part ->
      part.lowercase().replaceFirstChar { it.uppercase() }
    }

  fun String.transformUpperSnakeCase(): String = replace(Regex("([a-z])([A-Z])"), "$1_$2")
    .replace(Regex("_+"), "_")
    .uppercase()
}
