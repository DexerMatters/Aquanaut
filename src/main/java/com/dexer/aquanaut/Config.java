package com.dexer.aquanaut;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
        private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        public static final ModConfigSpec.IntValue DIVING_TANK_CAPACITY_BONUS = BUILDER
                        .comment("Extra max-air capacity granted by an equipped diving tank.")
                        .defineInRange("divingTankCapacityBonus", 1200, 0, Integer.MAX_VALUE);

        public static final ModConfigSpec.IntValue DIVING_MASK_REGEN_BONUS = BUILDER
                        .comment("Additional air ticks restored per regen tick while a diving mask is equipped.")
                        .defineInRange("divingMaskRegenBonus", 6, 0, Integer.MAX_VALUE);

        public static final ModConfigSpec.DoubleValue DIVING_FLIPPER_SPEED_MULTIPLIER = BUILDER
                        .comment("Underwater movement multiplier while diving flippers are equipped.")
                        .defineInRange("divingFlipperSpeedMultiplier", 1.25D, 1.0D, 5.0D);

        public static final ModConfigSpec.BooleanValue ORESUCKER_GRAZING = BUILDER
                        .comment("Allow oresuckers to grind exposed ore blocks into stone and store the metal.",
                                        "Disable to keep the world untouched; the fish still gathers what it already holds.")
                        .define("oresuckerGrazing", true);

        static final ModConfigSpec SPEC = BUILDER.build();

        private Config() {
        }
}
