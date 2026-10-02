# Stage 2 ? Semantic Core

Authority: workspace-root `Buildup-Situational-Crosshair-Development-Stages.md`,
global principles and Stage 2. This document describes the implemented contract.

## Pipeline

Client composition root ? visibility preflight / ContextCapture ? immutable
CrosshairContextSnapshot ? CandidateProvider / per-cycle CandidateCollector ?
CrosshairResolver ? CrosshairSemanticState ? ClassicPresentation ? HUD renderer.

PRIMARY is the left-click action channel; SECONDARY is the right-click/use channel.
They are resolved independently, never as first/second place in a shared ranking.

## Facts and vocabulary

Snapshots contain only the values needed for Classic parity: target type, visibility,
creative state, breakability, harvestability, and both hands' ranged identity,
stored charge and use state. No mutable ItemStack/player/world escapes capture.
All providers see the same snapshot object. More fields require a Stage 3 use case.
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
Deny/override/fallback rule modes are deliberately deferred to Stage 4; priority alone
cannot defeat stronger specificity/source evidence.

Visibility short-circuits provider collection for HIDE and VANILLA. Client preflight
handles absent context, F1 and third person. Vanilla's actual draw request remains
the final visibility gate for spectator, spyglass, debug and GUI states, preserving
Stage 1's exact suppression mechanism rather than duplicating complex vanilla logic.

## Active providers

- BaseTargetProvider: conservative NONE defaults for both slots.
- HarvestProvider: block PRIMARY MINE/NORMAL or MINE/INVALID from current harvest facts.
- EntityAttackProvider: entity-hit PRIMARY ATTACK/NORMAL, matching the local baseline;
  this is an action-channel classification, not a guarantee of server damage permission.
- ClassicCrossbowProvider: only the pre-existing charged-crossbow exception,
  SECONDARY USE/READY with historical offhand precedence. No new bow charging,
  ammo discovery, effective-use simulation or general ranged capability system.

The full source/specificity/confidence vocabulary is represented but external
provider discovery, public Mod API and JSON rule loading are not implemented.

## Classic mapping

- MISS/NONE ? DOT.
- BLOCK + PRIMARY MINE/NORMAL ? BLOCK.
- BLOCK + PRIMARY MINE/INVALID ? ERROR.
- ENTITY + PRIMARY ATTACK ? ATTACK.
- Existing SECONDARY USE/READY charged-crossbow result ? ATTACK glyph, without
  changing PRIMARY to ATTACK.
- Unknown/unrepresentable result ? vanilla fallback.

Presentation is pure Java with no textures, world reads or render calls. Renderer
receives a presentation supplier and owns texture selection; it does not read
Minecraft target/player state. Textures, blend pipeline and minimal Mixin are unchanged.

## Tests

`./gradlew semanticTest` runs the dependency-free executable contract suite;
`build`/`check` depend on it. The standard JUnit task excludes this executable class
only; future JUnit tests are not disabled. `runClientGameTest` executes the real
Minecraft regression suite separately.
