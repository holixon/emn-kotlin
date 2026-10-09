package io.holixon.emn.verify.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class EmnVerifyGradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions.create(EXTENSION_NAME, EmnVerifyExtension::class.java).apply {
      failOnWarnings.convention(false)
      classDirectories.from(
        project.layout.buildDirectory.dir("classes/kotlin/main"),
        project.layout.buildDirectory.dir("classes/java/main"),
        project.layout.buildDirectory.dir("classes/kotlin/test"),
        project.layout.buildDirectory.dir("classes/java/test"),
      )
    }
    val verifyTask = project.tasks.register(VERIFY_TASK_NAME, VerifyEmnTask::class.java) { task ->
      task.group = "verification"
      task.description = "Verifies Axon 5 handlers and specifications against EMN models."
      task.emnFiles.from(extension.emnFiles)
      task.classDirectories.from(extension.classDirectories)
      task.failOnWarnings.convention(extension.failOnWarnings)
      task.ignoredIssueCodes.convention(project.provider {
        extension.ignoredIssues.rules().map { it.code.name }
      })
      task.ignoredIssueElementIds.convention(project.provider {
        extension.ignoredIssues.rules().map { it.elementId }
      })
      task.ignoredIssueReasons.convention(project.provider {
        extension.ignoredIssues.rules().map { it.reason.orEmpty() }
      })
      task.dependsOn(project.tasks.matching { it.name == "classes" || it.name == "testClasses" })
    }

    project.tasks.matching { it.name == "check" }.configureEach { check ->
      check.dependsOn(verifyTask)
    }
  }

  companion object {
    const val PLUGIN_ID = "io.holixon.emn.verify"
    const val EXTENSION_NAME = "emnVerify"
    const val VERIFY_TASK_NAME = "verifyEmn"
  }
}
