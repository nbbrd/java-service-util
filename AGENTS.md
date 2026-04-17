# AGENTS.md — Java Service Util

## Overview

`java-service-util` is a Java library of **compile-time utilities for the Java SPI** (Service Provider Interface) mechanism.
It provides annotations and annotation processors that reduce the boilerplate of declaring, registering, and loading services,
while enforcing correctness at compile time rather than at runtime.

Key characteristics:
- **No runtime dependency**: all code generation and validation happens during compilation via JSR-269 annotation processing.
- **Java 8 minimum** with full JPMS (`module-info.java`) compatibility.
- **Zero-configuration classpath registration**: `@ServiceProvider` automatically writes `META-INF/services` files.
- **Generated, type-safe loaders**: `@ServiceDefinition` produces a specialized `*Loader` class per service with built-in filtering, sorting, and quantifier enforcement.
- Licensed under the **European Union Public License (EUPL)**.
- Published to Maven Central under `com.github.nbbrd.java-service-util`.

## Architecture

The project is a **multi-module Maven build** (`java-service-parent`, v2.x):

```
java-service-parent (pom)
├── java-service-annotation   — public API: annotations + Quantifier enum
├── java-service-processor    — JSR-269 annotation processors (shaded fat-jar)
├── java-service-bom          — Bill of Materials for version management
└── java-service-examples     — usage examples (non-deployable)
```

### Module: `java-service-annotation` (`nbbrd.service`)

Contains the entire public API consumed by end-users at compile time (`provided` scope):

| Type                 | Role                                                                         |
|----------------------|------------------------------------------------------------------------------|
| `@ServiceProvider`   | Marks a class as a service provider; triggers `META-INF/services` generation |
| `@ServiceDefinition` | Defines service loading contract; triggers `*Loader` class generation        |
| `@ServiceId`         | Marks the no-arg `String` method that identifies a provider                  |
| `@ServiceFilter`     | Marks the no-arg `boolean` method that filters providers                     |
| `@ServiceSorter`     | Marks the no-arg numeric/comparable method that sorts providers              |
| `Quantifier`         | Enum used by `@ServiceDefinition`: `OPTIONAL`, `SINGLE`, `MULTIPLE`          |

### Module: `java-service-processor` (`nbbrd.service.processor`)

Contains two independent JSR-269 `AbstractProcessor` implementations registered via NetBeans `@ServiceProvider`.
It is distributed as a **shaded fat-jar** that relocates its runtime dependencies
(`javaparser-core`, `javapoet`, `mustache.java`) into `internal.nbbrd.service.*` to avoid classpath pollution.

#### `ServiceProviderProcessor`

Handles `@ServiceProvider` / `@ServiceProvider.List`.

Pipeline (Collect → Check → Generate across annotation-processing rounds):
1. **`ServiceProviderCollector`** — accumulates `ProviderRef` instances across rounds.
2. **`ServiceProviderChecker`** — validates provider/service relationships; checks coherence with `module-info.java` via `ModulePathRegistry`.
3. **`ServiceProviderGenerator`** — writes `META-INF/services/<service-fqn>` files via `ClassPathRegistry`.

#### `ServiceDefinitionProcessor`

Handles `@ServiceDefinition`, `@ServiceFilter`, `@ServiceSorter`, `@ServiceId`.

Pipeline (single round):
1. **`ServiceDefinitionCollector`** — reads annotations into a `LoadData` model (`LoadDefinition`, `LoadFilter`, `LoadSorter`, `LoadId`).
2. **`ServiceDefinitionChecker`** — validates constraints (quantifier/fallback coherence, method signatures, uniqueness, pattern validity) and `module-info.java` coherence.
3. **`ServiceDefinitionGenerator`** — generates `*Loader` Java source files using **JavaPoet**; loader name resolved via a **Mustache** template or automatic `<Service>Loader` naming; nested loaders are grouped under a single top-level `<TopLevel>Loader` class.

Generated loaders expose:
- A static `load()` / `get()` method (return type shaped by `Quantifier`).
- A `builder()` for custom backends (e.g. NetBeans Lookup).
- Optional `ID_PATTERN` constant (from `@ServiceId#pattern`).

### Module: `java-service-bom`

A standard Maven BOM (Bill of Materials) that pins versions of `java-service-annotation` and `java-service-processor` for consumers who import it.

### Module: `java-service-examples`

Non-deployable examples demonstrating all annotations and design patterns (API/SPI split, batch types, custom backends). Excluded from the default build by the `nonDeployableModules` Maven property.


## Build & Test

```shell
mvn clean install                 # full build + tests + enforcer checks
mvn clean install -Pyolo          # skip all checks (fast local iteration)
mvn test -pl <module-name> -Pyolo # fast test a single module
mvn test -pl <module-name> -am    # full test a single module
```

- **Java 8 target** with JPMS `module-info.java` compiled separately on JDK 9+ (see `java8-with-jpms` profile in root POM)
- **JUnit 5** with parallel execution enabled (`junit.jupiter.execution.parallel.enabled=true`); **AssertJ** for assertions
- `heylogs-api` publishes a **test-jar** (`tests/` package) reused by extension modules for shared test fixtures

## Key Conventions

- **Lombok**: use lombok annotations when possible. Config in `lombok.config`: `addNullAnnotations=jspecify`, `builder.className=Builder`
- **Nullability**: `@org.jspecify.annotations.Nullable` for nullable; `@lombok.NonNull` for non-null parameters. Return types use `@Nullable` or the `OrNull` suffix (e.g., `getThingOrNull`)
- **Design annotations** use annotations from `java-design-util` such as `@VisibleForTesting`, `@StaticFactoryMethod`, `@DirectImpl`, `@MightBeGenerated`, `@MightBePromoted`
- **Internal packages**: `internal.<project>.*` are implementation details; public API lives in the root and `spi` packages
- **Static analysis**: `forbiddenapis` (no `jdk-unsafe`, `jdk-deprecated`, `jdk-internal`, `jdk-non-portable`, `jdk-reflection`), `modernizer`
- **Reproducible builds**: `project.build.outputTimestamp` is set in the root POM
- **Formatting/style**: 
  - Use IntelliJ IDEA default code style for Java
  - Follow existing formatting and match naming conventions exactly
  - Follow the principles of "Effective Java"
  - Follow the principles of "Clean Code"
- **Java/JVM**: 
  - Target version defined in root POM properties; some modules may require higher versions
  - Use modern Java feature compatible with defined version

## Agent behavior

- Do respect existing architecture, coding style, and conventions
- Do prefer minimal, reviewable changes
- Do preserve backward compatibility
- Do not introduce new dependencies without justification
- Do not rewrite large sections for cleanliness
- Do not reformat code
- Do not propose additional features or changes beyond the scope of the task
