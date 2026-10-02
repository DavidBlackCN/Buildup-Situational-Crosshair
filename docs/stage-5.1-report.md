# Stage 5.1 — Classic+ Visual Density & Immersive Presentation Correction

## Result: PASS

Version **0.1.0-alpha.5.1+mc26.3**. This corrective stage supersedes Stage 5's
visual policy, not its semantic capabilities. Stage 6 is not started.

Buildup Situational Crosshair is an immersive contextual hint system, not a dense functional HUD.

Semantic information may intentionally remain invisible when Minecraft already communicates it adequately or when its visual value does not justify occupying the center of the screen.

**Semantic State ≠ Visible Icon.** The workspace AGENTS.md now carries this
long-term principle; it remains outside the production Git repository.

## Changes and visual budget

- CrosshairPresentation has only visibility, base and optional modifier. Both
  independent status channels, all five status glyphs and their layout anchors
  are removed. Semantic ActionKind/ActionState, resolver, providers, evidence and
  JSON rule schema are unchanged. SPECIAL remains a semantic/rule action.
- Four original micro shapes represent NORMAL INTERACT, PLACE, TRANSFORM and
  target-relevant USE. Their foreground is 3×3, with a bottom/right-only drop
  shadow. The complete 4×4 envelope is at (11,11) inside the original 15×15 canvas.
  Default maximum footprint is **15×15 GUI pixels**, with at most **one** auxiliary
  component. No full halo, inverse GUI scaling, large bitmap, text or progress UI.
- Primary attack COOLDOWN has no additional icon; Vanilla attack indicators stay
  on the unchanged original rendering path. CHARGING and secondary COOLDOWN,
  BLOCKED and INVALID do not draw hints. MINE/INVALID retains ERROR without an X.
  Other valid secondary information may still accompany the target's base.
- Effective native charged-crossbow USE/READY reuses the ATTACK-style base only.
  Existing resolved origin and hand facts select the effective main/off hand;
  unrelated charged items and generic pack READY cannot fabricate this alias.
  Semantics remain SECONDARY USE/READY. Other READY has no extra visual.
- USE on MISS and SPECIAL are hidden. Known native consumable, shield/blocking,
  spyglass and ranged self-use is also hidden over BLOCK/ENTITY; other NORMAL
  targeted USE may show a drop. No semantic classifier or compatibility expansion.
- Classic still delegates to its exact existing mapper, with no decorations.
  All four source PNG hashes match local 1.20.1. Their actual alpha masks have:
  DOT 1 opaque pixel at (7,7); BLOCK 21, ATTACK 9 and ERROR 25 opaque pixels within
  x/y 4–10. Every base/modifier combination is checked against the real PNG mask.
- The 100 ms SUBTLE micro fade and OFF remain unchanged. The base is immediate,
  opaque and stationary. HudElementRegistry and minimal named-sprite suppression
  are unchanged. Detection/render separation and Vanilla visibility remain intact.

| Comparison | Stage 5 | Stage 5.1 |
| --- | --- | --- |
| Maximum default footprint | 23×16 GUI pixels | 15×15 GUI pixels |
| Simultaneous auxiliary components | Up to 3 | At most 1 |
| Independent status glyph designs | 5 | 0 |
| Secondary foreground / shadow envelope | 5×5 / 7×7 full halo | 3×3 / 4×4 partial shadow |
| Hierarchy | Base surrounded by action and two statuses | Classic base with one embedded micro hint |

## Validation

Toolchain unchanged: Minecraft 26.3, JDK 25.0.3, Loader 0.19.5, Fabric API
0.161.0+26.3, Loom 1.18.2, Gradle 9.7.1. No new dependency or framework.

- `clean build runClientGameTest`: successful. A final `build runClientGameTest`
  after correcting screenshot scale calculation also succeeds.
- **246 pure presentation/transition checks**: exact Classic mapping, selective
  action/state visibility across all targets, unchanged semantic evidence,
  loaded-crossbow main/off provenance, unrelated-hand/pack READY rejection,
  self-use suppression, visibility gates, three-field render model, four micro
  shapes and existing fade timing/reset contracts.
- Unchanged **85 semantic** and **88 rule-engine** checks pass, including rule
  indexing and state handling. No semantic/rule source or schema was modified.
