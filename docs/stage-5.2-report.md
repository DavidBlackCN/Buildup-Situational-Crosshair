# Stage 5.2 — Classic+ Sidecar Redesign

## Result: PASS

Version **0.1.0-alpha.5.2+mc26.3**, branch **26.3**, starting from Stage 5.1
commit **8b240e385f66371daa666f2664e488f4c6d0c72e**. Stage 6 is not started.
No previous commits were rewritten. The dedicated workspace Stage 5.2 prompt
supersedes the older micro-hint policy. Workspace AGENTS.md now reflects fixed
subordinate action/state roles; it remains outside the production repository.

**Classic+ uses sidecars as immersive contextual cues. The base crosshair remains the visual anchor; sidecars are expressive enough to be intentional designs, but visually subordinate enough to preserve Vanilla immersion.**

**Small does not automatically mean immersive. A cue must have enough visual structure to read as intentional; visual hierarchy, not microscopic size, is what prevents it from stealing focus.**

## Bounded exploration and selected design

Twenty-four real screenshots compare the same six scenes (INTERACT, PLACE,
TRANSFORM, target USE, entity INTERACT, BLOCKED placement) at GUI 1/2/3 on dark
and light backgrounds. Exploration remains test-only, excluded from the mod jar.

| Treatment | Finding |
| --- | --- |
| A — 6×6 compact | Clearer than 3px hints, but tightly compressed hand/action structure and asymmetric contact mark still look terse at GUI 1. |
| B — 7×7 balanced | Enough room for complete cube/tool/contact silhouettes without the heavier 8px stroke mass. Initial INTERACT brackets resemble a second crosshair. |
| C — 8×8 expressive | Clearest large outline, but its heavier center/outline competes with Classic at GUI 3. Shadow envelope also expands the full layout to 37px. Rejected. |
| B-refined — selected 7×7 | A touch/hand replaces INTERACT brackets; stronger partial shadow improves light-panel readability. It keeps B's bounded size and sparse other silhouettes. |

[Exploration comparison index](validation/stage-5.2-exploration/README.md) links
all candidates at all tested scales/backgrounds. Codex inspected the eighteen
initial and six refined screenshots before finalizing production styling.

Production ships five independently authored code-defined designs: right hand,
cube, generic wrench and contact sparkle; rare left lock. No axe classification,
tool-specific variants, mod artwork or held-item sprite is added. New code/icons
fall under the existing independent MIT code scope. Classic artwork keeps its
original CC BY-NC-SA 4.0 terms and notices.

- Right foreground: 7×7 design, at **(17,4)** relative to unchanged base origin,
  **68%** final alpha. Left: **(-10,4)**, **52%** final alpha.
- Shadow: one bottom/right offset, **80%** of each foreground alpha, without a
  full halo. Design/shadow envelope is at most 8×8 per side.
- Resting canvas gaps: **2 GUI px**. Full layout/motion envelope:
  **35×15 GUI px**, x [-10,25), y [0,15). No inverse GUI scaling.
- Typed left/right enums prevent swapping ordinary actions into the state slot.
  Left BLOCKED must have a visible right action. Maximum auxiliary count: two.
- SUBTLE: **100ms** cubic ease-out, appearance 45%→100% of selected weight,
  integer docking offset at most **2px** outward. OFF is immediate. Base always
  stays fully opaque, immediate and stationary. No loop or one-shot accent added.

## Policy and architecture

See [Classic+](classic-plus.md) for the complete policy matrix and scene legend.

INTERACT, PLACE, TRANSFORM and eligible targeted USE show right-side action cues
on recognized BLOCK/ATTACK bases. Air and uncertain DOT remain base-only. Known
native self-use and SPECIAL stay quiet. CHARGING and COOLDOWN have no generic
sidecar; Vanilla attack indicators retain their existing rendering path.

Reliable effective BLOCKED (AUTHORITATIVE/EXACT/STRONG evidence) displays left
lock + base + right action. No blocked action variant duplicates the state.
Secondary INVALID remains silent rather than adding an unclear second symbol.
MINE/INVALID is ERROR only. Effective native charged-crossbow USE/READY keeps the
compact ATTACK-style base alias; it does not become semantic ATTACK. Other READY
remains quiet. Normal chest INTERACT still wins visually over an ineffective
held charged crossbow because presentation uses resolved evidence.

Semantic/rule sources and schema are byte-unchanged from baseline. The pure
PresentationResolver maps effective results without re-ranking the candidate
pool. Renderer receives only typed composition and animation values. It contains
no world, held-item or rule query. The HUD layer and minimal suppression Mixin
are unchanged. Glyph definitions are cached; no runtime rasterization, full-screen
effects or new framework dependency is introduced.

Changing meaning is immediate, even on the first animation frame. Only current
sidecars are drawn. The two entry clocks are independent: removing/adding left
state does not restart stable right action; a changed action re-docks its state.
Base-only changes do not replay unchanged cues. Hide/fallback clears history.

## Visual acceptance review

Final production screenshots were inspected at actual GUI scales 1/2/3 on both
backgrounds, along with deterministic 0/25/50/100ms docking samples.

