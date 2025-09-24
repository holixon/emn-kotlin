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
    values.filterIsInstance<Event>()
  }
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
