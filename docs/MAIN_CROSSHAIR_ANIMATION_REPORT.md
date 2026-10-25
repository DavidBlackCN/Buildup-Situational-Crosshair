# Main crosshair animation ? first version

2026-10-25 ? `26.3` ? `0.1.0-core-animation.1+mc26.3`
Baseline: `d2a40bd` (Core Reset). Result: implementation and automated validation PASS;
subjective motion feel and modpack acceptance remain for manual playtesting.

## Behavior

The four original 15x15 PNGs and Classic detection policy are unchanged.
The common center stays at full strength. Default EXPAND gently unfolds the
BLOCK outline and ATTACK strokes from DOT; returning to DOT contracts/fades
the peripheral pixels. Travel is at most one GUI pixel per axis. FADE changes
strength at fixed positions. OFF restores immediate original texture switching.

Timing uses monotonic nanoseconds with cubic ease-out: 140ms for general changes,
100ms when returning to DOT, 65ms when entering ERROR. BLOCK/ERROR shared pixels
stay unchanged, so only the diagonal appears/disappears. First display after a
reset snaps to the correct state. Repeated identical types do not restart motion.
An interruption samples the currently displayed coverage and starts from there;
there is no queued transition or restart from a stale original icon. Complicated
interrupted changes use a coverage fade, preserving continuity rather than
trying to reconstruct an artificial geometric shape.

Commands (client-session only):

- `/crosshair animation expand` ? default, subtle motion and fade.
- `/crosshair animation fade` ? no movement.
- `/crosshair animation off` ? immediate switching.
- `/crosshair enable` and `/crosshair disable` remain available.

## Rendering decisions

`CrosshairTransition` is a small pure controller, after the unchanged resolver.
`CrosshairTextures` reads selected resource-pack masks once per resource reload.
There is no texture decoding, resource creation or filesystem access per frame.
The reload listener publishes a complete set of masks and resets animation.

Actual 26.3 dependency inspection confirmed CROSSHAIR uses INVERT with
ONE_MINUS_DST_COLOR / ONE_MINUS_SRC_COLOR RGB factors. Merely reducing alpha
would not fade the inverted color. Animation therefore modulates RGB strength
with opaque alpha and combines coverage before drawing: one submission per
occupied GUI pixel, sampled from the original DOT's white center. It never
blends two complete overlapping icons. The center is drawn exactly once.

Motion distributes coverage between neighboring integer GUI pixels instead of
scaling the entire bitmap or changing stroke width. Intermediate motion frames
are deliberately softer, while settled states return to a single original PNG
blit. CPU work is bounded by the 225-cell canvas during transitions; no broad
performance claim is made without a representative pack benchmark.

Non-15x15 textures, non-white/translucent visible pixels, a missing common center,
or a DOT with extra pixels use immediate original-texture rendering. Thus packs
outside this narrow Classic pixel model retain their artwork. Compatible packs
are read from the resource manager, not hardcoded replacement art. The sampler,
render pipeline and minimal existing HUD Mixin remain unchanged.

Vanilla retains HUD visibility, debug/spectator behavior and attack indicators.
The controller resets on fallback/hidden extraction, option changes, resource
reload and client ticks observing hidden HUD, third person, absent player or a
changed world. No exit animation delays restoration of Vanilla or draws outside
its visibility gate. No sidecar, rule engine, theme or semantic framework returns.

## Validation

`gradlew.bat clean build runClientGameTest --offline`: PASS, exit 0, final run
32 seconds. Existing JDK 25.0.3, Gradle 9.7.1, Loom/Fabric/Minecraft versions
were retained; no runtime dependency was added.

- 76,625 deterministic assertions using actual PNG masks: all type/mode pairs,
  exact endpoints, shared center/outline, bounded coverage, 5ms rapid interruptions,
  reset and OFF/fallback behavior. The number counts assertions, not scenarios.
- Existing real-client mining/crossbow/HUD parity suite passes with animation OFF.
- Actual wrapped HUD with EXPAND verifies custom animated pixel submission,
  one full-strength center, no Vanilla central sprite, and disable during motion.
- All three animation commands execute through the real client dispatcher.
- 160 deterministic production-renderer samples validate inverse pipeline,
  source coordinates, RGB strength, bounded coordinates and no repeated pixels.
- Actual resource reload replaces the mask generation successfully.
- World disconnect/re-entry and normal test-client shutdown pass.
- Six real-client galleries cover GUI 1/2/3 on light/dark panels. Final screenshots
  were inspected. A test timing issue initially captured the reload overlay;
  the test now waits until the overlay is absent before collecting evidence.
- GUI 2 screenshot pixel audit: all 30 centers per panel have the same RGB,
  (203,179,205) on dark and (43,43,51) on light, including intermediate frames.
- Final JAR retains exactly four original PNGs byte-for-byte, unchanged notices
  and licenses; no sidecar/rule/semantic or test classes are packaged.

Evidence:

- [Final build/client log](validation/animation-final.log)
- [Artifact and screenshot-pixel audit](validation/main-crosshair-animation/artifact-audit.txt)
- GUI 1: [dark](validation/main-crosshair-animation/animation-scale-1-dark.png), [light](validation/main-crosshair-animation/animation-scale-1-light.png)
- GUI 2: [dark](validation/main-crosshair-animation/animation-scale-2-dark.png), [light](validation/main-crosshair-animation/animation-scale-2-light.png)
- GUI 3: [dark](validation/main-crosshair-animation/animation-scale-3-dark.png), [light](validation/main-crosshair-animation/animation-scale-3-light.png)

## Visual assessment and limits

FADE preserves a crisp fixed silhouette throughout. EXPAND has a compact, softer
20?50ms phase, then converges to the original outline/strokes without overshoot.
The center remains visible throughout both. EXPAND is the initial default for
the requested unfolding effect; FADE remains available for a crisper preference.
ERROR's diagonal appears quickly while its established outline stays stable.

These are deterministic frame samples and automated client checks, not a claim
of manual motion acceptance. Playtest rapid air/block/entity sweeps, foliage,
snow/caves, real low FPS, F1/camera changes and the intended modpack. In
particular, assess whether the short soft phase is pleasant in motion. Unsupported
resource-pack fallback is implemented defensively; arbitrary packs have not been
exhaustively tested. Inherited anisotropic-option/Realms/harness warnings remain
visible in the successful log.

The deleted historical stage plan is no longer a reference. The workspace
AGENTS.md was updated to record the user's instruction (outside the independent
26.3 Git repository). No subsequent product feature was started.
