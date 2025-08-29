package io.holixon.emn.model

data class Definitions(
    val nodeTypes: List<FlowNodeType>,
    val flowTypes: List<MessageFlowType>,
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
    inline fun <reified T : FlowNodeType> getFlowElementType(): List<T> =
        nodeTypes.filterIsInstance<T>()

    /**
     * Get all elements.
     * @param <T> type of the element.
     * @return list of all elements of given type.
     */
    inline fun <reified T : FlowNode> getFlowElement(): List<T> =
        timelines.map { it.flowElements }.flatten().filterIsInstance<T>()

    fun timelines(event: Event): List<Timeline> {
        return timelines.filter { it.flowElements.events().contains(event) }
    }

    fun aggregates(): List<AggregateLane> {
      return timelines.flatMap {
        it.laneSet.aggregateLaneSet
      }
    }

    fun aggregates(event: Event): List<AggregateLane> {
        return timelines(event).flatMap { t ->
            t.laneSet.aggregateLaneSet.filter { a ->
                a.flowElements.events().contains(event)
            }
        }
    }
}


