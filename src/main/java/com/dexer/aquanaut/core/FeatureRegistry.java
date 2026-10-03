package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.worldgen.CalderaLakeFeature;
import com.dexer.aquanaut.common.worldgen.AshDriftFeature;
import com.dexer.aquanaut.common.worldgen.BrimstoneGardenFeature;
import com.dexer.aquanaut.common.worldgen.BrineMirrorFeature;
import com.dexer.aquanaut.common.worldgen.BrineTerraceFeature;
import com.dexer.aquanaut.common.worldgen.CalciteQuillFeature;
import com.dexer.aquanaut.common.worldgen.CoralForestPillarFeature;
import com.dexer.aquanaut.common.worldgen.CrystalGrottoFeature;
import com.dexer.aquanaut.common.worldgen.DruseVeinFeature;
import com.dexer.aquanaut.common.worldgen.FumaroleFieldFeature;
import com.dexer.aquanaut.common.worldgen.GypsumGardenFeature;
import com.dexer.aquanaut.common.worldgen.HaliteOrganFeature;
import com.dexer.aquanaut.common.worldgen.HopperGardenFeature;
import com.dexer.aquanaut.common.worldgen.HotSpringFeature;
import com.dexer.aquanaut.common.worldgen.JellyJungleBulgeFeature;
import com.dexer.aquanaut.common.worldgen.JellyJungleCrackFeature;
import com.dexer.aquanaut.common.worldgen.JellyJungleStemForestFeature;
import com.dexer.aquanaut.common.worldgen.JellyJungleVegetationFeature;
import com.dexer.aquanaut.common.worldgen.SaltFringeFeature;
import com.dexer.aquanaut.common.worldgen.SaltArchFeature;
import com.dexer.aquanaut.common.worldgen.SaltCascadeFeature;
import com.dexer.aquanaut.common.worldgen.SaltDiapirFeature;
import com.dexer.aquanaut.common.worldgen.SmokerClusterFeature;
import com.dexer.aquanaut.common.worldgen.SulfurVeinFeature;
import com.dexer.aquanaut.common.worldgen.VentFloraFeature;
import com.dexer.aquanaut.common.worldgen.MudZoneSedimentFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FeatureRegistry {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE,
            Aquanaut.MODID);
    public static final DeferredHolder<Feature<?>, CoralForestPillarFeature> CORAL_FOREST_PILLAR = FEATURES.register(
            "coral_forest_pillar",
            CoralForestPillarFeature::new);
    public static final DeferredHolder<Feature<?>, JellyJungleBulgeFeature> JELLY_JUNGLE_BULGE = FEATURES.register(
            "jelly_jungle_bulge",
            JellyJungleBulgeFeature::new);
    public static final DeferredHolder<Feature<?>, JellyJungleCrackFeature> JELLY_JUNGLE_CRACK = FEATURES.register(
            "jelly_jungle_crack",
            JellyJungleCrackFeature::new);
    public static final DeferredHolder<Feature<?>, JellyJungleVegetationFeature> JELLY_JUNGLE_VEGETATION =
            FEATURES.register("jelly_jungle_vegetation",
                    JellyJungleVegetationFeature::new);
    public static final DeferredHolder<Feature<?>, JellyJungleStemForestFeature> JELLY_JUNGLE_STEM_FOREST =
            FEATURES.register("jelly_jungle_stem_forest",
                    JellyJungleStemForestFeature::new);
    public static final DeferredHolder<Feature<?>, BrineTerraceFeature> BRINE_TERRACES =
            FEATURES.register("brine_terraces", BrineTerraceFeature::new);
    public static final DeferredHolder<Feature<?>, BrineMirrorFeature> BRINE_MIRRORS =
            FEATURES.register("brine_mirrors", BrineMirrorFeature::new);
    public static final DeferredHolder<Feature<?>, HaliteOrganFeature> HALITE_ORGAN =
            FEATURES.register("halite_organ", HaliteOrganFeature::new);
    public static final DeferredHolder<Feature<?>, CalciteQuillFeature> CALCITE_QUILL_FIELD =
            FEATURES.register("calcite_quill_field", CalciteQuillFeature::new);
    public static final DeferredHolder<Feature<?>, SaltFringeFeature> SALT_FRINGE_CURTAIN =
            FEATURES.register("salt_fringe_curtain", SaltFringeFeature::new);
    public static final DeferredHolder<Feature<?>, HotSpringFeature> HOT_SPRING =
            FEATURES.register("hot_spring", HotSpringFeature::new);
    public static final DeferredHolder<Feature<?>, SmokerClusterFeature> SMOKER_CLUSTER =
            FEATURES.register("smoker_cluster", SmokerClusterFeature::new);
    public static final DeferredHolder<Feature<?>, FumaroleFieldFeature> FUMAROLE_FIELD =
            FEATURES.register("fumarole_field", FumaroleFieldFeature::new);
    public static final DeferredHolder<Feature<?>, SulfurVeinFeature> SULFUR_VEINS =
            FEATURES.register("sulfur_veins", SulfurVeinFeature::new);
    public static final DeferredHolder<Feature<?>, BrimstoneGardenFeature> BRIMSTONE_GARDEN =
            FEATURES.register("brimstone_garden", BrimstoneGardenFeature::new);
    public static final DeferredHolder<Feature<?>, AshDriftFeature> ASH_DRIFTS =
            FEATURES.register("ash_drifts", AshDriftFeature::new);
    public static final DeferredHolder<Feature<?>, CalderaLakeFeature> CALDERA_LAKE =
            FEATURES.register("caldera_lake", CalderaLakeFeature::new);
    public static final DeferredHolder<Feature<?>, VentFloraFeature> VENT_FLORA =
            FEATURES.register("vent_flora", VentFloraFeature::new);
    public static final DeferredHolder<Feature<?>, SaltDiapirFeature> SALT_DIAPIR =
            FEATURES.register("salt_diapir", SaltDiapirFeature::new);
    public static final DeferredHolder<Feature<?>, CrystalGrottoFeature> CRYSTAL_GROTTO =
            FEATURES.register("crystal_grotto", CrystalGrottoFeature::new);
    public static final DeferredHolder<Feature<?>, SaltArchFeature> SALT_ARCH =
            FEATURES.register("salt_arch", SaltArchFeature::new);
    public static final DeferredHolder<Feature<?>, DruseVeinFeature> DRUSE_VEIN =
            FEATURES.register("druse_vein", DruseVeinFeature::new);
    public static final DeferredHolder<Feature<?>, HopperGardenFeature> HOPPER_GARDEN =
            FEATURES.register("hopper_garden", HopperGardenFeature::new);
    public static final DeferredHolder<Feature<?>, GypsumGardenFeature> GYPSUM_GARDEN =
            FEATURES.register("gypsum_garden", GypsumGardenFeature::new);
    public static final DeferredHolder<Feature<?>, SaltCascadeFeature> SALT_CASCADE =
            FEATURES.register("salt_cascade", SaltCascadeFeature::new);
    public static final DeferredHolder<Feature<?>, MudZoneSedimentFeature> MUD_ZONE_SEDIMENT = FEATURES.register(
            "mud_zone_sediment", MudZoneSedimentFeature::new);

    private FeatureRegistry() {
    }

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }
}
