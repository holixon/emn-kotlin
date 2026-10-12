package io.holixon.emn.gradle

import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.TaskProvider

class EmnGradlePlugin : Plugin<Project> {

  override fun apply(project: Project) {
    val extension = project.extensions.create(EXTENSION_NAME, EmnExtension::class.java).apply {
      kotlin.enabled.convention(false)
      kotlin.outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/emn-kotlin"))
      kotlin.annotationEnhancers.convention(emptyList())
      specifications.enabled.convention(false)
      specifications.outputDirectory.convention(project.layout.buildDirectory.dir("generated/sources/emn-specification-tests"))
      schemaExport.enabled.convention(false)
      schemaExport.outputDirectory.convention(project.layout.buildDirectory.dir("generated/resources/emn-schemas"))
      schemaExport.sourceRoot.convention(project.projectDir.absolutePath)
      verification.enabled.convention(false)
      verification.failOnWarnings.convention(false)
      verification.classDirectories.from(
        project.layout.buildDirectory.dir("classes/kotlin/main"),
        project.layout.buildDirectory.dir("classes/java/main"),
        project.layout.buildDirectory.dir("classes/kotlin/test"),
        project.layout.buildDirectory.dir("classes/java/test"),
      )
    }

    val generateKotlin = registerKotlinGeneration(project, extension)
    val generateSpecifications = registerSpecificationGeneration(project, extension)
    val exportSchemas = registerSchemaExport(project, extension)
    val verify = registerVerification(project, extension)

    project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
      registerGeneratedKotlinSources(project, "main", generateKotlin)
      registerGeneratedKotlinSources(project, "test", generateSpecifications)
    }
    project.tasks.matching { it.name == "processResources" }.configureEach { processResources ->
      processResources.dependsOn(exportSchemas)
      (processResources as? AbstractCopyTask)?.from(exportSchemas)
    }
    project.tasks.matching { it.name == "check" }.configureEach { it.dependsOn(verify) }
  }

  private fun registerKotlinGeneration(
    project: Project,
    extension: EmnExtension,
  ): TaskProvider<GenerateEmnSchemaKotlinTask> =
    project.tasks.register(
      GENERATE_KOTLIN_TASK_NAME,
      GenerateEmnSchemaKotlinTask::class.java,
    ) { task ->
      task.group = "code generation"
      task.description = "Generates Kotlin classes from JSON Schemas embedded in EMN files."
      task.emnFiles.from(extension.emnFiles)
      task.outputDirectory.convention(extension.kotlin.outputDirectory)
      task.rootPackageName.convention(extension.rootPackageName.orElse(""))
      task.annotationEnhancers.convention(extension.kotlin.annotationEnhancers)
      task.onlyIf("emn.kotlin.enabled is true") { extension.kotlin.enabled.get() }
    }

  private fun registerSpecificationGeneration(
    project: Project,
    extension: EmnExtension,
  ): TaskProvider<GenerateEmnSpecificationTestsTask> =
    project.tasks.register(
      GENERATE_SPECIFICATIONS_TASK_NAME,
      GenerateEmnSpecificationTestsTask::class.java,
    ) { task ->
      task.group = "verification"
      task.description = "Generates Axon 5 fixture tests from EMN specifications."
      task.emnFiles.from(extension.emnFiles)
      task.outputDirectory.convention(extension.specifications.outputDirectory)
      task.rootPackageName.convention(extension.rootPackageName.orElse(""))
      task.generationEnabled.convention(extension.specifications.enabled)
      task.fixtureProvider.convention(extension.specifications.fixtureProvider)
      task.errorVerifier.convention(extension.specifications.errorVerifier)
      task.onlyIf("emn.specifications.enabled is true") { extension.specifications.enabled.get() }
    }

  private fun registerSchemaExport(
    project: Project,
    extension: EmnExtension,
  ): TaskProvider<ExportEmnSchemasTask> =
    project.tasks.register(
      EXPORT_SCHEMAS_TASK_NAME,
      ExportEmnSchemasTask::class.java,
    ) { task ->
      task.group = "code generation"
      task.description = "Exports JSON Schemas and metadata from EMN files."
      task.emnFiles.from(extension.emnFiles)
      task.schemaResourceDirectories.from(extension.schemaResourceDirectories)
      task.outputDirectory.convention(extension.schemaExport.outputDirectory)
      task.sourceRoot.convention(extension.schemaExport.sourceRoot)
      task.onlyIf("emn.schemaExport.enabled is true") { extension.schemaExport.enabled.get() }
    }

  private fun registerVerification(
    project: Project,
    extension: EmnExtension,
  ): TaskProvider<VerifyEmnTask> =
    project.tasks.register(VERIFY_TASK_NAME, VerifyEmnTask::class.java) { task ->
      task.group = "verification"
      task.description = "Verifies Axon 5 handlers and specifications against EMN models."
      task.emnFiles.from(extension.emnFiles)
      task.classDirectories.from(extension.verification.classDirectories)
      task.failOnWarnings.convention(extension.verification.failOnWarnings)
      task.ignoredIssueCodes.convention(project.provider {
        extension.verification.ignoredIssues.rules().map { it.code.name }
      })
      task.ignoredIssueElementIds.convention(project.provider {
        extension.verification.ignoredIssues.rules().map { it.elementId }
      })
      task.ignoredIssueReasons.convention(project.provider {
        extension.verification.ignoredIssues.rules().map { it.reason.orEmpty() }
      })
      task.dependsOn(project.tasks.matching { it.name == "classes" || it.name == "testClasses" })
      task.onlyIf("emn.verification.enabled is true") { extension.verification.enabled.get() }
    }

  private fun registerGeneratedKotlinSources(
    project: Project,
    sourceSetName: String,
    generateTask: TaskProvider<out org.gradle.api.Task>,
  ) {
    val kotlinExtension = project.extensions.getByName("kotlin")
    val sourceSets = kotlinExtension.javaClass.methods
      .firstOrNull { it.name == "getSourceSets" && it.parameterCount == 0 }
      ?.invoke(kotlinExtension) as? NamedDomainObjectContainer<*>
      ?: throw GradleException("The applied Kotlin JVM plugin does not expose Kotlin source sets.")
    val sourceSet = sourceSets.getByName(sourceSetName)
    val generatedKotlin = sourceSet.javaClass.methods
      .firstOrNull { it.name == "getGeneratedKotlin" && it.parameterCount == 0 }
      ?.invoke(sourceSet) as? SourceDirectorySet
      ?: throw GradleException(
        "This plugin requires a Kotlin Gradle Plugin version that exposes KotlinSourceSet.generatedKotlin."
      )
    generatedKotlin.srcDir(generateTask)
  }

  companion object {
    const val PLUGIN_ID = "io.holixon.emn"
    const val EXTENSION_NAME = "emn"
    const val GENERATE_KOTLIN_TASK_NAME = "generateEmnSchemaKotlin"
    const val GENERATE_SPECIFICATIONS_TASK_NAME = "generateEmnSpecificationTests"
    const val EXPORT_SCHEMAS_TASK_NAME = "exportEmnSchemas"
    const val VERIFY_TASK_NAME = "verifyEmn"
  }
}
