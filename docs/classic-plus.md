# Classic / Classic+ presentation

Buildup Situational Crosshair is an immersive contextual hint system, not a dense functional HUD.

Semantic information may intentionally remain invisible when Minecraft already communicates it adequately or when its visual value does not justify occupying the center of the screen.

**Classic+ uses sidecars as immersive contextual cues. The base crosshair remains the visual anchor; sidecars are expressive enough to be intentional designs, but visually subordinate enough to preserve Vanilla immersion.**

**Small does not automatically mean immersive. A cue must have enough visual structure to read as intentional; visual hierarchy, not microscopic size, is what prevents it from stealing focus.**

**Immersion is achieved through hierarchy and coherence, not merely by minimizing pixel count.**

**The sidecar is allowed to be a complete visual object. It remains immersive because it is subordinate, selective, stylistically unified with the base, and shown only when it adds meaningful contextual information.**

Semantic State != Visible Icon remains the policy boundary. Stage 5.3 replaces
Stage 5.2's runtime-filled glyphs with original sprites while preserving its
left/right composition, selective visibility and redundancy decisions. Classic
is unchanged: original mapper, four byte-identical PNGs, no sidecars.

## Selection and visual budget

Eighteen real screenshots compare A (8px), B (10px), C (12px) action families,
with smaller 6/8/10px left locks, on nine identical scenes at actual GUI 1/2/3,
dark/light. Each also includes an original axe-shaped test prototype. The
selected **B-refined 10px** action family has a press/hand with cuff, a cube with
facet and grounding stroke, an open-headed wrench with a hollow handle end,
and a target-contact droplet. The left lock remains 8px and lighter.

| Element | Layout and weight |
| --- | --- |
| Classic base | Original centered 15x15 canvas; original full-strength inversion, stationary |
| Right action | 10x10 canvas at (18,2), relative to base origin; 70% RGB/alpha weight |
| Left state | 8x8 canvas at (-11,3); 52% RGB/alpha weight |
| Canvas gaps | 3 GUI px at rest, at least 1px during docking |
| Maximum layout/motion envelope | 39x15 GUI pixels, x [-11,28), y [0,15) |

The same texture/CROSSHAIR inversion pipeline now draws base and sidecars.
Classic's opaque source pixels are white; the old warm-white fill plus dark
shadow used a different material. New sprites use matching white, neutral
#c8c8c8 secondary and sparse #707070 recessed edges, plus transparency. There
is no full outline halo. A strength multiplier attenuates RGB as well as alpha:
alpha alone does not fade INVERT RGB blending. Palette consistency was reviewed
in actual dark/light screenshots, rather than inferred from the source RGBs.

Natural GUI scaling, integer anchors, explicit nearest-neighbor metadata and
thin outlines preserve clarity. Most states have zero or one sidecar; rare
reliable blocked actions have two. Larger artwork is subordinate through
contrast, negative space, limited frequency and fixed role separation.

## Production policy

| Resolved semantic fact | Classic+ presentation |
| --- | --- |
| Air / MISS | DOT only |
| Normal MINE | BLOCK, with eligible secondary action on the right |
| MINE / INVALID | ERROR only, no redundant invalid cue or secondary sidecars |
| Entity ATTACK, including COOLDOWN | ATTACK, with eligible secondary action; Vanilla attack indicator retained |
| INTERACT / PLACE / TRANSFORM, NORMAL | Right action sidecar on a recognized BLOCK/ATTACK base |
| Targeted USE / NORMAL | Right contact cue under the same conservative gate |
| Reliable BLOCKED target action | Left lock + base + right action; no blocked variant or extra symbol |
| CHARGING / secondary COOLDOWN / secondary INVALID | Base only |
| Effective native loaded crossbow USE / READY | ATTACK-style base only; semantic result remains SECONDARY USE/READY |
| Other READY | Base only |
| SPECIAL | Base only |

BLOCKED requires existing effective evidence with AUTHORITATIVE, EXACT or STRONG
confidence, a recognized base and an eligible action. It does not inspect or
re-rank the candidate pool. Unknown/inferred blocked actions remain visually
silent. A left state can never appear without a qualifying right action.

