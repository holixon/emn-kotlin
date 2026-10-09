# university-json-schema

This example generates Kotlin data classes from embedded JSON Schemas in an EMN file.
It also enables the `axon5` annotation enhancer, which maps neutral JSON Schema hints to Axon annotations:

- `x-emn-role: "entity-id"` -> `@TargetEntityId`
- `x-emn-tag-key: "course"` -> `@EventTag(key = "course")`

The Maven build copies the repository-level `_emn` folder into generated resources and runs:

```bash
./mvnw -pl examples/university-json-schema -am generate-sources
```

After installing the plugin artifacts in Maven Local, the same example can be built with Gradle:

```bash
gradle -p examples :university-json-schema:build
```

The generated classes are written to:

```text
examples/university-json-schema/target/generated-sources/emn-schema-kotlin # Maven
examples/university-json-schema/build/generated/sources/emn-schema-kotlin # Gradle
```

The configured root package is `io.holixon.emn.example.university`. Commands are generated in
`.command`, events in `.event`, queries in `.query`, and common schema definitions in `.common`.

The example currently generates:

- `CourseType`
- `CreateCourse`
- `CourseCreated`
- `CourseOverview`

from:

```text
_emn/json-schema/faculty.emn
```
