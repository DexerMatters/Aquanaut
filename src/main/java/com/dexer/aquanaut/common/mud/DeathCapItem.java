package com.dexer.aquanaut.common.mud;

import net.minecraft.nbt.CompoundTag;
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
 * hallucination that ends in death, but once in a lifetime a permanent boon of two extra
 * health. The boon can only ever be earned once (a persistent flag), so the mushroom can never
 * be farmed for infinite health; after that the visions always come.
 */
public final class DeathCapItem extends Item {
    private static final ResourceLocation BONUS_ID = ResourceLocation.fromNamespaceAndPath("aquanaut", "death_cap_bonus");
    private static final String BLESSED_TAG = "aquanaut_death_cap_blessed";
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
        CompoundTag data = player.getPersistentData();
        if (!data.getBoolean(BLESSED_TAG) && player.getRandom().nextFloat() < BLESSING_CHANCE) {
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.removeModifier(BONUS_ID);
                health.addPermanentModifier(new AttributeModifier(BONUS_ID, BONUS_HEALTH,
                        AttributeModifier.Operation.ADD_VALUE));
                player.setHealth(player.getMaxHealth());
            }
            data.putBoolean(BLESSED_TAG, true);
            player.displayClientMessage(Component.translatable("message.aquanaut.death_cap.blessing"), false);
        } else {
            // Already blessed (there is no further boon to win) or unlucky: the visions come.
            MudZoneEvents.beginHallucinationDeath(player);
        }
        return result;
    }
}
