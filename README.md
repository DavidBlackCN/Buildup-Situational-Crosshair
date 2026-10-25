# Buildup Situational Crosshair

Client-only Fabric mod for Minecraft **26.3**, version `0.1.0-core-reset+mc26.3`.
No server installation is required. Core Reset replaces the previous expansion
roadmap; subsequent product design is undecided.

The mod retains the original 15x15 Classic DOT, BLOCK, ATTACK and ERROR textures.
Air uses DOT; entity targets use ATTACK; mining checks native breakability and
harvest-tool suitability (creative bypasses these checks). Charged crossbows
retain Classic's ranged off-hand precedence. Unavailable contexts and spectator
block targets delegate to Vanilla. Vanilla still controls HUD visibility,
third-person/spectator restrictions, debug crosshair and attack indicators.

Sidecars, Classic+, JSON rules, secondary-action prediction and animations have
been removed. There is no rule loading or theme selection.

## Configuration

- `/crosshair disable`: restore Vanilla's crosshair immediately.
- `/crosshair enable`: restore the Classic dynamic crosshair (default).

The setting lasts for the current client session, including world changes.
`CrosshairOptions.enabled()` / `setEnabled(boolean)` is the minimal client-thread
interface for a future GUI. There is no settings screen, persistence or extra
configuration library in this version.

## Build and validation

Use **JDK 25** and the existing **Gradle 9.7.1** Wrapper. Set `JAVA_HOME` if needed.
Loom 1.18.2, Fabric Loader 0.19.5 and Fabric API 0.161.0+26.3 are unchanged.

```sh
./gradlew clean build
./gradlew runClientGameTest
./gradlew runClient
```

Windows: use `gradlew.bat`. Output is in `build/libs/`. The client integration
suite creates temporary worlds under `build/run/clientGameTest`; its classes and
assets do not ship in the mod JAR. It checks actual native mining/item behavior
and transformed HUD submissions. Automated tests do not replace manual visual
and modpack acceptance. See [Core Reset report](docs/CORE_RESET_REPORT.md).

## Attribution

Classic design and the four unchanged PNGs originate from Kiley (KiOrlando)'s
Crosshair Mod. Independently authored code uses MIT; inherited artwork retains
its original CC BY-NC-SA 4.0 terms and recorded distribution restrictions. The
combined mod is not exclusively MIT licensed. See
[third-party notices](THIRD_PARTY_NOTICES.md) and `licenses/CC-BY-NC-SA-4.0.txt`.
