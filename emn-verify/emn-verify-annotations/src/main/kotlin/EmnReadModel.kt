package io.holixon.emn.verify.annotation

/** Associates a read-model component with one or more EMN query type IDs. */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EmnReadModel(vararg val queryTypeIds: String)
