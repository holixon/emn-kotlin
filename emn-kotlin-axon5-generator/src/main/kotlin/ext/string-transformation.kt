// copied from kotlin-gists
package io.holixon.emn.generation.ext

import kotlin.text.replace
import kotlin.text.split

/**
 * A transformation function for strings, which can be used to apply various transformations to a string.
 */
fun interface StringTransformation : (String) -> String {
  operator fun plus(other: StringTransformation): StringTransformation = TODO()
//    if (this is StringTransformations.CHAIN) {
//    StringTransformations.CHAIN(this.list + other)
//  }
}

@Suppress("ClassName", "FunctionName")
data object StringTransformations {
  fun String.transform(f: StringTransformation) = f(this)

  val EMPTY = emptyList<StringTransformation>()

  @JvmInline
  value class CHAIN(val list: List<StringTransformation>) : StringTransformation {
    override fun invoke(input: String): String = list.fold(input) { acc, cur -> cur(acc) }
  }

  data object NOOP : StringTransformation {
    override fun invoke(input: String): String = input
  }

  class WRAP(private val wrap: String = "'") : StringTransformation {
    override fun invoke(input: String): String = if (wrap.isEmpty()) NOOP(input) else "$wrap$input$wrap"
    override fun toString(): String = "WRAP(wrap='$wrap')"
  }

  fun SHORTEN(maxLength: Int?, append: String = "...") =
    if (maxLength == null || maxLength == Int.MAX_VALUE) NOOP else stringTransformation("shorten=$maxLength") {
      if (maxLength != null && it.length > maxLength)
        it.take(maxLength) + append
      else it
    }


  /**
   * Converts a string to UpperSnakeCase, which is a common naming convention
   * for static constants in programming.
   *
   * Example: `fooBarHelloWorld` becomes `FOO_BAR_HELLO_WORLD`.
   */
  data object TO_UPPER_SNAKE_CASE : StringTransformation {
    override fun invoke(input: String): String = input.replace(Regex("([a-z])([A-Z])"), "$1_$2")
      .replace(Regex("_+"), "_")
      .uppercase()
  }

  /**
   * Converts a string to UpperCamelCase, which is a common naming convention
   * for TypeNames in programming.
   *
   * Example: `foo_bar_hello_world` becomes `FooBarHelloWorld`.
   */
  data object TO_UPPER_CAMEL_CASE : StringTransformation {
    override fun invoke(input: String): String = input.split(Regex("_+|\\s+|(?<=[a-z])(?=[A-Z])"))
      .filter { it.isNotEmpty() }
      .joinToString("") { part ->
        part.lowercase().replaceFirstChar { it.uppercase() }
      }
  }

  /**
   * Removes spaces and converts to lower case.
   *
   * Example: 'Add Customer` becomes `addcustomer`
   */
  data object WITHOUT_SPACES_TO_LOWER : StringTransformation {
    override fun invoke(input: String): String = input.replace(" ", "").lowercase()
  }

  private fun stringTransformation(toString: String, inner: StringTransformation) = object : StringTransformation {
    override fun invoke(source: String) = inner(source)
    override fun toString() = "StringTransformation($toString)"
  }

}
