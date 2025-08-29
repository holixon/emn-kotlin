package io.holixon.emn.model

data class Timeline(
  val sliceSet: List<Slice>,
  val laneSet: LaneSet,
  val nodes: List<FlowNode>,
  val messages: List<MessageFlow>,
) {
  val flowElements: List<FlowElement> by lazy {
    nodes + messages
  }

  fun aggregatesForSlice(filter: Slice): List<AggregateLane> {
    return this.laneSet.aggregateLaneSet.filter {
      it.flowElements.any { elementInLane -> filter.flowElements.contains(elementInLane) }
    }
  }

}
