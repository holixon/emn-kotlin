package io.holixon.emn.generation.schema

import _ktx.ResourceKtx.resourceUrl
import io.holixon.emn.EmnDocumentParser
import io.holixon.emn.generation.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.nio.file.Files
import kotlin.io.path.readText

class EmnSchemaKotlinGeneratorTest {

  private val parser = EmnDocumentParser()
  private val generator = EmnSchemaKotlinGenerator()

  @Test
  fun `generates structural data classes from embedded json schemas`() {
    val definitions = parser.parseDefinitions(resourceUrl("json-schema/faculty.emn"))
    val outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()

    val files = generator.generateToDirectory(
      definitions = definitions,
      rootPackageName = "io.holixon.emn.example.faculty",
      outputDirectory = outputDirectory
    )

    assertThat(files.map { it.name }).containsExactly(
      "CourseType.kt",
      "CreateCourse.kt",
      "CourseCreated.kt",
      "CourseOverviewItem.kt",
      "CourseOverview.kt"
    )

    val createCourse = files.first { it.name == "CreateCourse.kt" }.toPath().readText()
    assertThat(createCourse).contains("package io.holixon.emn.example.faculty.command")
    assertThat(createCourse).contains("import io.holixon.emn.example.faculty.common.CourseType")
    assertThat(createCourse).contains("data class CreateCourse(")
    assertThat(createCourse).contains("import java.time.OffsetDateTime")
    assertThat(createCourse).contains("import java.util.UUID")
    assertThat(createCourse).contains("val courseType: CourseType")
    assertThat(createCourse).contains("val courseId: UUID")
    assertThat(createCourse).contains("val name: String")
    assertThat(createCourse).contains("val capacity: Long? = null")
    assertThat(createCourse).contains("val createdAt: OffsetDateTime? = null")

    val courseType = files.first { it.name == "CourseType.kt" }.toPath().readText()
    assertThat(courseType).contains("package io.holixon.emn.example.faculty.common")
    assertThat(courseType).contains("enum class CourseType")
    assertThat(courseType).contains("LIVE")
    assertThat(courseType).contains("REMOTE")

    val courseCreated = files.first { it.name == "CourseCreated.kt" }.toPath().readText()
    assertThat(courseCreated).contains("package io.holixon.emn.example.faculty.event")
    assertThat(courseCreated).contains("import io.holixon.emn.example.faculty.common.CourseType")
  }

  @Test
  fun `generates top level array query as an alias to its named item schema`() {
    val definitions = parser.parseDefinitions(resourceUrl("json-schema/faculty.emn"))
    val outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()

    val files = generator.generateToDirectory(
      definitions = definitions,
      rootPackageName = "io.holixon.emn.example.faculty",
      outputDirectory = outputDirectory
    )

    val courseOverview = files.first { it.name == "CourseOverview.kt" }.toPath().readText()
    assertThat(courseOverview).contains("package io.holixon.emn.example.faculty.query")
    assertThat(courseOverview).contains("typealias CourseOverview = List<CourseOverviewItem>")
    assertThat(courseOverview).doesNotContain("import io.holixon.emn.example.faculty.common.CourseOverviewItem")
    assertThat(courseOverview).doesNotContain("class CourseOverview")

    val item = files.first { it.name == "CourseOverviewItem.kt" }.toPath().readText()
    assertThat(item).contains("package io.holixon.emn.example.faculty.query")
    assertThat(item).contains("data class CourseOverviewItem(")
    assertThat(item).contains("val courseId: UUID")
    assertThat(item).contains("val name: String")
    assertThat(item).contains("val courseType: CourseType")
    assertThat(item).contains("val createdAt: OffsetDateTime? = null")
    assertThat(item).contains("val students: Long? = null")
  }

