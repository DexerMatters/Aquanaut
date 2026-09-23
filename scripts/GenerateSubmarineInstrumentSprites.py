#!/usr/bin/env python3
"""Generate the Aquanaut "location cursor" and "submarine compass" item sprites.

Both sprites are 16x16 item textures drawn so the object reads as a *solid*
three-dimensional instrument rather than a flat sticker: the scene is a small
3D model, rendered through a real perspective camera with a key/fill/rim light
rig, then reduced to the item canvas and snapped to a curated palette.  That is
what "drawn in perspective" buys over hand-placed flat blocks -- the ellipses,
the foreshortened dial and the light ramp all fall out of the geometry.

The location cursor follows the shipped
`assets/aquanaut/textures/entity/cursor.png` entity (anodised keel, copper hull,
four pale bladder floats, steel head pod with a dark port window, coil mast and
an amber beacon tip), restricted to the parts that survive at 16x16.

Output:
  src/main/resources/assets/aquanaut/textures/item/cursor.png          16x16, one sprite
  src/main/resources/assets/aquanaut/textures/item/submarine_compass.png
                          16x(16*FRAMES): one 16x16 frame per needle bearing,
                          stacked top-to-bottom so the compass states its own
                          direction from the texture, with no HUD or GUI.

Two steps, like every other item sprite in this repo -- the generator paints,
the shared outline pass finishes the silhouette ("stressed" profiles in
`item_outline_profiles.py`):

  python3 scripts/GenerateSubmarineInstrumentSprites.py
  python3 scripts/RefineItemOutlines.py --only cursor.png,submarine_compass.png
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image

OUT_DIR = Path("src/main/resources/assets/aquanaut/textures/item")
TARGET = 16          # final sprite edge, in pixels
MARGIN = 1.0         # sprite margin, in final pixels
FRAMES = 32          # compass needle bearings stacked into the long sprite
COMPASS_ELEVATION = 50.0   # lower = more case wall (thickness); also de-skews the needle

# Render scale, per sprite.  The compass is reduced only 2x because at 4x the box
# average smeared its graduations into mush; the cursor is small and sparse and
# still benefits from the extra anti-aliasing, so it keeps 4x.
CURSOR_SUPERSAMPLE = 4
COMPASS_SUPERSAMPLE = 2


# --------------------------------------------------------------------------
# small vector helpers
# --------------------------------------------------------------------------

def vadd(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def vsub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def vmul(a, k):
    return (a[0] * k, a[1] * k, a[2] * k)


def vdot(a, b):
    return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]


def vcross(a, b):
    return (
        a[1] * b[2] - a[2] * b[1],
        a[2] * b[0] - a[0] * b[2],
        a[0] * b[1] - a[1] * b[0],
    )


def vlen(a):
    return math.sqrt(vdot(a, a))


def vnorm(a):
    length = vlen(a)
    if length < 1e-12:
        return (0.0, 0.0, 0.0)
    return (a[0] / length, a[1] / length, a[2] / length)


def rodrigues(v, axis, angle):
    """Rotate `v` about a unit `axis` by `angle` radians."""
    c = math.cos(angle)
    s = math.sin(angle)
    return vadd(
        vadd(vmul(v, c), vmul(vcross(axis, v), s)),
        vmul(axis, vdot(axis, v) * (1.0 - c)),
    )


def rotate_between(v, from_dir, to_dir):
    """Rotate `v` by the shortest rotation taking `from_dir` to `to_dir`."""
    axis = vcross(from_dir, to_dir)
    s = vlen(axis)
    c = vdot(from_dir, to_dir)
    if s < 1e-9:
        return v if c > 0 else vmul(v, -1.0)
    return rodrigues(v, vmul(axis, 1.0 / s), math.atan2(s, c))


# --------------------------------------------------------------------------
# palette -- ramps only, so the snapped sprite keeps a designed colour plan
# --------------------------------------------------------------------------

def ramp(*hexes):
    return [tuple(int(h[i:i + 2], 16) / 255.0 for i in (0, 2, 4)) for h in hexes]


STEEL = ramp("D6DADD", "C0C6CA", "A8AEB2", "8E969A", "6E767C", "585F65", "3A4045", "23262A")
COPPER = ramp("E8A468", "D68C4E", "C97A3E", "BB6430", "A55527", "964E22", "6E3517")
BRASS = ramp("F4DEA6", "E6C87E", "D4AF5C", "C9A24E", "AE8A3C", "8E7030", "6A5222")
PALE = ramp("F6FAF9", "E8EFED", "D2DEDC", "B4C2C0", "94A2A0")
IVORY = ramp("F0EADA", "DED6BE", "C4BAA0")
RUBBER = ramp("3A4046", "23262A", "141619")
GLASS = ramp("BED2DE", "8AA0B0", "4A5864", "2A3138")
AMBER = ramp("FFF0C0", "FFD060", "E8A020", "B47814")
RED = ramp("E8705C", "C0392B", "8E241B")

PALETTE = STEEL + COPPER + BRASS + PALE + IVORY + RUBBER + GLASS + AMBER + RED



class Material:
    """A surface that is shaded *within its own ramp*, never across ramps.

    `bias` shifts the chosen ramp step, which is how one ramp yields a family of
    parts (steel / steel_dark / steel_light) without letting shaded steel drift
    into the copper or brass ramps.
    """

    __slots__ = ("ramp", "spec", "shin", "alpha", "glass", "emissive", "bias", "highlight")

    def __init__(self, ramp, spec=0.30, shin=28.0, alpha=1.0, glass=False,
                 emissive=0.0, bias=0, highlight=None):
        self.ramp = ramp
        self.spec = spec
        self.shin = shin
        self.alpha = alpha
        self.glass = glass
        self.emissive = emissive
        self.bias = bias
        self.highlight = highlight


# --------------------------------------------------------------------------
# scene construction
# --------------------------------------------------------------------------

class Scene:
    def __init__(self):
        self.tris = []

    def add_tri(self, a, b, c, mat):
        self.tris.append((a, b, c, mat))

    def add_quad(self, a, b, c, d, mat):
        self.add_tri(a, b, c, mat)
        self.add_tri(a, c, d, mat)

    def add_fan(self, hub, ring, mat):
        n = len(ring)
        for i in range(n):
            self.add_tri(hub, ring[i], ring[(i + 1) % n], mat)

    def add_tube(self, points, radius, segments=20, mat=None, caps=True,
                 closed=False, ref=(0.0, 1.0, 0.0)):
        """Sweep a circular profile along a polyline (parallel-transported)."""
        pts = [tuple(float(c) for c in p) for p in points]
        n = len(pts)
        radii = [float(radius)] * n if isinstance(radius, (int, float)) else [float(r) for r in radius]
        tangents = []
        for i in range(n):
            if closed:
                t = vsub(pts[(i + 1) % n], pts[(i - 1) % n])
            elif i == 0:
                t = vsub(pts[1], pts[0])
            elif i == n - 1:
                t = vsub(pts[-1], pts[-2])
            else:
                t = vsub(pts[i + 1], pts[i - 1])
            tangents.append(vnorm(t))

        refv = tuple(float(c) for c in ref)
        if abs(vdot(tangents[0], refv)) > 0.9:
            refv = (1.0, 0.0, 0.0)
        nrm = vnorm(vsub(refv, vmul(tangents[0], vdot(refv, tangents[0]))))

        rings = []
        for i in range(n):
            if i > 0:
                nrm = rotate_between(nrm, tangents[i - 1], tangents[i])
                nrm = vnorm(vsub(nrm, vmul(tangents[i], vdot(nrm, tangents[i]))))
            binorm = vcross(tangents[i], nrm)
            ring = []
            for s in range(segments):
                ang = 2.0 * math.pi * s / segments
                off = vadd(vmul(nrm, radii[i] * math.cos(ang)), vmul(binorm, radii[i] * math.sin(ang)))
                ring.append(vadd(pts[i], off))
            rings.append(ring)

        last = n if closed else n - 1
        for i in range(last):
            a = rings[i]
            b = rings[(i + 1) % n]
            for s in range(segments):
                s2 = (s + 1) % segments
                self.add_quad(a[s], a[s2], b[s2], b[s], mat)

        if caps and not closed:
            self.add_fan(pts[0], list(reversed(rings[0])), mat)
            self.add_fan(pts[-1], rings[-1], mat)

    def add_sphere(self, center, radius, mat, segments=20, rings=10):
        """Full sphere via latitude/longitude quads (poles as fans)."""
        cx, cy, cz = center
        rows = []
        for i in range(1, rings):
            phi = math.pi * i / rings
            r = radius * math.sin(phi)
            y = cy + radius * math.cos(phi)
            rows.append([(cx + r * math.cos(2 * math.pi * j / segments),
                          y,
                          cz + r * math.sin(2 * math.pi * j / segments)) for j in range(segments)])
        top = (cx, cy + radius, cz)
        bottom = (cx, cy - radius, cz)
        self.add_fan(top, rows[0], mat)
        for i in range(len(rows) - 1):
            for j in range(segments):
                j2 = (j + 1) % segments
                self.add_quad(rows[i][j], rows[i][j2], rows[i + 1][j2], rows[i + 1][j], mat)
        self.add_fan(bottom, list(reversed(rows[-1])), mat)

    @staticmethod
    def _basis(normal):
        n = vnorm(normal)
        ref = (0.0, 1.0, 0.0) if abs(n[1]) < 0.9 else (1.0, 0.0, 0.0)
        tangent = vnorm(vcross(n, ref))
        return tangent, vcross(n, tangent)

    @classmethod
    def _circle(cls, center, radius, normal, segments):
        tangent, binormal = cls._basis(normal)
        pts = []
        for i in range(segments):
            ang = 2.0 * math.pi * i / segments
            offset = vadd(vmul(tangent, radius * math.cos(ang)), vmul(binormal, radius * math.sin(ang)))
            pts.append(vadd(center, offset))
        return pts

    def add_dome(self, center, radius, mat, segments=24, rings=8, lower=False):
        """Hemisphere: upper (a dome) by default, lower (a bowl) with lower=True."""
        cx, cy, cz = center
        rows = []
        for i in range(rings):
            phi = (math.pi / 2.0) * i / rings
            r = radius * math.sin(phi)
            y = cy - radius * math.cos(phi) if lower else cy + radius * math.cos(phi)
            rows.append([(cx + r * math.cos(2 * math.pi * j / segments),
                          y,
                          cz + r * math.sin(2 * math.pi * j / segments)) for j in range(segments)])
        pole = (cx, cy - radius, cz) if lower else (cx, cy + radius, cz)
        self.add_fan(pole, rows[0], mat)
        for i in range(len(rows) - 1):
            for j in range(segments):
                j2 = (j + 1) % segments
                self.add_quad(rows[i][j], rows[i][j2], rows[i + 1][j2], rows[i + 1][j], mat)

    def add_disc(self, center, radius, mat, segments=24, normal=(0.0, 1.0, 0.0)):
        self.add_fan(center, self._circle(center, radius, normal, segments), mat)

    def add_annulus(self, center, r_in, r_out, mat, segments=24, normal=(0.0, 1.0, 0.0)):
        inner = self._circle(center, r_in, normal, segments)
        outer = self._circle(center, r_out, normal, segments)
        for i in range(segments):
            j = (i + 1) % segments
            self.add_quad(inner[i], outer[i], outer[j], inner[j], mat)

    def add_torus(self, center, major, minor, mat, normal=(0.0, 1.0, 0.0), segments=24, sides=8):
        ring = self._circle(center, major, normal, segments)
        self.add_tube(ring, minor, segments=sides, mat=mat, caps=False, closed=True, ref=vnorm(normal))


# --------------------------------------------------------------------------
# instruments
# --------------------------------------------------------------------------

def build_cursor() -> Scene:
    """The under-sea location pin, following the cursor entity's part list.

    Trimmed to the parts that still read at 16x16: an anodised ballast keel, a
    copper barrel with an ivory graduation band, four pale bladder floats, a
    steel head pod with a dark port window, and a short mast carrying a coil and
    an amber beacon.
    """
    s = Scene()
    copper = Material(COPPER, spec=0.34, shin=30.0)
    copper_light = Material(COPPER, spec=0.46, shin=34.0, bias=1)
    steel = Material(STEEL, spec=0.46, shin=38.0)
    steel_dark = Material(STEEL, spec=0.34, shin=28.0, bias=-1)
    steel_light = Material(STEEL, spec=0.58, shin=42.0, bias=1)
    anodized = Material(STEEL, spec=0.28, shin=26.0, bias=-2)
    anodized_dark = Material(STEEL, spec=0.22, shin=20.0, bias=-3)
    rubber = Material(RUBBER, spec=0.16, shin=12.0)
    bladder = Material(PALE, spec=0.22, shin=18.0)
    bladder_dark = Material(PALE, spec=0.16, shin=16.0, bias=-1)
    band = Material(IVORY, spec=0.20, shin=16.0, bias=1)
    port = Material(GLASS, spec=0.70, shin=64.0, bias=-2)
    beacon = Material(AMBER, spec=0.45, shin=36.0, emissive=0.85)

    # ballast keel
    s.add_tube([(0, -1.14, 0), (0, -0.98, 0)], [0.26, 0.46], segments=18, mat=anodized_dark)
    s.add_tube([(0, -0.98, 0), (0, -0.90, 0)], [0.46, 0.58], segments=18, mat=anodized)

    # copper barrel, with an ivory graduation band and a rubber shoulder
    s.add_tube([(0, -0.90, 0), (0, -0.72, 0)], [0.58, 0.70], segments=22, mat=copper)
    s.add_tube([(0, -0.72, 0), (0, 0.24, 0)], [0.70, 0.70], segments=22, mat=copper, caps=False)
    s.add_tube([(0, -0.18, 0), (0, -0.06, 0)], [0.708, 0.708], segments=22, mat=band, caps=False)
    s.add_tube([(0, 0.24, 0), (0, 0.36, 0)], [0.70, 0.64], segments=22, mat=rubber)

    # four pale bladder floats riding the barrel shoulder
    for ang in (45.0, 135.0, 225.0, 315.0):
        rad = math.radians(ang)
        x = 0.78 * math.cos(rad)
        z = 0.78 * math.sin(rad)
        s.add_tube([(x, -0.78, z), (x, -0.22, z)], 0.235, segments=14, mat=bladder)
        s.add_sphere((x, -0.78, z), 0.235, bladder_dark, segments=14, rings=7)
        s.add_sphere((x, -0.22, z), 0.235, bladder, segments=14, rings=7)

    # head pod and its forward port window
    s.add_tube([(0, 0.36, 0), (0, 0.46, 0)], [0.60, 0.60], segments=22, mat=rubber, caps=False)
    s.add_tube([(0, 0.46, 0), (0, 0.92, 0)], [0.58, 0.48], segments=22, mat=steel)
    look = vnorm((0.60, 0.0, 0.80))
    s.add_tube([vmul(look, 0.26), vmul(look, 0.58)], [0.22, 0.25], segments=16, mat=steel_light)
    port_center = vadd((0.0, 0.70, 0.0), vmul(look, 0.60))
    s.add_disc(port_center, 0.205, port, segments=16, normal=look)

    # antenna mount, coil mast and amber beacon
    s.add_tube([(0, 0.92, 0), (0, 1.04, 0)], [0.24, 0.10], segments=16, mat=steel_dark)
    s.add_tube([(0, 1.04, 0), (0, 1.40, 0)], 0.058, segments=10, mat=steel_light)
    coil = []
    for i in range(21):
        t = i / 20.0
        ang = 2.0 * math.pi * 3.0 * t
        coil.append((0.135 * math.cos(ang), 1.06 + 0.28 * t, 0.135 * math.sin(ang)))
    s.add_tube(coil, 0.030, segments=8, mat=copper_light, caps=False)
    s.add_tube([(0, 1.36, 0), (0, 1.44, 0)], [0.07, 0.10], segments=12, mat=steel_dark)
    s.add_sphere((0, 1.54, 0), 0.155, beacon, segments=16, rings=8)
    return s


def rot_y(p, deg):
    """Rotate a point about the vertical axis -- the compass needle's bearing."""
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    return (p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c)


