package io.holixon.emn.example.university.faculty.type.course

import java.util.*

data class CourseId(val value: String) {

  override fun toString(): String = "CourseId($value)"

  companion object {
    const val ENTITY_ID = "COURSE"
    fun random(): CourseId = CourseId(ENTITY_ID + ":" + UUID.randomUUID().toString())
  }
}