  @Test
  fun `generates List alias for top level arrays referencing a common object schema`() {
    val commonSchema = EmnSchemaDefinition(
      typeId = "GameDto",
      typeKind = EmnSchemaTypeKind.SCHEMA_DEFINITION,
      schemaFormat = "application/schema+json",
      schemaContent = """{"type":"object","properties":{"gameId":{"type":"string"}},"required":["gameId"]}"""
    )
    val querySchema = EmnSchemaDefinition(
      typeId = "SearchGames",
      typeKind = EmnSchemaTypeKind.QUERY,
      schemaFormat = "application/schema+json",
      schemaContent = """{"type":"array","items":{"${'$'}ref":"GameDto"}}"""
    )
    val outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()

    val files = generator.generateToDirectory(
      schemas = listOf(commonSchema, querySchema),
      rootPackageName = "io.holixon.emn.example.quizmania",
      outputDirectory = outputDirectory
    )

    val searchGames = files.first { it.name == "SearchGames.kt" }.toPath().readText()
    assertThat(searchGames).contains("package io.holixon.emn.example.quizmania.query")
    assertThat(searchGames).contains("import io.holixon.emn.example.quizmania.common.GameDto")
    assertThat(searchGames).contains("typealias SearchGames = List<GameDto>")
    assertThat(searchGames).doesNotContain("class SearchGames")
  }

  @Test
  fun `generates Set alias for unique top level arrays`() {
    val generated = generateSchemas(
      schema("GameDto", """{"type":"object","properties":{"gameId":{"type":"string"}}}"""),
      schema(
        "SearchGames",
        """{"type":"array","uniqueItems":true,"items":{"${'$'}ref":"GameDto"}}""",
        EmnSchemaTypeKind.QUERY
      )
    )

    assertThat(generated.getValue("SearchGames.kt")).contains("typealias SearchGames = Set<GameDto>")
  }

  @Test
  fun `generates an item class and alias for top level arrays with inline object items`() {
    val generated = generateSchemas(
      schema("GameId", """{"type":"string","format":"uuid"}"""),
      schema(
        "StartRoundAutomationQuery",
        """{"type":"array","items":{"type":"object","properties":{"gameId":{"${'$'}ref":"GameId"}},"required":["gameId"]}}""",
        EmnSchemaTypeKind.QUERY
      )
    )

    assertThat(generated.getValue("StartRoundAutomationQueryItem.kt")).contains(
      "data class StartRoundAutomationQueryItem(",
      "val gameId: GameId"
    )
    assertThat(generated.getValue("StartRoundAutomationQuery.kt"))
      .contains("typealias StartRoundAutomationQuery = List<StartRoundAutomationQueryItem>")
  }

  @Test
  fun `rejects unsupported top level array item schemas clearly`() {
    val schema = schema("Names", """{"type":"array","items":{"type":"string"}}""", EmnSchemaTypeKind.QUERY)

    assertThatThrownBy {
      generator.generateToDirectory(
        schemas = listOf(schema),
        rootPackageName = "io.holixon.emn.example.quizmania",
        outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()
      )
    }.hasMessageContaining("Only arrays whose items use a named local \$ref are supported")
  }

  @Test
  fun `rejects invalid TypeIds`() {
    val schema = EmnSchemaDefinition(
      typeId = "123 Course",
      typeKind = EmnSchemaTypeKind.COMMAND,
      schemaFormat = "application/schema+json",
      schemaContent = """{"type":"object","properties":{"courseId":{"type":"string"}}}"""
    )

    assertThatThrownBy {
      generator.generateToDirectory(
        schemas = listOf(schema),
        rootPackageName = "io.holixon.emn.example.faculty",
        outputDirectory = TestFixtures.createGeneratedSourcesDir().toFile()
      )
    }.hasMessageContaining("Invalid Kotlin class name")
  }

  @Test
  fun `rejects entity id hint on non command schemas`() {
    val schema = EmnSchemaDefinition(
      typeId = "CourseCreated",
      typeKind = EmnSchemaTypeKind.EVENT,
      schemaFormat = "application/schema+json",
      schemaContent = """{"type":"object","properties":{"courseId":{"type":"string","x-emn-role":"entity-id"}}}"""
    )

    assertThatThrownBy {
      generator.generateToDirectory(
        schemas = listOf(schema),
        rootPackageName = "io.holixon.emn.example.faculty",
        outputDirectory = TestFixtures.createGeneratedSourcesDir().toFile()
      )
    }.hasMessageContaining("x-emn-role: entity-id is only supported on command schemas")
  }