def build_compass(angle_deg: float = 0.0) -> Scene:
    """A Minecraft-style compass: one big dial, and the needle carries the bearing.

    The case is an iron can with a genuinely tall wall, a chamfered lip and a
    stepped bezel, so the sprite reads as a solid object with thickness rather
    than a flat disc: the wall shows as a ~2px band at 16x16 and the dial sits
    sunk in a recess below the bezel.  The graduations are deliberately few and
    chunky, because anything thinner than a pixel is what turns to mush in the
    reduction.

    `angle_deg` turns the needle about the vertical axis; `main` renders one
    frame per bearing and stacks them into a long sprite, the way a compass
    indicates direction from its own texture: frame `i` is the bearing
    `i * 360 / FRAMES` degrees clockwise from north, and the sprite is therefore
    a drop-in replacement for a vanilla compass strip.
    """
    s = Scene()
    iron = Material(STEEL, spec=0.40, shin=34.0)
    iron_light = Material(STEEL, spec=0.62, shin=48.0, bias=1)
    iron_deep = Material(STEEL, spec=0.22, shin=20.0, bias=-4)
    face = Material(GLASS, spec=0.14, shin=14.0, bias=-3)
    mark = Material(PALE, spec=0.14, shin=14.0)
    needle_north = Material(RED, spec=0.34, shin=30.0)
    needle_south = Material(PALE, spec=0.30, shin=26.0, bias=-1)
    hub = Material(STEEL, spec=0.64, shin=50.0, bias=1)

    r_out = 1.00
    r_dial = 0.74
    case_bottom = -0.34
    case_top = 0.02
    lip_top = 0.07
    bezel_top = 0.13
    dial_y = 0.01

    # iron can: underside, a tall wall, and a bright chamfer where it turns over
    s.add_disc((0, case_bottom, 0), r_out, iron_deep, segments=30, normal=(0.0, -1.0, 0.0))
    s.add_tube([(0, case_bottom, 0), (0, case_top, 0)], r_out, segments=30, mat=iron, caps=False)
    s.add_tube([(0, case_top, 0), (0, lip_top, 0)], [r_out, 0.94], segments=30, mat=iron_light, caps=False)
    s.add_tube([(0, lip_top, 0), (0, bezel_top, 0)], 0.94, segments=30, mat=iron, caps=False)
    s.add_annulus((0, bezel_top, 0), r_dial, 0.94, iron_light, segments=30)

    # dial sunk into the bezel; the recess wall reads as depth on the far side
    s.add_tube([(0, dial_y, 0), (0, bezel_top, 0)], r_dial, segments=30, mat=iron_deep, caps=False)
    s.add_disc((0, dial_y, 0), r_dial, face, segments=30)

    # eight chunky white graduations; the four cardinals longer and heavier
    for i in range(8):
        ang = 2.0 * math.pi * i / 8
        cardinal = i % 2 == 0
        inner_r = 0.44 if cardinal else 0.50
        outer_r = 0.68 if cardinal else 0.64
        radius = 0.13 if cardinal else 0.10
        inner = (inner_r * math.cos(ang), dial_y + 0.014, inner_r * math.sin(ang))
        outer = (outer_r * math.cos(ang), dial_y + 0.014, outer_r * math.sin(ang))
        s.add_tube([inner, outer], radius, segments=6, mat=mark, caps=False)

    # dark index notch cut into the bezel, marking north
    s.add_quad((-0.060, bezel_top + 0.012, -0.76), (0.060, bezel_top + 0.012, -0.76),
               (0.060, bezel_top + 0.012, -0.94), (-0.060, bezel_top + 0.012, -0.94), iron_deep)

    # Compass bearings grow clockwise (N -> E -> S -> W), which is a negative
    # rotation about +Y, so frame i lands on bearing i * 360 / FRAMES.  A flat
    # dial seen from COMPASS_ELEVATION is squashed vertically, which would drag
    # the needle towards the cardinals, so solve for the turn whose *projected*
    # direction is the requested bearing: screen = (sin t, sin(elev) * cos t).
    squash = math.sin(math.radians(COMPASS_ELEVATION))
    wanted = math.radians(angle_deg)
    swing = -math.degrees(math.atan2(squash * math.sin(wanted), math.cos(wanted)))

    needle_plane = dial_y + 0.045

    def needle_kite(tip_z, mid_z, hub_z, half, mat):
        # deliberately flat: a raised ridge tilts the *visible* needle off the
        # requested bearing, because its height shifts the tip up-screen by a
        # constant.  Flat keeps the de-skew above exact.
        tip = (0.0, needle_plane, tip_z)
        inner = (0.0, needle_plane, hub_z)
        right = (half, needle_plane, mid_z)
        left = (-half, needle_plane, mid_z)
        for p, q in ((right, inner), (inner, left)):
            s.add_tri(rot_y(tip, swing), rot_y(p, swing), rot_y(q, swing), mat)

    needle_kite(-0.62, -0.30, -0.03, 0.240, needle_north)
    needle_kite(0.42, 0.18, 0.03, 0.240, needle_south)
    s.add_sphere((0.0, needle_plane + 0.035, 0.0), 0.075, hub, segments=12, rings=6)
    return s


