package io.holixon.emn.model

data class Timeline(
  val sliceSet: List<Slice>,
  val laneSet: LaneSet,
  val nodes: List<FlowNode>,
  val messages: List<InformationFlow>,
) {
  val flowElements: List<FlowElement> by lazy {
    nodes + messages
  }

  fun conceptsForSlice(filter: Slice): List<ConceptLane> {
    return this.laneSet.conceptLaneSet.filter {
      it.flowElements.any { elementInLane -> filter.flowElements.contains(elementInLane) }
    }
  }

}
