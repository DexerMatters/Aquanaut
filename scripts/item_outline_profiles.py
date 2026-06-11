#!/usr/bin/env python3
from __future__ import annotations

from dataclasses import dataclass, replace


@dataclass(frozen=True)
class OutlineProfile:
    sample_radius: int = 2
    edge_mix: float = 0.62
    retain_original: float = 0.34
    shadow_depth: int = 14
    max_channel_delta: int = 58
    alpha_mix: float = 0.18
    min_alpha_ratio: float = 0.80
    enabled: bool = True


def _t(base: OutlineProfile, **changes) -> OutlineProfile:
    return replace(base, **changes)


BIO_SOFT = OutlineProfile(sample_radius=2, edge_mix=0.72, retain_original=0.30, shadow_depth=9,
                          max_channel_delta=42, alpha_mix=0.34, min_alpha_ratio=0.72)
BIO_DENSE = OutlineProfile(sample_radius=2, edge_mix=0.68, retain_original=0.36, shadow_depth=12,
                           max_channel_delta=48, alpha_mix=0.24, min_alpha_ratio=0.80)
BONE = OutlineProfile(sample_radius=2, edge_mix=0.60, retain_original=0.40, shadow_depth=13,
                      max_channel_delta=50, alpha_mix=0.10, min_alpha_ratio=0.86)
METAL = OutlineProfile(sample_radius=2, edge_mix=0.58, retain_original=0.42, shadow_depth=15,
                       max_channel_delta=54, alpha_mix=0.08, min_alpha_ratio=0.90)
TOOL_ORGANIC = OutlineProfile(sample_radius=2, edge_mix=0.61, retain_original=0.38, shadow_depth=13,
                              max_channel_delta=50, alpha_mix=0.10, min_alpha_ratio=0.86)
SOFT_FABRIC = OutlineProfile(sample_radius=2, edge_mix=0.66, retain_original=0.34, shadow_depth=11,
                             max_channel_delta=46, alpha_mix=0.18, min_alpha_ratio=0.82)
GLOW = OutlineProfile(sample_radius=3, edge_mix=0.74, retain_original=0.28, shadow_depth=7,
                      max_channel_delta=38, alpha_mix=0.30, min_alpha_ratio=0.70)
GLASSY = OutlineProfile(sample_radius=2, edge_mix=0.78, retain_original=0.22, shadow_depth=5,
                        max_channel_delta=30, alpha_mix=0.42, min_alpha_ratio=0.64)
SPAWN_EGG = OutlineProfile(sample_radius=2, edge_mix=0.70, retain_original=0.30, shadow_depth=10,
                           max_channel_delta=40, alpha_mix=0.22, min_alpha_ratio=0.78)
FOOD = OutlineProfile(sample_radius=2, edge_mix=0.63, retain_original=0.36, shadow_depth=12,
                      max_channel_delta=48, alpha_mix=0.14, min_alpha_ratio=0.84)
UTILITY = OutlineProfile(sample_radius=2, edge_mix=0.52, retain_original=0.46, shadow_depth=10,
                         max_channel_delta=42, alpha_mix=0.08, min_alpha_ratio=0.90)
BOOK = OutlineProfile(sample_radius=1, edge_mix=0.45, retain_original=0.52, shadow_depth=7,
                      max_channel_delta=28, alpha_mix=0.04, min_alpha_ratio=0.94)