# --------------------------------------------------------------------------
# camera + renderer
# --------------------------------------------------------------------------

class Camera:
    def __init__(self, azimuth_deg=32.0, elevation_deg=25.0, distance=6.0, target=(0.0, 0.0, 0.0)):
        self.target = target
        self.distance = distance
        az = math.radians(azimuth_deg)
        el = math.radians(elevation_deg)
        direction = (math.cos(el) * math.sin(az), math.sin(el), math.cos(el) * math.cos(az))
        self.eye = vadd(target, vmul(direction, distance))
        forward = vnorm(vsub(target, self.eye))
        right = vnorm(vcross(forward, (0.0, 1.0, 0.0)))
        up = vcross(right, forward)
        self.right = right
        self.up = up
        self.forward = forward

    def view(self, p):
        d = vsub(p, self.eye)
        return (vdot(d, self.right), vdot(d, self.up), vdot(d, self.forward))


def normal_of(a, b, c):
    return vnorm(vcross(vsub(b, a), vsub(c, a)))


# key from the camera's upper left, so the face we actually look at is lit
LIGHTS = (
    (vnorm((-0.38, 0.66, 0.72)), 0.75),
    (vnorm((0.78, 0.30, 0.35)), 0.22),
    (vnorm((0.0, -0.45, 0.89)), 0.16),
)
INTENSITY_FLOOR = 0.20
INTENSITY_CEIL = 0.95