  @Test
  fun `rejects tag key hint on non event schemas`() {
    val schema = EmnSchemaDefinition(
      typeId = "CreateCourse",
      typeKind = EmnSchemaTypeKind.COMMAND,
      schemaFormat = "application/schema+json",
      schemaContent = """{"type":"object","properties":{"courseId":{"type":"string","x-emn-tag-key":"course"}}}"""
    )

    assertThatThrownBy {
      generator.generateToDirectory(
        schemas = listOf(schema),
        rootPackageName = "io.holixon.emn.example.faculty",
        outputDirectory = TestFixtures.createGeneratedSourcesDir().toFile()
      )
    }.hasMessageContaining("x-emn-tag-key is only supported on event schemas")
  }

  @Test
  fun `adds axon5 annotations from neutral json schema hints`() {
    val definitions = parser.parseDefinitions(resourceUrl("json-schema/faculty.emn"))
    val outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()
    val axonGenerator = EmnSchemaKotlinGenerator(
      annotationEnhancers = listOf(Axon5KotlinSchemaAnnotationEnhancer())
    )

    val files = axonGenerator.generateToDirectory(
      definitions = definitions,
      rootPackageName = "io.holixon.emn.example.faculty",
      outputDirectory = outputDirectory
    )

    val createCourse = files.first { it.name == "CreateCourse.kt" }.toPath().readText()
    assertThat(createCourse).contains("import org.axonframework.modelling.annotation.TargetEntityId")
    assertThat(createCourse).contains("@TargetEntityId\nval courseId: UUID")

    val courseCreated = files.first { it.name == "CourseCreated.kt" }.toPath().readText()
    assertThat(courseCreated).contains("import org.axonframework.eventsourcing.annotation.EventTag")
    assertThat(courseCreated).contains("@EventTag(key = \"course\")\nval courseId: UUID")
  }

  @Test
  fun `adds jackson polymorphism annotations from discriminator mappings`() {
    val definitions = parser.parseTypeDefinitions(resourceUrl("quizmania/quizmania-blueprint.emn"))
    val outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()
    val jacksonGenerator = EmnSchemaKotlinGenerator(
      annotationEnhancers = listOf(JacksonKotlinSchemaAnnotationEnhancer())
    )

    val files = jacksonGenerator.generateToDirectory(
      definitions = definitions,
      rootPackageName = "io.holixon.emn.example.quizmania",
      outputDirectory = outputDirectory
    )

    val question = files.first { it.name == "Question.kt" }.toPath().readText()
    assertThat(files).noneMatch { it.name == "QuestionBase.kt" }
    assertThat(question).contains("import com.fasterxml.jackson.annotation.JsonSubTypes")
    assertThat(question).contains("import com.fasterxml.jackson.annotation.JsonTypeInfo")
    assertThat(question).contains("use = JsonTypeInfo.Id.NAME")
    assertThat(question).contains("include = JsonTypeInfo.As.EXISTING_PROPERTY")
    assertThat(question).contains("property = \"type\"")
    assertThat(question).contains("visible = true")
    assertThat(question).contains("open class Question(")
    assertThat(question).contains("val type: QuestionType", "val phrase: String", "val correctAnswer: String")
    assertThat(question).contains(
      "JsonSubTypes.Type(value = ChoiceQuestion::class, name = \"CHOICE\")",
      "JsonSubTypes.Type(value = MultipleChoiceQuestion::class, name = \"MULTIPLE_CHOICE\")",
      "JsonSubTypes.Type(value = FreeInputQuestion::class, name = \"FREE_INPUT\")",
      "JsonSubTypes.Type(value = EstimateQuestion::class, name = \"ESTIMATE\")",
      "JsonSubTypes.Type(value = SortQuestion::class, name = \"SORT\")"
    )

    val choiceQuestion = files.first { it.name == "ChoiceQuestion.kt" }.toPath().readText()
    assertThat(choiceQuestion).contains(": Question(type, id, phrase, imagePath, answerImagePath, correctAnswer)")
    assertThat(choiceQuestion).doesNotContain(", Question")
  }

