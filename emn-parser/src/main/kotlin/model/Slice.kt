package io.holixon.emn.model

data class Slice(
    val id: String,
    val name: String? = null,
    val flowElements: List<FlowElement>
)