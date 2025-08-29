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

  fun timelines(eventType: EventType): List<Timeline> {
    return timelines.filter { it.flowElements.events().any { e -> e.typeReference == eventType } }
  }

  /**
   * Retrieves all aggregates in all timelines.
   */
  fun aggregates(): List<AggregateLane> {
    return timelines.flatMap {
      it.laneSet.aggregateLaneSet
    }
  }

  /**
   * Retrieves all aggregates the event is created in.
   */
  fun aggregates(event: Event): List<AggregateLane> {
    return timelines(event).flatMap { t ->
      t.laneSet.aggregateLaneSet.filter { a ->
        a.flowElements.events().contains(event)
      }
    }
  }

  /**
   * Delivers all aggregates the events of given event type are created in.
   */
  fun aggregates(eventType: EventType): List<AggregateLane> {
    return timelines(eventType).flatMap { t ->
      t.laneSet.aggregateLaneSet.filter { a ->
        a.flowElements.events().any { e -> e.typeReference == eventType }
      }
    }
  }

  /**
   * Delivers all aggregates responsible for receiving this command.
   */
  fun aggregates(command: Command): List<AggregateLane> {
    return command.possibleEvents().flatMap { e ->
      aggregates(e)
    }
  }

  /**
   * Delivers all aggregates responsible for receiving all commands of this type.
   */
  fun aggregates(commandType: CommandType): List<AggregateLane> {
    return timelines
      .flatMap { t -> t.flowElements.commands().filter { e -> e.typeReference == commandType } }
      .flatMap { c -> aggregates(c) }
  }
}


