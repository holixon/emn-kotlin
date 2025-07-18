package io.holixon.emn.model

data class Specification(
    val id: String,
    val name: String,
    val scenario: String? = null,
    val slice: Slice? = null,
    val givenStage: Stage.GivenStage? = null,
    val whenStage: Stage.WhenStage? = null,
    val thenStage: Stage.ThenStage? = null
)