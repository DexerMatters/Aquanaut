package com.dexer.aquanaut.core;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.diving.DivingEquipmentState;
import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AttachmentRegistry {
        public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister
                        .create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Aquanaut.MODID);

        /**
         * Maximum extra air capacity added on top of the base 1200 ticks. Default: 0.
         */
        public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MAX_EXTRA_AIR_SUPPLY = ATTACHMENT_TYPES
                        .register("max_extra_air_supply",
                                        () -> AttachmentType.builder(() -> 0).serialize(Codec.INT).build());

        /**
         * Logical diving equipment state tracked independently from vanilla armor.
         */
        public static final DeferredHolder<AttachmentType<?>, AttachmentType<DivingEquipmentState>> DIVING_EQUIPMENT_STATE = ATTACHMENT_TYPES
                        .register("diving_equipment_state",
                                        () -> AttachmentType.builder(() -> DivingEquipmentState.EMPTY)
                                                        .serialize(DivingEquipmentState.CODEC)
                                                        .build());

        public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> DIVING_MASK_STACK = ATTACHMENT_TYPES
                        .register("diving_mask_stack",
                                        () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                                                        .serialize(ItemStack.CODEC,
                                                                        stack -> stack != null && !stack.isEmpty())
                                                        .build());

        public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> DIVING_TANK_STACK = ATTACHMENT_TYPES
                        .register("diving_tank_stack",
                                        () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                                                        .serialize(ItemStack.CODEC,
                                                                        stack -> stack != null && !stack.isEmpty())
                                                        .build());

        public static final DeferredHolder<AttachmentType<?>, AttachmentType<ItemStack>> DIVING_FLIPPERS_STACK = ATTACHMENT_TYPES
                        .register("diving_flippers_stack",
                                        () -> AttachmentType.builder(() -> ItemStack.EMPTY)
                                                        .serialize(ItemStack.CODEC,
                                                                        stack -> stack != null && !stack.isEmpty())
                                                        .build());

        private AttachmentRegistry() {
        }

        public static void register(IEventBus modEventBus) {
                ATTACHMENT_TYPES.register(modEventBus);
        }
}
