package io.holixon.emn.model

import io.holixon.emn.model.FlowElement.FlowNode.Event

data class Definitions(
    val nodeTypes: List<FlowElementType.FlowNodeType>,
    val flowTypes: List<FlowElementType.MessageFlowType>,
    val timelines: List<Timeline>,
    val specifications: List<Specification>
) {
    val typeDefinitions: List<FlowElementType> by lazy {
        nodeTypes + flowTypes
    }

    /**
     * Get all element types.
     * @param <T> type of the element type.
     * @return list of all elements type of given type.
     */
    inline fun <reified T : FlowElementType.FlowNodeType> getFlowElementType(): List<T> =
        nodeTypes.filterIsInstance<T>()

    /**
     * Get all elements.
     * @param <T> type of the element.
     * @return list of all elements of given type.
     */
    inline fun <reified T : FlowElement.FlowNode> getFlowElement(): List<T> =
        timelines.map { it.flowElements }.flatten().filterIsInstance<T>()

    fun timelines(event: Event): List<Timeline> {
        return timelines.filter { it.flowElements.events().contains(event) }
    }

    fun aggregates(event: Event): List<Lane.AggregateLane> {
        return timelines(event).flatMap { t ->
            t.laneSet.aggregateLaneSet.filter { a ->
                a.flowElements.events().contains(event)
            }
        }
    }

}


