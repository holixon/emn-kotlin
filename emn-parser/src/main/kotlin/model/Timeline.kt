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
}
