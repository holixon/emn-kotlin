package io.holixon.emn.model

sealed class Stage(
  open val id: String, open val values: List<FlowNode>
)

data class GivenStage(
  override val id: String,
  override val values: List<FlowNode>,
  val stateName: String? = null,
) : Stage(id, values)

data class WhenStage(
  override val id: String,
  override val values: List<FlowNode>,
) : Stage(id, values)

data class ThenStage(
  override val id: String,
  override val values: List<FlowNode>,
) : Stage(id, values)
