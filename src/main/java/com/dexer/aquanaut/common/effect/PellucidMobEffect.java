package com.dexer.aquanaut.common.effect;

import com.dexer.aquanaut.Aquanaut;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Clear water sharpens the senses: the bearer mines and swings as if hasted and sees as if the
 * water itself were lit -- all of it from this one effect, with nothing else on the HUD.
 *
 * <p>
 * Haste is reproduced, not granted. Vanilla's haste is an effect whose only payload is an attribute
 * modifier, so borrowing it would mean a second instance and a second icon; instead this effect
 * declares those same modifiers itself, at vanilla's own per-level amounts: tool speed is
 * multiplied by 1 + 0.2 per level, which is the factor vanilla applies inside
 * {@code Player#getDigSpeed}, and attack speed gains 0.1 per level. The curve is the mod's own
 * three-tier one rather than vanilla's unbounded linear one, so an outsized amplifier from a
 * command or another mod stays at level III numbers. The one thing vanilla's haste does that an
 * attribute cannot is shorten the arm-swing animation, so the swing stays at its normal rate.
 *
 * <p>
 * The sight half is client side: {@code PellucidSightMixin} feeds this effect's level into the
 * vanilla night-vision lightmap blend, and the value it feeds in is steady. Vanilla's own scale
 * oscillates while a night vision effect nears its end -- the shimmer a player would see if the
 * effect were granted and refreshed instead.
 */
public final class PellucidMobEffect extends MobEffect {

    /** Vanilla haste multiplies tool speed by {@code 1 + 0.2 * level}. */
    private static final double MINING_SPEED_PER_LEVEL = 0.2D;

    /** Vanilla haste adds {@code 0.1 * level} to attack speed. */
    private static final double ATTACK_SPEED_PER_LEVEL = 0.1D;

    public PellucidMobEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x63D6E8);
        addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, id("effect.pellucid_mining_speed"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, EffectLevelScaling::hasteMiningBonus);
        addAttributeModifier(Attributes.ATTACK_SPEED, id("effect.pellucid_attack_speed"),
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, EffectLevelScaling::hasteAttackBonus);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, path);
    }
}
