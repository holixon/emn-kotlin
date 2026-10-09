package io.holixon.emn.model

data class VariableDefinition(
  val name: String,
  val value: String,
)

data class Variable(
  val name: String,
  val value: String,
)

data class EventSequenceDefinition(
  val id: String,
  val typeReference: EventSequenceType,
  val variableDefinitions: List<VariableDefinition>,
  val events: List<Event>,
)
