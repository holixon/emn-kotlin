package io.holixon.emn.model

data class Definitions(
  val nodes: List<FlowElementType.FlowNodeType>,
  val flows: List<FlowElementType.MessageFlowType>
) {
  val typeDefinitions: List<FlowElementType> by lazy {
    nodes + flows
  }
}

sealed class FlowElementType(
  open val id: String,
  open val name: String? = null
) {

  data class MessageFlowType(
    override val id: String,
    override val name: String?,
    val source: FlowNodeType,
    val target: FlowNodeType
  ) : FlowElementType(id = id, name = name)

  sealed class FlowNodeType(
    override val id: String,
    override val name: String?,
    open val schema: Schema?,
    open val incoming: MutableList<MessageFlowType> = mutableListOf(),
    open val outgoing: MutableList<MessageFlowType> = mutableListOf(),
  ): FlowElementType(id = id, name = name) {
    class CommandType(id: String, name: String, schema: Schema?): FlowNodeType(id = id, name = name, schema = schema)
    class QueryType(id: String, name: String, schema: Schema?): FlowNodeType(id = id, name = name, schema = schema)
    class EventType(id: String, name: String, schema: Schema?): FlowNodeType(id = id, name = name, schema = schema)
    class ExternalEventType(id: String, name: String, schema: Schema?): FlowNodeType(id = id, name = name, schema = schema)
    class ViewType(id: String, name: String?, schema: Schema?): FlowNodeType(id = id, name = name, schema = schema)
    class TranslationType(id: String, name: String?, schema: Schema?): FlowNodeType(id = id, name = name, schema = schema)
    class AutomationType(id: String, name: String?, schema: Schema?): FlowNodeType(id = id, name = name, schema = schema)
    class ExternalSystemType(id: String, name: String?, schema: Schema?): FlowNodeType(id = id, name = name, schema = schema)
    class ErrorType(id: String, name: String?, schema: Schema?): FlowNodeType(id = id, name = name, schema = schema)
    class FlowNodeTypeReference(id: String): FlowNodeType(id = id, name = null, schema = null)
  }
}

sealed class Schema(
  open val schemaFormat: String,
) {
  data class EmbeddedSchema(override val schemaFormat: String, val content: String): Schema(schemaFormat = schemaFormat) {
    override fun printable(): String = "content: \"${content}\""
  }
  data class ResourceSchema(override val schemaFormat: String, val resource: String): Schema(schemaFormat = schemaFormat) {
    override fun printable(): String = "resource: $resource"
  }

  open fun printable(): String = TODO()
}

