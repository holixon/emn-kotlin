package io.holixon.emn.model

import io.holixon.emn.model.FlowElementType.FlowNodeType
import io.holixon.emn.model.FlowElementType.FlowNodeType.*
import io.holixon.emn.model.FlowElementType.MessageFlowType
import io.holixon.emn.model.Lane.*

data class Definitions(
  val nodeTypes: List<FlowNodeType>,
  val flowTypes: List<MessageFlowType>,
  val timelines: List<Timeline>
) {
  val typeDefinitions: List<FlowElementType> by lazy {
    nodeTypes + flowTypes
  }

  inline fun <reified T : FlowNodeType> getFlowElementType(): List<T> = nodeTypes.filterIsInstance<T>()
}

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

    class MessageTypeReference(id: String) : MessageFlowType(id = id, name = null, source = NoTypeReference, target = NoTypeReference)
  }


  sealed class FlowNodeType(
    override val id: String,
    override val name: String?,
    open val schema: Schema?,
    open val incoming: MutableList<MessageFlowType> = mutableListOf(),
    open val outgoing: MutableList<MessageFlowType> = mutableListOf(),
  ) : FlowElementType(id = id, name = name) {
    class CommandType(id: String, name: String, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
    class QueryType(id: String, name: String, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
    class EventType(id: String, name: String, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
    class ExternalEventType(id: String, name: String, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
    class ViewType(id: String, name: String?, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
    class TranslationType(id: String, name: String?, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
    class AutomationType(id: String, name: String?, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
    class ExternalSystemType(id: String, name: String?, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
    class ErrorType(id: String, name: String?, schema: Schema?) : FlowNodeType(id = id, name = name, schema = schema)
    open class FlowNodeTypeReference(id: String) : FlowNodeType(id = id, name = null, schema = null)
    object NoTypeReference : FlowNodeTypeReference(id = "NONE")
  }

  override fun toString(): String = "${this::class.simpleName}(id=$id, name=$name)"
}

sealed class Schema(
  open val schemaFormat: String,
) {
  data class EmbeddedSchema(override val schemaFormat: String, val content: String) : Schema(schemaFormat = schemaFormat) {
    override fun toString(): String = "content: \'${content}\'"
  }

  data class ResourceSchema(override val schemaFormat: String, val resource: String) : Schema(schemaFormat = schemaFormat) {
    override fun toString(): String = "resource: $resource"
  }

}

data class Timeline(
  val sliceSet: List<Slice>,
  val laneSet: LaneSet,
  val nodes: List<FlowElement.FlowNode>,
  val messages: List<FlowElement.MessageFlow>,
) {
  val flowElements: List<FlowElement> by lazy {
    nodes + messages
  }
}

data class Slice(
  val id: String,
  val name: String? = null,
  val flowElements: List<FlowElement>
)

data class LaneSet(
  val triggerLaneSet: List<TriggerLane> = emptyList(),
  val interactionLane: InteractionLane,
  val aggregateLaneSet: List<AggregateLane> = emptyList(),
) {
  constructor(id: String) : this(interactionLane = InteractionLane(id = id))
}

sealed class FlowElement(
  open val id: String,
  open val typeReference: FlowElementType,
) {

  data class MessageFlow(
    override val id: String,
    override val typeReference: MessageFlowType,
    val source: FlowNode,
    val target: FlowNode
  ) : FlowElement(id = id, typeReference = typeReference)

  sealed class FlowNode(
    override val id: String,
    override val typeReference: FlowNodeType,
    open val incoming: MutableList<MessageFlow> = mutableListOf(),
    open val outgoing: MutableList<MessageFlow> = mutableListOf(),
  ) : FlowElement(id = id, typeReference = typeReference) {

    class Command(id: String, name: String, typeReference: CommandType) : FlowNode(id = id, typeReference = typeReference)
    class Query(id: String, name: String, typeReference: QueryType) : FlowNode(id = id, typeReference = typeReference)
    class Event(id: String, name: String, typeReference: EventType) : FlowNode(id = id, typeReference = typeReference)
    class ExternalEvent(id: String, name: String, typeReference: ExternalEventType) : FlowNode(id = id, typeReference = typeReference)
    class View(id: String, name: String?, typeReference: ViewType) : FlowNode(id = id, typeReference = typeReference)
    class Translation(id: String, name: String?, typeReference: TranslationType) : FlowNode(id = id, typeReference = typeReference)
    class Automation(id: String, name: String?, typeReference: AutomationType) : FlowNode(id = id, typeReference = typeReference)
    class ExternalSystem(id: String, name: String?, typeReference: ExternalSystemType) : FlowNode(id = id, typeReference = typeReference)
    class Error(id: String, name: String?, typeReference: ErrorType) : FlowNode(id = id, typeReference = typeReference)
    class FlowNodeReference(id: String) : FlowNode(id = id, typeReference = NoTypeReference)
  }

  override fun toString(): String = "${this::class.simpleName}(id=$id, name=${typeReference.name}, type=${typeReference.id})"

}

sealed class Lane(
  open val id: String,
  open val name: String?,
  open val flowElements: List<FlowElement>
) {

  data class TriggerLane(override val id: String, override val name: String? = null, override val flowElements: List<FlowElement> = emptyList()) :
  Lane(id = id, name = name, flowElements = flowElements)

  data class InteractionLane(override val id: String, override val name: String? = null, override val flowElements: List<FlowElement> = emptyList()) :
  Lane(id = id, name = name, flowElements = flowElements)

  data class AggregateLane(override val id: String, override val name: String? = null, override val flowElements: List<FlowElement> = emptyList()) :
  Lane(id = id, name = name, flowElements = flowElements)
}
