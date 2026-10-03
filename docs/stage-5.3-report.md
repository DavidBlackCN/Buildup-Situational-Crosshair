# Stage 5.3 — Sprite-based Sidecar Art Redesign

## Result: PASS

Version **0.1.0-alpha.5.3+mc26.3**, branch **26.3**, baseline Stage 5.2
**e2c8a7ae35cd2a48825a8bf8a19a053e57b65d36**. No history rewritten.
Stage 6 has not started. Toolchain unchanged: Minecraft 26.3, Fabric Loader
0.19.5, Fabric API 0.161.0+26.3, Loom 1.18.2, Gradle 9.7.1, JDK 25.0.3.

**Immersion is achieved through hierarchy and coherence, not merely by minimizing pixel count.**

**The sidecar is allowed to be a complete visual object. It remains immersive because it is subordinate, selective, stylistically unified with the base, and shown only when it adds meaningful contextual information.**

## A/B/C exploration and selection

The Stage 5.2 GUI 1/2/3 evidence was inspected first. Its layout remains useful,
but small warm-white runtime fill strokes and dark shadows differ visibly from
the Classic texture inversion material. The new exploration deliberately keeps
roles and semantic scenes stable while changing the artwork and rendering path.

Eighteen actual client screenshots compare three original sprite families at
GUI 1/2/3 on dark/light backgrounds. Each contains BLOCK + INTERACT/PLACE/
TRANSFORM/target USE, ATTACK + INTERACT, BLOCKED + PLACE, base-only BLOCK and
ATTACK, and an explicitly labelled axe prototype. Exploration uses the same
SidecarSprite texture drawing helper as production, plus the production Classic
base renderer. No image-composited previews were used as gameplay evidence.

| Family | Action / state canvas | Review and decision |
| --- | --- | --- |
| A | 8×8 / 6×6 | Compact, but hand/contact and lock interiors remain cramped at GUI 1. Tool silhouette still too terse. Rejected. |
| B | 10×10 / 8×8 | Allows distinct finger/cuff, cube facet/ground, contact drop and lock structure. Keeps sparse outline weight below the base; selected for refinement. |
| C | 12×12 / 10×10 | Most expressive, but long hand and tool silhouettes draw too much focus at GUI 3, especially beside sparse ATTACK. Rejected. |

All eighteen initial screenshots were inspected before selection. Final B refines
the generic tool into a more open wrench head with a hollow handle end; it is not
a resized Stage 5.2 glyph. Original SVG art and exported test PNGs retain the
three alternatives for reproducibility, excluded from the shipped mod jar.

See [exploration index](validation/stage-5.3-exploration/README.md).

## Production art and composition

Production ships five original PNGs under
`assets/buildup_situational_crosshair/textures/gui/sidecar/`.
Editable static integer-grid SVG sources live in `art/sidecar/production/`;
an offline lossless exporter produces exact-size PNGs without scaling/filtering.
This is an authored texture family, not runtime-generated geometry or ItemStack art.
New artwork and test prototypes use MIT; inherited Classic attribution/license
remain intact and the four Classic PNGs remain byte-for-byte unchanged.

| Sprite | Design and policy |
| --- | --- |
| INTERACT | Press/hand silhouette, single finger and quiet cuff; right slot |
| PLACE | Small faceted cube with a separate grounding stroke; right slot |
| TRANSFORM | Generic open-headed wrench with hollow handle end; right slot |
| USE | Contact droplet with sparse application marks; targeted use only, right slot |
| BLOCKED | Smaller open-interior lock/keyhole; only reliable blocked actions, left slot |
| INVALID | No sprite; primary invalid mining remains ERROR, secondary invalid stays silent |
| READY | No sprite; resolved native loaded crossbow remains ATTACK-style base only |

Right canvases are **10×10**, left **8×8**. Transparent margins are intentional.
Visible bounds (including sparse recessed pixels): INTERACT **8×9**, PLACE
**8×9**, TRANSFORM **7×9**, USE **7×9**, BLOCKED **6×7**. None fills its canvas.
Anchors relative to unchanged 15×15 Classic origin: right **(18,2)**, left
**(-11,3)**. Canvas gaps are **3px** at rest and at least **1px** on the first
docking frame. Maximum composition/motion envelope is **39×15** GUI pixels,
x [-11,28), y [0,15). No inverse GUI scaling or fractional coordinates.

The base is full-strength and stationary; right weight **70%**, left **52%**.
Thin outlines, sparse interior accents, transparent negative space and rare
left-slot eligibility keep these larger objects subordinate. No halo, full
inventory sprite, bright status color or additional visual channel is added.

## Palette/material review

