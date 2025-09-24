package io.holixon.emn.generation

import io.github.oshai.kotlinlogging.KLogger
import io.holixon.emn.model.FlowElementType


fun KLogger.noAggregateFoundLogger(emnElementType: FlowElementType) = {
  this.info { "No aggregate found for ${emnElementType.name}" }
}

fun KLogger.conflictingAggregatesFound(emnElementType: FlowElementType) = {
  this.warn { "Found conflicting EMN declaration, elements of type ${emnElementType.name} belong to different aggregate lanes." }
}