def shade(mat, normal, point, eye):
    """Cel-shade onto the material's own ramp; returns (colour, alpha)."""
    view = vnorm(vsub(eye, point))
    intensity = 0.22 + 0.14 * max(0.0, normal[1])
    gloss = 0.0
    for light_dir, energy in LIGHTS:
        intensity += energy * max(0.0, vdot(normal, light_dir))
        half = vnorm(vadd(light_dir, view))
        gloss += (max(0.0, vdot(normal, half)) ** mat.shin) * energy
    gloss *= mat.spec

    ramp = mat.ramp
    steps = len(ramp)
    t = (intensity - INTENSITY_FLOOR) / (INTENSITY_CEIL - INTENSITY_FLOOR)
    step = int(round(max(0.0, min(1.0, t)) * (steps - 1))) + mat.bias
    step = max(0, min(steps - 1, step))
    color = ramp[steps - 1 - step]

    if gloss > 0.42:
        color = mat.highlight if mat.highlight is not None else ramp[0]
    if mat.emissive > 0.0:
        hot = ramp[0]
        color = tuple(color[i] * (1.0 - mat.emissive) + hot[i] * mat.emissive for i in range(3))

    alpha = mat.alpha
    if mat.glass:
        fresnel = (1.0 - max(0.0, vdot(normal, view))) ** 2
        alpha = min(1.0, mat.alpha + 0.80 * fresnel + (0.45 if gloss > 0.30 else 0.0))
    return list(color), alpha


