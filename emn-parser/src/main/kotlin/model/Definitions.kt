package io.holixon.emn.model

data class Definitions(
    val nodeTypes: List<FlowNodeType>,
    val flowTypes: List<InformationFlowType>,
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

  fun timelines(eventType: EventType): List<Timeline> {
    return timelines.filter { it.flowElements.events().any { e -> e.typeReference == eventType } }
  }

  /**
   * Retrieves all concepts in all timelines.
   */
  fun concepts(): List<ConceptLane> {
    return timelines.flatMap {
      it.laneSet?.conceptLaneSet ?: emptyList()
    }
  }

  /**
   * Retrieves all concepts the event is created in.
   */
  fun concepts(event: Event): List<ConceptLane> {
    return timelines(event).flatMap { t ->
      t.laneSet?.conceptLaneSet?.filter { a ->
        a.flowElements.events().contains(event)
      } ?: emptyList()
    }
  }

  /**
   * Delivers all aggregates the events of given event type are created in.
   */
  fun concepts(eventType: EventType): List<ConceptLane> {
    return timelines(eventType).flatMap { t ->
      t.laneSet?.conceptLaneSet?.filter { a ->
        a.flowElements.events().any { e -> e.typeReference == eventType }
      } ?: emptyList()
    }
  }

  /**
   * Delivers all aggregates responsible for receiving this command.
   */
  fun concepts(command: Command): List<ConceptLane> {
    return command.possibleEvents().flatMap { e ->
      concepts(e)
    }
  }

  /**
   * Delivers all aggregates responsible for receiving all commands of this type.
   */
  fun concepts(commandType: CommandType): List<ConceptLane> {
    return timelines
      .flatMap { t -> t.flowElements.commands().filter { e -> e.typeReference == commandType } }
      .flatMap { c -> concepts(c) }
  }
}
