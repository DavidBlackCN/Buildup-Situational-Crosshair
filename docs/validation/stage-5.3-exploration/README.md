# Stage 5.3 original sprite family exploration

Actual Fabric client-rendered screenshots, not composited mockups. Same Classic
base renderer and SidecarSprite texture helper as production. A/B/C vary artwork
and canvas; nine scenes include base-only comparisons and original test-only axe.
All eighteen images were inspected before selecting/refining B. Neither the
alternatives nor axe sprites ship in the production jar.

| Family | Action / state canvas | GUI 1 | GUI 2 | GUI 3 |
| --- | --- | --- | --- | --- |
| A | 8 / 6 | [dark](stage-5.3-exploration-A-scale-1-dark.png) / [light](stage-5.3-exploration-A-scale-1-light.png) | [dark](stage-5.3-exploration-A-scale-2-dark.png) / [light](stage-5.3-exploration-A-scale-2-light.png) | [dark](stage-5.3-exploration-A-scale-3-dark.png) / [light](stage-5.3-exploration-A-scale-3-light.png) |
| B | 10 / 8 | [dark](stage-5.3-exploration-B-scale-1-dark.png) / [light](stage-5.3-exploration-B-scale-1-light.png) | [dark](stage-5.3-exploration-B-scale-2-dark.png) / [light](stage-5.3-exploration-B-scale-2-light.png) | [dark](stage-5.3-exploration-B-scale-3-dark.png) / [light](stage-5.3-exploration-B-scale-3-light.png) |
| C | 12 / 10 | [dark](stage-5.3-exploration-C-scale-1-dark.png) / [light](stage-5.3-exploration-C-scale-1-light.png) | [dark](stage-5.3-exploration-C-scale-2-dark.png) / [light](stage-5.3-exploration-C-scale-2-light.png) | [dark](stage-5.3-exploration-C-scale-3-dark.png) / [light](stage-5.3-exploration-C-scale-3-light.png) |

A is compact but cramped; C is expressive but competes at GUI 3. B wins the
balance of readable outline structure and visual weight. Final production refines
the wrench head/handle after this comparison. See [Stage 5.3 report](../../stage-5.3-report.md)
for final production screenshots, palette review and real gameplay evidence.

Sources: art/sidecar/exploration/{A,B,C}; PNG exports: gametest resources only.
Original Buildup artwork under MIT. Classic artwork keeps its original license.