| Criterion | Review |
| --- | --- |
| Base dominance | Classic contours stay the higher-contrast focal anchor. Muted warm sidecars are sparse and offset from center. |
| Recognizability | Hand, cube, wrench, contact mark and lock are complete silhouettes at GUI 1, easier to read at GUI 2/3; no single-pixel corner blemish remains. |
| Immersion | Most scenes retain Classic alone or one muted action cue. There is no top/bottom status cluster or boxed toolbar. |
| Spacing | Two-pixel canvas gaps keep the sides attached as context, without touching the Classic alpha mask. |
| Density | 16 representative scenes: 9 base-only, 5 with right action, 2 meaningful BLOCKED scenes with both sides. |
| Dark/light | Warm foreground separates on dark; a stronger partial shadow carries the contour on light. Sidecars stay lower contrast than Classic. |
| Motion | Samples show a short bounded settle and stable base. No old-meaning crossfade, loop or tail is present. |
| Long-play plausibility | No large bright fills, repeated motion or ubiquitous state icons. Natural-play comfort remains a manual check, not a screenshot claim. |

Production gallery: GUI [1 dark](validation/stage-5.2-gallery-scale-1-dark.png) /
[1 light](validation/stage-5.2-gallery-scale-1-light.png),
[2 dark](validation/stage-5.2-gallery-scale-2-dark.png) /
[2 light](validation/stage-5.2-gallery-scale-2-light.png),
[3 dark](validation/stage-5.2-gallery-scale-3-dark.png) /
[3 light](validation/stage-5.2-gallery-scale-3-light.png).
Docking: [dark](validation/stage-5.2-docking-scale-2-dark.png) /
[light](validation/stage-5.2-docking-scale-2-light.png).

## Stage comparison

| Criterion | Stage 5 | Stage 5.1 | Stage 5.2 |
| --- | --- | --- | --- |
| Auxiliary components | Up to 3 | At most 1 | 0–1 normally; 2 for reliable BLOCKED |
| Icon design space | 5×5 with 7×7 halo | 3×3 with 4×4 shadow | 7×7 with 8×8 partial shadow |
| Maximum layout envelope | 23×16 | 15×15 | 35×15 |
| Base dominance | Action/status cluster competes | Base strong, cues too terse | Strong anchor; muted fixed-role sides |
| Status policy | Five independent state icons | All secondary states silent except base READY alias | Same silence, plus rare qualifying BLOCKED left cue |
| Animation | 100ms detail fade | 100ms micro fade | Independent 100ms fade + ≤2px outward docking |
| GUI readability | Rich but crowded | Excellent density, micro blemish problem | Complete sidecar silhouettes at GUI 1/2/3 |
| Immersive intent | Too much functional information | Restraint at expense of visual structure | Intentional expressive context with strict hierarchy |

## Validation

Toolchain unchanged: Minecraft 26.3, JDK 25.0.3, Loader 0.19.5, Fabric API
0.161.0+26.3, Loom 1.18.2, Gradle 9.7.1. Existing 26.3 dependency signatures were
checked for Player attack-strength reset and Options attack indicator.

- `clean build runClientGameTest`: **PASS**, production jar generated.
- **500 pure presentation/transition checks**: exact Classic mapping with every
  target/action/state combination; role/visibility invariants; selective native
  self-use; loaded-crossbow main/off provenance; conservative uncertain/invalid
  bases; reliable versus inferred BLOCKED; bounded, monotonic docking; independent
  state clocks; base changes, rapid switches, OFF, hide/fallback and clock rollback.
- Existing **85 semantic** and **88 JSON rule** checks: **PASS**.
- **882 Classic+ client checks**: actual commands, native chest/blocked placement,
  native bow/crossbow semantics and visuals; all four Classic bases × legal action
  and state compositions at 0/25/50/100ms; exact foreground coordinates, both
  alpha weights, actual PNG collision checks, 35×15 envelope, one-pixel alignment,
  no duplicate alpha blending; actual rapid target switches, F1/third-person and
  fallback; Vanilla attack indicator directly observed after cooldown reset.
- Existing **16 Classic**, **7 HUD visibility**, **44 native capability** and
  **19 real resource/reload** checks: **PASS**.
- **24 exploration + 6 final gallery + 2 docking screenshots** saved and visually
  inspected. Actual GUI scale is asserted, not merely taken from the option.
  GUI 1/2 are 854×480; GUI 3 is 1280×960 with GUI dimensions recalculated first.
- Normal `runClient`: **PASS** for alpha.5.2 loading, entrypoint initialization,
  resource atlas loading and rule-manager startup. No manual menu/gameplay claim.
- Four historical PNG SHA-256 values match local 1.20.1; metadata is client-only;
  test/exploration classes and assets are excluded from the production jar.
  Root has no .git; original reference directory is untouched by this stage.
  No critical Mixin failure or missing Classic texture appears in logs.

Logs: [exploration](validation/stage-5.2-exploration.log),
[clean build and game tests](validation/stage-5.2-build-gametest.log),
[normal startup](validation/stage-5.2-client.log).

## Known limitations and manual checks

- Static flat panels and deterministic motion phases cannot prove comfort over
  arbitrary scenery, high DPI, different packs or long natural aiming sessions.
  Manual survival/creative hand swapping, spectator/spyglass and other HUD mods
  remain useful checks; previous manual Stage 3/4 cases remain pending.
- Targeted USE remains a conservative existing-origin/target policy. Unknown
  modded self-use over a target is not universally classified.
- Secondary INVALID and unsupported/uncertain primary meanings are intentionally
  not visualized. Their semantics and rule states are retained for later tooling.
- Theme/animation settings still last only for the session; persistence is Stage 7.
- Existing development warnings (offline Realms authentication, test anisotropic
  option, empty client-resource directory, end_of_frame post effect) remain.
  Malformed-rule fixture warnings are intentional; all checks still pass.

## Next

Stage 6 — Debug & Diagnostics.
