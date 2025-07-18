package io.holixon.emn.model

import io.holixon.emn.model.FlowElementType.FlowNodeType.NoTypeReference

sealed class FlowElementType(
    open val id: String,
    open val name: String? = null
) {

    open class MessageFlowType(
        override val id: String,
        override val name: String?,
        val source: FlowNodeType,
        val target: FlowNodeType
    ) : FlowElementType(id = id, name = name) {

        fun copy(source: FlowNodeType, target: FlowNodeType): MessageFlowType = MessageFlowType(
            id = this.id,
            name = this.name,
            source = source,
            target = target
        )

        class MessageTypeReference(id: String) :
            MessageFlowType(id = id, name = null, source = NoTypeReference, target = NoTypeReference)
    }


    sealed class FlowNodeType(
        override val id: String,
        override val name: String,
        open val schema: Schema?,
        open val incoming: MutableList<MessageFlowType> = mutableListOf(),
        open val outgoing: MutableList<MessageFlowType> = mutableListOf(),
    ) : FlowElementType(id = id, name = name) {
        class CommandType(id: String, name: String, schema: Schema?) :
            FlowNodeType(id = id, name = name, schema = schema)

        class QueryType(id: String, name: String, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
        class EventType(id: String, name: String, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
        class ExternalEventType(id: String, name: String, schema: Schema?) :
            FlowNodeType(id = id, name = name, schema = schema)

        class ViewType(id: String, name: String, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
        class TranslationType(id: String, name: String, schema: Schema?) :
            FlowNodeType(id = id, name = name, schema = schema)

        class AutomationType(id: String, name: String, schema: Schema?) :
            FlowNodeType(id = id, name = name, schema = schema)

        class ExternalSystemType(id: String, name: String, schema: Schema?) :
            FlowNodeType(id = id, name = name, schema = schema)

        class ErrorType(id: String, name: String, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
        open class FlowNodeTypeReference(id: String) : FlowNodeType(id = id, name = id, schema = null)
        object NoTypeReference : FlowNodeTypeReference(id = "NONE")

        fun schemaReference(): String {
            requireNotNull(this.schema) { "No schema found for $this" }
            return (this.schema!! as Schema.EmbeddedSchema).content
        }
    }

    override fun toString(): String = "${this::class.simpleName}(id=$id, name=$name)"
}