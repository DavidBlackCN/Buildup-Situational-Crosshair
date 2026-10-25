# Buildup Situational Crosshair

Client-only Fabric mod for Minecraft **26.3**, version `0.1.0-core-animation.1+mc26.3`.
No server installation is required. Core Reset replaces the previous expansion
roadmap. This first follow-up adds transitions to the central Classic crosshair.

The mod retains the original 15x15 Classic DOT, BLOCK, ATTACK and ERROR textures.
Air uses DOT; entity targets use ATTACK; mining checks native breakability and
harvest-tool suitability (creative bypasses these checks). Charged crossbows
retain Classic's ranged off-hand precedence. Unavailable contexts and spectator
block targets delegate to Vanilla. Vanilla still controls HUD visibility,
third-person/spectator restrictions, debug crosshair and attack indicators.

Sidecars, Classic+, JSON rules and secondary-action prediction remain removed.
The central crosshair now has a small time-based transition; there is no theme system.

## Configuration

- `/crosshair disable`: restore Vanilla's crosshair immediately.
- `/crosshair enable`: restore the Classic dynamic crosshair (default).
- `/crosshair animation expand`: gentle outward/inward transition (default).
- `/crosshair animation fade`: change only pixel strength, without movement.
- `/crosshair animation off`: immediate original texture switching.

The settings last for the current client session, including world changes.
`CrosshairOptions.enabled()` / `setEnabled(boolean)` is the minimal client-thread
interface for a future GUI. There is no settings screen, persistence or extra
configuration library in this version. Animation options use the same
client-thread `CrosshairOptions.animation()` / `setAnimation(Mode)` interface.

The center remains steady. DOT-to-outline/attack takes 140ms, return to DOT
100ms, and entry into ERROR 65ms. Shared BLOCK/ERROR outline pixels remain
unchanged; interruptions continue from the displayed frame. Expand distributes
moving pixels across neighboring GUI pixels during its maximum one-pixel travel
per axis, so intermediate frames are softer; endpoints use the original PNGs.
Resource reload refreshes the masks. Packs with non-15x15, colored/translucent
pixels, missing shared centers or a non-single-pixel DOT render immediately
without animation, preserving their selected textures.

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
and modpack acceptance. `build` runs deterministic transition contracts via
`animationTest`; client tests also capture six transition galleries.
See [animation report](docs/MAIN_CROSSHAIR_ANIMATION_REPORT.md) and the historical
[Core Reset report](docs/CORE_RESET_REPORT.md).

## Attribution

Classic design and the four unchanged PNGs originate from Kiley (KiOrlando)'s
Crosshair Mod. Independently authored code uses MIT; inherited artwork retains
its original CC BY-NC-SA 4.0 terms and recorded distribution restrictions. The
combined mod is not exclusively MIT licensed. See
[third-party notices](THIRD_PARTY_NOTICES.md) and `licenses/CC-BY-NC-SA-4.0.txt`.
