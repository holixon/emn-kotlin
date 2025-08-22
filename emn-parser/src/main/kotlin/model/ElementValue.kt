package io.holixon.emn.model

sealed class ElementValue(
  open val valueFormat: String,
)

data class EmbeddedValue(override val valueFormat: String, val content: String) : ElementValue(valueFormat = valueFormat) {
  override fun toString(): String = "content: \'${content}\'"
}

data class ResourceValue(override val valueFormat: String, val resource: String) : ElementValue(valueFormat = valueFormat) {
  override fun toString(): String = "resource: $resource"
}
