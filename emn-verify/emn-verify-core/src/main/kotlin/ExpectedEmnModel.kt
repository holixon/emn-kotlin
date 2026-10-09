package io.holixon.emn.verify

import io.holixon.emn.model.CommandType
import io.holixon.emn.model.Definitions
import io.holixon.emn.model.EventType
import io.holixon.emn.model.EventSequenceType
import io.holixon.emn.model.QueryType

data class ExpectedEmnModel(
  val commandTypeIds: Set<String>,
  val eventTypeIds: Set<String>,
  val queryTypeIds: Set<String>,
  val consumedEventTypeIdsByQuery: Map<String, Set<String>>,
  val specificationIds: Set<String>,
)

object ExpectedEmnModelFactory {

  fun create(definitions: Definitions): ExpectedEmnModel {
    val queryTypeIds = definitions.nodeTypes.filterIsInstance<QueryType>().mapTo(sortedSetOf()) { it.id }
    val consumedEventsByQuery = definitions.flowTypes
      .filter { (it.source is EventType || it.source is EventSequenceType) && it.target is QueryType }
      .groupBy { it.target.id }
      .mapValues { (_, flows) ->
        flows.flatMapTo(sortedSetOf()) { flow ->
          when (val source = flow.source) {
            is EventType -> listOf(source.id)
            is EventSequenceType -> source.eventTypes.map { it.id }
            else -> emptyList()
          }
        }
      }
      .toSortedMap()

    return ExpectedEmnModel(
      commandTypeIds = definitions.nodeTypes.filterIsInstance<CommandType>().mapTo(sortedSetOf()) { it.id },
      eventTypeIds = definitions.nodeTypes.filterIsInstance<EventType>().mapTo(sortedSetOf()) { it.id },
      queryTypeIds = queryTypeIds,
      consumedEventTypeIdsByQuery = consumedEventsByQuery,
      specificationIds = definitions.specifications.mapTo(sortedSetOf()) { it.id },
    )
  }
}
