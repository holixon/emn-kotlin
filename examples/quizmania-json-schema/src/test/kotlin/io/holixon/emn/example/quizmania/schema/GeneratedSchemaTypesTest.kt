package io.holixon.emn.example.quizmania

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.holixon.emn.example.quizmania.command.*
import io.holixon.emn.example.quizmania.common.*
import io.holixon.emn.example.quizmania.event.*
import io.holixon.emn.example.quizmania.query.SearchGames
import org.assertj.core.api.Assertions.assertThat
import org.axonframework.eventsourcing.annotation.EventTag
import org.axonframework.modelling.annotation.TargetEntityId
import org.junit.jupiter.api.Test
import java.io.File
import java.util.UUID

class GeneratedSchemaTypesTest {

  @Test
  fun `generated quizmania schema classes can be instantiated`() {
    val gameId = UUID.fromString("9b6ecaa8-bb6d-4d65-92e7-0d10ba9cb7ba")
    val command = CreateGameCommand(
      gameId = gameId,
      name = "Friday Quiz",
      config = GameConfig(maxPlayers = 8, questionSetId = "general-knowledge"),
      creator = "player-1",
      moderator = "player-1"
    )

    assertThat(command.gameId).isEqualTo(gameId)
    assertThat(command.config.maxPlayers).isEqualTo(8)
    assertThat(GameStatus.values()).isNotEmpty
    assertThat(Question::class.java.isInterface).isFalse()
    assertThat(Question::class.java.declaredFields.map { it.name })
      .contains("type", "id", "phrase", "correctAnswer")
  }

  @Test
  fun `all commands inherit their annotated game id`() {
    listOf(
      EvaluateBuzzesCommand::class.java,
      CreateGameCommand::class.java,
      JoinGameCommand::class.java,
      LeaveGameCommand::class.java,
      AbandonGameCommand::class.java,
      StartGameCommand::class.java,
      StartNextRoundCommand::class.java,
      ScoreRoundCommand::class.java,
      CloseRoundCommand::class.java,
      AnswerQuestionCommand::class.java,
      OverrideAnswerCommand::class.java,
      BuzzQuestionCommand::class.java,
      AnswerBuzzerQuestionCommand::class.java,
      AskNextQuestionCommand::class.java,
      CloseQuestionCommand::class.java,
      ExpireQuestionCommand::class.java,
      CompleteQuestionIfReadyCommand::class.java,
      ScoreQuestionCommand::class.java
    ).forEach { commandType ->
      assertThat(commandType.superclass).isEqualTo(GameCommand::class.java)
    }

    assertThat(GameCommand::class.java.getDeclaredField("gameId").isAnnotationPresent(TargetEntityId::class.java)).isTrue()
  }

  @Test
  fun `events use the game event hierarchy and inherited tags`() {
    val gameEvents = listOf(
      GameCreatedEvent::class.java,
      PlayerJoinedGameEvent::class.java,
      PlayerLeftGameEvent::class.java,
      GameStartedEvent::class.java,
      GameEndedEvent::class.java,
      GameCanceledEvent::class.java,
      RoundStartedEvent::class.java,
      RoundScoredEvent::class.java,
      RoundClosedEvent::class.java
    )
    val questionEvents = listOf(
      QuestionAskedEvent::class.java,
      QuestionAnsweredEvent::class.java,
      QuestionAnswerOverriddenEvent::class.java,
      QuestionBuzzedEvent::class.java,
      QuestionBuzzerWonEvent::class.java,
      BuzzerCollectionStartedEvent::class.java,
      QuestionBuzzerReopenedEvent::class.java,
      QuestionClosedEvent::class.java,
      QuestionScoredEvent::class.java
    )

    gameEvents.forEach { eventType ->
      assertThat(eventType.superclass).isEqualTo(GameEvent::class.java)
    }
    questionEvents.forEach { eventType ->
      assertThat(eventType.superclass).isEqualTo(GameQuestionEvent::class.java)
      assertThat(GameEvent::class.java.isAssignableFrom(eventType)).isTrue()
    }
    assertThat(GameQuestionEvent::class.java.superclass).isEqualTo(GameEvent::class.java)
    assertThat(GameEvent::class.java.getDeclaredField("gameId").getAnnotation(EventTag::class.java).key).isEqualTo("gameId")
    assertThat(
      GameQuestionEvent::class.java.getDeclaredField("gameQuestionId").getAnnotation(EventTag::class.java).key
    ).isEqualTo("gameQuestionId")
  }

  @Test
  fun `jackson deserializes questions using the type discriminator`() {
    val questions = jacksonObjectMapper().readValue<List<Question>>(
      """
      [
        {
          "type": "CHOICE",
          "id": "question-1",
          "phrase": "Which answer is correct?",
          "correctAnswer": "A",
          "answerOptions": ["A", "B"]
        },
        {
          "type": "FREE_INPUT",
          "id": "question-2",
          "phrase": "What is the answer?",
          "correctAnswer": "42"
        }
      ]
      """.trimIndent()
    )

    assertThat(questions).hasSize(2)
    assertThat(questions[0]).isInstanceOf(ChoiceQuestion::class.java)
    assertThat(questions[1]).isInstanceOf(FreeInputQuestion::class.java)
    assertThat((questions[0] as ChoiceQuestion).answerOptions).containsExactly("A", "B")
  }

  @Test
  fun `question scores use UUID keys and Int values`() {
    val gameId = UUID.fromString("9b6ecaa8-bb6d-4d65-92e7-0d10ba9cb7ba")
    val questionId = UUID.fromString("3cdcb2ec-e71b-49cf-863b-a865da84c9e8")
    val playerId = UUID.fromString("f0176932-68b3-4362-9036-210654889d82")

    val event = QuestionScoredEvent(
      gameId = gameId,
      gameQuestionId = questionId,
      points = mapOf(playerId to 100)
    )

    assertThat(event.points).containsEntry(playerId, 100)
  }

  @Test
  fun `search games preserves its root array payload`() {
    val games: SearchGames = emptyList()

    assertThat(games).isEmpty()
  }

  @Test
  fun `schema export contains frontend schemas and role metadata`() {
    val exportDirectory = listOf(
      File("target/generated-resources/emn-schemas"),
      File("build/generated/resources/emn-schemas")
    ).first(File::isDirectory)
    val manifest = jacksonObjectMapper().readTree(File(exportDirectory, "manifest.json"))

    val entries = manifest.path("schemas").associateBy { it.path("typeId").asText() }
    assertThat(entries.getValue("CreateGameCommand").path("kind").asText()).isEqualTo("command")
    assertThat(entries.getValue("GameCreatedEvent").path("kind").asText()).isEqualTo("event")
    assertThat(entries.getValue("SearchGames").path("kind").asText()).isEqualTo("query")
    assertThat(entries.getValue("GameDto").path("kind").asText()).isEqualTo("common")

    val searchGames = jacksonObjectMapper().readTree(File(exportDirectory, "schemas/SearchGames.schema.json"))
    assertThat(searchGames.path("type").asText()).isEqualTo("array")
    assertThat(searchGames.at("/items/${'$'}ref").asText()).isEqualTo("./GameDto.schema.json")
  }
}
