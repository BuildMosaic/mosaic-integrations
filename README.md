# Mosaic Integrations

[![CI](https://github.com/BuildMosaic/mosaic-integrations/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/BuildMosaic/mosaic-integrations/actions/workflows/ci.yml)

**Make Mosaic fit naturally into the frameworks your application already uses.**

The home of official framework integrations for [Mosaic](https://github.com/BuildMosaic/Mosaic),
a Kotlin library for composable backend orchestration. Learn more at
[buildmosaic.org](https://buildmosaic.org).

## Design

Integrations connect an application's existing dependency injection, lifecycle,
and telemetry infrastructure to Mosaic's public APIs. Framework-managed services
stay framework-owned; adapters should save application developers from wiring the
same connections by hand.

Mosaic core owns Tile execution, caching, batching, deduplication, cancellation,
Canvas behavior, and execution observation. Integrations translate framework
behavior into those capabilities. They depend on released Mosaic artifacts from
Maven Central.

## Availability

The [Spring Canvas adapter](mosaic-spring/README.md) lets existing Tiles resolve
Spring services without registering each service in Canvas:

```kotlin
canvas { dependencyResolver(SpringCanvasResolver(beanFactory)) }
```

No adapter artifacts are published. See the adapter guide for the exact tested
versions, qualifier mapping, ownership, and request-scope boundaries.

## Development

Use JDK 21 and the checked-in Gradle wrapper:

```bash
./gradlew clean build --no-daemon
git diff --check
```

The build runs adapter tests with real framework contexts. See
[Contributing](.github/CONTRIBUTING.md) for the build baseline and how to propose
an integration.

## License

Mosaic Integrations is licensed under the [Apache License 2.0](LICENSE).
