package io.holixon.emn.model

data class Timeline(
  val sliceSet: List<Slice> = emptyList(),
  val laneSet: LaneSet? = null,
  val nodes: List<FlowNode> = emptyList(),
  val messages: List<InformationFlow> = emptyList(),
) {
  val flowElements: List<FlowElement> by lazy {
    nodes + messages
  }

  fun conceptsForSlice(filter: Slice): List<ConceptLane> {
    return this.laneSet?.conceptLaneSet?.filter {
      it.flowElements.any { elementInLane -> filter.flowElements.contains(elementInLane) }
    } ?: emptyList()
  }

  /**
   * Checks if the timeline is well-formed.
   * A timeline is well-formed if it has a laneSet and the laneSet is well-formed.
   *
   * @return true if the timeline is well-formed, false otherwise
   */
  fun isWellFormed(): Boolean = laneSet != null && laneSet.isWellFormed()
}
