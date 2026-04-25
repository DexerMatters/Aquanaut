package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.entity.AirBubbleEntity;
import com.dexer.aquanaut.common.entity.AnglerfishEntity;
import com.dexer.aquanaut.common.entity.DonutfishEntity;
import com.dexer.aquanaut.common.entity.ElectrofishEntity;
import com.dexer.aquanaut.common.entity.HelicoprionEntity;
import com.dexer.aquanaut.common.entity.IcerailEntity;
import com.dexer.aquanaut.common.entity.OctopusEntity;
import com.dexer.aquanaut.common.entity.SardineEntity;
import com.dexer.aquanaut.common.entity.SpringfishEntity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.MOD)
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

        public static final DeferredHolder<EntityType<?>, EntityType<AirBubbleEntity>> AIR_BUBBLE = ENTITIES.register(
                        "air_bubble",
                        () -> EntityType.Builder
                                        .<AirBubbleEntity>of(AirBubbleEntity::new, MobCategory.MISC)
                                        .sized(0.9375F, 0.9375F)
                                        .build("air_bubble"));

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
        }
}
