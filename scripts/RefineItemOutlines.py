#!/usr/bin/env python3
from __future__ import annotations

import math
from pathlib import Path

from PIL import Image

from item_outline_profiles import OutlineProfile, TEXTURE_PROFILES


ROOT = Path("src/main/resources/assets/aquanaut/textures/item")
TRANSPARENT_ALPHA = 8


def _blend(a: float, b: float, t: float) -> float:
    return a * (1.0 - t) + b * t


def _blend_rgb(a: tuple[float, float, float], b: tuple[float, float, float], t: float) -> tuple[float, float, float]:
    return (
        _blend(a[0], b[0], t),
        _blend(a[1], b[1], t),
        _blend(a[2], b[2], t),
    )


def _darken(rgb: tuple[float, float, float], amount: int) -> tuple[float, float, float]:
    factor = max(0.0, 1.0 - amount / 255.0)
    return (rgb[0] * factor, rgb[1] * factor, rgb[2] * factor)


def _clamp_delta(inner: tuple[float, float, float], rgb: tuple[float, float, float],
                 max_delta: int) -> tuple[float, float, float]:
    clamped: list[float] = []
    for source, target in zip(inner, rgb):
        delta = target - source
        if delta > max_delta:
            delta = float(max_delta)
        elif delta < -max_delta:
            delta = float(-max_delta)
        clamped.append(source + delta)
    return (clamped[0], clamped[1], clamped[2])


def _is_opaque(pixel: tuple[int, int, int, int]) -> bool:
    return pixel[3] > TRANSPARENT_ALPHA


def _find_edges(pixels: list[list[tuple[int, int, int, int]]]) -> set[tuple[int, int]]:
    height = len(pixels)
    width = len(pixels[0]) if height else 0
    edges: set[tuple[int, int]] = set()
    for y in range(height):
        for x in range(width):
            if not _is_opaque(pixels[y][x]):
                continue
            for ny in range(y - 1, y + 2):
                for nx in range(x - 1, x + 2):
                    if nx == x and ny == y:
                        continue
                    if nx < 0 or ny < 0 or nx >= width or ny >= height or not _is_opaque(pixels[ny][nx]):
                        edges.add((x, y))
                        break
                else:
                    continue
                break
    return edges


def _sample_inner(pixels: list[list[tuple[int, int, int, int]]], edges: set[tuple[int, int]],
                  x: int, y: int, radius: int) -> tuple[float, float, float, float] | None:
    height = len(pixels)
    width = len(pixels[0]) if height else 0
    total_weight = 0.0
    total_r = total_g = total_b = total_a = 0.0
    for ny in range(max(0, y - radius), min(height, y + radius + 1)):
        for nx in range(max(0, x - radius), min(width, x + radius + 1)):
            if (nx, ny) == (x, y) or (nx, ny) in edges:
                continue
            pixel = pixels[ny][nx]
            if not _is_opaque(pixel):
                continue
            dx = nx - x
            dy = ny - y
            distance_sq = dx * dx + dy * dy
            if distance_sq == 0:
                continue
            weight = pixel[3] / 255.0 / distance_sq
            total_weight += weight
            total_r += pixel[0] * weight
            total_g += pixel[1] * weight
            total_b += pixel[2] * weight
            total_a += pixel[3] * weight
    if total_weight <= 0.0:
        return None
    return (
        total_r / total_weight,
        total_g / total_weight,
        total_b / total_weight,
        total_a / total_weight,
    )


def _refine_edge(pixel: tuple[int, int, int, int], inner: tuple[float, float, float, float],
                 profile: OutlineProfile) -> tuple[int, int, int, int]:
    current_rgb = (float(pixel[0]), float(pixel[1]), float(pixel[2]))
    inner_rgb = (inner[0], inner[1], inner[2])

    tinted = _blend_rgb(inner_rgb, current_rgb, profile.retain_original)
    shaded = _darken(tinted, profile.shadow_depth)
    refined = _blend_rgb(current_rgb, shaded, profile.edge_mix)
    refined = _clamp_delta(inner_rgb, refined, profile.max_channel_delta)

    alpha_floor = pixel[3] * profile.min_alpha_ratio
    alpha_value = _blend(float(pixel[3]), inner[3], profile.alpha_mix)
    alpha_value = max(alpha_floor, alpha_value)

    return (
        max(0, min(255, round(refined[0]))),
        max(0, min(255, round(refined[1]))),
        max(0, min(255, round(refined[2]))),
        max(0, min(255, round(alpha_value))),
    )


def process_texture(path: Path, profile: OutlineProfile) -> bool:
    image = Image.open(path).convert("RGBA")
    width, height = image.size
    pixels = [[image.getpixel((x, y)) for x in range(width)] for y in range(height)]
    original = [row[:] for row in pixels]
    edges = _find_edges(original)

    changed = False
    for x, y in sorted(edges):
        inner = _sample_inner(original, edges, x, y, profile.sample_radius)
        if inner is None:
            continue
        refined = _refine_edge(original[y][x], inner, profile)
        if refined != original[y][x]:
            pixels[y][x] = refined
            changed = True

    if changed:
        output = Image.new("RGBA", (width, height))
        for y in range(height):
            for x in range(width):
                output.putpixel((x, y), pixels[y][x])
        output.save(path)
    return changed


def main() -> None:
    actual_files = sorted(path.name for path in ROOT.glob("*.png"))
    missing = [name for name in actual_files if name not in TEXTURE_PROFILES]
    extra = [name for name in TEXTURE_PROFILES if not (ROOT / name).exists()]
    if missing:
        raise SystemExit(f"Missing explicit outline profiles for: {', '.join(missing)}")
    if extra:
        raise SystemExit(f"Profiles point to missing textures: {', '.join(extra)}")

    changed_count = 0
    for name in actual_files:
        profile = TEXTURE_PROFILES[name]
        if not profile.enabled:
            print(f"Skipped {name}")
            continue
        if process_texture(ROOT / name, profile):
            changed_count += 1
            print(f"Refined {name}")
        else:
            print(f"Unchanged {name}")

    print(f"Processed {len(actual_files)} textures, changed {changed_count}.")


if __name__ == "__main__":
    main()