- **170 Classic+ client checks**: actual client commands, real chest semantics,
  one custom base/no Vanilla duplicate, F1/third-person/fallback without ghosts,
  Classic without decoration, all four base × five modifier choices (including
  NONE), exact foreground submissions and fade alpha, real loaded-crossbow
  USE/READY→ATTACK appearance, real bow CHARGING→DOT appearance, all gallery scenes
  and six assertions of actual GUI scales. Every detail submission also checks
  original alpha-mask collision, one-pixel alignment, no repeated alpha blending
  and 15×15 bounds.
- Existing **16 Classic**, **7 transformed HUD**, **44 Vanilla capability** and
  **19 resource/reload** client checks pass.
- Normal `runClient` loads alpha.5.1 and initializes the client entrypoint,
  Minecraft resource atlases and rule manager. This is startup/log validation;
  no manual natural gameplay or desktop-menu inspection is claimed.
- Production jar exists, is client-only, excludes test classes/resources and
  retains MIT code plus original CC BY-NC-SA 4.0 artwork notices. Workspace root
  has no .git. No missing-texture or critical Mixin failure appears in the logs.

Evidence:

- [Clean build and client tests](validation/stage-5.1-build-gametest.log)
- [Final build, tests and corrected gallery scales](validation/stage-5.1-gallery-gametest.log)
- [Normal client startup](validation/stage-5.1-client.log)

## Gallery and Codex visual review

Fifteen representative semantic scenes pass through the production policy and
renderer. Labels are test-only. The scene legend is in [Classic+](classic-plus.md).
GUI 1/2 screenshots are 854×480; GUI 3 uses the official screenshot API at
1280×960 after recalculating GUI dimensions. The test asserts actual scale rather
than trusting the requested option. Evidence is saved outside the transient game
directory and copied into this repository.

- GUI 1: [dark](validation/stage-5.1-gallery-scale-1-dark.png), [light](validation/stage-5.1-gallery-scale-1-light.png)
- GUI 2: [dark](validation/stage-5.1-gallery-scale-2-dark.png), [light](validation/stage-5.1-gallery-scale-2-light.png)
- GUI 3: [dark](validation/stage-5.1-gallery-scale-3-dark.png), [light](validation/stage-5.1-gallery-scale-3-light.png)

Codex inspected the actual screenshots and compared the Stage 5 gallery:

| Criterion | Review |
| --- | --- |
| Center dominance | Classic contours remain identifiable and spatially dominant; hints are smaller corner marks. Air/bow charging remains the original single dot. |
| Footprint | No left status, upper status or detached cluster. The single corner hint stays inside the existing canvas. |
| GUI scale 2 | The foreground is 6×6 physical pixels versus the base's 14×14 opaque-contour extent. It remains visibly smaller. |
| Dark/light | Foreground separates from the dark panel; the partial shadow anchors the mark on the light panel without a full black halo. |
| Density | Each scenario has zero or one auxiliary hint. Charging, cooldown, blocked, invalid-secondary and SPECIAL deliberately have none. |
| Relative to Stage 5 | The action/status cluster has disappeared. Small corner hints convey less literal detail, with a much cleaner central silhouette. |

The 3×3 shapes are intentionally terse; INTERACT and PLACE differ by one corner
pixel. GUI 1 demands close attention to distinguish them. This tradeoff is
recorded rather than claiming large-icon readability or universal self-explanation.
SUBTLE remains available based on timing tests; static screenshots cannot decide
whether the fade is comfortable during rapid natural aiming.

## Known limitations and pending manual checks

- The conservative USE gate does not identify every modded self-use action on a
  target; providers/rules determine its resolved meaning. Unknown behavior is not
  inferred by presentation.
- Theme/animation choices are session-only, as before. Persistent settings belong
  to Stage 7. No settings GUI is added.
- [ ] Natural survival/creative aiming and hand swapping over varied scenery.
- [ ] Long-play comfort and 100 ms fade during rapid target changes.
- [ ] Spectator/spyglass and coexistence with other HUD mods during manual play.
- [ ] Carry forward previous Stage 3 manual cases and Stage 4 resource-pack UI test.
- Existing development/test warnings remain: offline Realms authentication,
  anisotropic-filter test option, missing empty client-resource directory and
  requested end_of_frame post effect. Malformed-rule warnings are intentional
  regression fixtures. They do not prevent the build or client tests passing.

## Next

Stage 6 — Debug & Diagnostics.
