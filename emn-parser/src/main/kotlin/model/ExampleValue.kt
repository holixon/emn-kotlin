package io.holixon.emn.model

sealed class ExampleValue(
    open val valueFormat: String,
) {
    data class EmbeddedValue(override val valueFormat: String, val content: String) :
        ExampleValue(valueFormat = valueFormat) {
        override fun toString(): String = "content: \'${content}\'"
    }

    data class ResourceValue(override val valueFormat: String, val resource: String) :
        ExampleValue(valueFormat = valueFormat) {
        override fun toString(): String = "resource: $resource"
    }
}