# Stage 2 — Semantic Core

## Result: PASS

Implemented against workspace-root `Buildup-Situational-Crosshair-Development-Stages.md`
(global principles and Stage 2 §§2.1–2.11). Stage 3 is not started.
Version: **0.1.0-alpha.2+mc26.3**.

Toolchain unchanged: Minecraft 26.3, Oracle JDK 25.0.3, Fabric Loader 0.19.5,
Fabric API 0.161.0+26.3, Loom 1.18.2, Gradle 9.7.1.

## Architecture and acceptance

| Requirement | Implementation |
| --- | --- |
| Immutable context | ContextCapture reads game facts once; value-only CrosshairContextSnapshot shared by all providers |
| Stable types | TargetType, ActionSlot, ActionKind, ActionState, Visibility; no texture knowledge |
| Candidate metadata | Slot/action/state, source, specificity, confidence, bounded priority, namespaced origin |
| Collector | Per-cycle CandidateCollector; validated candidates; immutable deterministic iteration and deduplication |
| Providers | BaseTargetProvider, HarvestProvider, EntityAttackProvider, limited ClassicCrossbowProvider |
| Independent channels | CrosshairResolver resolves PRIMARY and SECONDARY separately; both winners retained |
| Semantic result | CrosshairSemanticState with per-slot action/state/evidence and candidate pool |
| Classic mapping | Pure ClassicPresentation maps semantic state to four existing glyph choices |
| Rendering boundary | Renderer receives a presentation supplier; no world/target/player reads |

Source/specificity/confidence categories follow the authoritative design. Ordering
is lexicographic, not a weighted score: specificity → source → priority [-100,100]
→ confidence → origin → stable semantic ties. Weak/unknown confidence is retained
as evidence but cannot produce strong hints. Priority cannot override stronger
runtime evidence. Rule modes are Stage 4 and are not implemented here.

ERROR is presentation only. Invalid harvest is PRIMARY MINE/INVALID. Underlying
breakability and harvestability remain separate snapshot facts. Entity ATTACK
classifies the targeted input channel; it does not promise successful server damage.

SECONDARY is normally NONE. The pre-existing charged-crossbow exception uses
SECONDARY USE/READY and maps to Classic ATTACK appearance while leaving PRIMARY
intact. Historical offhand precedence is preserved. This limited parity provider
does not implement Stage 3's comprehensive ranged/use/interaction detection.

## Visibility and vanilla suppression

HIDE/VANILLA short-circuit candidate collection. F1/third-person preflight and
missing-context fallback are explicit. Complex spectator/debug/spyglass/GUI
visibility remains gated by the actual vanilla sprite request. The Stage 1 minimal
Mixin is unchanged: no new injection points, ordinal or general draw Redirect.
Vanilla attack indicators still execute through the original HUD layer.

## Validation

- Baseline before editing: build and existing client game tests passed.
- Final `clean build runClientGameTest`: **BUILD SUCCESSFUL**, exit 0.
- `semanticTest` (part of build/check): **76 contract checks passed**, including
  all four pure presentation mappings, independent slots, illegal combinations,
  bounded priorities, confidence fallback, source authority, 32 shuffled candidate
  orders, stable ties, immutable collector results and shared snapshot identity.
- Real Minecraft client: **16 Classic behavior cases + 7 HUD visibility/submission
  cases passed** through the new pipeline. Added assertions establish independent
  break/harvest facts and snapshot stability across inventory changes.
- Normal `runClient` launched separately; startup and Semantic Core entrypoint
  checked in logs. Automated game tests also reached a loaded world and exited
  normally. No desktop UI automation was used this stage.
- Four Classic PNG hashes still match 1.20.1. Semantic package scan found no
  Minecraft imports, texture paths, PNG references or rendering dependencies.
- Build jar excludes test classes. Client-only metadata retained.

Evidence under `docs/validation/`: `stage-2-build.log`, `stage-2-gametest.log`,
`stage-2-client.log`. Detailed contract: [semantic-core.md](semantic-core.md).

## Manual checks still required

- [ ] Natural aiming visual comparison of all four glyphs at several GUI scales.
- [ ] Spectator interactive targets, GUI transitions, death/respawn and dimension transitions.
- [ ] Full attack cooldown indicator visual check and multi-tick hand swaps.
- [ ] Coexistence with other HUD mods; not covered by the automated regression.

The unchanged Stage 1 renderer/resources and passing draw-submission checks support
Classic parity, but do not constitute exhaustive pixel-level or modpack validation.

## Known issues / limitations

- Existing license provenance conflict remains documented in THIRD_PARTY_NOTICES.md.
- Offline development credentials can produce Realms authentication messages.
- Loom can list empty client-resource outputs in a classpath warning. No critical
  mixin failure or missing Classic texture was found.
- Fabric's client-game-test default options log an invalid anisotropic filtering
  value; the test harness corrects/continues and all tests pass. Not a new mod option.
- Rich native interactions, rule modes, stable public API and compatibility inference
  remain intentionally outside this stage.

## Next

Stage 3 — Vanilla 26.3 Capability Providers.
