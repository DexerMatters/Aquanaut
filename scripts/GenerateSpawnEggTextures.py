#!/usr/bin/env python3
"""
Draw the Aquanaut spawn-egg sprites for the creatures added with ``models.zip``.

Why this exists
---------------
The 24 shipped spawn eggs share one visual language: a 12x14 pixel egg inside a
16x16 canvas, lit from the top/upper-left, with a bright band across the top, a
dark rim along the lower-right edge, and - most importantly - a large, bold,
high-contrast emblem that reads as the creature at a glance (``helicoprion``
wears its tooth whorl, ``donutfish`` a ring with a dark hole, ``blue_jellyfish``
a bell over trailing tentacles).

Rather than inventing a new look, this script *measures* that language from the
existing sprites and then paints each new egg from its own creature texture:

* the silhouette comes from the mean occupancy mask of the 24 reference eggs,
  with a small deterministic per-species edge jitter (5% of boundary pixels,
  matching the reference deviation rate) and optional "drip" pixels below the
  egg for jellyfish-like species;
* the lighting comes from the mean normalised luminance map of the reference
  eggs, so highlights and shadows land exactly where the originals put them;
* the colours come from the species' entity texture (body cluster, accent
  cluster, darkest outline tone);
* the creature's identity comes from a hand-built emblem drawn with the small
  pixel primitives below (discs, rings, arcs, lines, spirals, lightning) in a
  ten-role tone palette - outline, deep/mid/dark base, accent light/mid/dark,
  glint - plus a body texture pass (scales, speckles, bubbles, gills, arcs).

Every generated sprite is checked against the reference ranges (opaque pixel
count, distinct colours, relative luminance spread, mean luminance) and the run
fails if a sprite drifts away from the shipped set.  Suggested spawn-egg
primary/secondary colours are printed for ``ItemRegistry``.

Usage
-----
    python3 scripts/GenerateSpawnEggTextures.py
    python3 scripts/GenerateSpawnEggTextures.py --preview
    python3 scripts/GenerateSpawnEggTextures.py --only golden_carp --preview
"""

from __future__ import annotations

import argparse
import math
import warnings
import random
import statistics
import sys
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent
ASSETS = REPO_ROOT / "src" / "main" / "resources" / "assets" / "aquanaut"
ITEM_DIR = ASSETS / "textures" / "item"
ENTITY_DIR = ASSETS / "textures" / "entity"

SIZE = 16

# Reference ranges measured across the 24 shipped spawn eggs.
REFERENCE_OPAQUE = (120, 148)
REFERENCE_COLOURS = (45, 130)
REFERENCE_RELATIVE_STD = (0.10, 0.60)
REFERENCE_MEAN_LUMA = (45, 195)  # the shipped set spans 25..184
REFERENCE_DEVIATION_RATE = 0.055
# Median relative luminance spread and median adjacent-pixel contrast of the shipped eggs.  Each new
# sprite is contrast-matched on the local metric (that is what makes an emblem readable at 16 px)
# while being kept inside the reference spread.
REFERENCE_RELATIVE_STD_TARGET = 0.40
REFERENCE_LOCAL_CONTRAST = 36.0
REFERENCE_LOCAL_CONTRAST_LIMIT_SPREAD = 0.57
# Eggs must not all look alike.  Measured as the mean absolute luminance difference over the 16x16
# canvas; the shipped set's closest pair (both jellyfish) sits at 19.6 and its median at 59.1.
REFERENCE_DISTINCTNESS_MIN = 30.0

SHADE_STRENGTH_SCALE = 0.95
RIM_DARKEN = 0.22
RIM_MIX = 0.35


class Species:
    def __init__(self, slug: str, emblem: str, texture: str = "mottle", canvas: str = "m",
                 drips: int = 0, accent_shift: float = 1.0, note: str = ""):
        self.slug = slug
        self.emblem = emblem
        self.texture = texture
        self.canvas = canvas  # tone role the unpainted hide starts from
        self.drips = drips
        self.accent_shift = accent_shift
        self.note = note


SPECIES: tuple[Species, ...] = (
    Species("vamprey", "sucker", "mottle", canvas="d", accent_shift=0.9,
            note="pale body, blood-red toothed sucker"),
    Species("oresucker", "ore_gems", "rock", canvas="d", accent_shift=1.15,
            note="rocky hide studded with ore"),
    Species("flagellonautilus", "shell_spiral", "mottle", canvas="d", drips=1, accent_shift=1.05,
            note="chambered shell spiral"),
    Species("skeleton_carp", "fish_skeleton", "dark_water", canvas="d", accent_shift=1.0,
            note="skull, spine and ribs"),
    Species("golden_carp", "scale_drift", "scales", canvas="d", accent_shift=1.1,
            note="overlapping golden scales"),
    Species("silver_carp", "shoal_flash", "scales", canvas="d", accent_shift=0.9,
            note="scales split by a bright flash"),
    Species("gentlefish", "top_hat", "felt", canvas="m", accent_shift=0.9,
            note="top hat, monocle and bow tie"),
    Species("slimmy", "slime_stack", "bubbles", canvas="d", drips=1, accent_shift=1.2,
            note="slime beads over a drip"),
    Species("ionfin", "plasma_ring", "arcs", canvas="b", accent_shift=1.25,
            note="plasma torus with arcs"),
    Species("opticichthus", "great_lens", "radial", canvas="d", accent_shift=1.1,
            note="one enormous lens eye"),
    Species("gemini_jellyfish", "twin_bells", "bubbles", canvas="d", drips=2, accent_shift=1.15,
            note="twin bells over tentacles"),
    Species("ecofish", "reef_sprig", "bubbles", canvas="d", drips=3, accent_shift=1.2,
            note="coral sprig with polyps"),
    Species("pale_abyss_hydra", "hydra_maw", "gills", canvas="d", drips=2, accent_shift=0.95,
            note="toothed maw ringed by arms"),
    Species("three_headed_shark", "three_jaws", "gills", canvas="d", accent_shift=0.9,
            note="three heads, three rows of teeth"),
)

# -- reference analysis ---------------------------------------------------


