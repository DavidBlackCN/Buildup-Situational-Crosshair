# Classic / Classic+ presentation

Buildup Situational Crosshair is an immersive contextual hint system, not a dense functional HUD.

Semantic information may intentionally remain invisible when Minecraft already communicates it adequately or when its visual value does not justify occupying the center of the screen.

**Classic+ uses sidecars as immersive contextual cues. The base crosshair remains the visual anchor; sidecars are expressive enough to be intentional designs, but visually subordinate enough to preserve Vanilla immersion.**

**Small does not automatically mean immersive. A cue must have enough visual structure to read as intentional; visual hierarchy, not microscopic size, is what prevents it from stealing focus.**

Semantic State ≠ Visible Icon remains the policy boundary. Stage 5.2 supersedes
Stage 5.1's 3px micro-hints, preserving its selective visibility and redundancy
decisions. Classic is unchanged: original mapper, four PNGs, no sidecars.

## Selection and visual budget

A bounded exploration compared 6px compact, 7px balanced and 8px expressive
families on the same six scenes at actual GUI scales 1/2/3, dark and light.
The selected **7px B-refined** family uses a touch/hand for INTERACT, a cube for
PLACE, a generic wrench for TRANSFORM and a contact sparkle for targeted USE.
The hand replaces an initial bracket shape that resembled a second crosshair.
Only reliable BLOCKED actions qualify for the rarer left lock cue.

| Element | Layout and weight |
| --- | --- |
| Classic base | Original centered 15×15 texture; fully opaque, stationary |
| Right action | 7×7 design at (17,4), relative to base origin; 68% foreground alpha |
| Left state | 7×7 design at (-10,4); 52% foreground alpha |
| Shadow | Bottom/right only, one pixel; 80% of the corresponding foreground alpha |
| Maximum layout envelope | 35×15 GUI pixels, x [-10,25), y [0,15) |

Each sidecar's 8×8 shadow envelope has a two-pixel gap from the 15px canvas at
rest. Sparse warm-white strokes and a selective dark shadow carry the silhouette;
there is no full halo, frame, high-saturation badge or inventory sprite. All
elements follow normal Minecraft GUI scale without inverse scaling. Most states
have zero or one sidecar; two sidecars explain one reliable exceptional action.

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
current sides over **100ms**, using cubic ease-out: foreground appearance rises
from 45% to full selected weight, while each side moves outward by at most **2
integer GUI pixels**. The base never moves, fades or scales. Final anchors are
unchanged by animation; the motion envelope remains within 35×15.

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
classes. Five original code-defined glyphs are cached, with no runtime texture
generation. Historical Classic assets and license attribution remain unchanged.
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

## Visual evidence

- [Test-only A/B/C and refinement comparison index](validation/stage-5.2-exploration/README.md)
- GUI 1: [dark](validation/stage-5.2-gallery-scale-1-dark.png), [light](validation/stage-5.2-gallery-scale-1-light.png)
- GUI 2: [dark](validation/stage-5.2-gallery-scale-2-dark.png), [light](validation/stage-5.2-gallery-scale-2-light.png)
- GUI 3: [dark](validation/stage-5.2-gallery-scale-3-dark.png), [light](validation/stage-5.2-gallery-scale-3-light.png)
- Production docking samples: [dark](validation/stage-5.2-docking-scale-2-dark.png), [light](validation/stage-5.2-docking-scale-2-light.png)

Sixteen production-policy scenes, left-to-right/top-to-bottom: Air, Block,
Bad tool, Entity, Chest, Place, Convert (generic TRANSFORM), Crop use, Trade,
Bow draw, Xbow rdy, Atk CD, Blocked placement, Invalid secondary, Special,
Locked UI. Only the two BLOCKED scenes have both sides. Other quiet states
deliberately remain base-only. Labels and backgrounds belong only to test screens.

The galleries use synthetic semantic scenes through the production policy and
renderer; real chest, blocked placement, bow and crossbow capture is separately
tested. Static review and deterministic motion samples do not establish long-play
comfort on every scenery/resource pack. See the [Stage 5.2 report](stage-5.2-report.md).
