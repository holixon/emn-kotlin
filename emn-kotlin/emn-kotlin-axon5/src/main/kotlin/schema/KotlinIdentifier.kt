package io.holixon.emn.generation.schema

object KotlinIdentifier {
  private val identifier = Regex("[A-Za-z_][A-Za-z0-9_]*")
  private val keywords = setOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while"
  )

  fun requireClassName(value: String, sourceDescription: String = "class name"): String {
    require(value.matches(identifier) && value.first().isUpperCase() && value !in keywords) {
      "Invalid Kotlin $sourceDescription '$value'. Use a valid PascalCase TypeId for schema generation."
    }
    return value
  }

  fun requirePropertyName(value: String, sourceDescription: String = "property name"): String {
    require(value.matches(identifier) && value !in keywords) {
      "Invalid Kotlin $sourceDescription '$value'. Use a valid Kotlin property name in the JSON Schema."
    }
    return value
  }

  fun requirePackageName(value: String): String {
    require(value.isNotBlank()) { "rootPackageName must not be blank." }
    value.split(".").forEach { segment ->
      require(segment.matches(identifier) && segment !in keywords) {
        "Invalid Kotlin package name '$value'. Segment '$segment' is not a valid identifier."
      }
    }
    return value
  }
}