def reference_sprites() -> list[Image.Image]:
    """The shipped eggs only: the generator must never measure its own output.

    Regenerating with previously generated eggs in the reference set would make each run drift, so
    anything in :data:`SPECIES` is excluded explicitly.
    """
    generated = {f"{species.slug}_spawn_egg.png" for species in SPECIES}
    files = [path for path in sorted(ITEM_DIR.glob("*_spawn_egg.png")) if path.name not in generated]
    if len(files) < 12:
        raise SystemExit("not enough reference spawn eggs found; run from the repository root")
    return [Image.open(path).convert("RGBA") for path in files]


def build_reference_model() -> tuple[list[list[int]], list[list[float]], list[Image.Image]]:
    """Mean silhouette and mean lighting ratio map of the shipped spawn eggs."""
    sprites = reference_sprites()
    occupancy = [[0] * SIZE for _ in range(SIZE)]
    ratio_sum = [[0.0] * SIZE for _ in range(SIZE)]
    ratio_count = [[0] * SIZE for _ in range(SIZE)]

    for image in sprites:
        pixels = image.load()
        values = [
            luminance(pixels[x, y])
            for y in range(SIZE)
            for x in range(SIZE)
            if pixels[x, y][3] > 16
        ]
        mean = statistics.mean(values) or 1.0
        for y in range(SIZE):
            for x in range(SIZE):
                r, g, b, a = pixels[x, y]
                if a <= 16:
                    continue
                occupancy[y][x] += 1
                ratio_sum[y][x] += luminance((r, g, b, a)) / mean
                ratio_count[y][x] += 1

    threshold = max(6, len(sprites) // 2)
    mask = [[1 if occupancy[y][x] >= threshold else 0 for x in range(SIZE)] for y in range(SIZE)]
    # Lighting template: how much brighter/darker the average egg is at each
    # pixel relative to its own mean.  Applying this is what keeps the new
    # sprites in the same light as the shipped ones.
    ratio = [[1.0] * SIZE for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            if ratio_count[y][x] >= max(4, len(sprites) // 4):
                ratio[y][x] = ratio_sum[y][x] / ratio_count[y][x]
    ratio = smooth(ratio, mask, passes=1)
    return mask, ratio, sprites


def smooth(grid: list[list[float]], mask: list[list[int]], passes: int = 1) -> list[list[float]]:
    weights = ((1, 1, 1), (1, 4, 1), (1, 1, 1))
    for _ in range(passes):
        out = [[0.0] * SIZE for _ in range(SIZE)]
        for y in range(SIZE):
            for x in range(SIZE):
                total = 0.0
                weight_sum = 0.0
                for dy in (-1, 0, 1):
                    for dx in (-1, 0, 1):
                        ny, nx = y + dy, x + dx
                        if 0 <= ny < SIZE and 0 <= nx < SIZE and mask[ny][nx]:
                            weight = weights[dy + 1][dx + 1]
                            total += grid[ny][nx] * weight
                            weight_sum += weight
                out[y][x] = total / weight_sum if weight_sum else grid[y][x]
        grid = out
    return grid


def luminance(pixel) -> float:
    return 0.2126 * pixel[0] + 0.7152 * pixel[1] + 0.0722 * pixel[2]


# -- palette --------------------------------------------------------------


def cluster_palette(slug: str) -> dict:
    """Body / accent / outline colours sampled from the species' entity texture."""
    path = ENTITY_DIR / f"{slug}.png"
    if not path.exists():
        raise SystemExit(f"missing entity texture for {slug}: {path}")
    image = Image.open(path).convert("RGBA")
    pixels = [pixel for pixel in pixel_data(image) if pixel[3] > 200]
    if not pixels:
        raise SystemExit(f"entity texture for {slug} has no opaque pixels")

    buckets: dict[tuple[int, int, int], list[int]] = {}
    for r, g, b, _ in pixels:
        key = (r >> 4, g >> 4, b >> 4)
        buckets.setdefault(key, []).append(r << 16 | g << 8 | b)

    ranked = sorted(buckets.items(), key=lambda item: -len(item[1]))
    body_rgb = normalise_luma(mean_rgb(ranked[0][1]), 72, 188)
    body_luma = luminance((*body_rgb, 255))

    accent_rgb = body_rgb
    best_score = -1.0
    for key, values in ranked[:8]:
        candidate = normalise_luma(mean_rgb(values), 45, 210)
        candidate_luma = luminance((*candidate, 255))
        saturation = max(candidate) - min(candidate)
        score = saturation / 255.0 + abs(candidate_luma - body_luma) / 255.0 * 0.6
        if score > best_score:
            best_score = score
            accent_rgb = candidate

    # Keep the accent inside the same tonal band as the body: the shipped eggs
    # are tonal studies of one creature, not high-contrast emblems.
    accent_rgb = clamp_luma(accent_rgb, body_luma - 48, body_luma + 52)
    return {
        "body": body_rgb,
        "accent": accent_rgb,
        "dark": mix(body_rgb, (0, 0, 0), 0.45),
        "light": mix(body_rgb, (255, 255, 255), 0.34),
    }


def tone_at(rgb: tuple[int, int, int], target: float) -> tuple[int, int, int]:
    """Push a colour's luminance towards ``target`` while keeping its hue.

    The shipped eggs are tonal studies: deep shadows (their darkest 5% sits around luma 45, some as
    low as 3) with bright crests (top 5% around 190).  Mixing towards black/white lets an emblem hit
    those anchors whatever the creature's palette is.
    """
    current = luminance((*rgb, 255))
    if target >= current:
        return mix(rgb, (255, 255, 255), min(0.94, (target - current) / max(1.0, 255.0 - current)))
    return mix(rgb, (0, 0, 0), min(0.94, (current - target) / max(1.0, current)))


def tones(palette: dict, accent_shift: float) -> dict[str, tuple[int, int, int]]:
    """The ten painting roles an emblem may use, anchored on the shipped tonal band."""
    body = palette["body"]
    accent = scale_rgb(palette["accent"], accent_shift)
    base = luminance((*body, 255))
    return {
        "l": tone_at(body, min(230.0, base + 0.36 * (255.0 - base))),   # body highlight
        "m": body,                                                      # body
        "d": tone_at(body, max(54.0, base * 0.62)),                     # body shade
        "D": tone_at(body, max(34.0, base * 0.38)),                     # body deep shade
        "a": tone_at(accent, max(62.0, base * 0.92)),                   # accent
        "A": tone_at(accent, min(228.0, base + 0.42 * (255.0 - base))),  # accent light
        "b": tone_at(accent, max(46.0, base * 0.50)),                   # accent dark
        "o": tone_at(accent, 30.0),                                     # outline / deep shadow
        "w": tone_at(accent, 238.0),                                    # glint / teeth
        "g": tone_at(body, min(232.0, base + 0.70 * (255.0 - base))),   # foam / flash
    }


def normalise_luma(rgb: tuple[int, int, int], low: float, high: float) -> tuple[int, int, int]:
    value = luminance((*rgb, 255))
    if value < low:
        return mix(rgb, (255, 255, 255), min(0.75, (low - value) / max(1.0, 255.0 - value)))
    if value > high:
        return mix(rgb, (0, 0, 0), min(0.75, (value - high) / max(1.0, value)))
    return rgb


def clamp_luma(rgb: tuple[int, int, int], low: float, high: float) -> tuple[int, int, int]:
    value = luminance((*rgb, 255))
    if value < low:
        return mix(rgb, (230, 230, 230), min(0.7, (low - value) / 120.0))
    if value > high:
        return mix(rgb, (30, 30, 30), min(0.7, (value - high) / 120.0))
    return rgb


def mean_rgb(packed: list[int]) -> tuple[int, int, int]:
    r = sum((value >> 16) & 0xFF for value in packed) // len(packed)
    g = sum((value >> 8) & 0xFF for value in packed) // len(packed)
    b = sum(value & 0xFF for value in packed) // len(packed)
    return (r, g, b)


def scale_rgb(rgb: tuple[int, int, int], factor: float) -> tuple[int, int, int]:
    return tuple(max(0, min(255, int(round(channel * factor)))) for channel in rgb)


def mix(a: tuple[int, int, int], b: tuple[int, int, int], t: float) -> tuple[int, int, int]:
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


# -- silhouette -----------------------------------------------------------


def body_mask(species: Species, canonical: list[list[int]]) -> list[list[int]]:
    rng = random.Random(f"aquanaut-egg-{species.slug}")
    mask = [row[:] for row in canonical]

    # Match the reference sprites: a small share of boundary pixels differ by one
    # pixel, which is what gives the shipped eggs their hand-painted wobble.
    boundary = []
    for y in range(SIZE):
        for x in range(SIZE):
            if not canonical[y][x]:
                continue
            neighbours = [
                canonical[ny][nx]
                for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1))
                if 0 <= ny < SIZE and 0 <= nx < SIZE
            ]
            if any(value == 0 for value in neighbours):
                boundary.append((y, x))
    rng.shuffle(boundary)
    for y, x in boundary[: int(len(boundary) * REFERENCE_DEVIATION_RATE)]:
        if neighbours_opaque(mask, y, x) >= 3:
            mask[y][x] = 0

    for y, x in boundary[: int(len(boundary) * REFERENCE_DEVIATION_RATE * 0.6)]:
        for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
            if 0 <= ny < SIZE and 0 <= nx < SIZE and mask[ny][nx] == 0 and neighbours_opaque(mask, ny, nx) >= 3:
                mask[ny][nx] = 1

    # Jellyfish-like species trail a few pixels below the egg, like the shipped
    # blue jellyfish / catfish sprites do.
    if species.drips:
        bottom = max(y for y in range(SIZE) for x in range(SIZE) if mask[y][x])
        for index in range(species.drips):
            x = 6 + (index % 3)
            y = bottom + 1 + (index // 3)
            if y < SIZE:
                mask[y][x] = 1
    return mask


def neighbours_opaque(mask: list[list[int]], y: int, x: int) -> int:
    total = 0
    for ny, nx in ((y - 1, x), (y + 1, x), (y, x - 1), (y, x + 1)):
        if 0 <= ny < SIZE and 0 <= nx < SIZE and mask[ny][nx]:
            total += 1
    return total


def boundary_mask(mask: list[list[int]]) -> list[list[int]]:
    out = [[0] * SIZE for _ in range(SIZE)]
    for y in range(SIZE):
        for x in range(SIZE):
            if mask[y][x] and neighbours_opaque(mask, y, x) < 4:
                out[y][x] = 1
    return out


# -- pixel primitives -----------------------------------------------------


class Painter:
    """Collects per-pixel tone roles for one egg.  Only solid pixels are painted."""

    def __init__(self, mask: list[list[int]], canvas: str | None = None):
        self.mask = mask
        self.canvas = canvas
        self.body_only = False
        self.role: list[list[str | None]] = [[None] * SIZE for _ in range(SIZE)]

    def get(self, x: int, y: int) -> str | None:
        if 0 <= x < SIZE and 0 <= y < SIZE and self.mask[y][x]:
            return self.role[y][x]
        return None

    def put(self, x: int, y: int, role: str) -> None:
        if not (0 <= x < SIZE and 0 <= y < SIZE and self.mask[y][x]):
            return
        if self.body_only and self.role[y][x] not in (None, self.canvas):
            return  # the texture pass never paints over an emblem
        self.role[y][x] = role

    def disc(self, cx: float, cy: float, radius: float, role: str) -> None:
        for y in range(SIZE):
            for x in range(SIZE):
                if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= radius * radius:
                    self.put(x, y, role)

    def ring(self, cx: float, cy: float, radius: float, thickness: float, role: str) -> None:
        inner = max(0.0, radius - thickness)
        for y in range(SIZE):
            for x in range(SIZE):
                distance = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if inner <= distance <= radius:
                    self.put(x, y, role)

    def arc(self, cx: float, cy: float, radius: float, start: float, end: float, role: str) -> None:
        step = max(2.0, 24.0 / max(1.0, radius))
        angle = start
        while angle <= end:
            x = int(round(cx + radius * math.cos(math.radians(angle)) - 0.5))
            y = int(round(cy - radius * math.sin(math.radians(angle)) - 0.5))
            self.put(x, y, role)
            angle += step

    def spiral(self, cx: float, cy: float, radius: float, decay: float, turns: float, role: str) -> None:
        total = turns * 360.0
        angle = 0.0
        while angle <= total:
            r = radius * math.exp(-decay * angle / 360.0)
            x = int(round(cx + r * math.cos(math.radians(angle)) - 0.5))
            y = int(round(cy - r * math.sin(math.radians(angle)) - 0.5))
            self.put(x, y, role)
            angle += 9.0

    def line(self, x0: float, y0: float, x1: float, y1: float, role: str) -> None:
        steps = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for index in range(steps + 1):
            t = index / steps if steps else 0.0
            x = int(round(x0 + (x1 - x0) * t - 0.5))
            y = int(round(y0 + (y1 - y0) * t - 0.5))
            self.put(x, y, role)

    def rect(self, x0: int, y0: int, x1: int, y1: int, role: str) -> None:
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for x in range(min(x0, x1), max(x0, x1) + 1):
                self.put(x, y, role)

    def outline(self, role: str = "o") -> None:
        """Paint a one pixel border around every already-painted pixel."""
        edges = [
            (x + dx, y + dy)
            for y in range(SIZE)
            for x in range(SIZE)
            if self.role[y][x] is not None
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
        ]
        for x, y in edges:
            if 0 <= x < SIZE and 0 <= y < SIZE and self.mask[y][x] and self.role[y][x] is None:
                self.role[y][x] = role

    def painted(self, x: int, y: int) -> bool:
        return 0 <= x < SIZE and 0 <= y < SIZE and self.role[y][x] is not None


# -- emblems --------------------------------------------------------------


def sparkle(p: Painter, cx: float, cy: float, size: float = 2.0, role: str = "w") -> None:
    """A four point star - the pack's shorthand for something valuable or charged."""
    p.line(cx - size, cy, cx + size, cy, role)
    p.line(cx, cy - size, cx, cy + size, role)
    p.put(int(cx), int(cy), "w")


def minnow(p: Painter, cx: float, cy: float, size: float = 1.0, body: str = "b", tail: str = "o") -> None:
    """A tiny fish silhouette: dab of body, forked tail, bright eye."""
    p.disc(cx, cy, size + 0.6, body)
    p.put(int(cx - 1), int(cy - 1), "w")
    p.line(cx + size, cy - 0.6 * size, cx + size + 1.6 * size, cy - 1.5 * size, tail)
    p.line(cx + size, cy + 0.6 * size, cx + size + 1.6 * size, cy + 1.5 * size, tail)
    p.line(cx + size, cy, cx + size + 1.6 * size, cy, tail)


def emblem_sucker(p: Painter) -> None:
    """vamprey: the huge lamprey sucker fills the lower egg, one eye glows above it."""
    p.disc(8.4, 10.4, 6.6, "o")
    p.disc(8.4, 10.4, 5.5, "a")
    p.disc(8.4, 10.4, 4.0, "o")
    p.disc(8.4, 10.4, 2.7, "b")
    p.disc(8.4, 10.4, 1.2, "o")
    for index in range(12):
        angle = math.radians(index * 30.0 + 15.0)
        for radius in (4.7, 3.3):
            p.put(int(round(8.4 + radius * math.cos(angle) - 0.5)),
                  int(round(10.4 - radius * math.sin(angle) - 0.5)), "w")
    p.disc(4.6, 3.6, 1.8, "o")
    p.disc(4.6, 3.6, 1.0, "w")
    p.line(10.0, 2.4, 12.4, 6.0, "g")
    p.line(11.0, 2.4, 13.0, 5.2, "o")


def emblem_ore_gems(p: Painter) -> None:
    """oresucker: a boulder hide packed with ore, grinding sucker at the bottom rim."""
    for x, y, size in ((3, 3, 3), (9, 2, 2), (10, 6, 3), (3, 8, 2), (2, 12, 2), (11, 11, 2)):
        p.rect(x - 1, y - 1, x + size, y + size, "o")
        p.rect(x, y, x + size - 1, y + size - 1, "g")
        p.rect(x, y, x + size - 2, y + size - 2, "a")
        p.put(x, y, "w")
    p.disc(8.0, 15.0, 3.0, "o")
    p.disc(8.0, 15.0, 2.1, "g")
    p.disc(8.0, 15.0, 1.0, "o")


def emblem_shell_spiral(p: Painter) -> None:
    """flagellonautilus: a chambered shell coiling up the egg, tentacles trailing below."""
    p.disc(6.6, 6.6, 6.8, "o")
    p.spiral(6.6, 6.6, 5.2, 0.20, 2.4, "g")
    p.spiral(6.6, 6.6, 4.4, 0.20, 2.2, "o")
    p.spiral(6.6, 6.6, 3.4, 0.20, 2.0, "g")
    p.spiral(6.6, 6.6, 2.6, 0.20, 1.8, "o")
    p.disc(6.6, 6.6, 1.0, "w")
    for index in range(4):
        degrees = index * 90.0 + 30.0
        angle = math.radians(degrees)
        radius = 5.2 * math.exp(-0.20 * degrees / 360.0)
        p.line(6.6, 6.6, 6.6 + radius * math.cos(angle), 6.6 - radius * math.sin(angle), "o")
    for cx, cy, radius, start_angle, end_angle in ((10.6, 10.6, 3.6, 170.0, 330.0),
                                                   (12.4, 13.0, 2.8, 190.0, 350.0)):
        p.arc(cx, cy, radius, start_angle, end_angle, "g")
        p.arc(cx, cy, radius + 0.8, start_angle, end_angle, "o")


def emblem_fish_skeleton(p: Painter) -> None:
    """skeleton_carp: the fossil laid corner to corner, skull high, tail fin low."""
    p.disc(4.4, 4.0, 3.2, "o")
    p.disc(4.4, 4.0, 2.6, "w")
    p.disc(4.0, 3.8, 1.0, "o")
    p.line(1.8, 6.4, 7.0, 5.6, "o")
    for index in range(6):
        x = 6.4 + index * 1.2
        y = 6.2 + index * 1.1
        p.rect(int(x), int(y), int(x) + 1, int(y) + 1, "w")
    for index in range(4):
        x = 7.0 + index * 1.5
        y = 6.6 + index * 1.35
        p.line(x, y, x + 2.6, y - 2.6, "w")
        p.line(x, y, x + 2.8, y + 1.8, "w")
    p.arc(13.4, 13.6, 3.0, -110.0, 70.0, "w")
    p.arc(13.4, 13.6, 3.8, -110.0, 70.0, "o")


def emblem_scale_drift(p: Painter) -> None:
    """golden_carp: a golden flank of scales, fan tail and the luck sparkle."""
    p.line(2.2, 6.6, 8.8, 1.8, "o")
    p.line(2.8, 6.6, 8.8, 2.4, "g")
    p.line(3.6, 7.0, 9.4, 3.2, "w")
    for row in range(3):
        base = 7.2 + row * 2.6
        for column in range(4):
            cx = 2.8 + column * 3.0 + (row % 2) * 1.5
            if not p.mask[min(15, int(base))][max(0, min(15, int(cx)))]:
                continue
            p.arc(cx, base, 2.4, 190.0, 350.0, "o")
            p.arc(cx, base, 1.7, 195.0, 345.0, "g" if (row + column) % 2 else "b")
    for index in range(4):
        p.line(10.6, 12.6, 12.8 + index * 0.3, 8.4 + index * 1.5, "g")
        p.line(10.6, 12.6, 13.4, 9.6 + index * 1.5, "o")
    p.disc(4.4, 4.6, 1.8, "o")
    p.disc(4.4, 4.6, 1.0, "w")
    sparkle(p, 11.0, 2.6, 1.8)


def emblem_shoal_flash(p: Painter) -> None:
    """silver_carp: a shoal of little fish racing along the blinding flash."""
    for index in range(13):
        x = 2.2 + index * 0.8
        p.line(x, 13.2 - index * 0.62, x + 1.6, 13.2 - index * 0.62, "g")
    p.line(2.0, 9.6, 13.0, 5.2, "w")
    for x, y in ((3.4, 3.6), (6.4, 5.4), (9.2, 7.2), (11.6, 9.0)):
        minnow(p, x, y, 1.0, "b", "o")
    sparkle(p, 12.6, 3.4, 1.6)


def emblem_top_hat(p: Painter) -> None:
    """gentlefish: top hat, monocle, bow tie and a curling moustache."""
    p.rect(4, 0, 11, 5, "o")
    p.rect(5, 1, 10, 4, "b")
    p.rect(5, 1, 6, 4, "D")
    p.rect(7, 1, 9, 3, "g")
    p.rect(3, 5, 12, 6, "o")
    p.rect(5, 4, 10, 4, "a")
    p.ring(9.8, 9.8, 3.4, 1.4, "o")
    p.ring(9.8, 9.8, 2.2, 1.0, "g")
    p.put(8, 8, "w")
    p.line(3.6, 11.6, 7.4, 10.8, "a")
    p.line(3.6, 11.6, 7.4, 13.2, "a")
    p.line(11.8, 11.6, 8.0, 10.8, "b")
    p.line(11.8, 11.6, 8.0, 13.2, "b")
    p.put(7, 12, "w")


def emblem_slime_stack(p: Painter) -> None:
    """slimmy: glossy slime beads crawling down the egg, leaving a drip trail."""
    for cx, cy, radius in ((6.0, 5.0, 3.4), (10.4, 8.6, 2.6), (4.8, 11.4, 2.2)):
        p.disc(cx, cy, radius + 0.7, "o")
        p.disc(cx, cy, radius, "a")
        p.disc(cx - radius * 0.35, cy - radius * 0.35, radius * 0.5, "g")
        p.put(int(cx - radius * 0.6), int(cy - radius * 0.6), "w")
    p.disc(6.0, 5.0, 0.9, "o")
    p.disc(7.6, 5.0, 0.9, "o")
    p.put(6, 4, "w")
    p.put(8, 4, "w")
    for x, drop in ((7.0, 3.0), (9.0, 4.0), (5.6, 3.6)):
        p.line(x, 12.0, x - 0.4, 12.0 + drop, "a")
        p.put(int(x), int(12.0 + drop), "g")
    p.line(8.4, 13.6, 10.4, 13.6, "a")


def emblem_plasma_ring(p: Painter) -> None:
    """ionfin: a lightning bolt through the egg, plasma coil clamped around it."""
    p.arc(8.0, 7.6, 4.6, 200.0, 340.0, "g")
    p.arc(8.0, 7.6, 5.4, 200.0, 340.0, "o")
    p.arc(8.0, 9.0, 4.6, 20.0, 160.0, "g")
    p.arc(8.0, 9.0, 5.4, 20.0, 160.0, "o")
    p.line(9.8, 1.6, 6.4, 7.2, "o")
    p.line(10.4, 1.6, 7.0, 7.2, "w")
    p.line(6.4, 7.2, 9.4, 7.2, "o")
    p.line(9.4, 7.2, 5.6, 14.4, "o")
    p.line(10.0, 7.2, 6.2, 14.4, "w")
    for x, y in ((3.2, 4.2), (12.6, 5.0), (4.2, 12.0), (12.2, 11.6)):
        sparkle(p, x, y, 1.6)


def emblem_great_lens(p: Painter) -> None:
    """opticichthus: one enormous eye - lid, iris, pupil and a hard glint."""
    p.disc(7.8, 8.6, 7.0, "o")
    p.disc(7.8, 8.6, 5.6, "g")
    p.disc(7.8, 8.6, 4.4, "A")
    for index in range(8):
        angle = math.radians(index * 45.0 + 22.0)
        p.line(7.8 + 2.8 * math.cos(angle), 8.6 - 2.8 * math.sin(angle),
               7.8 + 4.6 * math.cos(angle), 8.6 - 4.6 * math.sin(angle), "b")
    p.disc(7.8, 8.6, 2.6, "o")
    p.disc(7.8, 8.6, 1.4, "w")
    p.line(1.2, 3.6, 14.6, 3.6, "o")
    p.line(2.0, 4.4, 13.8, 4.4, "b")
    p.put(6, 6, "w")
    p.put(7, 6, "w")


def emblem_twin_bells(p: Painter) -> None:
    """gemini_jellyfish: twin bells mirrored across the egg - one favouring, one devouring."""
    p.disc(8.0, 4.6, 5.0, "o")
    p.disc(8.0, 4.8, 4.2, "g")
    p.disc(8.0, 5.2, 2.6, "w")
    for index in range(7):
        angle = math.radians(index * 26.0 + 6.0)
        p.put(int(round(8.0 + 3.8 * math.cos(angle) - 0.5)),
              int(round(4.6 - 3.4 * math.sin(angle) - 0.5)), "b")
    p.disc(8.0, 12.2, 4.2, "o")
    p.disc(8.0, 12.0, 3.4, "b")
    p.disc(8.0, 11.6, 1.8, "a")
    for index, x in enumerate((3, 6, 10, 13)):
        p.line(x, 8.4 + (index % 2) * 0.4, x + 0.6, 14.2, "a")
        p.put(x, 9 + index // 2, "w")
    p.put(5, 2, "w")


def emblem_reef_sprig(p: Painter) -> None:
    """ecofish: a pocket reef - coral, weed, polyps, a minnow and rising bubbles."""
    p.rect(1, 13, 14, 15, "o")
    p.rect(2, 13, 13, 14, "A")
    p.rect(6, 5, 9, 14, "o")
    p.rect(7, 6, 8, 13, "g")
    p.rect(7, 6, 7, 12, "w")
    for y in (9, 6):
        p.line(8.0, y, 3.6, y - 2.4, "o")
        p.line(8.0, y, 3.6, y - 2.4, "g")
        p.line(8.0, y, 12.4, y - 2.4, "o")
        p.line(8.0, y, 12.4, y - 2.4, "g")
        p.put(3, y - 3, "w")
        p.put(12, y - 3, "w")
    for x, y in ((3.4, 4.2), (12.4, 7.0), (6.4, 3.2)):
        p.disc(x + 0.5, y + 0.5, 1.7, "o")
        p.disc(x + 0.5, y + 0.5, 1.0, "w")
    minnow(p, 11.0, 10.4, 1.0, "b", "o")
    p.put(4, 11, "w")


def emblem_hydra_maw(p: Painter) -> None:
    """pale_abyss_hydra: a glowing maw with six arms reaching the egg's edge."""
    p.disc(8.0, 8.4, 5.0, "o")
    p.disc(8.0, 8.4, 4.0, "b")
    p.disc(8.0, 8.4, 2.4, "o")
    p.disc(8.0, 8.4, 1.4, "w")
    for index in range(11):
        angle = math.radians(index * 33.0 + 8.0)
        p.put(int(round(8.0 + 3.3 * math.cos(angle) - 0.5)),
              int(round(8.4 - 3.3 * math.sin(angle) - 0.5)), "w")
    for index in range(6):
        angle = math.radians(index * 60.0 + 15.0)
        ex = 8.0 + 7.2 * math.cos(angle)
        ey = 8.4 - 7.2 * math.sin(angle)
        mx = 8.0 + 5.0 * math.cos(angle)
        my = 8.4 - 5.0 * math.sin(angle)
        p.line(8.0, 8.4, mx, my, "o")
        p.line(mx, my, ex, ey, "g")
        p.put(int(round(ex - 0.5)), int(round(ey - 0.5)), "w")
        p.put(int(round(mx - 0.5)), int(round(my - 0.5)), "w")


def emblem_three_jaws(p: Painter) -> None:
    """three_headed_shark: three bright heads in a diagonal pack, teeth bared."""
    for cx, cy, radius in ((5.0, 10.6, 3.4), (8.6, 6.6, 2.9), (11.6, 3.8, 2.2)):
        p.disc(cx, cy, radius + 0.8, "o")
        p.disc(cx, cy, radius, "g")
        p.disc(cx, cy, radius - 1.2, "A")
        p.arc(cx, cy, radius - 0.5, 25.0, 155.0, "w")
        p.disc(cx - radius * 0.3, cy - radius * 0.4, 1.0, "o")
        p.put(int(cx) - 1, int(cy) - 1, "w")
        jaw = int(cy + radius - 0.2)
        p.line(cx - radius, jaw, cx + radius, jaw, "o")
        for index in range(int(radius)):
            p.put(int(cx) - int(radius) + 1 + index * 2, jaw - 1, "w")
            p.put(int(cx) - int(radius) + 2 + index * 2, jaw - 1, "o")
    for index in range(3):
        p.line(2.2, 4.6 + index * 1.0, 3.0, 8.6 + index * 1.4, "o")
        p.line(2.8, 4.6 + index * 1.0, 3.6, 8.6 + index * 1.4, "b")


EMBLEMS = {
    "sucker": emblem_sucker,
    "ore_gems": emblem_ore_gems,
    "shell_spiral": emblem_shell_spiral,
    "fish_skeleton": emblem_fish_skeleton,
    "scale_drift": emblem_scale_drift,
    "shoal_flash": emblem_shoal_flash,
    "top_hat": emblem_top_hat,
    "slime_stack": emblem_slime_stack,
    "plasma_ring": emblem_plasma_ring,
    "great_lens": emblem_great_lens,
    "twin_bells": emblem_twin_bells,
    "reef_sprig": emblem_reef_sprig,
    "hydra_maw": emblem_hydra_maw,
    "three_jaws": emblem_three_jaws,
}


# -- body textures --------------------------------------------------------


def texture_body(painter: Painter, species: Species, rng: random.Random) -> None:
    """Fill the untouched body pixels: the species canvas plus its texture."""
    for y in range(SIZE):
        for x in range(SIZE):
            if painter.mask[y][x] and painter.role[y][x] is None:
                painter.role[y][x] = species.canvas

    painter.body_only = True
    kind = species.texture
    if kind == "scales":
        for row in range(6):
            base = 2.4 + row * 2.2
            for column in range(6):
                cx = 1.6 + column * 2.8 + (row % 2) * 1.4
                if painter.painted(int(cx), int(base)):
                    continue
                painter.arc(cx, base, 1.3, 205.0, 335.0, "o" if (row + column) % 2 else "a")
    elif kind == "bubbles":
        for index in range(6):
            cx = rng.uniform(2.5, 13.5)
            cy = rng.uniform(2.5, 13.5)
            radius = rng.choice((0.9, 1.1, 1.4))
            if not painter.mask[int(cy)][int(cx)] or painter.role[int(cy)][int(cx)] != species.canvas:
                continue
            painter.disc(cx, cy, radius, "o" if index % 2 else "a")
            painter.put(int(cx), int(cy), "w")
    elif kind == "gills":
        for index in range(4):
            x = 2.6 + index * 0.7
            painter.line(x, 7.0 + index * 0.8, x + 0.6, 11.0 + index * 0.4, "o")
        for index in range(3):
            painter.line(11.4 + index * 0.5, 8.0 + index * 0.7, 11.8 + index * 0.5, 11.4, "o")
    elif kind == "rock":
        for index in range(8):
            x = rng.randint(2, 13)
            y = rng.randint(2, 13)
            if painter.role[y][x] != species.canvas:
                continue
            painter.put(x, y, "o" if index % 2 else "b")
            if painter.get(x, y + 1) == species.canvas:
                painter.put(x, y + 1, "D")
    elif kind == "dark_water":
        for y in range(SIZE):
            for x in range(SIZE):
                if painter.role[y][x] == species.canvas and (x * 2 + y) % 5 == 0:
                    painter.role[y][x] = "b"
    elif kind == "felt":
        for y in range(SIZE):
            for x in range(SIZE):
                if painter.role[y][x] == species.canvas and (x * 2 + y) % 5 == 0:
                    painter.role[y][x] = "d"
    elif kind == "arcs":
        for index in range(4):
            painter.arc(8.0, 8.0, 5.8 + index * 0.9, 20.0 + index * 25.0, 90.0 + index * 25.0, "o")
        for index in range(5):
            x, y = rng.randint(2, 13), rng.randint(2, 13)
            if painter.role[y][x] == species.canvas:
                painter.put(x, y, "A")
    elif kind == "radial":
        for index in range(12):
            angle = math.radians(index * 30.0 + 15.0)
            painter.line(8.0 + 3.0 * math.cos(angle), 8.0 - 3.0 * math.sin(angle),
                         8.0 + 6.6 * math.cos(angle), 8.0 - 6.6 * math.sin(angle), "o")
    else:  # mottle
        for index in range(8):
            x = rng.randint(2, 13)
            y = rng.randint(2, 13)
            if painter.role[y][x] == species.canvas:
                painter.put(x, y, "o" if index % 2 else "b")
            if painter.get(x + 1, y) == species.canvas and index % 3 == 0:
                painter.put(x + 1, y, "l")


# -- render ---------------------------------------------------------------


def render(species: Species, canonical: list[list[int]], ratio: list[list[float]],
           contrast: float = 1.0) -> Image.Image:
    palette = cluster_palette(species.slug)
    role_colours = tones(palette, species.accent_shift)
    if contrast != 1.0:
        # Pull every role towards the body's own luminance.  The emblems are drawn with deliberately
        # deep shadows and bright crests; this is the knob that lands each egg on the shipped tonal
        # spread instead of blowing past it.
        base = luminance((*palette["body"], 255))
        role_colours = {
            role: tone_at(colour, base + (luminance((*colour, 255)) - base) * contrast)
            for role, colour in role_colours.items()
        }
    mask = body_mask(species, canonical)
    rng = random.Random(f"aquanaut-egg-{species.slug}")

    painter = Painter(mask, species.canvas)
    emblem = EMBLEMS.get(species.emblem)
    if emblem is None:
        raise SystemExit(f"unknown emblem {species.emblem!r} for {species.slug}")
    emblem(painter)
    texture_body(painter, species, rng)

    rim = boundary_mask(mask)
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    pixels = image.load()
    for y in range(SIZE):
        for x in range(SIZE):
            if not mask[y][x]:
                continue
            role = painter.role[y][x] or "m"
            base = role_colours[role]
            # Measured mean lighting of the shipped eggs, compressed slightly so
            # a saturated palette cannot blow out the highlight band.
            factor = 1.0 + (ratio[y][x] - 1.0) * SHADE_STRENGTH_SCALE
            shaded = scale_rgb(base, factor)
            if rim[y][x]:
                shaded = scale_rgb(shaded, 1.0 - RIM_DARKEN)
                shaded = mix(shaded, palette["dark"], RIM_MIX * 0.5)
            pixels[x, y] = (*shaded, 255)
    return image


def local_contrast(image: Image.Image) -> float:
    """Mean luminance step between neighbouring solid pixels - how crisp the emblem reads."""
    pixels = image.load()
    differences = []
    for y in range(SIZE):
        for x in range(SIZE):
            if pixels[x, y][3] <= 16:
                continue
            value = luminance(pixels[x, y])
            for nx, ny in ((x + 1, y), (x, y + 1)):
                if nx < SIZE and ny < SIZE and pixels[nx, ny][3] > 16:
                    differences.append(abs(value - luminance(pixels[nx, ny])))
    return statistics.mean(differences) if differences else 0.0


def contrast_matched(species: Species, canonical: list[list[int]], ratio: list[list[float]]) -> Image.Image:
    """Render the egg with the strongest contrast that still fits the shipped tonal envelope.

    Pixel art lives or dies on the luminance step between neighbouring pixels, so walk the contrast
    down from "full punch" and keep the first setting whose overall spread fits inside the reference
    band: that is the most readable egg which still reads as a shipped-style egg.
    """
    limit = REFERENCE_LOCAL_CONTRAST_LIMIT_SPREAD
    best = None
    best_spread = None
    for step in range(41):
        contrast = 1.80 - step * 0.04
        image = render(species, canonical, ratio, contrast)
        spread = stats(image)[2]
        if spread <= limit:
            return image
        if best_spread is None or spread < best_spread:
            best, best_spread = image, spread
    return best


def egg_difference(left: Image.Image, right: Image.Image) -> float:
    """Mean luminance difference between two sprites, counting silhouette changes heavily."""
    a, b = left.load(), right.load()
    differences = []
    for y in range(SIZE):
        for x in range(SIZE):
            pa, pb = a[x, y], b[x, y]
            if pa[3] <= 16 and pb[3] <= 16:
                continue
            if pa[3] <= 16 or pb[3] <= 16:
                differences.append(90.0)
            else:
                differences.append(abs(luminance(pa) - luminance(pb)))
    return statistics.mean(differences) if differences else 0.0


def closest_pair(sprites: dict[str, Image.Image]) -> tuple[float, str, str]:
    slugs = sorted(sprites)
    best = (float("inf"), "", "")
    for index, first in enumerate(slugs):
        for second in slugs[index + 1:]:
            difference = egg_difference(sprites[first], sprites[second])
            if difference < best[0]:
                best = (difference, first, second)
    return best


def pixel_data(image: Image.Image) -> list[tuple[int, ...]]:
    """Pixel list for the statistics below.

    ``Image.getdata`` is deprecated in current Pillow releases but is still the cheapest way to read
    a sprite; the warning is noise here because every sprite is 16x16 RGBA.
    """
    with warnings.catch_warnings():
        warnings.simplefilter("ignore", DeprecationWarning)
        return list(image.getdata())


def stats(image: Image.Image) -> tuple[int, int, float, float]:
    pixels = [pixel for pixel in pixel_data(image) if pixel[3] > 16]
    values = [luminance(pixel) for pixel in pixels]
    distinct = len({pixel[:3] for pixel in pixels})
    mean = statistics.mean(values) if values else 0.0
    relative = statistics.pstdev(values) / mean if mean else 0.0
    return len(pixels), distinct, relative, mean


def dominant_tone(image: Image.Image, brightest: bool) -> tuple[int, int, int]:
    """Mean colour of the brightest/darkest third of the sprite - what the egg reads as."""
    pixels = [pixel[:3] for pixel in pixel_data(image) if pixel[3] > 16]
    ranked = sorted(pixels, key=lambda rgb: luminance((*rgb, 255)), reverse=brightest)
    sample = ranked[: max(1, len(ranked) // 3)]
    return mean_rgb([(r << 16) | (g << 8) | b for r, g, b in sample])


def ascii_preview(image: Image.Image, title: str = "") -> str:
    ramp = " .:-=+*#%@"
    lines = [f"--- {title}"] if title else []
    pixels = image.load()
    for y in range(SIZE):
        row = ""
        for x in range(SIZE):
            r, g, b, a = pixels[x, y]
            if a <= 16:
                row += " "
                continue
            value = luminance((r, g, b, a)) / 255.0
            row += ramp[min(len(ramp) - 1, int(value * len(ramp)))]
        lines.append("   |" + row + "|")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--only", action="append", default=None, help="draw only these slugs")
    parser.add_argument("--preview", action="store_true", help="print ASCII previews and stats")
    parser.add_argument("--dry-run", action="store_true", help="do not write files")
    args = parser.parse_args(argv)

    canonical, ratio, _ = build_reference_model()
    species_list = [species for species in SPECIES if not args.only or species.slug in args.only]
    if not species_list:
        print("error: --only did not match any species", file=sys.stderr)
        return 2

    failures: list[str] = []
    sprites: dict[str, Image.Image] = {}
    for species in species_list:
        image = contrast_matched(species, canonical, ratio)
        sprites[species.slug] = image
        palette = cluster_palette(species.slug)
        opaque, distinct, relative, mean = stats(image)
        ok = (
            REFERENCE_OPAQUE[0] <= opaque <= REFERENCE_OPAQUE[1]
            and REFERENCE_COLOURS[0] <= distinct <= REFERENCE_COLOURS[1]
            and REFERENCE_RELATIVE_STD[0] <= relative <= REFERENCE_RELATIVE_STD[1]
            and REFERENCE_MEAN_LUMA[0] <= mean <= REFERENCE_MEAN_LUMA[1]
        )
        primary = dominant_tone(image, brightest=True)
        secondary = dominant_tone(image, brightest=False)
        print(
            f"{'ok  ' if ok else 'FAIL'} {species.slug:<20} px={opaque:<4} colours={distinct:<4} "
            f"relStd={relative:.2f} luma={mean:5.1f}  primary=0x{primary[0]:02X}{primary[1]:02X}{primary[2]:02X} "
            f"secondary=0x{secondary[0]:02X}{secondary[1]:02X}{secondary[2]:02X}"
            + (f"  ({species.note})" if species.note else "")
        )
        if not ok:
            failures.append(
                f"{species.slug}: px={opaque}, colours={distinct}, relStd={relative:.2f}, luma={mean:.1f} "
                f"outside reference ranges {REFERENCE_OPAQUE}/{REFERENCE_COLOURS}/"
                f"{REFERENCE_RELATIVE_STD}/{REFERENCE_MEAN_LUMA}"
            )
        if args.preview:
            print(ascii_preview(image, species.slug))
        if not args.dry_run:
            ITEM_DIR.mkdir(parents=True, exist_ok=True)
            image.save(ITEM_DIR / f"{species.slug}_spawn_egg.png")

    if len(species_list) > 1:
        difference, first, second = closest_pair(sprites)
        flag = "ok" if difference >= REFERENCE_DISTINCTNESS_MIN else "FAIL"
        print(f"\n{flag} distinctness: closest pair {first}/{second} differs by {difference:.1f} "
              f"(minimum {REFERENCE_DISTINCTNESS_MIN:.0f})")
        if difference < REFERENCE_DISTINCTNESS_MIN:
            failures.append(f"{first}/{second} are too similar ({difference:.1f})")

    if failures:
        print("\nFAILURES:", file=sys.stderr)
        for failure in failures:
            print(f"  - {failure}", file=sys.stderr)
        return 1
    print(f"\n{len(species_list)} spawn eggs {'analysed' if args.dry_run else 'written'} to "
          f"{ITEM_DIR.relative_to(REPO_ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
