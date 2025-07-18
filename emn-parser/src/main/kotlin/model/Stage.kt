package io.holixon.emn.model

sealed class Stage(
    open val id: String, open val examples: List<FlowElement.FlowNode>
) {
    data class GivenStage(
        override val id: String,
        override val examples: List<FlowElement.FlowNode>,
        val stateName: String? = null,
    ) : Stage(id, examples)

    data class WhenStage(
        override val id: String,
        override val examples: List<FlowElement.FlowNode>,
    ) : Stage(id, examples)

    data class ThenStage(
        override val id: String,
        override val examples: List<FlowElement.FlowNode>,
    ) : Stage(id, examples)

}