def compute_fit(scene: Scene, camera: Camera, size: int):
    """Frame the scene once, so every animation frame shares one transform."""
    raw = []
    for tri in scene.tris:
        for v in tri[:3]:
            vx, vy, vz = camera.view(v)
            if vz > 0.05:
                raw.append((vx / vz, vy / vz))
    min_u = min(r[0] for r in raw)
    max_u = max(r[0] for r in raw)
    min_v = min(r[1] for r in raw)
    max_v = max(r[1] for r in raw)
    margin = MARGIN * size / TARGET
    scale = min((size - 2 * margin) / max(1e-6, max_u - min_u),
                (size - 2 * margin) / max(1e-6, max_v - min_v))
    return scale, (min_u + max_u) * 0.5, (min_v + max_v) * 0.5


def render(scene: Scene, camera: Camera, size: int, fit=None) -> Image.Image:
    view_pts = [camera.view(v) for tri in scene.tris for v in tri[:3]]

    raw = []
    for vx, vy, vz in view_pts:
        raw.append((vx / vz, vy / vz) if vz > 0.05 else None)

    if fit is None:
        valid = [r for r in raw if r is not None]
        min_u = min(r[0] for r in valid)
        max_u = max(r[0] for r in valid)
        min_v = min(r[1] for r in valid)
        max_v = max(r[1] for r in valid)
        margin = MARGIN * size / TARGET
        scale = min((size - 2 * margin) / max(1e-6, max_u - min_u),
                    (size - 2 * margin) / max(1e-6, max_v - min_v))
        cu = (min_u + max_u) * 0.5
        cv = (min_v + max_v) * 0.5
    else:
        scale, cu, cv = fit

    def project(index):
        r = raw[index]
        if r is None:
            return None
        sx = size * 0.5 + (r[0] - cu) * scale
        sy = size * 0.5 - (r[1] - cv) * scale
        return (sx, sy, view_pts[index][2])

    screen = [project(i) for i in range(len(view_pts))]

    color = [[0.0, 0.0, 0.0] for _ in range(size * size)]
    alpha = [0.0] * (size * size)
    depth = [1e18] * (size * size)

    opaque = [i for i, tri in enumerate(scene.tris) if not tri[3].glass]
    glass = [i for i, tri in enumerate(scene.tris) if tri[3].glass]

    def raster(idx, blend):
        base = idx * 3
        p0, p1, p2 = screen[base], screen[base + 1], screen[base + 2]
        if p0 is None or p1 is None or p2 is None:
            return
        mat = scene.tris[idx][3]
        a, b, c = scene.tris[idx][0], scene.tris[idx][1], scene.tris[idx][2]
        n = normal_of(a, b, c)
        centroid = vmul(vadd(vadd(a, b), c), 1.0 / 3.0)
        if vdot(n, vsub(camera.eye, centroid)) < 0.0:
            n = vmul(n, -1.0)
        col, a_src = shade(mat, n, centroid, camera.eye)

        x0, y0 = p0[0], p0[1]
        x1, y1 = p1[0], p1[1]
        x2, y2 = p2[0], p2[1]
        area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
        if abs(area) < 1e-9:
            return
        inv = 1.0 / area
        lo_x = max(0, int(math.floor(min(x0, x1, x2))))
        hi_x = min(size - 1, int(math.ceil(max(x0, x1, x2))))
        lo_y = max(0, int(math.floor(min(y0, y1, y2))))
        hi_y = min(size - 1, int(math.ceil(max(y0, y1, y2))))
        for py in range(lo_y, hi_y + 1):
            fy = py + 0.5
            for px in range(lo_x, hi_x + 1):
                fx = px + 0.5
                w0 = ((x1 - fx) * (y2 - fy) - (x2 - fx) * (y1 - fy)) * inv
                if w0 < 0.0:
                    continue
                w1 = ((x2 - fx) * (y0 - fy) - (x0 - fx) * (y2 - fy)) * inv
                if w1 < 0.0:
                    continue
                w2 = 1.0 - w0 - w1
                if w2 < 0.0:
                    continue
                z = w0 * p0[2] + w1 * p1[2] + w2 * p2[2]
                offset = py * size + px
                if z >= depth[offset]:
                    continue
                if blend:
                    for i in range(3):
                        color[offset][i] = col[i] * a_src + color[offset][i] * (1.0 - a_src)
                    alpha[offset] = a_src + alpha[offset] * (1.0 - a_src)
                else:
                    color[offset][0] = col[0]
                    color[offset][1] = col[1]
                    color[offset][2] = col[2]
                    alpha[offset] = 1.0
                    depth[offset] = z

    for idx in opaque:
        raster(idx, blend=False)
    for idx in sorted(glass, key=lambda i: -sum(screen[i * 3 + k][2] for k in range(3))):
        raster(idx, blend=True)

    image = Image.new("RGBA", (size, size))
    pixels = image.load()
    for py in range(size):
        for px in range(size):
            a = alpha[py * size + px]
            if a <= 0.0:
                pixels[px, py] = (0, 0, 0, 0)
                continue
            r, g, b = color[py * size + px]
            pixels[px, py] = (
                max(0, min(255, int(round(r * 255)))),
                max(0, min(255, int(round(g * 255)))),
                max(0, min(255, int(round(b * 255)))),
                max(0, min(255, int(round(a * 255)))),
            )
    return image


