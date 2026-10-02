# Buildup Situational Crosshair

Client-only Fabric mod targeting Minecraft 26.3.
Development version: `0.1.0-alpha.2+mc26.3`.

Use JDK 25 or newer and run `./gradlew clean build` or
`./gradlew runClient` (Windows: `./gradlew.bat`). Set JAVA_HOME to that JDK
if the shell defaults to an older version. No server installation is required.

Run actual client integration tests with `./gradlew runClientGameTest`.
`./gradlew build` also runs the pure semantic/presentation contract suite via
`semanticTest`. See [Semantic Core](docs/semantic-core.md) and the
[Stage 2 report](docs/stage-2-report.md).
Tests create an isolated temporary world under build/run/clientGameTest and do
not ship in the mod jar. Classic texture and behavior provenance is retained;
consult THIRD_PARTY_NOTICES.md before redistribution.

See [Classic parity](docs/classic-parity.md),
[stage report](docs/stage-0-1-report.md) and
[license provenance](THIRD_PARTY_NOTICES.md).
