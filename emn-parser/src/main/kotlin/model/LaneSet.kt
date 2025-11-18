package io.holixon.emn.model

data class LaneSet(
  val triggerLaneSet: List<TriggerLane> = emptyList(),
  val interactionLane: InteractionLane,
  val conceptLaneSet: List<ConceptLane> = emptyList(),
) {
  constructor(id: String = "UNSET") : this(interactionLane = InteractionLane(id = id))
}
