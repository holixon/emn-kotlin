package io.holixon.emn.model

data class LaneSet(
    val triggerLaneSet: List<Lane.TriggerLane> = emptyList(),
    val interactionLane: Lane.InteractionLane,
    val aggregateLaneSet: List<Lane.AggregateLane> = emptyList(),
) {
    constructor(id: String) : this(interactionLane = Lane.InteractionLane(id = id))
}