  @Test
  fun `jackson enhancer imports mapped subtypes from another generated package`() {
    val message = schema(
      "Message",
      """{"type":"object","properties":{"kind":{"type":"string"}},"required":["kind"],"discriminator":{"propertyName":"kind","mapping":{"CREATED":"CreatedEvent"}}}"""
    )
    val createdEvent = schema(
      "CreatedEvent",
      """{"allOf":[{"${'$'}ref":"Message"},{"type":"object","properties":{"kind":{"const":"CREATED"}}}]}""",
      EmnSchemaTypeKind.EVENT
    )
    val outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()
    val jacksonGenerator = EmnSchemaKotlinGenerator(
      annotationEnhancers = listOf(JacksonKotlinSchemaAnnotationEnhancer())
    )

    val files = jacksonGenerator.generateToDirectory(
      schemas = listOf(message, createdEvent),
      rootPackageName = "io.holixon.emn.example.messages",
      outputDirectory = outputDirectory
    )

    assertThat(files.first { it.name == "Message.kt" }.readText()).contains(
      "package io.holixon.emn.example.messages.common",
      "import io.holixon.emn.example.messages.event.CreatedEvent",
      "JsonSubTypes.Type(value = CreatedEvent::class, name = \"CREATED\")"
    )
    assertThat(files.first { it.name == "CreatedEvent.kt" }.readText()).contains(
      "package io.holixon.emn.example.messages.event",
      "import io.holixon.emn.example.messages.common.Message",
      ": Message(kind)"
    )
  }

  @Test
  fun `adds target entity id to a common schema inherited only by commands`() {
    val gameCommand = EmnSchemaDefinition(
      typeId = "GameCommand",
      typeKind = EmnSchemaTypeKind.SCHEMA_DEFINITION,
      schemaFormat = "application/schema+json",
      schemaContent = """{"type":"object","properties":{"gameId":{"type":"string","format":"uuid","x-emn-role":"entity-id"}},"required":["gameId"]}"""
    )
    val evaluateBuzzes = EmnSchemaDefinition(
      typeId = "EvaluateBuzzesCommand",
      typeKind = EmnSchemaTypeKind.COMMAND,
      schemaFormat = "application/schema+json",
      schemaContent = """{"allOf":[{"${'$'}ref":"GameCommand"},{"type":"object","properties":{"windowId":{"type":"string","format":"uuid"}},"required":["windowId"]}]}"""
    )
    val outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()
    val axonGenerator = EmnSchemaKotlinGenerator(
      annotationEnhancers = listOf(Axon5KotlinSchemaAnnotationEnhancer())
    )

    val files = axonGenerator.generateToDirectory(
      schemas = listOf(gameCommand, evaluateBuzzes),
      rootPackageName = "io.holixon.emn.example.quizmania",
      outputDirectory = outputDirectory
    )

    val generatedBase = files.first { it.name == "GameCommand.kt" }.toPath().readText()
    assertThat(generatedBase).contains("import org.axonframework.modelling.annotation.TargetEntityId")
    assertThat(generatedBase).contains("@TargetEntityId\nval gameId: UUID")

    val generatedCommand = files.first { it.name == "EvaluateBuzzesCommand.kt" }.toPath().readText()
    assertThat(generatedCommand).contains(
      "package io.holixon.emn.example.quizmania.command",
      "import io.holixon.emn.example.quizmania.common.GameCommand"
    )
    assertThat(generatedCommand).contains(": GameCommand(gameId)")
  }

  @Test
  fun `rejects entity id on a common schema inherited by an event`() {
    val gameMessage = EmnSchemaDefinition(
      typeId = "GameMessage",
      typeKind = EmnSchemaTypeKind.SCHEMA_DEFINITION,
      schemaFormat = "application/schema+json",
      schemaContent = """{"type":"object","properties":{"gameId":{"type":"string","x-emn-role":"entity-id"}}}"""
    )
    val gameCreated = EmnSchemaDefinition(
      typeId = "GameCreatedEvent",
      typeKind = EmnSchemaTypeKind.EVENT,
      schemaFormat = "application/schema+json",
      schemaContent = """{"allOf":[{"${'$'}ref":"GameMessage"},{"type":"object","properties":{"name":{"type":"string"}}}]}"""
    )

    assertThatThrownBy {
      generator.generateToDirectory(
        schemas = listOf(gameMessage, gameCreated),
        rootPackageName = "io.holixon.emn.example.quizmania",
        outputDirectory = TestFixtures.createGeneratedSourcesDir().toFile()
      )
    }.hasMessageContaining("x-emn-role: entity-id is only supported on command schemas")
  }

