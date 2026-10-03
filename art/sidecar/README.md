# Original sidecar artwork

Copyright (c) 2026 Buildup contributors. MIT; see the root LICENSE.
The historical Classic PNGs are separate third-party artwork and are not covered
by this grant. No Minecraft inventory or external mod artwork is used here.

`exploration/A`, `B`, `C` contain independently drawn 8/10/12px action families,
with 6/8/10px left locks and an original axe prototype in each family.
`production/` contains the selected, refined 10px action / 8px state family.
The production wrench has a more open head and hollow handle end than initial B.

SVG files are editable integer-grid source artwork. Rectangles describe static
art pixels, not a runtime glyph system. Transparent negative space, a one-pixel
primary outline, occasional facet/cuff/grounding midtones and sparse recessed
edges form the family. Exports never resize or filter the source artwork.

Palette: primary `#ffffff` (matches actual opaque Classic pixels), secondary
`#c8c8c8`, recessed edge `#707070`, transparent. Recessed pixels attenuate inversion;
they do not draw an unrelated hard-black outline or a halo. Production renderer
uses the Classic CROSSHAIR inversion pipeline; RGB and alpha weights together
attenuate the material towards the scenery. See the Stage 5.3 report for actual
dark/light review, hierarchy and the reason alpha alone would be insufficient.

Re-export from repository root (Python + Pillow, offline; not a runtime/build dependency):

```text
python art/sidecar/export.py
python art/sidecar/export.py --production
```

Exploration PNGs go only to gametest resources; only production PNGs ship in the
mod. Minecraft texture metadata explicitly disables blur. Renderer caches the
five production identifiers/dimensions and never generates textures per frame.