# --------------------------------------------------------------------------
# post: downsample, snap to palette, hard alpha
# --------------------------------------------------------------------------

def downsample(image: Image.Image, target: int) -> Image.Image:
    """Premultiplied box reduction so edges do not fringe dark."""
    width, height = image.size
    small = Image.new("RGBA", (target, target))
    src = image.load()
    dst = small.load()
    step = width // target
    for ty in range(target):
        for tx in range(target):
            total = [0.0, 0.0, 0.0]
            total_a = 0.0
            for y in range(ty * step, (ty + 1) * step):
                for x in range(tx * step, (tx + 1) * step):
                    r, g, b, a = src[x, y]
                    af = a / 255.0
                    total[0] += r * af
                    total[1] += g * af
                    total[2] += b * af
                    total_a += af
            count = step * step
            if total_a <= 1e-6:
                dst[tx, ty] = (0, 0, 0, 0)
                continue
            dst[tx, ty] = (
                max(0, min(255, int(round(total[0] / total_a)))),
                max(0, min(255, int(round(total[1] / total_a)))),
                max(0, min(255, int(round(total[2] / total_a)))),
                max(0, min(255, int(round(255.0 * total_a / count)))),
            )
    return small


def snap_to_palette(image: Image.Image) -> Image.Image:
    """Quantise to the designed ramps and harden the alpha channel."""
    palette = [(int(c[0] * 255), int(c[1] * 255), int(c[2] * 255)) for c in PALETTE]
    small = Image.new("RGBA", image.size)
    src = image.load()
    dst = small.load()
    for y in range(image.size[1]):
        for x in range(image.size[0]):
            r, g, b, a = src[x, y]
            if a < 110:
                dst[x, y] = (0, 0, 0, 0)
                continue
            best, best_d = palette[0], 1 << 30
            for pr, pg, pb in palette:
                # luma-weighted distance keeps ramps perceptually ordered
                d = 2 * (r - pr) ** 2 + 4 * (g - pg) ** 2 + 3 * (b - pb) ** 2
                if d < best_d:
                    best_d, best = d, (pr, pg, pb)
            dst[x, y] = (best[0], best[1], best[2], 255)
    return small


