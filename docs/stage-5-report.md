# Stage 5 — Classic+ Presentation

> Historical Stage 5 baseline. Its visual policy is superseded by
> [Stage 5.1](stage-5.1-report.md); original validation evidence is retained.

## Result: PASS

Version **0.1.0-alpha.5+mc26.3**. Scope: authoritative workspace-root development
stages specification §§5.1–5.7. Stage 6 is not started. Previous stages' pending
manual checks remain pending.

Toolchain unchanged: Minecraft 26.3, JDK 25.0.3, Loader 0.19.5, Fabric API
0.161.0+26.3, Loom 1.18.2, Gradle 9.7.1. No extra runtime dependency.

## Implementation and decisions

- `PresentationResolver` maps resolved semantic actions to immutable
  `CrosshairPresentation`; it never selects from the candidate list again.
- Classic is available and uses the existing pure Classic mapper. Classic+ is
  the default and uses existing target-based glyphs plus one effective secondary
  symbol and per-slot status marks. Ranged charge is represented as USE state.
- Five original code-defined action symbols distinguish INTERACT, USE, PLACE,
  TRANSFORM and SPECIAL. Five original status symbols cover CHARGING, READY,
  COOLDOWN, BLOCKED and INVALID. They are cached 5×5 pixels with a one-pixel dark
  outline. Shapes and placement carry meaning without a color requirement.
- The original texture and center position are preserved. Foreground/outline
  coordinates stay clear of the original opaque pixels. Maximum decoration
  footprint is 23×16 GUI pixels; no texture redesign or new large icon set.
- `TransitionController` runs after presentation. OFF is immediate; SUBTLE uses
  a 100 ms smooth fade-in for current details. The base stays fully opaque. Old
  glyphs disappear immediately; hiding/fallback resets history without an exit
  animation. Animation never changes semantic candidates or delays visibility.
- Minimal client commands select Classic/Classic+ and OFF/SUBTLE for the current
  session. English/Chinese feedback is included. Persistent config/GUI is not part
  of this stage. The command root can be extended for Stage 6 diagnostics later.
- HudElementRegistry still wraps the vanilla crosshair layer. The unchanged HUD
  Mixin suppresses only the named central sprite. Vanilla owns visibility and
  attack indicator rendering; the renderer receives only presentation values.
- Code-defined symbols are independently authored under the current MIT scope;
  Classic artwork retains its existing original license/attribution.

GUI signatures were checked in the resolved 26.3 classes (GuiGraphicsExtractor,
RenderPipelines, Screen); notably the current text API is `text`, not `drawString`.
Client command registration follows the actual dependency's ClientCommands and
ClientCommandRegistrationCallback APIs, supported by the
[official command documentation](https://docs.fabricmc.net/develop/commands/basics).

## Validation

Evidence under `docs/validation/`: final build/game-test log, normal client startup
log, and real game-rendered galleries at GUI scales 1 and 2.

- `clean build runClientGameTest`: successful; production jar generated.
- **85 presentation/transition contracts**: exact Classic mapping, all secondary
  actions/states, independent primary cooldown, visibility gates, charged-crossbow
  theme difference, resolved-action-only behavior, distinct pixel shapes, elapsed
  fade timing, mid-transition disable, hide/reset and backward clock handling.
- **46 Classic+ client checks**: actual command dispatcher switching, real chest
  BLOCK+INTERACT, one custom base/no vanilla duplicate, Classic no decoration,
  F1/third-person/fallback without ghosts, all action and primary-status pixel
  submissions, original base dimensions and alpha in GUI extraction. Every
  decoration pixel submission is additionally checked for Classic pixel overlap.
- Existing **85 semantic**, **88 rule**, **16 Classic**, **7 HUD**, **44 vanilla
  capability** and **19 real reload** checks pass through the updated pipeline.
- Actual gallery screenshots were inspected on dark/light backgrounds at GUI
  scales 1 and 2. The static action/status symbols are distinguishable; the center
  remains the original Classic glyph. This is visual inspection of test-rendered
  screenshots, not manual gameplay testing.
- Normal `runClient` loads the alpha.5 entrypoint and initializes the HUD and rules.
- Four Classic texture hashes match 1.20.1; jar is client-only and excludes tests.
  MIT/third-party notices retained. No critical Mixin or missing-texture error.

## Manual checks still useful

- [ ] Natural aiming/hand swapping in survival and creative, over varied scenery.
- [ ] Decide whether 100 ms detail fade is comfortable during rapid target changes.
- [ ] Spectator interactions, spyglass and other HUD mods during normal play.
- [ ] Carry forward the uncompleted Stage 3 manual matrix and Stage 4 pack UI test.

## Known limitations

- Theme/animation settings last only for the current launch; persistence belongs
  to Stage 7. Themes are selected through commands while in a world.
- Secondary semantics remain limited by native providers and pack rules; unknown
  behavior is not inferred by the presentation layer.
- Static gallery inspection does not prove all modpack/UI-scale combinations or
  real-play animation comfort. Those remain manual/Stage 8 validation work.
- Existing development authentication, test options and post-effect warnings
  remain; rule regression fixtures deliberately emit rejection warnings.

## Next

Stage 6 — Debug & Diagnostics.
