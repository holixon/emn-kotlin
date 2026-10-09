package io.holixon.emn.verify.axon5

import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.verify.ActualCodeModel
import io.holixon.emn.verify.VerificationReport
import io.holixon.emn.verify.EmnVerifier
import io.holixon.emn.verify.ExpectedEmnModel
import io.holixon.emn.verify.ExpectedEmnModelFactory
import java.io.File

data class Axon5EmnVerificationRequest(
  val emnFiles: Collection<File>,
  val classDirectories: Collection<File>,
)

data class Axon5EmnVerificationResult(
  val expected: ExpectedEmnModel,
  val actual: ActualCodeModel,
  val report: VerificationReport,
) {
  fun summaryLines(): List<String> {
    val actualEventHandlers = actual.readModels.flatMap { it.eventHandlers }
    return listOf(
      "EMN model: ${expected.commandTypeIds.size} command(s), ${expected.eventTypeIds.size} event(s), " +
        "${expected.queryTypeIds.size} query type(s), ${expected.specificationIds.size} specification(s).",
      "Code inspection: ${actual.commandHandlers.size} command handler(s), ${actualEventHandlers.size} event handler(s), " +
        "${actual.readModels.size} read model(s), ${actual.specifications.size} specification implementation(s).",
    )
  }

  fun debugLines(): List<String> = buildList {
    expected.commandTypeIds.sorted().forEach { id ->
      add(mapping("COMMAND", id, actual.commandHandlers.filter { it.commandTypeId == id }.map { it.location }))
    }
    expected.consumedEventTypeIdsByQuery.values.flatten().toSortedSet().forEach { id ->
      add(mapping("EVENT", id, actual.readModels.flatMap { readModel ->
        readModel.eventHandlers.filter { it.eventTypeId == id }.map { it.location }
      }))
    }
    expected.queryTypeIds.sorted().forEach { id ->
      add(mapping("QUERY", id, actual.readModels.filter { id in it.queryTypeIds }.map { it.location }))
    }
    expected.specificationIds.sorted().forEach { id ->
      add(mapping("SPECIFICATION", id, actual.specifications.filter { it.specificationId == id }.map { it.location }))
    }
  }

  private fun mapping(kind: String, id: String, locations: Collection<String>): String =
    "$kind $id -> ${locations.distinct().sorted().ifEmpty { listOf("<missing>") }.joinToString()}"
}

class Axon5EmnVerificationService(
  private val parser: EmnDocumentParser = EmnDocumentParser(),
  private val inspector: Axon5CodeModelInspector = Axon5CodeModelInspector(),
) {

  fun verify(request: Axon5EmnVerificationRequest): VerificationReport = evaluate(request).report

  fun evaluate(request: Axon5EmnVerificationRequest): Axon5EmnVerificationResult {
    require(request.emnFiles.isNotEmpty()) { "At least one EMN file is required." }
    val missingEmnFiles = request.emnFiles.filterNot(File::isFile)
    require(missingEmnFiles.isEmpty()) {
      "Configured EMN file(s) do not exist: ${missingEmnFiles.joinToString { it.path }}"
    }
    val expectedModels = request.emnFiles.sortedBy { it.path }
      .map { ExpectedEmnModelFactory.create(parser.parseDefinitions(it)) }
    val expected = merge(expectedModels)
    val actual = inspector.inspect(request.classDirectories.map { it.toPath() })
    return Axon5EmnVerificationResult(
      expected = expected,
      actual = actual,
      report = EmnVerifier.verify(expected, actual),
    )
  }

  private fun merge(models: List<ExpectedEmnModel>): ExpectedEmnModel = ExpectedEmnModel(
    commandTypeIds = models.flatMapTo(sortedSetOf()) { it.commandTypeIds },
    eventTypeIds = models.flatMapTo(sortedSetOf()) { it.eventTypeIds },
    queryTypeIds = models.flatMapTo(sortedSetOf()) { it.queryTypeIds },
    consumedEventTypeIdsByQuery = models
      .flatMap { it.consumedEventTypeIdsByQuery.entries }
      .groupBy({ it.key }, { it.value })
      .mapValues { (_, eventSets) -> eventSets.flatten().toSortedSet() }
      .toSortedMap(),
    specificationIds = models.flatMapTo(sortedSetOf()) { it.specificationIds },
  )
}
