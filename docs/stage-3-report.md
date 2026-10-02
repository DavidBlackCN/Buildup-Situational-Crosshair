# Stage 3 — Vanilla 26.3 Capability Providers

## Result: PARTIAL (implementation and automation complete; manual matrix pending)

Version: **0.1.0-alpha.3+mc26.3**. Scope follows the workspace-root development
stages specification §§3.1–3.10. Stage 4 has not started. The specification's
§3.8 explicitly requires manual validation; automated client checks below do not
claim to satisfy that requirement.

Toolchain unchanged: Minecraft 26.3, Oracle JDK 25.0.3, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3, Loom 1.18.2, Gradle 9.7.1.

## Implementation

- Native capture publishes ordered, immutable use attempts. The pure provider
  follows main/offhand and target/item execution order; unknown earlier handlers
  stop speculation. Cooldown permits an effective offhand action.
- PRIMARY remains independently resolved (harvest, attack, attack cooldown).
  Spectator gameplay mining/attack is blocked and secondary gameplay use suppressed.
- SECONDARY distinguishes target INTERACT, item USE, PLACE and TRANSFORM.
  Chest interaction wins over held-block placement; sneaking bypasses interaction
  when either hand is occupied. An active use action takes precedence over a new target.
- Block transformation reads `DataComponents.BLOCK_TRANSFORMER` and its actual
  face restrictions, rule predicates and state providers. Simple, copy-properties
  and recursively deterministic rule providers are supported. No axe/hoe/shovel
  lookup table is used. A private random source avoids consuming world RNG.
- Placement uses `BlockPlaceContext`, `updatePlacementContext`, permissions,
  world border, replacement and native survival/collision validation. A minimal
  `BlockItemAccessor` invokes protected `getPlacementState`; no placement executes.
- Bow/crossbow state uses ammunition, stored projectiles, active use and elapsed
  use ticks. Ranged actions are USE, with CHARGING/READY, never semantic ATTACK.
- Detection never calls mutating use/useOn/place/transformBlock to test an action.
  Unknown block overrides, entity behavior and item use fall back conservatively.

API evidence: actual resolved 26.3 Minecraft/Fabric classes and bytecode, together
with the [official Fabric 26.3 announcement](https://fabricmc.net/2026/09/15/263.html).
The public transformer execution method mutates the world; its read-only data
providers are inspected instead.

## Rendering and Classic differences

The HudElementRegistry integration and narrow vanilla sprite suppression are
unchanged. Four original 15×15 Classic PNGs are unchanged. No new glyphs, modifiers,
GUI or JSON rules were added. Rich SECONDARY actions currently have no new visual.

Classic charged-crossbow appearance uses the historical selected-hand rule
independently from the effective action. Thus a fully drawn bow does not acquire
the ATTACK glyph, while the existing crossbow appearance stays compatible.
Spectator block mining now resolves BLOCKED and falls back to the vanilla layer;
vanilla still decides whether an interactive spectator crosshair is visible.

## Validation

- `clean build runClientGameTest`: **BUILD SUCCESSFUL**, exit 0; recorded in
  `validation/stage-3-build-gametest.log`. Production jar generated successfully.
- **85** pure contract checks passed, covering deterministic independent slots, confidence,
  immutable snapshots, effective action order, unknown barriers, cooldown/offhand
  fallback and Classic mapping.
- Real Minecraft client tests retain 16 Classic cases and 7 transformed HUD
  visibility/submission cases, plus **44** Stage 3 capability checks, all passing.
- Normal `runClient` launched separately and logged the alpha.3 mod entrypoint
  and completed resource initialization. Startup evidence: `validation/stage-3-client.log`.
- All four texture SHA-256 hashes match the local 1.20.1 originals; the retained
  CC license hash also matches. Production jar contains both license texts and
  notices, client-only metadata and no test classes. Root has no `.git`.
- Test fixtures manipulate a temporary client world/hands/hit results for most
  adapter checks; the full-draw bow check uses actual server inventory and ticks.
  These are not end-to-end guarantees of server-authoritative interaction results.

| Scenario | Automated coverage | Manual |
| --- | --- | --- |
| Air, normal block, correct/incorrect tool, unbreakable, creative | Classic regression | Pending |
| Chest normal/sneaking, cooldown held item, wooden/iron door, button | Native adapter | Pending |
| Villager, hostile target, sheep/shears, boat | Native adapter | Pending |
| Block/torch placement, occupied destination | Native adapter | Pending |
| Bone meal on growing/mature crop | Native adapter | Pending |
| Empty water pickup / filled water bucket | Fluid-aware native raycast | Pending |
| Axe, shovel, hoe, disallowed face, obstruction | Component providers | Pending |
| Transformer component on generic stick | Data-driven recognition and no world/item mutation | Pending |
| Shield blocks main-hand transform intent | Native adapter | Pending |
| Bow charging/full draw, crossbow charging/loaded | Native adapter + real tick full draw | Pending |
| Attack cooldown, item cooldown, offhand fallback | Native adapter + pure contracts | Pending |
| Consumable hunger eligibility | Native component | Pending |
| Spectator gameplay restrictions | Native adapter | Pending |
| F1, spyglass, first/third person, spectator HUD | Existing HUD regression | Pending |

Also manually check natural targeting, hand swaps, GUI transitions, multiple GUI
scales and other HUD mods. No desktop UI automation was used this stage.

## Licensing

At the user's confirmed direction, independently authored code/documentation uses
MIT. Original artwork and adaptations retain their original terms. The previous
license is preserved verbatim in `licenses/CC-BY-NC-SA-4.0.txt`; attribution and
conflicting upstream distribution statements remain in THIRD_PARTY_NOTICES.md.
The jar includes both license texts and notices. Metadata lists both licenses;
this is a mixed-license distribution, not a relicensing of the original artwork.

## Known limits

- Native coverage is conservative, not exhaustive. Unknown custom handlers,
  probabilistic/noise state providers, waterlogged/special bucket pickup and
  entity interactions outside the supported cases yield no strong use hint.
- Client-visible permissions cannot predict server plugins or protected-region
  decisions. Full adventure-mode constraints are not covered by this stage's matrix.
- Classic remains visually limited to four glyphs; new secondary semantics are
  intentionally not exposed through new graphics yet.
- Existing offline Realms authentication and Fabric test-option/classpath warnings
  may appear in development logs; they are separate from mod loading/rendering.
  The normal client also logged a minimized-window surface warning; the test
  harness logged a missing `minecraft:end_of_frame` post effect. No critical
  Mixin failure, missing Classic texture or Fabric metadata error was found.
- Required manual acceptance remains outstanding.

## Next

Stage 4 — JSON Rule Engine v0.1.
