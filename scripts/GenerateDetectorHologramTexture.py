#!/usr/bin/env python3
"""Generate the biological detector hologram's flat white sampler.

The hologram is drawn with vanilla's `rendertype_crumbling` program. That program
is the only unfogged core shader whose vertex format carries both a lightmap
(`UV2`) and a normal, which is what makes the projection readable in the murk and
what lets a shader pack light it properly — but it is also a *textured* program,
and it multiplies whatever it samples by the vertex colour. Every fragment of the
hologram is therefore drawn with this single opaque white texel bound, so the
texture contributes nothing and the vertex colour is the whole picture. That
keeps the vanilla path byte-for-byte identical to an untextured additive draw
while still handing a shader pack the attributes it needs.

Emitting it from a script rather than checking in an opaque blob, so the reason
the file exists is written down next to the file itself.

Output:
  src/main/resources/assets/aquanaut/textures/entity/detector_hologram_white.png

Run from the repository root:
  python3 scripts/GenerateDetectorHologramTexture.py
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image

OUTPUT = Path("src/main/resources/assets/aquanaut/textures/entity/detector_hologram_white.png")

# One texel: the smallest thing that can be sampled, and the only value that can
# be sampled without tinting the vertex colour.
SIZE = 1
WHITE = (255, 255, 255, 255)


def main() -> None:
    image = Image.new("RGBA", (SIZE, SIZE), WHITE)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    image.save(OUTPUT)
    print(f"wrote {OUTPUT} ({SIZE}x{SIZE})")


if __name__ == "__main__":
    main()
