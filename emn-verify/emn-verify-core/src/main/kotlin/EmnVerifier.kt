package io.holixon.emn.verify

object EmnVerifier {

  fun verify(expected: ExpectedEmnModel, actual: ActualCodeModel): VerificationReport {
    val issues = mutableListOf<VerificationIssue>()
    verifyCommandHandlers(expected, actual, issues)
    verifyReadModels(expected, actual, issues)
    verifySpecifications(expected, actual, issues)
    return VerificationReport(issues.sortedWith(compareBy({ it.severity }, { it.code }, { it.emnElementId }, { it.codeLocation })))
  }

  private fun verifyCommandHandlers(
    expected: ExpectedEmnModel,
    actual: ActualCodeModel,
    issues: MutableList<VerificationIssue>,
  ) {
    val handlersByCommand = actual.commandHandlers.groupBy { it.commandTypeId }
    expected.commandTypeIds.forEach { commandTypeId ->
      val handlers = handlersByCommand[commandTypeId].orEmpty()
      when {
        handlers.isEmpty() -> issues += error(
          VerificationIssueCode.MISSING_COMMAND_HANDLER,
          "Command type '$commandTypeId' has no command handler.",
          commandTypeId,
        )
        handlers.size > 1 -> issues += error(
          VerificationIssueCode.DUPLICATE_COMMAND_HANDLER,
          "Command type '$commandTypeId' has ${handlers.size} command handlers: ${handlers.joinToString { it.location }}.",
          commandTypeId,
        )
      }
    }
  }

  private fun verifyReadModels(
    expected: ExpectedEmnModel,
    actual: ActualCodeModel,
    issues: MutableList<VerificationIssue>,
  ) {
    actual.readModels.forEach { readModel ->
      readModel.queryTypeIds.filterNot(expected.queryTypeIds::contains).forEach { queryTypeId ->
        issues += error(
          VerificationIssueCode.UNKNOWN_QUERY_TYPE,
          "Read model '${readModel.location}' references unknown EMN query type '$queryTypeId'.",
          queryTypeId,
          readModel.location,
        )
      }

      val modeledEvents = readModel.queryTypeIds
        .flatMapTo(sortedSetOf()) { expected.consumedEventTypeIdsByQuery[it].orEmpty() }
      (readModel.consumedEventTypeIds - modeledEvents).sorted().forEach { eventTypeId ->
        issues += warning(
          VerificationIssueCode.UNMODELED_EVENT_HANDLER,
          "Read model '${readModel.location}' consumes event '$eventTypeId', but that event is not connected to any of its query types ${readModel.queryTypeIds.sorted()} in EMN.",
          eventTypeId,
          readModel.location,
        )
      }
    }

    expected.consumedEventTypeIdsByQuery.forEach { (queryTypeId, expectedEvents) ->
      val matchingReadModels = actual.readModels.filter { queryTypeId in it.queryTypeIds }
      if (matchingReadModels.isEmpty()) {
        issues += error(
          VerificationIssueCode.MISSING_READ_MODEL,
          "Query type '$queryTypeId' consumes events in EMN but has no associated @EmnReadModel.",
          queryTypeId,
        )
      } else {
        val consumedEvents = matchingReadModels.flatMapTo(mutableSetOf()) { it.consumedEventTypeIds }
        (expectedEvents - consumedEvents).sorted().forEach { eventTypeId ->
          issues += error(
            VerificationIssueCode.MISSING_EVENT_HANDLER,
            "Query type '$queryTypeId' consumes event '$eventTypeId' in EMN, but none of its associated read models handles it.",
            queryTypeId,
          )
        }
      }
    }
  }

  private fun verifySpecifications(
    expected: ExpectedEmnModel,
    actual: ActualCodeModel,
    issues: MutableList<VerificationIssue>,
  ) {
    val implementationsById = actual.specifications.groupBy { it.specificationId }
    expected.specificationIds.forEach { specificationId ->
      val implementations = implementationsById[specificationId].orEmpty()
      when {
        implementations.isEmpty() -> issues += error(
          VerificationIssueCode.MISSING_SPECIFICATION,
          "Specification '$specificationId' has no test annotated with @EmnSpecification.",
          specificationId,
        )
        implementations.size > 1 -> issues += error(
          VerificationIssueCode.DUPLICATE_SPECIFICATION,
          "Specification '$specificationId' is implemented ${implementations.size} times: ${implementations.joinToString { it.location }}.",
          specificationId,
        )
      }
    }
    (implementationsById.keys - expected.specificationIds).sorted().forEach { specificationId ->
      implementationsById.getValue(specificationId).forEach { implementation ->
        issues += error(
          VerificationIssueCode.UNKNOWN_SPECIFICATION,
          "Test '${implementation.location}' references unknown EMN specification '$specificationId'.",
          specificationId,
          implementation.location,
        )
      }
    }
  }

  private fun error(
    code: VerificationIssueCode,
    message: String,
    emnElementId: String? = null,
    codeLocation: String? = null,
  ) = VerificationIssue(VerificationSeverity.ERROR, code, message, emnElementId, codeLocation)

  private fun warning(
    code: VerificationIssueCode,
    message: String,
    emnElementId: String? = null,
    codeLocation: String? = null,
  ) = VerificationIssue(VerificationSeverity.WARNING, code, message, emnElementId, codeLocation)
}
