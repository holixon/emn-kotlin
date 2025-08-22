package io.holixon.emn.model

data class LaneSet(
  val triggerLaneSet: List<TriggerLane> = emptyList(),
  val interactionLane: InteractionLane,
  val aggregateLaneSet: List<AggregateLane> = emptyList(),
) {
  constructor(id: String) : this(interactionLane = InteractionLane(id = id))
}