TEXTURE_PROFILES: dict[str, OutlineProfile] = {
    "aerogel.png": _t(GLASSY, shadow_depth=4, edge_mix=0.80),
    "air_sac.png": _t(GLASSY, shadow_depth=6, edge_mix=0.76),
    "air_sandwich.png": _t(FOOD, shadow_depth=10, edge_mix=0.61),
    "air_soup.png": _t(FOOD, shadow_depth=9, edge_mix=0.60),
    "anglerfish_mask.png": _t(TOOL_ORGANIC, shadow_depth=12, retain_original=0.40),
    "anglerfish_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=11, edge_mix=0.69),
    "aquanaut_notebook.png": BOOK,
    "big_scoop_net.png": _t(SOFT_FABRIC, shadow_depth=12, retain_original=0.40),
    "blue_lamp_fruit.png": _t(GLOW, shadow_depth=8, edge_mix=0.72),
    "bubble_gun.png": _t(UTILITY, shadow_depth=9, retain_original=0.48),
    "catfish_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.68),
    "cooked_fishnut.png": _t(FOOD, shadow_depth=11),
    "cooked_octopus_shreds.png": _t(FOOD, shadow_depth=12),
    "cooked_sadine.png": _t(FOOD, shadow_depth=12),
    "cooked_shark_fins.png": _t(FOOD, shadow_depth=12, retain_original=0.38),
    "copper_harpoon.png": _t(METAL, shadow_depth=14, retain_original=0.44),
    "coral_axe.png": _t(TOOL_ORGANIC, shadow_depth=13, retain_original=0.40),
    "coral_flippers.png": _t(SOFT_FABRIC, shadow_depth=11, edge_mix=0.64),
    "coral_fragments.png": _t(BIO_DENSE, shadow_depth=11),
    "coral_harpoon.png": _t(TOOL_ORGANIC, shadow_depth=13, retain_original=0.40),
    "coral_hoe.png": _t(TOOL_ORGANIC, shadow_depth=12, retain_original=0.40),
    "coral_mask.png": _t(TOOL_ORGANIC, shadow_depth=11, retain_original=0.42),
    "coral_pickaxe.png": _t(TOOL_ORGANIC, shadow_depth=13, retain_original=0.40),
    "coral_shovel.png": _t(TOOL_ORGANIC, shadow_depth=12, retain_original=0.40),
    "coral_stick.png": _t(BIO_DENSE, shadow_depth=10, retain_original=0.38),
    "creeporpedo_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=12, edge_mix=0.67),
    "diamond_harpoon.png": _t(METAL, shadow_depth=15, max_channel_delta=50),
    "donutfish_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=9, edge_mix=0.72),
    "elastic_biomass.png": _t(BIO_SOFT, shadow_depth=8, edge_mix=0.75),
    "electrofish_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.71),
    "ender_mask.png": _t(GLOW, shadow_depth=10, retain_original=0.34),
    "essence_of_the_abyss.png": _t(GLOW, shadow_depth=7, edge_mix=0.76),
    "essence_of_the_euphoria.png": _t(GLOW, shadow_depth=6, edge_mix=0.78),
    "essence_of_the_fear.png": _t(GLOW, shadow_depth=8, edge_mix=0.74),
    "eye_of_the_abyss.png": _t(GLOW, shadow_depth=9, retain_original=0.32),
    "fang.png": _t(BONE, shadow_depth=14, retain_original=0.42),
    "fishing_net.png": _t(SOFT_FABRIC, shadow_depth=9, edge_mix=0.68, retain_original=0.30),
    "fishnut.png": _t(FOOD, shadow_depth=11),
    "gas_flow_meter.png": _t(UTILITY, shadow_depth=8, edge_mix=0.50, retain_original=0.50),
    "gas_flow_meter_model.png": _t(UTILITY, shadow_depth=8, edge_mix=0.50, retain_original=0.52),
    "giant_abyss_worm_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=12, edge_mix=0.66),
    "giant_octopus_tentacle_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=11, edge_mix=0.68),
    "gloomgazer_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=11, edge_mix=0.68),
    "gold_harpoon.png": _t(METAL, shadow_depth=13, max_channel_delta=46),
    "golden_jelly.png": _t(BIO_SOFT, shadow_depth=8, edge_mix=0.76),
    "hard_rib.png": _t(BONE, shadow_depth=13, retain_original=0.44),
    "hard_shell.png": _t(BONE, shadow_depth=13, retain_original=0.42),
    "hard_shell_axe.png": _t(BONE, shadow_depth=14, retain_original=0.42),
    "hard_shell_flippers.png": _t(SOFT_FABRIC, shadow_depth=12, retain_original=0.38),
    "hard_shell_harpoon.png": _t(BONE, shadow_depth=14, retain_original=0.42),
    "hard_shell_hoe.png": _t(BONE, shadow_depth=13, retain_original=0.42),
    "hard_shell_mask.png": _t(BONE, shadow_depth=12, retain_original=0.44),
    "hard_shell_oxygen_tank.png": _t(BONE, shadow_depth=13, retain_original=0.42),
    "hard_shell_pickaxe.png": _t(BONE, shadow_depth=14, retain_original=0.42),
    "hard_shell_shovel.png": _t(BONE, shadow_depth=13, retain_original=0.42),
    "helicoprion_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=11, edge_mix=0.67),
    "ice_core.png": _t(GLASSY, shadow_depth=6, edge_mix=0.77),
    "ice_fin.png": _t(GLASSY, shadow_depth=7, edge_mix=0.74),
    "icerail_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.70),
    "iron_harpoon.png": _t(METAL, shadow_depth=15),
    "iron_mask.png": _t(METAL, shadow_depth=14, retain_original=0.44),
    "iron_oxygen_tank.png": _t(METAL, shadow_depth=15, retain_original=0.44),
    "kelp.png": _t(BIO_DENSE, shadow_depth=10, edge_mix=0.70),
    "large_scoop_net.png": _t(SOFT_FABRIC, shadow_depth=12, retain_original=0.40),
    "light_cyan_jelly.png": _t(BIO_SOFT, shadow_depth=7, edge_mix=0.78),
    "lighting_worm_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=9, edge_mix=0.72),
    "lightning_pearl.png": _t(GLOW, shadow_depth=7, edge_mix=0.77),
    "luminous_cube.png": _t(GLASSY, shadow_depth=5, edge_mix=0.79),
    "magic_barnacles.png": _t(BIO_DENSE, shadow_depth=10, edge_mix=0.69),
    "manta_ray_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.69),
    "marine_alloy.png": _t(METAL, shadow_depth=14, retain_original=0.46),
    "marine_alloy_axe.png": _t(METAL, shadow_depth=15, retain_original=0.44),
    "marine_alloy_flippers.png": _t(METAL, shadow_depth=13, retain_original=0.42),
    "marine_alloy_harpoon.png": _t(METAL, shadow_depth=15, retain_original=0.44),
    "marine_alloy_hoe.png": _t(METAL, shadow_depth=14, retain_original=0.44),
    "marine_alloy_mask.png": _t(METAL, shadow_depth=13, retain_original=0.46),
    "marine_alloy_oxygen_tank.png": _t(METAL, shadow_depth=14, retain_original=0.46),
    "marine_alloy_pickaxe.png": _t(METAL, shadow_depth=15, retain_original=0.44),
    "marine_alloy_shovel.png": _t(METAL, shadow_depth=14, retain_original=0.44),
    "medium_scoop_net.png": _t(SOFT_FABRIC, shadow_depth=11, retain_original=0.40),
    "netherite_harpoon.png": _t(METAL, shadow_depth=17, retain_original=0.48, max_channel_delta=58),
    "octopus_shreds.png": _t(BIO_DENSE, shadow_depth=11, edge_mix=0.67),
    "octopus_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.69),
    "organic_matter.png": _t(BIO_DENSE, shadow_depth=11, edge_mix=0.68),
    "oxygen_breeder_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.70),
    "plexiglass.png": _t(GLASSY, shadow_depth=3, edge_mix=0.82, alpha_mix=0.48),
    "preview.png": _t(UTILITY, enabled=False),
    "radioanemone_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=9, edge_mix=0.73),
    "red_jelly.png": _t(BIO_SOFT, shadow_depth=9, edge_mix=0.74),
    "red_jellyfish_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.71),
    "ring_rib.png": _t(BONE, shadow_depth=13, retain_original=0.44),
    "ringfish_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.70),
    "rotten_tissue.png": _t(BIO_DENSE, shadow_depth=13, edge_mix=0.66, retain_original=0.40),
    "sadine.png": _t(FOOD, shadow_depth=11),
    "sardine_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=9, edge_mix=0.71),
    "scoop_net.png": _t(SOFT_FABRIC, shadow_depth=10, retain_original=0.40),
    "shark_fins.png": _t(BIO_DENSE, shadow_depth=12, retain_original=0.40),
    "shark_flippers.png": _t(SOFT_FABRIC, shadow_depth=12, retain_original=0.40),
    "shark_skin.png": _t(BIO_DENSE, shadow_depth=10, retain_original=0.38),
    "shell.png": _t(BONE, shadow_depth=12, retain_original=0.42),
    "shell_axe.png": _t(BONE, shadow_depth=13, retain_original=0.42),
    "shell_flippers.png": _t(SOFT_FABRIC, shadow_depth=11, retain_original=0.40),
    "shell_harpoon.png": _t(BONE, shadow_depth=13, retain_original=0.42),
    "shell_hoe.png": _t(BONE, shadow_depth=12, retain_original=0.42),
    "shell_mask.png": _t(BONE, shadow_depth=11, retain_original=0.44),
    "shell_oxygen_tank.png": _t(BONE, shadow_depth=12, retain_original=0.42),
    "shell_pickaxe.png": _t(BONE, shadow_depth=13, retain_original=0.42),
    "shell_shovel.png": _t(BONE, shadow_depth=12, retain_original=0.42),
    "slime_mask.png": _t(GLASSY, shadow_depth=7, edge_mix=0.74),
    "spring.png": _t(BONE, shadow_depth=12, retain_original=0.44),
    "springfish_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.70),
    "stone_harpoon.png": _t(BONE, shadow_depth=14, retain_original=0.42),
    "strange_fragments.png": _t(GLOW, shadow_depth=8, edge_mix=0.72),
    "suspicious_fang.png": _t(BONE, shadow_depth=15, retain_original=0.44),
    "swirl_maker_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=11, edge_mix=0.67),
    "transparent_tissue.png": _t(BIO_SOFT, shadow_depth=7, edge_mix=0.78, alpha_mix=0.40),
    "tripod_spawn_egg.png": _t(SPAWN_EGG, shadow_depth=10, edge_mix=0.69),
    "viscous_tissue.png": _t(BIO_SOFT, shadow_depth=9, edge_mix=0.74),
    "white_jelly.png": _t(BIO_SOFT, shadow_depth=6, edge_mix=0.79),
    "wood_flippers.png": _t(SOFT_FABRIC, shadow_depth=11, retain_original=0.42),
    "wood_harpoon.png": _t(TOOL_ORGANIC, shadow_depth=14, retain_original=0.44),
    "wood_oxygen_tank.png": _t(TOOL_ORGANIC, shadow_depth=13, retain_original=0.42),
    "yellow_lamp_fruit.png": _t(GLOW, shadow_depth=8, edge_mix=0.73),
}
