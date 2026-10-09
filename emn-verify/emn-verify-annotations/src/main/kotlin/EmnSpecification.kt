package io.holixon.emn.verify.annotation

/** Links a class or test function to an EMN specification ID. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class EmnSpecification(val id: String)
