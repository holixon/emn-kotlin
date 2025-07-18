package io.holixon.emn.model

sealed class Lane(
    open val id: String, open val name: String?, open val flowElements: List<FlowElement>
) {

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
    ) : Lane(id = id, name = name, flowElements = flowElements)
}