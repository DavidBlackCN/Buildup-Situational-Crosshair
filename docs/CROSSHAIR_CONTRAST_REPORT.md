# Unified timing and inverse contrast outline

Baseline `aac03fc`; branch `26.3`; version `0.1.0-core-animation.2+mc26.3`.
Scope: the two user-approved adjustments, with no detection/theme/sidecar changes.

## Changes

All shape changes now use the same 140ms duration and existing cubic ease-out.
The ERROR 65ms and return-to-DOT 100ms exceptions are removed. OFF mode, initial
visibility, resetting and handing control to Vanilla remain immediate.

Default rendering keeps the inverse body and adds a black, normally alpha-blended
contour before it. `/crosshair outline off` restores pure inversion immediately;
`/crosshair outline on` restores the contour. The switch is independent of the
animation mode and lasts for the current client session. No settings library,
framebuffer readback, custom shader or new texture is introduced.

A small pure `CrosshairOutline` computes four-neighbor coverage from the current
15x15 transition frame. Its output includes a one-pixel margin (17x17 maximum).
Neighbor contributions use maximum rather than sum, so corners do not accumulate
opacity. Maximum opacity is 42%; the center point contributes only 22%, without
diagonal neighbors. Full body pixels, especially the center, never get a dark
underlay. At fractional moving edges, contour coverage is multiplied by the
remaining uncovered fraction, so it fades continuously instead of popping at
zero/full coverage. This also means partially transparent motion pixels have a
slightly different background from pure inversion; fully covered body pixels
retain the exact original inverse color.

The same frame drives body and contour: no independent tail, timer or persistent
old silhouette. Visibility/fallback is still owned by the original HUD boundary.
Unsupported resource-pack masks already fall back to original-texture rendering;
they now also omit the contour rather than guessing their outline.

The four PNGs, attribution, licenses, Classic resolver and HUD Mixin are unchanged.
The new contour is a runtime rendering treatment, not newly authored replacement
art or relicensing of the inherited texture shapes.

## Validation

Existing JDK 25.0.3 / Gradle 9.7.1 / Minecraft 26.3 toolchain, no new dependencies.

- `clean build runClientGameTest --offline` initially stopped at `clean` because
  Windows held the empty `build/libs` directory open. Inspection showed old
  artifacts/classes had been removed (only the empty libs directory and Gradle's
  problem report remained). No user process was terminated.
- `build runClientGameTest --offline` then rebuilt sources and artifacts and passed.
  Final follow-up includes actual HUD contour visibility/off-switch assertions.
- 178,048 deterministic assertions: every type/mode pair, 140ms active boundary,
  center/shared-pixel preservation, rapid interruption continuity, contour opacity
  bounds, no darkening under full body pixels, four lighter DOT neighbors, and no
  stale contour after changing to DOT or an empty frame. Count is assertions,
  not distinct scenarios.
- Existing Classic real-client behavior, camera/HUD visibility, Vanilla attack
  indicator, enable/disable, world re-entry and resource reload tests remain.
- Actual client command dispatcher checks both contour commands; wrapped HUD
  checks no contour when disabled, hidden or delegated back to Vanilla.
- 160 production animation samples plus 240 paired contour-on/off samples.
  The paired records confirm identical inverse-body submissions, bounded black
  one-pixel alpha fills, no repeated contour pixel and the correct drawing order.
- GUI 1/2/3 comparison captures use native stone, snow and moss textures, plus
  stone darkened by a panel overlay. Each pair samples the same texture phase.
  These are GUI material panels, not natural in-world lighting screenshots.
- GUI 2 screenshot pixel comparison checks all fully covered pixels in the four
  resting crosshairs against pure inversion on each material panel.
- JAR audit checks original PNG bytes and licenses and absence of test classes
  and removed sidecar/rule systems.

Evidence:

- [Final build/client log](validation/outline-build-client.log)
- [Initial clean directory-lock log](validation/outline-final.log)
- [Artifact/pixel audit](validation/contrast-v2/artifact-audit.txt)
- [GUI 1 comparison](validation/contrast-v2/outline-comparison-scale-1.png)
- [GUI 2 comparison](validation/contrast-v2/outline-comparison-scale-2.png)
- [GUI 3 comparison](validation/contrast-v2/outline-comparison-scale-3.png)
- [140ms transitions on dark background](validation/contrast-v2/animation-scale-3-dark.png)
- [140ms transitions on light background](validation/contrast-v2/animation-scale-3-light.png)

## Visual result and remaining acceptance

In the captured stone panels, the dark contour makes BLOCK/ERROR/ATTACK much
more identifiable where pure inversion nearly disappears. DOT stays deliberately
restrained; it gains four faint cardinal neighbors instead of a full dark box.
On snow the contour makes the shape visibly heavier. On dark stone the bright
inverse body remains dominant. This is a tradeoff for playtesting, not a claim
that contrast is guaranteed on every background. In particular, the one-pixel
center is still subject to inverse blending's mid-gray limitation.

All three material galleries were visually inspected. The texture source bytes
and fully covered inverse pixels remain unchanged. Test samples do not replace
manual judgment of fast target sweeps, motion softness, foreground foliage,
real day/night lighting or the intended modpack. Compare `/crosshair outline on`
and `off` while playing; no subsequent color-adaptive design was implemented.

The successful client log still contains inherited anisotropic-option, offline
Realms and harness post-effect messages. No new Mixin or assertion failures are
hidden by the clean-directory workaround.
