package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.entity.VampreyEntity;
import com.dexer.aquanaut.common.entity.OresuckerEntity;
import com.dexer.aquanaut.common.entity.FlagellonautilusEntity;
import com.dexer.aquanaut.common.entity.SkeletonCarpEntity;
import com.dexer.aquanaut.common.entity.GoldenCarpEntity;
import com.dexer.aquanaut.common.entity.SilverCarpEntity;
import com.dexer.aquanaut.common.entity.GentlefishEntity;
import com.dexer.aquanaut.common.entity.SlimyEntity;
import com.dexer.aquanaut.common.entity.IonfinEntity;
import com.dexer.aquanaut.common.entity.OpticichthusEntity;
import com.dexer.aquanaut.common.entity.GeminiJellyfishEntity;
import com.dexer.aquanaut.common.entity.EcofishEntity;
import com.dexer.aquanaut.common.entity.PaleAbyssHydraEntity;
import com.dexer.aquanaut.common.entity.ThreeHeadedSharkEntity;
import com.dexer.aquanaut.common.entity.AirBubbleEntity;
import com.dexer.aquanaut.common.entity.BiologicalDetectorEntity;
import com.dexer.aquanaut.common.entity.CursorEntity;
import com.dexer.aquanaut.common.entity.CursorGeometry;
import com.dexer.aquanaut.common.entity.DetectorGeometry;
import com.dexer.aquanaut.common.entity.DroneGeometry;
import com.dexer.aquanaut.common.entity.AnglerfishEntity;
import com.dexer.aquanaut.common.entity.BlueJellyfishEntity;
import com.dexer.aquanaut.common.entity.BlueRingedWormfishEntity;
import com.dexer.aquanaut.common.entity.CatfishEntity;
import com.dexer.aquanaut.common.entity.CreeporpedoEntity;
import com.dexer.aquanaut.common.entity.DonutfishEntity;
import com.dexer.aquanaut.common.entity.ElectrofishEntity;
import com.dexer.aquanaut.common.entity.FlatfishEntity;
import com.dexer.aquanaut.common.entity.GloomgazerEntity;
import com.dexer.aquanaut.common.entity.LightningEntity;
import com.dexer.aquanaut.common.entity.LightingWormEntity;
import com.dexer.aquanaut.common.entity.HarpoonEntity;
import com.dexer.aquanaut.common.entity.HelicoprionEntity;
import com.dexer.aquanaut.common.entity.IcerailEntity;
import com.dexer.aquanaut.common.entity.MantaRayEntity;
import com.dexer.aquanaut.common.entity.OctopusEntity;
import com.dexer.aquanaut.common.entity.OxygenBreederEntity;
import com.dexer.aquanaut.common.entity.RadioanemoneEntity;
import com.dexer.aquanaut.common.entity.RedJellyfishEntity;
import com.dexer.aquanaut.common.entity.RingfishEntity;
import com.dexer.aquanaut.common.entity.GiantAbyssWormEntity;
import com.dexer.aquanaut.common.entity.GiantOctopusTentacleEntity;
import com.dexer.aquanaut.common.entity.SardineEntity;
import com.dexer.aquanaut.common.entity.SpringfishEntity;
import com.dexer.aquanaut.common.entity.SubmarineDroneEntity;
import com.dexer.aquanaut.common.entity.SwirlEntity;
import com.dexer.aquanaut.common.entity.SwirlMakerEntity;
import com.dexer.aquanaut.common.entity.TripodEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Aquanaut.MODID)
public class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister
            .create(BuiltInRegistries.ENTITY_TYPE, Aquanaut.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<OctopusEntity>> OCTOPUS = ENTITIES.register(
            "octopus",
            () -> EntityType.Builder
                    .<OctopusEntity>of(OctopusEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.8F, 1.5F)
                    .build("octopus"));

    public static final DeferredHolder<EntityType<?>, EntityType<SardineEntity>> SARDINE = ENTITIES.register(
            "sardine",
            () -> EntityType.Builder
                    .<SardineEntity>of(SardineEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.55F, 0.42F)
                    .build("sardine"));

    public static final DeferredHolder<EntityType<?>, EntityType<AnglerfishEntity>> ANGLERFISH = ENTITIES.register(
            "anglerfish",
            () -> EntityType.Builder
                    .<AnglerfishEntity>of(AnglerfishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.3F, 1.5F)
                    .build("anglerfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<ElectrofishEntity>> ELECTROFISH = ENTITIES
            .register(
                    "electrofish",
                    () -> EntityType.Builder
                            .<ElectrofishEntity>of(ElectrofishEntity::new,
                                    MobCategory.WATER_CREATURE)
                            .sized(1.15F, 1.35F)
                            .build("electrofish"));

    public static final DeferredHolder<EntityType<?>, EntityType<DonutfishEntity>> DONUTFISH = ENTITIES.register(
            "donutfish",
            () -> EntityType.Builder
                    .<DonutfishEntity>of(DonutfishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.96F, 1.96F)
                    .build("donutfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<SpringfishEntity>> SPRINGFISH = ENTITIES.register(
            "springfish",
            () -> EntityType.Builder
                    .<SpringfishEntity>of(SpringfishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.75F, 0.58F)
                    .build("springfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<IcerailEntity>> ICERAIL = ENTITIES.register(
            "icerail",
            () -> EntityType.Builder
                    .<IcerailEntity>of(IcerailEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.0F, 0.78F)
                    .build("icerail"));

    public static final DeferredHolder<EntityType<?>, EntityType<HelicoprionEntity>> HELICOPRION = ENTITIES
            .register(
                    "helicoprion",
                    () -> EntityType.Builder
                            .<HelicoprionEntity>of(HelicoprionEntity::new,
                                    MobCategory.WATER_CREATURE)
                            .sized(1.6F, 1.45F)
                            .build("helicoprion"));

    public static final DeferredHolder<EntityType<?>, EntityType<CatfishEntity>> CATFISH = ENTITIES.register(
            "catfish",
            () -> EntityType.Builder
                    .<CatfishEntity>of(CatfishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.9F, 0.9F)
                    .build("catfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<MantaRayEntity>> MANTA_RAY = ENTITIES.register(
            "manta_ray",
            () -> EntityType.Builder
                    .<MantaRayEntity>of(MantaRayEntity::new, MobCategory.WATER_CREATURE)
                    .sized(2.6F, 0.55F)
                    .build("manta_ray"));

    public static final DeferredHolder<EntityType<?>, EntityType<AirBubbleEntity>> AIR_BUBBLE = ENTITIES.register(
            "air_bubble",
            () -> EntityType.Builder
                    .<AirBubbleEntity>of(AirBubbleEntity::new, MobCategory.MISC)
                    .sized(0.9375F, 0.9375F)
                    .build("air_bubble"));

    /**
     * The hitbox comes from {@link CursorGeometry}, which derives it from the shipped model, so it
     * cannot drift away from the geometry when cursor.geo.json is re-exported.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<CursorEntity>> CURSOR = ENTITIES.register(
            "cursor",
            () -> EntityType.Builder
                    .<CursorEntity>of(CursorEntity::new, MobCategory.MISC)
                    .sized(CursorGeometry.HITBOX_WIDTH, CursorGeometry.HITBOX_HEIGHT)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build("cursor"));

    public static final DeferredHolder<EntityType<?>, EntityType<HarpoonEntity>> HARPOON = ENTITIES.register(
            "harpoon",
            () -> EntityType.Builder
                    .<HarpoonEntity>of(HarpoonEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("harpoon"));

        public static final DeferredHolder<EntityType<?>, EntityType<LightningEntity>> LIGHTNING = ENTITIES.register(
                        "lightning",
                        () -> EntityType.Builder
                                        .<LightningEntity>of(LightningEntity::new, MobCategory.MISC)
                                        .sized(1.0F, 10.0F)
                                        .clientTrackingRange(10)
                                        .updateInterval(1)
                                        .build("lightning"));

    public static final DeferredHolder<EntityType<?>, EntityType<GiantAbyssWormEntity>> GIANT_ABYSS_WORM = ENTITIES
            .register(
                    "giant_abyss_worm",
                    () -> EntityType.Builder
                            .<GiantAbyssWormEntity>of(GiantAbyssWormEntity::new,
                                    MobCategory.WATER_CREATURE)
                            .sized(4.0F, 4.0F)
                            .clientTrackingRange(12)
                            .updateInterval(1)
                            .build("giant_abyss_worm"));

    public static final DeferredHolder<EntityType<?>, EntityType<GiantOctopusTentacleEntity>> GIANT_OCTOPUS_TENTACLE = ENTITIES
            .register(
                    "giant_octopus_tentacle",
                    () -> EntityType.Builder
                            .<GiantOctopusTentacleEntity>of(GiantOctopusTentacleEntity::new,
                                    MobCategory.WATER_CREATURE)
                            .sized(4.0F, 4.0F)
                            .clientTrackingRange(10)
                            .build("giant_octopus_tentacle"));

    public static final DeferredHolder<EntityType<?>, EntityType<LightingWormEntity>> LIGHTING_WORM = ENTITIES
            .register(
                    "lighting_worm",
                    () -> EntityType.Builder
                            .<LightingWormEntity>of(LightingWormEntity::new,
                                    MobCategory.WATER_CREATURE)
                            .sized(1.3F, 0.5F)
                            .build("lighting_worm"));

    public static final DeferredHolder<EntityType<?>, EntityType<CreeporpedoEntity>> CREEPORPEDO = ENTITIES
            .register(
                    "creeporpedo",
                    () -> EntityType.Builder
                            .<CreeporpedoEntity>of(CreeporpedoEntity::new,
                                    MobCategory.WATER_CREATURE)
                            .sized(1.4F, 0.75F)
                            .build("creeporpedo"));

    public static final DeferredHolder<EntityType<?>, EntityType<SwirlMakerEntity>> SWIRL_MAKER = ENTITIES
            .register(
                    "swirl_maker",
                    () -> EntityType.Builder
                            .<SwirlMakerEntity>of(SwirlMakerEntity::new,
                                    MobCategory.WATER_CREATURE)
                            .sized(2.2F, 1.8F)
                            .build("swirl_maker"));

    public static final DeferredHolder<EntityType<?>, EntityType<SwirlEntity>> SWIRL = ENTITIES
            .register(
                    "swirl",
                    () -> EntityType.Builder
                            .<SwirlEntity>of(SwirlEntity::new,
                                    MobCategory.MISC)
                            .sized(0.1F, 0.1F)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .build("swirl"));

    public static final DeferredHolder<EntityType<?>, EntityType<GloomgazerEntity>> GLOOMGAZER = ENTITIES
            .register("gloomgazer",
                    () -> EntityType.Builder.<GloomgazerEntity>of(GloomgazerEntity::new, MobCategory.WATER_CREATURE)
                            .sized(0.75F, 0.55F).build("gloomgazer"));

    public static final DeferredHolder<EntityType<?>, EntityType<RadioanemoneEntity>> RADIOANEMONE = ENTITIES
            .register("radioanemone",
                    () -> EntityType.Builder.<RadioanemoneEntity>of(RadioanemoneEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.3F, 1.3F).build("radioanemone"));

    public static final DeferredHolder<EntityType<?>, EntityType<OxygenBreederEntity>> OXYGEN_BREEDER = ENTITIES
            .register("oxygen_breeder",
                    () -> EntityType.Builder.<OxygenBreederEntity>of(OxygenBreederEntity::new, MobCategory.WATER_CREATURE)
                            .sized(0.5F, 0.45F).build("oxygen_breeder"));

    public static final DeferredHolder<EntityType<?>, EntityType<RedJellyfishEntity>> RED_JELLYFISH = ENTITIES
            .register("red_jellyfish",
                    () -> EntityType.Builder.<RedJellyfishEntity>of(RedJellyfishEntity::new, MobCategory.WATER_CREATURE)
                            .sized(0.85F, 0.7F).build("red_jellyfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<RingfishEntity>> RINGFISH = ENTITIES
            .register("ringfish",
                    () -> EntityType.Builder.<RingfishEntity>of(RingfishEntity::new, MobCategory.WATER_CREATURE)
                            .sized(0.75F, 0.6F).build("ringfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<TripodEntity>> TRIPOD = ENTITIES
            .register("tripod",
                    () -> EntityType.Builder.<TripodEntity>of(TripodEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.1F, 0.5F).build("tripod"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlueRingedWormfishEntity>> BLUE_RINGED_WORMFISH = ENTITIES
            .register("blue_ringed_wormfish",
                    () -> EntityType.Builder.<BlueRingedWormfishEntity>of(BlueRingedWormfishEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.0F, 0.35F).build("blue_ringed_wormfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<BlueJellyfishEntity>> BLUE_JELLYFISH = ENTITIES
            .register("blue_jellyfish",
                    () -> EntityType.Builder.<BlueJellyfishEntity>of(BlueJellyfishEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.5F, 1.0F).build("blue_jellyfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlatfishEntity>> FLATFISH = ENTITIES
            .register("flatfish",
                    () -> EntityType.Builder.<FlatfishEntity>of(FlatfishEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.3F, 0.65F).build("flatfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<VampreyEntity>> VAMPREY = ENTITIES.register(
            "vamprey",
            () -> EntityType.Builder
                    .<VampreyEntity>of(VampreyEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.6F, 0.5F)
                    .build("vamprey"));

    public static final DeferredHolder<EntityType<?>, EntityType<OresuckerEntity>> ORESUCKER = ENTITIES.register(
            "oresucker",
            () -> EntityType.Builder
                    .<OresuckerEntity>of(OresuckerEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.5F, 0.4F)
                    .build("oresucker"));

    public static final DeferredHolder<EntityType<?>, EntityType<FlagellonautilusEntity>> FLAGELLONAUTILUS = ENTITIES.register(
            "flagellonautilus",
            () -> EntityType.Builder
                    .<FlagellonautilusEntity>of(FlagellonautilusEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.9F, 1.2F)
                            .clientTrackingRange(10)
                    .build("flagellonautilus"));

    public static final DeferredHolder<EntityType<?>, EntityType<SkeletonCarpEntity>> SKELETON_CARP = ENTITIES.register(
            "skeleton_carp",
            () -> EntityType.Builder
                    .<SkeletonCarpEntity>of(SkeletonCarpEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.6F, 0.85F)
                    .build("skeleton_carp"));

    public static final DeferredHolder<EntityType<?>, EntityType<GoldenCarpEntity>> GOLDEN_CARP = ENTITIES.register(
            "golden_carp",
            () -> EntityType.Builder
                    .<GoldenCarpEntity>of(GoldenCarpEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.6F, 0.85F)
                    .build("golden_carp"));

    public static final DeferredHolder<EntityType<?>, EntityType<SilverCarpEntity>> SILVER_CARP = ENTITIES.register(
            "silver_carp",
            () -> EntityType.Builder
                    .<SilverCarpEntity>of(SilverCarpEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.6F, 0.85F)
                    .build("silver_carp"));

    public static final DeferredHolder<EntityType<?>, EntityType<GentlefishEntity>> GENTLEFISH = ENTITIES.register(
            "gentlefish",
            () -> EntityType.Builder
                    .<GentlefishEntity>of(GentlefishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.5F, 0.75F)
                    .build("gentlefish"));

    public static final DeferredHolder<EntityType<?>, EntityType<SlimyEntity>> SLIMMY = ENTITIES.register(
            "slimmy",
            () -> EntityType.Builder
                    .<SlimyEntity>of(SlimyEntity::new, MobCategory.WATER_CREATURE)
                    .sized(0.8F, 0.75F)
                    .build("slimmy"));

    public static final DeferredHolder<EntityType<?>, EntityType<IonfinEntity>> IONFIN = ENTITIES.register(
            "ionfin",
            () -> EntityType.Builder
                    .<IonfinEntity>of(IonfinEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.5F, 1.5F)
                            .clientTrackingRange(10)
                    .build("ionfin"));

    public static final DeferredHolder<EntityType<?>, EntityType<OpticichthusEntity>> OPTICICHTHUS = ENTITIES.register(
            "opticichthus",
            () -> EntityType.Builder
                    .<OpticichthusEntity>of(OpticichthusEntity::new, MobCategory.WATER_CREATURE)
                    .sized(1.3F, 1.0F)
                            .clientTrackingRange(10)
                    .build("opticichthus"));

    public static final DeferredHolder<EntityType<?>, EntityType<GeminiJellyfishEntity>> GEMINI_JELLYFISH = ENTITIES.register(
            "gemini_jellyfish",
            () -> EntityType.Builder
                    .<GeminiJellyfishEntity>of(GeminiJellyfishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(4.5F, 9.0F)
                            .clientTrackingRange(16)
                            .updateInterval(2)
                    .build("gemini_jellyfish"));

    public static final DeferredHolder<EntityType<?>, EntityType<EcofishEntity>> ECOFISH = ENTITIES.register(
            "ecofish",
            () -> EntityType.Builder
                    .<EcofishEntity>of(EcofishEntity::new, MobCategory.WATER_CREATURE)
                    .sized(8.0F, 12.0F)
                            .clientTrackingRange(16)
                            .updateInterval(2)
                    .build("ecofish"));

    public static final DeferredHolder<EntityType<?>, EntityType<PaleAbyssHydraEntity>> PALE_ABYSS_HYDRA = ENTITIES.register(
            "pale_abyss_hydra",
            () -> EntityType.Builder
                    .<PaleAbyssHydraEntity>of(PaleAbyssHydraEntity::new, MobCategory.WATER_CREATURE)
                    .sized(6.0F, 6.0F)
                            .clientTrackingRange(16)
                            .updateInterval(2)
                    .build("pale_abyss_hydra"));

    public static final DeferredHolder<EntityType<?>, EntityType<ThreeHeadedSharkEntity>> THREE_HEADED_SHARK = ENTITIES.register(
            "three_headed_shark",
            () -> EntityType.Builder
                    .<ThreeHeadedSharkEntity>of(ThreeHeadedSharkEntity::new, MobCategory.WATER_CREATURE)
                    .sized(2.4F, 1.5F)
                            .clientTrackingRange(12)
                            .updateInterval(2)
                    .build("three_headed_shark"));

    /**
     * The submarine drone. Registered as a plain machine in {@link MobCategory#MISC} on purpose:
     * that is what keeps it out of the fishing pipelines — the scoop net reaches for
     * {@code WaterAnimal}s and the aquarium catalogue is built from the aquatic mob categories — so
     * a drone can be neither caught nor filed away, without either system having to know the drone
     * exists.
     *
     * <p>
     * It has no spawn egg and no attributes: a machine is deployed from its own item rather than
     * hatched, and it has no health to give it.
     *
     * <p>
     * Its eye height is the middle of its nose sensor, derived from the shipped model by
     * {@link DroneGeometry}, because the pilot's camera rides there and the feed has to look out of
     * the pod rather than over the top of the hull.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<SubmarineDroneEntity>> SUBMARINE_DRONE = ENTITIES.register(
            "submarine_drone",
            () -> EntityType.Builder
                    .<SubmarineDroneEntity>of(SubmarineDroneEntity::new, MobCategory.MISC)
                    .sized(0.6F, 0.6F)
                    .eyeHeight(DroneGeometry.EYE_HEIGHT)
                            .clientTrackingRange(10)
                    .build("submarine_drone"));

    /**
     * The biological detector. A second machine in {@link MobCategory#MISC} for the same reason the
     * drone is one: the fishing and aquarium pipelines work in terms of aquatic mobs, and a buoy
     * that can be neither scooped up nor filed away needs no exception in either of them.
     *
     * <p>
     * It has no spawn egg and no attributes: a detector is deployed from its own item rather than
     * hatched, and it has no health to take — a hit is read as "pick that up" by its entity class.
     *
     * <p>
     * Half a block across and half a block tall, which is exactly the ball in its folded state; the
     * opened shell adds a hair over a tenth of a block above the hitbox, which is not worth a
     * taller box on something a diver swims past.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<BiologicalDetectorEntity>> BIOLOGICAL_DETECTOR = ENTITIES.register(
            "biological_detector",
            () -> EntityType.Builder
                    .<BiologicalDetectorEntity>of(BiologicalDetectorEntity::new, MobCategory.MISC)
                    .sized(DetectorGeometry.FOLDED_HEIGHT, DetectorGeometry.FOLDED_HEIGHT)
                            .clientTrackingRange(10)
                            .updateInterval(2)
                    .build("biological_detector"));

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(EntityRegistry.OCTOPUS.get(), OctopusEntity.createAttributes());
        event.put(EntityRegistry.SARDINE.get(), SardineEntity.createAttributes());
        event.put(EntityRegistry.ANGLERFISH.get(), AnglerfishEntity.createAttributes());
        event.put(EntityRegistry.ELECTROFISH.get(), ElectrofishEntity.createAttributes());
        event.put(EntityRegistry.DONUTFISH.get(), DonutfishEntity.createAttributes());
        event.put(EntityRegistry.SPRINGFISH.get(), SpringfishEntity.createAttributes());
        event.put(EntityRegistry.ICERAIL.get(), IcerailEntity.createAttributes());
        event.put(EntityRegistry.HELICOPRION.get(), HelicoprionEntity.createAttributes());
        event.put(EntityRegistry.CATFISH.get(), CatfishEntity.createAttributes());
        event.put(EntityRegistry.MANTA_RAY.get(), MantaRayEntity.createAttributes());
        event.put(EntityRegistry.GIANT_ABYSS_WORM.get(), GiantAbyssWormEntity.createAttributes());
        event.put(EntityRegistry.GIANT_OCTOPUS_TENTACLE.get(), GiantOctopusTentacleEntity.createAttributes());
        event.put(EntityRegistry.LIGHTING_WORM.get(), LightingWormEntity.createAttributes());
        event.put(EntityRegistry.CREEPORPEDO.get(), CreeporpedoEntity.createAttributes());
        event.put(EntityRegistry.SWIRL_MAKER.get(), SwirlMakerEntity.createAttributes());
        event.put(EntityRegistry.GLOOMGAZER.get(), GloomgazerEntity.createAttributes());
        event.put(EntityRegistry.RADIOANEMONE.get(), RadioanemoneEntity.createAttributes());
        event.put(EntityRegistry.OXYGEN_BREEDER.get(), OxygenBreederEntity.createAttributes());
        event.put(EntityRegistry.RED_JELLYFISH.get(), RedJellyfishEntity.createAttributes());
        event.put(EntityRegistry.RINGFISH.get(), RingfishEntity.createAttributes());
        event.put(EntityRegistry.TRIPOD.get(), TripodEntity.createAttributes());
        event.put(EntityRegistry.BLUE_RINGED_WORMFISH.get(), BlueRingedWormfishEntity.createAttributes());
        event.put(EntityRegistry.BLUE_JELLYFISH.get(), BlueJellyfishEntity.createAttributes());
        event.put(EntityRegistry.FLATFISH.get(), FlatfishEntity.createAttributes());
        event.put(EntityRegistry.VAMPREY.get(), VampreyEntity.createAttributes());
        event.put(EntityRegistry.ORESUCKER.get(), OresuckerEntity.createAttributes());
        event.put(EntityRegistry.FLAGELLONAUTILUS.get(), FlagellonautilusEntity.createAttributes());
        event.put(EntityRegistry.SKELETON_CARP.get(), SkeletonCarpEntity.createAttributes());
        event.put(EntityRegistry.GOLDEN_CARP.get(), GoldenCarpEntity.createAttributes());
        event.put(EntityRegistry.SILVER_CARP.get(), SilverCarpEntity.createAttributes());
        event.put(EntityRegistry.GENTLEFISH.get(), GentlefishEntity.createAttributes());
        event.put(EntityRegistry.SLIMMY.get(), SlimyEntity.createAttributes());
        event.put(EntityRegistry.IONFIN.get(), IonfinEntity.createAttributes());
        event.put(EntityRegistry.OPTICICHTHUS.get(), OpticichthusEntity.createAttributes());
        event.put(EntityRegistry.GEMINI_JELLYFISH.get(), GeminiJellyfishEntity.createAttributes());
        event.put(EntityRegistry.ECOFISH.get(), EcofishEntity.createAttributes());
        event.put(EntityRegistry.PALE_ABYSS_HYDRA.get(), PaleAbyssHydraEntity.createAttributes());
        event.put(EntityRegistry.THREE_HEADED_SHARK.get(), ThreeHeadedSharkEntity.createAttributes());
    }

    @SubscribeEvent
    public static void modifyEntityAttributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, NeoForgeMod.SWIM_SPEED);
    }
}
