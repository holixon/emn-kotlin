package io.holixon.emn.model

sealed class Stage(
  open val id: String, open val values: List<FlowNode>
)

data class GivenStage(
  override val id: String,
  override val values: List<FlowNode>,
  val stateName: String? = null,
) : Stage(id, values) {

  val events: List<Event> by lazy {
    values.flatMap { value ->
      when (value) {
        is Event -> listOf(value)
        is EventSequence -> value.expandedEvents()
        else -> emptyList()
      }
    }
  }
}

private fun EventSequence.expandedEvents(): List<Event> {
  val effectiveVariables = definitionReference.variableDefinitions.associate { it.name to it.value } +
    variables.associate { it.name to it.value }
  return definitionReference.events.map { event ->
    Event(
      id = event.id,
      typeReference = event.eventType,
      value = event.value.resolveVariables(effectiveVariables),
    )
  }
}

private fun ElementValue?.resolveVariables(variables: Map<String, String>): ElementValue? = when (this) {
  is EmbeddedValue -> copy(
    content = variables.entries.fold(content) { resolved, (name, value) ->
      resolved.replace("\${$name}", value)
    }
  )
  else -> this
}

data class WhenStage(
  override val id: String,
  override val values: List<FlowNode>,
) : Stage(id, values) {

  val commands: List<Command> by lazy {
    values.filterIsInstance<Command>()
  }
}

data class ThenStage(
  override val id: String,
  override val values: List<FlowNode>,
) : Stage(id, values) {
  val events: List<Event> by lazy {
    values.filterIsInstance<Event>()
  }
  val errors: List<Error> by lazy {
    values.filterIsInstance<Error>()
  }
}
