package com.dexer.aquanaut.common.mud;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The death cap: a rare mud-zone mushroom. Eating it is a gamble — usually a long, torturous
 * hallucination that ends in death, but sometimes a permanent boon of two extra health.
 */
public final class DeathCapItem extends Item {
    private static final ResourceLocation BONUS_ID = ResourceLocation.fromNamespaceAndPath("aquanaut", "death_cap_bonus");
    private static final float BLESSING_CHANCE = 0.25F;
    private static final double BONUS_HEALTH = 2.0D;

    public DeathCapItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
        ItemStack result = super.finishUsingItem(stack, level, living);
        if (level.isClientSide || !(living instanceof Player player)) {
            return result;
        }
        if (player.getRandom().nextFloat() < BLESSING_CHANCE) {
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.removeModifier(BONUS_ID);
                health.addPermanentModifier(new AttributeModifier(BONUS_ID, BONUS_HEALTH,
                        AttributeModifier.Operation.ADD_VALUE));
                player.setHealth(player.getMaxHealth());
            }
            player.displayClientMessage(Component.translatable("message.aquanaut.death_cap.blessing"), false);
        } else {
            MudZoneEvents.beginHallucinationDeath(player);
        }
        return result;
    }
}
