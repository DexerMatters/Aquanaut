"""Shared geometry and texture-atlas layout for the worn diving equipment.

The diving equipment worn on the player (mask, oxygen tank, flippers) is drawn as
real box geometry around the vanilla player model parts, not as flat sprites. Each
piece is a small set of cuboids attached to a player model part:

* masks   -> ``head``  (model part space: head cube spans x/z -4..4, y -8..0)
* tanks   -> ``body``  (body cube spans x -4..4, z -2..2, y 0..12)
* flippers-> ``leftLeg`` / ``rightLeg`` (leg cube spans x/z -2..2, y 0..12)

Every texture (one PNG per item, 64x64 texels) stores the UV-unwrapped faces of
those cuboids. The unwrap matches Minecraft's own box UV layout (the one
``CubeListBuilder.texOffs`` uses), scaled by ``S`` texels per model unit::

              +-----------+-----------+
              |    top    |   bottom  |   w*S x d*S each
    +---------+-----------+-----------+---------+
    |   -X    |   +Z     |   +X      |   -Z    |   d*S/w*S x h*S
    +---------+-----------+-----------+---------+

Boxes are written in the wearer's frame: +z points at the wearer's FRONT, y grows
downwards like vanilla cube space. The game renderer negates z when emitting
vertices, because LivingEntityRenderer composes YP.rotation(180 - bodyYaw) with
scale(-1, -1, 1), which leaves part-local +z pointing backwards in the world
(vanilla bakes that reversal into its cube UV layouts; our boxes compensate).

The exact numbers live here so the generator (GenerateDivingEquipmentOnBodyTextures)
and the preview renderer (PreviewDivingEquipmentOnBody) cannot drift apart. They
are mirrored in the game renderer:

    src/main/java/com/dexer/aquanaut/client/renderer/DivingEquipmentRenderLayer.java

Keep that file in sync when changing anything below.
"""

# Texels per model unit (1 model unit = 1/16 block). Vanilla player and armour
# textures use exactly one texel per model unit; matching it keeps the worn gear
# at Minecraft's native pixel scale instead of looking like an HD pack.
S = 1

# Every on-body texture is 64x64 texels.
ATLAS = 64

# Face keys and their orientation when the face is viewed from outside the box.
# ``u`` grows along the first axis listed, ``v`` (downwards in the texture) grows
# along the second.
FACES = ("top", "bottom", "-x", "+z", "+x", "-z")


def texels(units):
    """Model units -> texels (all layout sizes are multiples of 0.5 units)."""
    value = units * S
    rounded = round(value)
    assert abs(value - rounded) < 1e-6, f"size {units} does not land on whole texels"
    return int(rounded)


def region_size(w, h, d):
    """Atlas region size in texels for a box of the given model-unit size."""
    dw, hw, hh, dh = texels(d), texels(w), texels(h), texels(d)
    return 2 * (dw + hw), dh + hh


def face_rects(w, h, d):
    """Face rectangles (u, v, width, height) in texels, relative to the region."""
    dw, hw, hh, dh = texels(d), texels(w), texels(h), texels(d)
    return {
        "top":   (dh, 0, hw, dh),
        "bottom":(dh + hw, 0, hw, dh),
        "-x":    (0, dh, dw, hh),
        "+z":    (dw, dh, hw, hh),
        "+x":    (dw + hw, dh, dw, hh),
        "-z":    (dw + hw + dw, dh, hw, hh),
    }


class Box:
    """One cuboid of a worn piece.

    ``pos`` is the minimum corner in the parent model part's space, ``size`` the
    box dimensions in model units, ``uv`` the atlas region origin in texels.
    ``mirror_x`` marks the box whose twin is drawn mirrored across x=0 (the game
    renderer draws the twin itself).
    """

    def __init__(self, name, pos, size, uv, mirror_x=False):
        self.name = name
        self.pos = pos
        self.size = size
        self.uv = uv
        self.mirror_x = mirror_x

    @property
    def w(self):
        return self.size[0]

    @property
    def h(self):
        return self.size[1]

    @property
    def d(self):
        return self.size[2]

    def face_rects(self):
        """Face rects in atlas texels: {face: (u, v, width, height)}."""
        rects = face_rects(self.w, self.h, self.d)
        u0, v0 = self.uv
        return {face: (u0 + r[0], v0 + r[1], r[2], r[3]) for face, r in rects.items()}

    def corners(self, mirrored=False):
        """The 8 corners of the box in parent part space, plus mirrored twin."""
        x0, y0, z0 = self.pos
        x1, y1, z1 = x0 + self.w, y0 + self.h, z0 + self.d
        if mirrored:
            x0, x1 = -x1, -x0
        return (x0, y0, z0), (x1, y1, z1)


# ---------------------------------------------------------------------------
# Masks, attached to the player's head. Each one is a diving HELMET: a shell
# that encloses the whole head, a protruding rounded visor over the face, a
# valve on the crown and side pods at the ears - the worn version of the
# rounded mask icons.
# ---------------------------------------------------------------------------
MASK_SHELL = Box("shell", (-4.5, -8.5, -4.5), (9.0, 9.0, 9.0), (0, 0))
MASK_VISOR = Box("visor", (-3.0, -6.0, 4.2), (6.0, 4.0, 1.0), (36, 0))
MASK_VALVE = Box("valve", (-1.0, -9.4, -1.0), (2.0, 1.0, 2.0), (50, 0))
MASK_POD = Box("pod", (4.3, -5.5, -1.0), (1.0, 2.0, 2.0), (36, 5), mirror_x=True)
MASK_BOXES = (MASK_SHELL, MASK_VISOR, MASK_VALVE, MASK_POD)

