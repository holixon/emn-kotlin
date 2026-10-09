package io.holixon.emn.example.quizmania

import io.holixon.emn.example.quizmania.command.EvaluateBuzzesCommand
import io.holixon.emn.example.quizmania.common.GameCommand
import io.holixon.emn.example.quizmania.common.GameEvent
import io.holixon.emn.example.quizmania.event.GameStartedEvent
import io.holixon.emn.example.quizmania.event.QuestionScoredEvent
import io.holixon.emn.example.quizmania.query.SearchGames
import java.util.UUID
import kotlin.reflect.KClass

fun generatedCommandType(): KClass<out GameCommand> = EvaluateBuzzesCommand::class

fun generatedEventType(): KClass<out GameEvent> = GameStartedEvent::class

fun generatedSearchGames(): SearchGames = emptyList()

fun generatedScoreEvent(
  gameId: UUID,
  gameQuestionId: UUID,
  playerId: UUID,
): QuestionScoredEvent = QuestionScoredEvent(
  gameId = gameId,
  gameQuestionId = gameQuestionId,
  points = mapOf(playerId to 100),
)
