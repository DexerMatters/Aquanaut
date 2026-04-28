package com.dexer.aquanaut.common.diving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Player diving equipment state tracked separately from vanilla armor slots.
 */
public record DivingEquipmentState(boolean tankEquipped, boolean maskEquipped, boolean flippersEquipped) {

    public static final DivingEquipmentState EMPTY = new DivingEquipmentState(false, false, false);

    public static final Codec<DivingEquipmentState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("tank_equipped").forGetter(DivingEquipmentState::tankEquipped),
            Codec.BOOL.fieldOf("mask_equipped").forGetter(DivingEquipmentState::maskEquipped),
            Codec.BOOL.fieldOf("flippers_equipped").forGetter(DivingEquipmentState::flippersEquipped))
            .apply(instance, DivingEquipmentState::new));

    public DivingEquipmentState withTankEquipped(boolean value) {
        return new DivingEquipmentState(value, maskEquipped, flippersEquipped);
    }

    public DivingEquipmentState withMaskEquipped(boolean value) {
        return new DivingEquipmentState(tankEquipped, value, flippersEquipped);
    }

    public DivingEquipmentState withFlippersEquipped(boolean value) {
        return new DivingEquipmentState(tankEquipped, maskEquipped, value);
    }
}