USE on MISS and known native consumable, shield/blocking-item, spyglass and ranged
self-use remain suppressed. Other targeted NORMAL/BLOCKED USE on BLOCK/ENTITY is
eligible. This is a conservative target gate, not perfect classification of every
modded item. Pack rules keep their semantics. Uncertain primary results mapped
to DOT have no sidecars, so a full icon cannot overpower the single dot.

Loaded-crossbow appearance still requires the resolved native crossbow/main or
crossbow/off origin and that same hand's charged-crossbow fact. A READY bow,
unrelated charged hand or generic pack READY cannot trigger it. An effective
chest interaction over a held crossbow still shows BLOCK + right INTERACT.

## Motion

OFF is immediate at the selected visual weights. SUBTLE independently docks the
current sides over **100ms**, using cubic ease-out: inversion strength rises
from 45% to full selected weight, while each side moves outward by at most **2
integer GUI pixels**. The base never moves, fades or scales. Final anchors are
unchanged by animation; the motion envelope remains within 39×15.

Meaning updates immediately. Old sidecars disappear immediately, without trails
or exit animation. Removing a left state does not replay an unchanged right
action. Base-only changes also leave an unchanged action stable. Hidden/Vanilla
fallback resets history. There are no pulses, loops, bounces, rotations, repeated
state jolts or extra animation modes.

## Architecture and commands

```text
Resolved semantic state
  → PresentationResolver (selective policy)
  → CrosshairPresentation (visibility, base, typed left state, typed right action)
  → TransitionController.Frame (per-side opacity and integer offset)
  → CrosshairHudRenderer / CrosshairDecorationRenderer
```

The renderer queries no target, held item, block state, evidence or rule. Semantic
and rule source/schema are unchanged; the semantic layer imports no presentation
classes. Five original sprite identifiers/dimensions are cached; Minecraft loads
resources normally. There is no runtime rasterization or texture generation.
Historical Classic assets and license attribution remain unchanged.
The original HUD layer and named-sprite suppression still delegate visibility
and attack indicators to Vanilla.

Client commands, in a world:

```text
/crosshair theme classic
/crosshair theme classic_plus
/crosshair animation off
/crosshair animation subtle
```

Defaults remain Classic+ / SUBTLE. Choices last for this client launch across
world changes. Feedback supports English/Chinese. No server installation or
permission is required. Persistent configuration belongs to Stage 7.

## Tool-like prototypes and future hint boundary

Original axe variants exist in all three test families, excluded from the mod
jar. Production deliberately keeps generic TRANSFORM: effective candidates do
not carry a typed tool-category presentation hint. Inferring from held ItemStack
or fragile origin strings would weaken the renderer/presentation boundary.
A future evidence/provider-to-presentation hint can select dedicated tool sprites
without changing ActionKind or making the renderer query the world. No hint
framework or tool-class semantic enum is implemented in Stage 5.3.

## Visual evidence

- [A/B/C exploration index](validation/stage-5.3-exploration/README.md)
- GUI 1: [dark](validation/stage-5.3-gallery-scale-1-dark.png), [light](validation/stage-5.3-gallery-scale-1-light.png)
- GUI 2: [dark](validation/stage-5.3-gallery-scale-2-dark.png), [light](validation/stage-5.3-gallery-scale-2-light.png)
- GUI 3: [dark](validation/stage-5.3-gallery-scale-3-dark.png), [light](validation/stage-5.3-gallery-scale-3-light.png)
- Docking: [dark](validation/stage-5.3-docking-scale-2-dark.png), [light](validation/stage-5.3-docking-scale-2-light.png)
- Gameplay: [block](validation/stage-5.3-gameplay-normal-block.png), [chest](validation/stage-5.3-gameplay-chest.png), [log transform](validation/stage-5.3-gameplay-transform-log.png), [villager](validation/stage-5.3-gameplay-villager.png)

Sixteen gallery scenes, left-to-right/top-to-bottom: Air, Block, Bad tool,
Entity, Chest, Place, Convert, Crop use, Trade, Bow draw, Xbow rdy, Atk CD,
Blocked placement, Invalid secondary, Special, Locked UI. Only the two blocked
scenes have both sides. Quiet states deliberately stay base-only. Gallery labels
and backgrounds are test-only. Gameplay evidence uses server-backed scenery,
actual camera raycasts and the normal production HUD, without a synthetic Screen
or manually assigned hitResult. See the [Stage 5.3 report](stage-5.3-report.md)
for review findings and remaining human long-play checks.