# --------------------------------------------------------------------------
# preview + entry point
# --------------------------------------------------------------------------

RAMPS = (
    ("S", STEEL), ("C", COPPER), ("B", BRASS), ("P", PALE),
    ("I", IVORY), ("K", RUBBER), ("G", GLASS), ("A", AMBER), ("R", RED),
)


def classify(rgb) -> str:
    best, best_d = "S", 1 << 30
    for label, ramp_colors in RAMPS:
        for color in ramp_colors:
            pr, pg, pb = (int(c * 255) for c in color)
            d = 2 * (rgb[0] - pr) ** 2 + 4 * (rgb[1] - pg) ** 2 + 3 * (rgb[2] - pb) ** 2
            if d < best_d:
                best_d, best = d, label
    return best


def preview(image: Image.Image, title: str) -> None:
    px = image.load()
    lum_chars = ".:-=+*#%@"
    print(f"--- {title} (luminance, ' '=transparent) ---")
    for y in range(image.size[1]):
        row = []
        for x in range(image.size[0]):
            r, g, b, a = px[x, y]
            if a == 0:
                row.append(" ")
                continue
            lum = (r * 299 + g * 587 + b * 114) / 1000
            row.append(lum_chars[min(len(lum_chars) - 1, int(lum / 25.6))])
        print("|" + "".join(row) + "|")
    print(f"--- {title} (material) ---")
    for y in range(image.size[1]):
        row = []
        for x in range(image.size[0]):
            r, g, b, a = px[x, y]
            row.append(" " if a == 0 else classify((r, g, b)))
        print("|" + "".join(row) + "|")
    print()