# ---------------------------------------------------------------------------
# Oxygen tanks, attached to the player's body. A slim cylinder hangs on the
# back with its valve up by the shoulder and its boot ring at the bottom; thin
# webbing runs over the shoulders, down the chest, and around the waist.
# ---------------------------------------------------------------------------
TANK_BOTTLE = Box("bottle", (-2.5, 2.0, -4.9), (5.0, 8.0, 3.0), (0, 0))
TANK_CAP = Box("cap", (-1.5, 0.05, -4.2), (3.0, 2.0, 2.0), (16, 0))
TANK_VALVE = Box("valve", (-0.5, -0.9, -5.1), (1.0, 1.0, 1.0), (26, 0))
TANK_RING = Box("ring", (-3.0, 9.9, -5.1), (6.0, 1.0, 3.0), (0, 11))
TANK_SHOULDER = Box("harness_shoulder", (1.0, -0.6, -3.8), (2.0, 1.0, 6.0), (0, 15), mirror_x=True)
TANK_CHEST = Box("harness_chest", (1.0, 0.2, 1.6), (2.0, 9.0, 1.0), (16, 15), mirror_x=True)
TANK_BELT = Box("belt", (-4.5, 8.5, -2.5), (9.0, 1.0, 5.0), (0, 26))
TANK_BOXES = (TANK_BOTTLE, TANK_CAP, TANK_VALVE, TANK_RING, TANK_SHOULDER, TANK_CHEST, TANK_BELT)

# ---------------------------------------------------------------------------
# Flippers, attached to each leg. A slim foot pocket hugs the foot and a thin
# blade steps forward past the toes in three tapering segments.
# ---------------------------------------------------------------------------
FLIPPER_POCKET = Box("pocket", (-2.5, 8.5, -2.5), (5.0, 4.0, 5.0), (0, 0))
FLIPPER_STRAP = Box("strap", (-2.5, 8.0, -2.5), (5.0, 1.0, 5.0), (0, 9))
FLIPPER_BLADE1 = Box("blade1", (-2.5, 11.5, 2.4), (5.0, 1.0, 2.0), (0, 15))
FLIPPER_BLADE2 = Box("blade2", (-2.0, 11.4, 4.3), (4.0, 1.0, 1.0), (0, 18))
FLIPPER_BLADE3 = Box("blade3", (-1.5, 11.3, 5.2), (3.0, 1.0, 1.0), (0, 20))
FLIPPER_BOXES = (FLIPPER_POCKET, FLIPPER_STRAP, FLIPPER_BLADE1, FLIPPER_BLADE2, FLIPPER_BLADE3)

# Item id -> (category, variant) for the 19 diving equipment pieces.
EQUIPMENT = {
    "iron_oxygen_tank": ("tank", "iron"),
    "wood_oxygen_tank": ("tank", "wood"),
    "shell_oxygen_tank": ("tank", "shell"),
    "hard_shell_oxygen_tank": ("tank", "hard_shell"),
    "marine_alloy_oxygen_tank": ("tank", "marine_alloy"),
    "shark_flippers": ("flipper", "shark"),
    "wood_flippers": ("flipper", "wood"),
    "coral_flippers": ("flipper", "coral"),
    "shell_flippers": ("flipper", "shell"),
    "hard_shell_flippers": ("flipper", "hard_shell"),
    "marine_alloy_flippers": ("flipper", "marine_alloy"),
    "iron_mask": ("mask", "iron"),
    "coral_mask": ("mask", "coral"),
    "shell_mask": ("mask", "shell"),
    "hard_shell_mask": ("mask", "hard_shell"),
    "marine_alloy_mask": ("mask", "marine_alloy"),
    "anglerfish_mask": ("mask", "anglerfish"),
    "ender_mask": ("mask", "ender"),
    "slime_mask": ("mask", "slime"),
}

CATEGORY_BOXES = {"mask": MASK_BOXES, "tank": TANK_BOXES, "flipper": FLIPPER_BOXES}


def validate():
    """Assert every atlas region fits the canvas and no two regions overlap.

    Both the generator and the preview call this so a layout change that breaks
    the texture layout fails loudly instead of smearing art across boxes.
    """
    for category, boxes in CATEGORY_BOXES.items():
        seen = {}
        for box in boxes:
            rw, rh = region_size(box.w, box.h, box.d)
            u0, v0 = box.uv
            assert u0 >= 0 and v0 >= 0 and u0 + rw <= ATLAS and v0 + rh <= ATLAS, (
                f"{category}/{box.name} region ({u0},{v0},{rw},{rh}) leaves the {ATLAS}x{ATLAS} atlas")
            for other, (ou0, ov0, orw, orh) in seen.items():
                overlap = u0 < ou0 + orw and ou0 < u0 + rw and v0 < ov0 + orh and ov0 < v0 + rh
                assert not overlap, f"{category}/{box.name} overlaps {category}/{other}"
            seen[box.name] = (u0, v0, rw, rh)


validate()