  @Test
  fun `generates common schemas referenced from faculty json schema diagram`() {
    val definitions = parser.parseDefinitions(resourceUrl("faculty-json-schema/faculty.emn"))
    val outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()

    val files = generator.generateToDirectory(
      definitions = definitions,
      rootPackageName = "io.holixon.emn.example.faculty",
      outputDirectory = outputDirectory
    )

    assertThat(files.map { it.name }).contains(
      "CommandType_create_course.kt",
      "EventType_course_created.kt",
      "QueryType_courses.kt",
      "CommandType_enroll_student.kt",
      "EventType_student_enrolled.kt",
      "QueryType_students.kt",
      "CommandType_rename_course.kt",
      "EventType_course_renamed.kt"
    )
    val createCourse = files.first { it.name == "CommandType_create_course.kt" }.toPath().readText()
    assertThat(createCourse).contains("val courseId: CourseId")
    assertThat(createCourse).contains("val name: String")
    assertThat(createCourse).contains("val capacity: Long")

    val courses = files.first { it.name == "QueryType_courses.kt" }.toPath().readText()
    assertThat(courses).contains("typealias QueryType_courses = List<QueryType_coursesItem>")
    val courseItem = files.first { it.name == "QueryType_coursesItem.kt" }.toPath().readText()
    assertThat(courseItem).contains("data class QueryType_coursesItem(")
    assertThat(courseItem).contains("val courseId: CourseId")
  }

  @Test
  fun `generates named scalar schemas as type aliases`() {
    val generated = generateSchemas(
      schema("CourseCode", """{"type":"string"}"""),
      schema("GameId", """{"type":"string","format":"uuid"}"""),
      schema("GameRoundNumber", """{"type":"integer","format":"int32"}"""),
      schema("SequenceNumber", """{"type":"integer"}"""),
      schema("Score", """{"type":"number"}"""),
      schema("Enabled", """{"type":"boolean"}""")
    )

    assertThat(generated.getValue("CourseCode.kt")).contains("typealias CourseCode = String")
    assertThat(generated.getValue("GameId.kt"))
      .contains("import java.util.UUID", "typealias GameId = UUID")
    assertThat(generated.getValue("GameRoundNumber.kt")).contains("typealias GameRoundNumber = Int")
    assertThat(generated.getValue("SequenceNumber.kt")).contains("typealias SequenceNumber = Long")
    assertThat(generated.getValue("Score.kt"))
      .contains("import java.math.BigDecimal", "typealias Score = BigDecimal")
    assertThat(generated.getValue("Enabled.kt")).contains("typealias Enabled = Boolean")
  }

  @Test
  fun `keeps scalar aliases in generated object properties`() {
    val generated = generateSchemas(
      schema("GameId", """{"type":"string","format":"uuid"}"""),
      schema("GameRoundNumber", """{"type":"integer","format":"int32"}"""),
      schema(
        "RoundStarted",
        """{"type":"object","properties":{"gameId":{"${'$'}ref":"GameId"},"roundNumber":{"${'$'}ref":"GameRoundNumber"}},"required":["gameId","roundNumber"]}""",
        EmnSchemaTypeKind.EVENT
      )
    )

    assertThat(generated.getValue("RoundStarted.kt"))
      .contains("val gameId: GameId", "val roundNumber: GameRoundNumber")
  }

  @Test
  fun `generates quizmania domain aliases and preserves their references`() {
    val definitions = parser.parseTypeDefinitions(resourceUrl("quizmania/quizmania-blueprint.emn"))
    val outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()

    val files = generator.generateToDirectory(
      definitions = definitions,
      rootPackageName = "io.holixon.emn.example.quizmania",
      outputDirectory = outputDirectory
    ).associate { it.name to it.readText() }

    listOf("GameId", "GameQuestionId", "GamePlayerId", "GameRoundId").forEach { typeId ->
      assertThat(files.getValue("$typeId.kt")).contains("typealias $typeId = UUID")
    }
    assertThat(files.getValue("GameRoundNumber.kt")).contains("typealias GameRoundNumber = Int")
    assertThat(files.getValue("RoundQuestionNumber.kt")).contains("typealias RoundQuestionNumber = Int")
    assertThat(files.getValue("GameCommand.kt")).contains("val gameId: GameId")
    assertThat(files.getValue("RoundStartedEvent.kt")).contains("roundNumber: GameRoundNumber")
    assertThat(files.getValue("QuestionAskedEvent.kt")).contains("roundQuestionNumber: RoundQuestionNumber")
    assertThat(files.getValue("GamePlayerScores.kt"))
      .contains("typealias GamePlayerScores = Map<GamePlayerId, Int>")
    assertThat(files.getValue("QuestionScoredEvent.kt")).contains(
      "package io.holixon.emn.example.quizmania.event",
      "import io.holixon.emn.example.quizmania.common.GamePlayerScores",
      "val points: GamePlayerScores"
    )
    assertThat(files.getValue("SearchGames.kt")).contains(
      "package io.holixon.emn.example.quizmania.query",
      "import io.holixon.emn.example.quizmania.common.GameDto",
      "typealias SearchGames = List<GameDto>"
    )
  }