Classic PNG inspection found only **opaque white** visible pixels; green/other
RGB values in transparent pixels are not a visible palette. The actual on-screen
Classic color comes from `RenderPipelines.CROSSHAIR` / `BlendFunction.INVERT`.
Matching a warm RGB constant under ordinary alpha fill cannot reproduce it.

New sprites bake **#ffffff primary**, **#c8c8c8 secondary**, **#707070 recessed
edge**, and transparency. Primary is derived from the actual Classic source;
neutral lower luminance accents stay in the same material. Sparse recessed edges
are weaker inversion, not black-shadow halos. Both base and sidecars use the
same texture/CROSSHAIR pipeline. Texture metadata explicitly sets blur=false.

Important implementation detail: INVERT attenuates RGB with source/destination
color factors. Alpha alone does not fade its RGB result. SidecarSprite therefore
multiplies **RGB and alpha together** by per-role and transition strength.
The tests observe that actual packed color, dimensions and pipeline, not merely
an abstract opacity variable. This makes the dock fade genuinely visible.

All four bases and contextual sprites were compared on dark and light backgrounds.
Observed GUI 2 chest gallery pixels: dark background (52,76,50), base
(203,179,205), right primary (158,148,159); light background (212,212,204),
base (43,43,51), right primary (93,93,97). The subordinate primary tracks the
same inverse destination hue rather than retaining the old warm fill color.
Final gallery/gameplay visual review results are recorded under Validation below.
Color coherence is judged from screenshots, not source RGB equality alone.

## Tool-shaped prototype and boundary

Original axe-shaped transform sprites were explored at 8/10/12px alongside the
generic tool. Their head and handle read as a deliberate tool family; none uses
Minecraft item art or reference screenshot pixels. They remain test-only.

Production keeps generic TRANSFORM because current effective evidence has no
typed tool-category presentation hint. Origin-string/held-item inference would
make this art stage depend on fragile semantic details. A future clean route is
provider/evidence → presentation hint/classification → PresentationResolver →
sprite identifier, without extending ActionKind or querying world data in the
renderer. No speculative hint framework is implemented in this stage.

## Policy, architecture and motion

PresentationResolver, semantic sources, native providers, rule engine/schema,
Classic mapper, HUD layer and suppression Mixin are unchanged from Stage 5.2.
The typed composition remains visibility/base/left state/right action; no
speculative fields were added. See [Classic+ policy](classic-plus.md).

INTERACT/PLACE/TRANSFORM/target USE remain selective right cues. Native self-use,
SPECIAL, CHARGING, COOLDOWN and secondary INVALID remain quiet. MINE INVALID
is ERROR only. Reliable BLOCKED can use left lock plus the eligible right action;
left never appears alone. Native loaded-crossbow READY remains compact.

SidecarSprites maps presentation roles to five cached identifiers/dimensions.
SidecarSprite draws one texture per present side. The old PixelGlyph class and
production rectangle-fill loops are removed. Minecraft loads/caches resources;
no frame-time directory scan, texture creation or item rasterization occurs.
Renderer inspects no world, ItemStack, block, entity, evidence or rule.

OFF is immediate. SUBTLE retains the proven **100ms cubic ease-out**, **2 integer
GUI pixels** of outward dock movement and strength from 45% to full role weight.
Independent per-side clocks, immediate semantic updates, immediate disappearance
and hidden/fallback resets remain. Base never moves/fades. No pulse, rotation,
loop or extra action-specific animation was justified by the static comparisons.

## Validation

`gradlew.bat clean build runClientGameTest`: **PASS**, JDK 25.0.3, full final
run exits 0. Pure checks: **85 semantic**, **88 rule**, **492 presentation/transition**.
Removed eight obsolete runtime-glyph shape/size
assertions; resource/dimension/anchor/material checks now live in actual client
tests. Policy and motion contracts remain covered.

Client tests: **16 Classic resolver**, **7 transformed visibility/HUD**,
**44 native capability**, **19 real rule reload**, **2352 Classic+ HUD/command/
sprite** checks, **18 exploration screenshots**, **4 gameplay screenshots**.
Production gallery/docking screenshots add another eight images. All thirty
Stage 5.3 images were inspected; the earlier Stage 5.2 galleries were compared.
An initial run hit a Fabric synchronized world-disconnect deadlock (thread dump
showed client/server/test Phaser waits); it was stopped and the final full run
completed without production workaround. An observer initially intercepted
unrelated Vanilla colored blits; it was corrected to observe only mod sidecars.

Jar inspection passes: exactly five production sprite PNGs, expected version,
client-only metadata, no exploration/test classes or obsolete PixelGlyph.
The four packaged Classic files match baseline bytes and SHA-256 hashes.
Semantic/rule sources, PresentationResolver, transition logic, base renderer and
Mixin have no diff against Stage 5.2. Root still has no .git.

