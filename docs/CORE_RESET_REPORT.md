# Core Reset report

Date: 2026-10-25. Branch: `26.3`. Version: `0.1.0-core-reset+mc26.3`.
Result: **implementation/build/automated client acceptance PASS; manual visual acceptance pending**.

## Recovery checkpoint and scope

The worktree was clean before changes. Baseline:
`f9a1a2ae24801d06537f28e7f8c34be943f86f20`.
Annotated tag `core-reset-baseline` preserves the old implementation before edits.
No history was rewritten; no push was performed. Only `26.3/` was modified;
`1.20.1/` remains a read-only reference and the workspace root has no `.git`.

The user's Core Reset request supersedes the historical expansion roadmap and
its sidecar requirements for this work. No subsequent stage or new product
design was implemented. Historical stage reports, galleries and logs were
removed from the current tree; they remain available through the checkpoint.

## Removed and retained

Removed Classic+, sidecar drawing/sprites/layout/export artwork and A/B/C
resources; primary/secondary candidates, providers, confidence/priority and
provenance; Vanilla capability/use prediction and BlockItemAccessor; JSON rule
loading, parsing, matching and fixtures; presentation compositions, themes and
transitions; obsolete commands, language keys, test suites and Gradle tasks.
No disabled backup implementations or replacement semantic framework remain.

Retained four original Classic textures, original attribution/license evidence,
the existing HUD replacement integration and unchanged narrow sprite Mixin.
Runtime configuration is one enabled flag, default true. Commands and a future
GUI share `CrosshairOptions.enabled()` / `setEnabled(boolean)` on the client
thread. There is no GUI placeholder, persistence or added runtime dependency.

Classic policy was checked against baseline ClassicPresentation, capture,
harvest/entity providers and ClassicParityTest, rather than Classic+ policy:

- Air DOT; entities ATTACK, including attack cooldown.
- Blocks use native destroy speed and correct-tool-for-drops; creative bypasses
  both checks. Invalid mining uses ERROR.
- A ranged off-hand item takes precedence over main hand: an off-hand bow or
  uncharged crossbow masks a charged main-hand crossbow. The selected charged
  crossbow gives ATTACK before target-specific policy.
- Missing context delegates to Vanilla. Spectator block targets delegate to
  Vanilla unless the original charged-crossbow precedence applies.
- Vanilla controls actual visibility, spectator eligibility, debug crosshair and
  attack indicators. The Mixin suppresses only `hud/crosshair` during a custom
  frame; disabling the option delegates the complete original layer unchanged.

## Final structure

Production Java files: **45 -> 6**.

```text
src/main/java/dev/buildup/situationalcrosshair/
  crosshair/ClassicCrosshairType.java
src/client/java/dev/buildup/situationalcrosshair/
  BuildupSituationalCrosshairClient.java
  CrosshairOptions.java
  crosshair/ClassicCrosshairResolver.java
  hud/CrosshairHudRenderer.java
  mixin/HudCrosshairMixin.java
src/gametest/java/dev/buildup/situationalcrosshair/
  ClassicParityTest.java
  HudParityChecks.java
```

Data flow: native client reads -> Classic type (or null/Vanilla) -> HUD renderer.
No gameplay interactions are invoked by detection. The renderer knows textures,
not world semantics. Its temporary frame state is cleared in `finally`.

## Verification completed

Using existing JDK **25.0.3**, Gradle Wrapper **9.7.1**, Loom **1.18.2**, Loader
**0.19.5**, Fabric API **0.161.0+26.3** and Minecraft **26.3**:

`gradlew.bat clean build runClientGameTest --offline`: **PASS**, exit 0.
Final run: 31 seconds, 15 actionable tasks. The first sandboxed attempt could
not acquire the existing user Gradle-cache lock; the approved elevated run used
the same installed toolchain and cache. No dependency/toolchain upgrade.

The retained real-client integration test verifies air, entity, dirt, wrong and
insufficient-tier tools, correct tool, survival/creative bedrock, bow/use state,
uncharged/charged main-hand crossbow, off-hand bow precedence, charged off-hand
crossbow and unavailable-target fallback. Added checks cover the real command
dispatcher, only two remaining command children, enable/disable HUD restoration,
GUI scale 1/2/3 centering, preserved Vanilla attack indicator and spectator
container fallback. Existing F1, third person, spectator air, debug 3D crosshair
and spyglass checks remain. Tests observe the transformed HUD submissions,
resource existence and unchanged 15x15 rendering dimensions.

The client starts successfully, loads the Mixin/resources, enters a test world,
disconnects, verifies null-context fallback, enters a second world and verifies
DOT/custom HUD again, then closes gracefully. This is automated real-client
execution, not manual gameplay acceptance. Most resolver cases intentionally
assign hit results to isolate native behavior; the saved smoke screenshot is
not proof of natural aiming or long-play visual quality.

Static reference audit found no removed module references in source, mixin
configuration or Gradle tasks. Final JAR contains only four PNGs, all exactly
matching baseline SHA-256/bytes, the six production classes (plus javac's switch
helper), client-only metadata and one HUD Mixin. No rule/sidecar/presentation/
semantic modules, test classes or exploration assets are packaged. Original
license text is unchanged (Git LF versus existing Windows checkout CRLF), and
packaged license bytes match the checkout. `git diff --check` passes.

Evidence:

- [Build and client test log](validation/core-reset-build-gametest.log)
- [Final notice-only packaging build](validation/core-reset-package.log) (PASS, exit 0; packaged notice matches source)
- [Artifact audit and texture hashes](validation/core-reset-artifact-audit.txt)

The successful test log retains pre-existing environment messages about an
empty client-resource output directory, anisotropic option value, offline
Realms authentication and the harness's missing `end_of_frame` post effect.
No test assertion or Mixin loading failures occurred.

## Remaining manual acceptance and technical boundaries

Manually inspect ordinary gameplay for pixel appearance, flicker/ghosting,
GUI scales, rapidly changing targets and backgrounds; verify alongside the
intended HUD/render mods and representative modpack. Long-session stability and
arbitrary mod/resource-pack compatibility are not established by this suite.
A separate ordinary `runClient` smoke run was not performed in this reset;
startup/shutdown evidence comes from the actual GameTest client.

For later redesign, preserve native HUD visibility and attack-indicator
ownership, keep detection free of interaction side effects, and keep texture
selection separate from detection. Do not silently replace Classic's off-hand
precedence or spectator fallback. New product behavior requires a new approved
scope; the deleted framework is recoverable from Git, not a future API promise.