  @Test
  fun `generates dictionary alias with default string keys and long values`() {
    val generated = generateSchemas(
      schema("Scores", """{"type":"object","additionalProperties":{"type":"integer"}}""")
    )

    assertThat(generated.getValue("Scores.kt")).contains("typealias Scores = Map<String, Long>")
  }

  @Test
  fun `generates dictionary alias with UUID keys from propertyNames format`() {
    val generated = generateSchemas(
      schema(
        "Scores",
        """{"type":"object","propertyNames":{"type":"string","format":"uuid"},"additionalProperties":{"type":"string"}}"""
      )
    )

    assertThat(generated.getValue("Scores.kt"))
      .contains("import java.util.UUID", "typealias Scores = Map<UUID, String>")
  }

  @Test
  fun `keeps UUID backed scalar alias as dictionary key type`() {
    val generated = generateSchemas(
      schema("PlayerId", """{"type":"string","format":"uuid"}"""),
      schema(
        "Scores",
        """{"type":"object","propertyNames":{"${'$'}ref":"PlayerId"},"additionalProperties":{"type":"integer"}}"""
      )
    )

    assertThat(generated.getValue("Scores.kt"))
      .contains("typealias Scores = Map<PlayerId, Long>")
  }

  @Test
  fun `maps int32 dictionary values to Int`() {
    val generated = generateSchemas(
      schema(
        "Scores",
        """{"type":"object","additionalProperties":{"type":"integer","format":"int32"}}"""
      )
    )

    assertThat(generated.getValue("Scores.kt")).contains("typealias Scores = Map<String, Int>")
  }

  @Test
  fun `keeps references to generated dictionary aliases`() {
    val generated = generateSchemas(
      schema("Scores", """{"type":"object","additionalProperties":{"type":"integer"}}"""),
      schema(
        "ScoreEvent",
        """{"type":"object","properties":{"points":{"${'$'}ref":"Scores"}},"required":["points"]}""",
        EmnSchemaTypeKind.EVENT
      )
    )

    assertThat(generated.getValue("Scores.kt")).contains("typealias Scores = Map<String, Long>")
    assertThat(generated.getValue("ScoreEvent.kt")).contains("val points: Scores")
  }

  @Test
  fun `does not treat regular objects with declared properties as dictionaries`() {
    val generated = generateSchemas(
      schema(
        "Player",
        """{"type":"object","properties":{"name":{"type":"string"}},"additionalProperties":{"type":"integer"},"required":["name"]}"""
      )
    )

    assertThat(generated.getValue("Player.kt")).contains("data class Player(", "val name: String")
    assertThat(generated.getValue("Player.kt")).doesNotContain("typealias Player")
  }

  private fun generateSchemas(vararg schemas: EmnSchemaDefinition): Map<String, String> {
    val files = generator.generateToDirectory(
      schemas = schemas.toList(),
      rootPackageName = "io.holixon.emn.example.dictionary",
      outputDirectory = Files.createTempDirectory("emn-schema-kotlin-test-").toFile()
    )
    return files.associate { it.name to it.readText() }
  }

  private fun schema(
    typeId: String,
    content: String,
    typeKind: EmnSchemaTypeKind = EmnSchemaTypeKind.SCHEMA_DEFINITION
  ) = EmnSchemaDefinition(
    typeId = typeId,
    typeKind = typeKind,
    schemaFormat = "application/schema+json",
    schemaContent = content
  )
}