def stats(image: Image.Image):
    opaque = 0
    colors = set()
    for y in range(image.size[1]):
        for x in range(image.size[0]):
            pixel = image.getpixel((x, y))
            if pixel[3] > 0:
                opaque += 1
                colors.add(pixel)
    return opaque, len(colors)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    # the location cursor: one sprite
    cursor_camera = Camera(azimuth_deg=34.0, elevation_deg=26.0, distance=6.5, target=(0.0, 0.14, 0.0))
    cursor_size = TARGET * CURSOR_SUPERSAMPLE
    cursor = snap_to_palette(downsample(render(build_cursor(), cursor_camera, cursor_size), TARGET))
    cursor.save(OUT_DIR / "cursor.png")
    opaque, colors = stats(cursor)
    print(f"cursor.png  {TARGET}x{TARGET}  opaque={opaque}  colours={colors}")
    preview(cursor, "cursor")

    # the submarine compass: same dial, one frame per needle bearing
    compass_camera = Camera(azimuth_deg=0.0, elevation_deg=COMPASS_ELEVATION, distance=6.0,
                            target=(0.0, 0.0, 0.0))
    compass_size = TARGET * COMPASS_SUPERSAMPLE
    fit = compute_fit(build_compass(0.0), compass_camera, compass_size)
    sheet = Image.new("RGBA", (TARGET, TARGET * FRAMES), (0, 0, 0, 0))
    first = None
    for index in range(FRAMES):
        scene = build_compass(index * 360.0 / FRAMES)
        frame = snap_to_palette(downsample(render(scene, compass_camera, compass_size, fit=fit), TARGET))
        sheet.paste(frame, (0, index * TARGET))
        if first is None:
            first = frame
    sheet.save(OUT_DIR / "submarine_compass.png")
    opaque, colors = stats(sheet)
    print(f"submarine_compass.png  {TARGET}x{TARGET * FRAMES}  frames={FRAMES} "
          f"opaque={opaque}  colours={colors}")
    preview(first, "submarine_compass frame 00")


if __name__ == "__main__":
    main()
