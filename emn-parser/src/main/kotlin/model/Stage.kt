package io.holixon.emn.model

sealed class Stage(
  open val id: String, open val examples: List<FlowNode>
)

data class GivenStage(
  override val id: String,
  override val examples: List<FlowNode>,
  val stateName: String? = null,
) : Stage(id, examples)

data class WhenStage(
  override val id: String,
  override val examples: List<FlowNode>,
) : Stage(id, examples)

data class ThenStage(
  override val id: String,
  override val examples: List<FlowNode>,
) : Stage(id, examples)
