package io.holixon.emn.model

sealed class Schema(
    open val schemaFormat: String,
) {
    data class EmbeddedSchema(override val schemaFormat: String, val content: String) :
        Schema(schemaFormat = schemaFormat) {
        override fun toString(): String = "content: \'${content}\'"
    }

    data class ResourceSchema(override val schemaFormat: String, val resource: String) :
        Schema(schemaFormat = schemaFormat) {
        override fun toString(): String = "resource: $resource"
    }
}