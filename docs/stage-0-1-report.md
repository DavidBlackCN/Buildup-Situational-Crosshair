# Stage 0 / Stage 1 report

## Toolchain (verified 2026-10-02)

Minecraft 26.3; Fabric Loader 0.19.5; Fabric API 0.161.0+26.3;
Loom 1.18.2; Gradle 9.7.1; Oracle JDK 25.0.3+9-LTS-195.
Local JDK: `C:/Program Files/Java/jdk-25.0.3`.

Official sources:
- https://fabricmc.net/2026/09/15/263.html
- https://github.com/FabricMC/fabric-example-mod/tree/26.3
- https://meta.fabricmc.net/v2/versions/loader/26.3
- https://maven.fabricmc.net/net/fabricmc/fabric-loom/maven-metadata.xml
- https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml

The current official template uses Gradle 9.7.1 and Loom 1.18-SNAPSHOT;
we select published stable Loom 1.18.2 instead. These are newer than planning
values 1.17 / 9.6.0. Dependency resolution succeeded. Minecraft is unobfuscated:
`net.fabricmc.fabric-loom`, regular implementation dependencies, no Yarn or
mapping dependency. Loader's “Mappings not present!” is expected here.

## Stage 0 — PASS

Fresh Groovy project following the official template with split main/client
source sets; official wrapper downloaded rather than upgrading the old project.
Only 26.3 has been initialized as a new Git repository. Workspace root has no .git.
Metadata declares client environment and only a client entrypoint.

`clean build` completed and produced the mod jar. `runClient` loaded Minecraft
26.3, Loader and API, and logged “Buildup Situational Crosshair loaded (Stage 0)”.
Main menu visually observed with computer-use after first-run accessibility page.
See `validation/stage-0-client.log`.

Development offline credentials produce user-properties HTTP 401 and Realms
authentication errors; main menu still works. Empty source-set output directories
produce a Loader classpath warning; no missing mod classes or mixin failures.

## Stage 1 — PASS (automated acceptance; manual checklist remains)

Stage 0 passed before implementation started. ClassicCrosshairResolver reads current
vanilla hitResult and harvest/tool state; ClassicCrosshairType contains only four
presentation choices. CrosshairHudRenderer owns texture selection and drawing.
No Stage 2 framework, rule engine, config GUI, third-party framework or mod-specific
compatibility code has been introduced.

HudElementRegistry.replaceElement(CROSSHAIR) wraps the original HUD element.
The original element always executes. A small MixinExtras WrapWithCondition on
Hud.extractCrosshair's blitSprite checks the exact `minecraft:hud/crosshair`
identifier, records that vanilla requested the central sprite, and suppresses only
that sprite when a Classic result exists. The HUD wrapper then draws one 15×15
Classic texture through GuiGraphicsExtractor and RenderPipelines.CROSSHAIR.
Finally clears frame-local state even if extraction throws.

There is no ordinal, Redirect, raycast or world detection inside the mixin.
The mixin is necessary because HUD API replacement acts on the entire layer,
including attack indicators and internal visibility checks. Keeping the original
element retains these checks and indicators. If no target is available, vanilla
is untouched. Another mod replacing the same HUD element can still affect the
result; broad HUD-mod interoperability is not claimed.

## Validation

- `clean build`: passed before and after migration; final jar in build/libs.
- `runClient`: Minecraft 26.3 + Loader 0.19.5 + API loaded; Classic Theme entrypoint
  logged; no critical mixin failures or missing Classic textures.
- `runClientGameTest`: passed in a real integrated-server world, 16 resolver cases:
  air, entity, empty-hand dirt, empty-hand ore, insufficient tier, correct tier,
  wrong tool, survival bedrock, creative bedrock, bow, using bow, uncharged crossbow,
  charged crossbow, offhand bow precedence, offhand charged crossbow, null target.
- Every resolver case also invokes the transformed HUD and verifies one custom
  submission and zero vanilla central sprites (or vanilla-only for null fallback),
  resource presence, and 15×15 dimensions.
- Seven additional HUD cases: first person, F1, third person, spectator air,
  debug 3D crosshair, spyglass, null fallback. All passed.
- Four PNG hashes match the reference exactly. Test mod lives in src/gametest and
  is not part of the distributable jar.
- Game-test screenshot `validation/classic-gameplay-smoke.png` was inspected:
  the Classic BLOCK outline is visible in a loaded world with no second crosshair.
  This is a smoke screenshot, not pixel-perfect proof of all four target scenes.
- Main menu was visually observed during Stage 0. User stopped Computer Use with
  Escape during Stage 1; no further desktop automation was performed.

Evidence: `validation/stage-0-client.log`, `validation/stage-1-client.log`,
`validation/stage-1-gametest.log`. Run game tests separately from `build`:
`./gradlew runClientGameTest`. Gradle's standard `test` task has no unit tests;
the actual tests require the client game-test runtime.

## Manual checklist (not claimed as completed)

- [ ] Visually compare DOT/BLOCK/ATTACK/ERROR in natural aiming scenarios at several GUI scales.
- [ ] Spectator chest/entity interaction targets and non-interactable targets.
- [ ] Attack cooldown indicator visibility in combat, including full-ready state.
- [ ] Inventory, chat, pause, death/respawn and dimension changes.
- [ ] In-game bow/crossbow use and dual-hand swapping over multiple ticks.
- [ ] Coexistence with other HUD/crosshair mods (outside Stage 1 acceptance).

## Known limitations and Stage 2 prerequisites

- License evidence conflicts (CC BY-NC-SA LICENSE versus metadata/README restrictions).
  Attribution and original license retained; no publication performed or additional
  rights asserted. Resolve distribution provenance before public release.
- Development offline login creates Realms/user-properties authentication errors;
  it does not prevent local gameplay. Empty source-set classpath warnings remain.
- The narrow version-specific mixin must be rechecked on Minecraft updates;
  it uses the current MixinExtras v2 WrapWithCondition annotation.
- Manual checks above remain; there is no claim of comprehensive compatibility.
- Stage 2 can introduce Semantic Core behind the resolver boundary. ERROR remains
  Classic presentation, not a permanent world-state contract. Stage 2 is not started.
