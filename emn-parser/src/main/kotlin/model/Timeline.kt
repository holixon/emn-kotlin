package io.holixon.emn.model

data class Timeline(
    val sliceSet: List<Slice>,
    val laneSet: LaneSet,
    val nodes: List<FlowElement.FlowNode>,
    val messages: List<FlowElement.MessageFlow>,
) {
    val flowElements: List<FlowElement> by lazy {
        nodes + messages
    }
}