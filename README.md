# emn-kotlin

Event Modeling Notation (EMN) handling in Kotlin.

[![incubating](https://img.shields.io/badge/lifecycle-INCUBATING-orange.svg)](https://github.com/holisticon#open-source-lifecycle)
[![Build Status](https://github.com/holixon/emn-kotlin/workflows/Development%20branches/badge.svg)](https://github.com/holixon/emn-kotlin/actions)
[![sponsored](https://img.shields.io/badge/sponsoredBy-Holisticon-RED.svg)](https://holisticon.de/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.holixon.emn/emn-kotlin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.holixon.emn/emn-kotlin)

## Modules

The repository is grouped by capability. Runtime/core artifacts are independent of a
build tool; Maven and Gradle integrations are thin adapters around them.

- `emn-parser`: EMN document model and parser.
- `emn-kotlin`: Axon 5 Kotlin generation service plus Maven and Gradle adapters.
- `emn-schema-exporter`: build-tool-neutral schema export service plus Maven and Gradle adapters.
- `emn-verify`: neutral annotations and verification model, the Axon 5 inspector, and Maven and Gradle adapters.
- `examples`: one Maven reactor and one Gradle multi-project build over the same example directories.

Each Gradle plugin has a small marker POM module. Gradle resolves a plugin ID such as
`io.holixon.emn.verify` through the corresponding
`io.holixon.emn.verify.gradle.plugin` marker coordinate; the marker then depends on
the actual Gradle plugin implementation JAR.

## Export JSON Schemas

The schema export adapters write one JSON Schema file per EMN type plus a deterministic
`manifest.json`. The manifest identifies every schema as a `command`, `event`, `query`,
or `common` type and records its EMN source, schema origin, and referenced types. This
bundle can be passed to frontend tooling such as `json-schema-to-typescript` without
requiring Node.js in the EMN build plugin.

Maven:

```xml
<plugin>
  <groupId>io.holixon.emn.generation</groupId>
  <artifactId>emn-schema-exporter-maven-plugin</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <executions>
    <execution>
      <id>export-emn-schemas</id>
      <goals>
        <goal>export-schemas</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

The default output is `target/generated-resources/emn-schemas` and is registered as a
Maven resource directory.

Gradle:

```kotlin
plugins {
  id("io.holixon.emn.schema-export") version "0.0.1-SNAPSHOT"
}

emnSchemaExport {
  emnFiles.from(fileTree("src/main/resources") { include("**/*.emn") })
  schemaResourceDirectories.from(file("src/main/json-schema"))
}
```

Run `exportEmnSchemas`. The default output is
`build/generated/resources/emn-schemas`. Resource-backed schemas must resolve to local
files below the EMN directory or a configured `schemaResourceDirectories` entry;
network resources and unresolved references fail the export.

## Generate Axon Specification Tests

The Axon 5 Kotlin module can generate fixture tests from EMN Given/When/Then
specifications independently from command-handler generation. The neutral generation
service reads example values from the specification and uses the embedded JSON Schemas
to render constructor calls, including scalar aliases, UUID and date/time formats,
collections, maps, enums, references, and `allOf` object properties. Every generated
test method carries `@EmnSpecification(id = "...")`, so `emn-verify` discovers it.

Generated tests require `axon-test`, JUnit Jupiter, and the neutral annotations on the
consumer's test classpath:

```kotlin
dependencies {
  testImplementation("org.axonframework:axon-test:5.3.2")
  testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.2")
  testImplementation("io.holixon.emn:emn-verify-annotations:0.0.1-SNAPSHOT")
}
```

Gradle:

```kotlin
emnSchemaKotlin {
  emnFiles.from(file("src/main/resources/game.emn"))
  rootPackageName.set("org.example.game.api")
  specifications {
    generate = true
    fixtureProvider = "org.example.game.test.GameFixtureProvider"
    // Optional: overrides the built-in CommandExecutionException details verifier.
    errorVerifier = "org.example.game.test.GameErrorVerifier"
  }
}
```

The dedicated task is `generateEmnSpecificationTests`; generated files are registered
with the Kotlin `test` source set under
`build/generated/sources/emn-specification-tests`. Generation is opt-in and remains
separate from `generateEmnSchemaKotlin`.

Generated test classes use Axon's `AxonFrameworkExtension`. The configured fixture
provider implements `AxonTestFixtureProvider`; Axon creates a fresh fixture for each
test and stops it afterwards. No custom JUnit extension registration is required.

Maven:

```xml
<execution>
  <id>generate-emn-specification-tests</id>
  <goals>
    <goal>generate-specification-tests</goal>
  </goals>
  <configuration>
    <rootPackageName>org.example.game.api</rootPackageName>
    <includes>**/*.emn</includes>
    <fixtureProvider>org.example.game.test.GameFixtureProvider</fixtureProvider>
    <errorVerifier>org.example.game.test.GameErrorVerifier</errorVerifier>
  </configuration>
</execution>
```

The Maven goal runs in `generate-test-sources` and writes to
`target/generated-test-sources/emn-specification-tests`.

By default, an EMN error expectation requires Axon's `CommandExecutionException` and
compares the schema-typed JSON value from the specification with the exception's
structured details. Object comparison is recursive and treats the expected value as a
subset, so transported details may contain additional fields. Error schemas are
extracted and exported but do not generate Kotlin exception classes yet.

Applications with a different error representation can configure `errorVerifier`.
The named Kotlin object or Java class must provide a callable static-style method with
this contract:

```kotlin
object GameErrorVerifier {
  fun verify(
    errorTypeId: String,
    expectedDetails: Map<String, Any?>,
    exception: Throwable,
  ) {
    // Application-specific assertion.
  }
}
```

For the older AVRO-backed `generate` goal, command-handler and fixture-test generation
can now also be controlled independently with `generateCommandSlices` and
`generateCommandSliceTests`.

## Verify Code Against EMN

The Axon 5 verification service checks compiled classes against EMN type and
specification IDs. It currently verifies command-handler coverage, event-handler
coverage for query read models, and specification-ID coverage. An event handler that
is not connected to any query served by its read model is reported as a warning.

Add the neutral annotations to the application:

```kotlin
dependencies {
  implementation("io.holixon.emn:emn-verify-annotations:0.0.1-SNAPSHOT")
}
```

A read model can serve one or several EMN query types:

```kotlin
@EmnReadModel("GameState", "SearchGames")
class GameProjection {
  @EventHandler
  fun on(event: GameCreatedEvent) = Unit
}

@Test
@EmnSpecification("Specification_create_game")
fun `creates a game`() = Unit
```

`EmnSpecification` is framework-neutral metadata. Add the test annotation required
by JUnit, Kotest, or the test framework used by the application separately.

The Axon inspector matches an Axon command or event handler's payload class simple
name to the EMN type ID. `commandName`, `eventName`, and `payloadType` declared on
the Axon annotation take precedence. Query and specification IDs are supplied by
`EmnReadModel` and `EmnSpecification`.

Gradle:

```kotlin
plugins {
  id("io.holixon.emn.verify") version "0.0.1-SNAPSHOT"
}

emnVerify {
  emnFiles.from(file("src/main/resources/game.emn"))
  failOnWarnings.set(false)

  ignoredIssues {
    ignoreMissingReadModel("GameState", reason = "Implemented by the frontend")
    ignoreMissingCommandHandler("CloseGameCommand", reason = "Handled by an external service")
  }
}
```

The `verifyEmn` task is attached to `check` and scans the conventional
Kotlin and Java main/test class directories. A normal run prints model and code
discovery counts. Run `./gradlew verifyEmn --debug` to print every EMN
command, event, query, and specification ID together with its discovered code
location. A missing `emnFiles` configuration fails explicitly instead of silently
skipping the task. Ignored issues are matched by issue code and EMN element ID.
Normal output includes their count, while `--debug` prints each ignored issue and
its configured reason. Duplicate ignore rules fail the task, and rules that match
no issue produce a warning. Use `ignore(code, elementId, reason)` when no dedicated
convenience method exists.

Maven:

```xml
<plugin>
  <groupId>io.holixon.emn</groupId>
  <artifactId>emn-verify-maven-plugin</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <executions>
    <execution>
      <id>verify-emn</id>
      <goals>
        <goal>verify-emn</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

The Maven goal runs in `verify`, reads EMN files from `src/main/resources`, and scans
both main and test output directories. Set `failOnWarnings` to `true` to make warnings
fail either build adapter.

The verification service can also be called directly:

```kotlin
val report = Axon5EmnVerificationService().verify(
  Axon5EmnVerificationRequest(
    emnFiles = listOf(File("src/main/resources/game.emn")),
    classDirectories = listOf(
      File("build/classes/kotlin/main"),
      File("build/classes/kotlin/test"),
    ),
  )
)

report.requireSuccessful(failOnWarnings = false)
```

Use `Axon5CodeModelInspector` directly only when the discovered `ActualCodeModel`
is needed without comparison to an EMN model. Normal application builds should use
the Gradle task or Maven goal.


### Issue Template

* correct the URL to repo

### Issue Labels

* Check the release-notes.yml for details, but create the following labels: Type: dependencies, Type: bug, Type: documentation, Type: question, Type: enhancement
