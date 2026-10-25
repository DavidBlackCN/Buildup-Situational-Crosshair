# Dark outline rollback

2026-10-26. User rejected the inverse-body/dark-contour experiment.
Baseline: `acfe18f`; replacement version: `0.1.0-core-animation.3+mc26.3`.

Removed the contour computation/draws, setting and commands, language keys,
client comparison test and contour-specific assertions. Restored the renderer,
options and HUD parity checks to the pre-contour implementation (`aac03fc`).
The original inverse body and unchanged PNGs remain. All shape transitions
retain the separately approved **140ms** duration and existing cubic ease-out.
Added the 139ms boundary to the retained duration regression checks.

The prior contrast report and screenshots remain historical evidence, explicitly
marked as a rejected experiment; they do not describe the current release.
No alternative color scheme was implemented in this rollback.

Validation: `gradlew.bat build runClientGameTest --offline` **PASS**, exit 0,
34 seconds. Classic/HUD regressions, all-pair transition tests, real resource
reload and 160 production animation samples pass. Final JAR audit confirms no
contour classes/keys and unchanged original PNG/license bytes. The current JDK/Fabric/
Minecraft toolchain remains unchanged.

- [Build/client log](validation/outline-rollback.log)
- [Packaged artifact audit](validation/outline-rollback-audit.txt)

Future proposal only: evaluate a borderless high-contrast body that selects a
stable light/dark tone from a small background region. It must preserve the
original silhouette and avoid rapid polarity switching, unsafe framebuffer
feedback and CPU readback stalls. Its 26.3 renderer integration, frame cost and
modpack compatibility require investigation before implementation. Pure inverse
rendering remains the current behavior and the fallback candidate.
