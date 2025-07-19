package io.holixon.emn.model

data class Entity(
  val id: String,
  val name : String,
  override val schema: Schema? = null,
) : WithSchema
