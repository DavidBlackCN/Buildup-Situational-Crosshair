"""Export original integer-grid SVG rectangles to lossless PNGs. Offline art tool.

Usage: python art/sidecar/export.py [--production]
Requires Pillow; not a build or runtime dependency. Does not read Classic assets.
"""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET
from PIL import Image, ImageColor

ROOT = Path(__file__).resolve().parents[2]
production = "--production" in sys.argv
source = ROOT / "art/sidecar" / ("production" if production else "exploration")
destination = ROOT / ("src/main/resources" if production else "src/gametest/resources")
destination /= "assets/buildup_situational_crosshair/textures/gui/sidecar"
if not production:
    destination /= "exploration"
for path in sorted(source.rglob("*.svg")):
    svg = ET.parse(path).getroot()
    image = Image.new("RGBA", (int(svg.attrib["width"]), int(svg.attrib["height"])))
    for rect in svg:
        x, y, w, h = (int(rect.attrib[k]) for k in ("x", "y", "width", "height"))
        color = ImageColor.getrgb(rect.attrib["fill"]) + (255,)
        for dy in range(h):
            for dx in range(w):
                image.putpixel((x + dx, y + dy), color)
    output = destination / Path(str(path.relative_to(source).with_suffix(".png")).lower())
    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(output)
    Path(str(output) + ".mcmeta").write_text(
        '{"texture":{"blur":false,"clamp":true}}\n', encoding="utf-8")
