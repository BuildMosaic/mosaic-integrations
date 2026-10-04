# Mosaic Integrations

[![CI](https://github.com/BuildMosaic/mosaic-integrations/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/BuildMosaic/mosaic-integrations/actions/workflows/ci.yml)

**Make Mosaic fit naturally into the frameworks your application already uses.**

The home of official framework integrations for [Mosaic](https://github.com/Nick-Abbott/Mosaic),
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

No integrations are implemented or published yet. An integration is listed as
available only once it is implemented and published, with its supported versions
and a framework-specific guide.

## Development

Use JDK 21 and the checked-in Gradle wrapper:

```bash
./gradlew clean build --no-daemon
git diff --check
```

The build currently validates the Gradle foundation; there are no adapter modules
or tests yet. See [Contributing](.github/CONTRIBUTING.md) for the build baseline and
how to propose an integration.

## License

Mosaic Integrations is licensed under the [Apache License 2.0](LICENSE).
