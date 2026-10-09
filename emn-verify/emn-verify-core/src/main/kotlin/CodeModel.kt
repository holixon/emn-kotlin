package io.holixon.emn.verify

data class ActualCommandHandler(
  val commandTypeId: String,
  val location: String,
)

data class ActualEventHandler(
  val eventTypeId: String,
  val location: String,
)

data class ActualReadModel(
  val queryTypeIds: Set<String>,
  val consumedEventTypeIds: Set<String>,
  val location: String,
  val eventHandlers: List<ActualEventHandler> = emptyList(),
)

data class ActualSpecification(
  val specificationId: String,
  val location: String,
)

data class ActualCodeModel(
  val commandHandlers: List<ActualCommandHandler> = emptyList(),
  val readModels: List<ActualReadModel> = emptyList(),
  val specifications: List<ActualSpecification> = emptyList(),
)
