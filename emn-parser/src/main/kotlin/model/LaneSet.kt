package io.holixon.emn.model

data class LaneSet(
  val triggerLaneSet: List<TriggerLane> = emptyList(),
  val interactionLane: InteractionLane? = null,
  val conceptLaneSet: List<ConceptLane> = emptyList(),
) {
  fun isWellFormed() = interactionLane != null
}
