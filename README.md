# Buildup Situational Crosshair

Client-only Fabric mod targeting Minecraft 26.3.
Development version: `0.1.0-alpha.5.1+mc26.3`.

Use JDK 25 or newer and run `./gradlew clean build` or
`./gradlew runClient` (Windows: `./gradlew.bat`). Set JAVA_HOME to that JDK
if the shell defaults to an older version. No server installation is required.

Run actual client integration tests with `./gradlew runClientGameTest`.
`./gradlew build` also runs the pure semantic/presentation contract suite via
`semanticTest`, the JSON rule suite via `ruleTest`, and presentation/transition
contracts via `presentationTest`.
See [Semantic Core](docs/semantic-core.md), [rule authoring](docs/rules-v0.1.md)
and the [Stage 5.1 report](docs/stage-5.1-report.md).

Classic+ is the default theme. Switch during a game with `/crosshair theme classic`
or `/crosshair theme classic_plus`. Disable transitions with `/crosshair animation off`
or restore them with `/crosshair animation subtle`. These choices last for the
current client session; persistent settings arrive in Stage 7.
See the [Classic+ symbol guide](docs/classic-plus.md).
Tests create an isolated temporary world under build/run/clientGameTest and do
not ship in the mod jar. Classic texture and behavior provenance is retained;
consult THIRD_PARTY_NOTICES.md before redistribution. Independently authored code
uses MIT; the Classic artwork retains its original CC BY-NC-SA 4.0 terms. The
combined mod is not exclusively MIT licensed.

See [Classic parity](docs/classic-parity.md),
[stage report](docs/stage-0-1-report.md) and
[license provenance](THIRD_PARTY_NOTICES.md).