Ordinary `runClient`: **PASS** for alpha.5.3 load, normal client initialization,
texture atlas/resource load and graceful window close. Gradle exits 0. This
startup smoke check is separate from the in-world automated screenshots.

Production galleries cover sixteen scenes at GUI 1/2/3, dark/light, plus two
deterministic 0/25/50/100ms docking galleries. Client observers check referenced
resources, PNG dimensions, explicit nearest metadata, exact anchors, RGB/alpha
strength, stationary base, bounded envelope, no overlap/ghost, Classic and Vanilla
fallback, F1, third person and preserved Vanilla attack indicator.

SpriteGameplayTest builds server-backed grass-floor scenery, teleports/aims the
actual player and waits for the real camera hitResult/effective presentation.
It captures normal block, chest, axe/log TRANSFORM and villager INTERACT via the
normal HUD. It never assigns a synthetic hitResult or gallery Screen.

Visual review answers, based on the saved images:

| Required question | Finding |
| --- | --- |
| Base dominance | Yes: centered full-strength square/attack stays first; hand/tool/drop sits apart at lower strength. DOT/ERROR never gain competing sprites. |
| Icon completeness | Yes: finger/cuff, faceted grounded cube, open wrench with handle end, contact drop and hollow lock are distinct authored silhouettes. |
| Unified material | Yes: both invert scenery; no separate warm foreground or heavy black halo. Dark/light pixel measurements corroborate the visual review. |
| Immersion | Yes in these scenes: block and entity remain natural aiming anchors, most states stay base-only, rare locked actions use the smaller left cue. |
| GUI 1 | Yes: complete silhouettes retain crisp one-pixel structure and their identifying features; no filtered blur. |
| GUI 3 | Yes: lighter thin outlines remain subordinate; C's taller/heavier silhouettes were rejected. |
| Light/dark | Yes on tested panels and real chest/log/villager surfaces; no outline halo needed. |
| Motion | 0/25/50/100ms samples show actual strength settling and a bounded outward dock; no base movement or exit ghost. |

Evidence:

- [Final build/game-test log](validation/stage-5.3-build-gametest.log)
- [Ordinary client startup log](validation/stage-5.3-client.log)
- [A/B/C exploration index](validation/stage-5.3-exploration/README.md)
- Production GUI 1: [dark](validation/stage-5.3-gallery-scale-1-dark.png), [light](validation/stage-5.3-gallery-scale-1-light.png)
- Production GUI 2: [dark](validation/stage-5.3-gallery-scale-2-dark.png), [light](validation/stage-5.3-gallery-scale-2-light.png)
- Production GUI 3: [dark](validation/stage-5.3-gallery-scale-3-dark.png), [light](validation/stage-5.3-gallery-scale-3-light.png)
- Docking: [dark](validation/stage-5.3-docking-scale-2-dark.png), [light](validation/stage-5.3-docking-scale-2-light.png)
- Real gameplay: [block](validation/stage-5.3-gameplay-normal-block.png), [chest](validation/stage-5.3-gameplay-chest.png), [axe/log](validation/stage-5.3-gameplay-transform-log.png), [villager](validation/stage-5.3-gameplay-villager.png)

Manual checks still pending (not claimed by automation):

- Play 15–30 minutes alternating air, mining, chests, placement and targeted use;
  compare OFF/SUBTLE for fatigue and repeated target changes.
- Inspect over textured walls, foliage, snow and dim caves at GUI 1/2/3; use
  F1, third person, spectator and spyglass and check no additional HUD appears.
- Verify a representative pack with custom item/block artwork keeps subordinate
  cues; unknown targeted USE still inherits the Stage 5.2 conservative policy.

## Stage comparison

| Stage | Density/art result |
| --- | --- |
| 5.1 | Excellent density, but 3px micro hints can look like pixel blemishes. |
| 5.2 | Correct fixed left/right roles and selective docking; 7px runtime glyph art and fill/shadow material remain limiting. |
| 5.3 | Original complete 10px/8px sprite objects, neutral inverted material, same selective layout and stronger hierarchy through weight/negative space. |

## Known issues / practical limits

Long-play comfort and arbitrary resource-pack/scenery readability require the
manual checks above. Tool-specific production variants are deliberately deferred,
not silently inferred. No missing sidecar textures, critical Mixin failures or
metadata errors were found in the final run. The development client logs inherited
anisotropic-option validation and offline Realms-auth messages; the game-test
harness also logs missing end_of_frame post effect and intentionally rejected
malformed rule fixtures. These are not concealed or attributed to the new sprites.

## Next

Stage 6 — Debug & Diagnostics. Not started.
