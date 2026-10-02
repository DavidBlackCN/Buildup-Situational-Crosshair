# Semantic Core (Stages 2–5)

Authority: workspace-root `Buildup-Situational-Crosshair-Development-Stages.md`,
global principles and Stages 2–5. This document describes the implemented contract.

## Pipeline

Client composition root → visibility preflight / ContextCapture → immutable
CrosshairContextSnapshot → CandidateProvider / per-cycle CandidateCollector →
CrosshairResolver → CrosshairSemanticState → PresentationResolver (Classic/Classic+)
→ CrosshairPresentation → TransitionController → HUD renderer.

PRIMARY is the left-click action channel; SECONDARY is the right-click/use channel.
They are resolved independently, never as first/second place in a shared ranking.

## Facts and vocabulary

Snapshots contain target type, visibility,
creative state, breakability, harvestability, and both hands' ranged identity,
stored charge and use state, attack cooldown, spectator state and ordered native
use attempts (PASS / ACTION / UNKNOWN, hand, action/state and evidence), plus
rule selector identities/tags/properties and sneaking/use flags when rules are active.
No mutable ItemStack/player/world escapes capture.
All providers see the same snapshot object.
No provider invokes global Minecraft or executes world interactions.

TargetType: MISS, BLOCK, ENTITY.
ActionSlot: PRIMARY, SECONDARY.
ActionKind: NONE, MINE, ATTACK, INTERACT, USE, PLACE, TRANSFORM, SPECIAL.
ActionState: NORMAL, CHARGING, READY, COOLDOWN, BLOCKED, INVALID.
Visibility: SHOW, VANILLA, HIDE.
ERROR exists only in Classic presentation.

## Candidates and resolution

Each candidate carries slot/action/state, CandidateSource, Specificity, Confidence,
priority in [-100,100], and a validated namespaced origin string. Origin uses the
Minecraft identifier syntax without introducing game dependencies into the core.
Slot/action combinations, NONE state, nulls, identifiers and priority are validated.
The collector is cycle-local, deduplicates identical proposals, retains conflicting
proposals, and returns immutable deterministic lists.

Ordering within a slot is lexicographic: specificity, source authority, bounded local
priority, confidence, origin, then stable slot/action/state ties. Enum order explicitly
defines specificity/source/confidence order. There is no combined numeric score.
INFERRED and UNKNOWN candidates remain inspectable but cannot become selected hints.
With no reliable candidate, the slot resolves to NONE/NORMAL.
Stage 4 applies deny and per-slot override before ordinary resolution, and fallback
only when no reliable ordinary action remains. Priority alone cannot defeat stronger
specificity/source evidence. See [rule semantics](rules-v0.1.md) for exact mode order.

Visibility short-circuits provider collection for HIDE and VANILLA. Client preflight
handles absent context, F1 and third person. Vanilla's actual draw request remains
the final visibility gate for spectator, spyglass, debug and GUI states, preserving
Stage 1's exact suppression mechanism rather than duplicating complex vanilla logic.

## Active providers

- BaseTargetProvider: conservative NONE defaults for both slots.
- HarvestProvider: block PRIMARY MINE/NORMAL or MINE/INVALID from current harvest facts;
  spectator mining is BLOCKED.
- EntityAttackProvider: entity-hit PRIMARY ATTACK/NORMAL, COOLDOWN or spectator BLOCKED;
  this is an action-channel classification, not a guarantee of server damage permission.
- VanillaUseProvider: consumes ordered, immutable native probes to select the
  effective SECONDARY action. PASS continues; UNKNOWN stops conservatively;
  cooldown permits trying the other hand. The first consuming/blocked action wins.
  ClassicCrossbowProvider remains only as a legacy contract fixture and does not
  participate in the client pipeline.

VanillaCapabilityCapture reads native block interactions, placement validation,
BLOCK_TRANSFORMER providers, bonemeal, selected entity interactions, buckets,
consumables, blocking items and ranged state. It never calls use/useOn/place or
transformBlock to discover behavior. getPlacementState requires a narrow Mixin
Invoker because it is protected; native survival/collision checks are retained.
Base block interaction method ownership is cached per class, without executing
unknown handlers. Unknown behavior prevents claiming a later hand action.
See [Stage 3 report](stage-3-report.md) for supported cases and limits.

The full source/specificity/confidence vocabulary is represented but external
provider discovery and a stable public Mod API are not implemented. JSON rule loading
now uses immutable compiled resource generations and context indexes; it does not
execute JSON parsing on the HUD path.

## Classic mapping

- MISS/NONE → DOT.
- BLOCK + PRIMARY MINE/NORMAL → BLOCK.
- BLOCK + PRIMARY MINE/INVALID → ERROR.
- ENTITY + PRIMARY ATTACK → ATTACK.
- Historical selected-hand charged crossbow → ATTACK glyph, without changing
  PRIMARY or effective SECONDARY. Hand facts travel with the semantic result;
  only ClassicPresentation chooses the historical offhand visual precedence.
- Bow READY does not imply an ATTACK glyph.
- Unknown/unrepresentable result → vanilla fallback.

Presentation is pure Java with no textures, world reads or render calls. Renderer
receives a presentation supplier and owns texture selection; it does not read
Minecraft target/player state. Stage 5 adds PresentationResolver and
CrosshairPresentation followed by TransitionController; the renderer now consumes
these presentation values. ClassicPresentation still defines the exact Classic
policy, while Classic+ adds original pixel details. The original PNGs and HUD
suppression Mixin are unchanged. See [Classic+](classic-plus.md).

## Tests

`./gradlew semanticTest` runs the dependency-free executable contract suite;
`build`/`check` depend on it. The standard JUnit task excludes this executable class
only; future JUnit tests are not disabled. `runClientGameTest` executes the real
Minecraft regression suite separately.
