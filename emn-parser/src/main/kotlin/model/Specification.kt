package io.holixon.emn.model

data class Specification(
  val id: String,
  val name: String,
  val scenario: String? = null,
  val slice: Slice? = null,
  val givenStage: GivenStage? = null,
  val whenStage: WhenStage? = null,
  val thenStage: ThenStage? = null
)
