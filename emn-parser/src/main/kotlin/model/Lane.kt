package io.holixon.emn.model

sealed class Lane(
  open val id: String, open val name: String?, open val flowElements: List<FlowElement>
)

data class TriggerLane(
  override val id: String,
  override val name: String? = null,
  override val flowElements: List<FlowElement> = emptyList()
) : Lane(id = id, name = name, flowElements = flowElements)

data class InteractionLane(
  override val id: String,
  override val name: String? = null,
  override val flowElements: List<FlowElement> = emptyList()
) : Lane(id = id, name = name, flowElements = flowElements)

data class AggregateLane(
  override val id: String,
  override val name: String? = null,
  override val flowElements: List<FlowElement> = emptyList(),
  val idSchema: Schema? = null,
) : Lane(id = id, name = name, flowElements = flowElements) {

  fun schemaReference(): String {
    requireNotNull(this.idSchema) { "No schema found for $this" }
    require(this.idSchema is EmbeddedSchema) { "Only embedded schema is currently supported, but it was ${this.idSchema::class.simpleName}" }
    return (this.idSchema).content
  }
}
