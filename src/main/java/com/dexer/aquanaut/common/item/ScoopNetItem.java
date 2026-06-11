package com.dexer.aquanaut.common.item;

import com.dexer.aquanaut.common.inventory.aquarium.AquariumEntitySnapshot;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFish;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishCatalog;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishEntry;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishSpec;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumInventoryHelper;
import com.dexer.aquanaut.common.notebook.NotebookResearchEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class ScoopNetItem extends Item {

    private static final int FULL_CHARGE_TICKS = 40;
    private static final int MAX_USE_TICKS = 50;

    private final int maxSize;

    public ScoopNetItem(Properties properties, int maxSize, int extraSize) {
        super(properties);
        this.maxSize = maxSize;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.aquanaut.scoop_net.desc", maxSize)
                .withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_TICKS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        int chargeTicks = MAX_USE_TICKS - remainingUseDuration;
        if (chargeTicks == FULL_CHARGE_TICKS) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.6F, 1.5F);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) {
            return;
        }

        int chargeTicks = MAX_USE_TICKS - timeLeft;
        if (chargeTicks < FULL_CHARGE_TICKS) {
            return;
        }

        if (level.isClientSide) {
            player.swing(player.getUsedItemHand());
            return;
        }

        captureFish((ServerPlayer) player, stack, level);
    }
    private void captureFish(ServerPlayer player, ItemStack stack, Level level) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 target = eyePos.add(look.scale(1.5));

        AABB box = new AABB(eyePos.subtract(0.5, 0.5, 0.5), eyePos.add(look.scale(2.0)).add(0.5, 0.5, 0.5));
        List<Entity> entities = level.getEntities(player, box, e -> e instanceof WaterAnimal);

        Entity closest = null;
        double closestDist = Double.MAX_VALUE;
        for (Entity e : entities) {
            Optional<Vec3> hit = e.getBoundingBox().clip(eyePos, target);
            if (hit.isPresent()) {
                double dist = eyePos.distanceTo(hit.get());
                if (dist < closestDist) {
                    closestDist = dist;
                    closest = e;
                }
            }
        }

        if (closest == null || !(closest instanceof LivingEntity living)) {
            return;
        }

        AquariumFishSpec spec = AquariumFishCatalog.byEntityType(closest.getType()).orElse(null);
        if (spec == null) {
            return;
        }

        float size = getFishSize(living);
        int tierSize = maxSize;
        if (size <= tierSize) {
            doCapture(player, stack, level, living, spec);
        } else if (size <= Math.ceil(tierSize * 1.5F) && level.random.nextFloat() < 0.40F) {
            doCapture(player, stack, level, living, spec);
        } else if (size <= Math.ceil(tierSize * 2.0F) && level.random.nextFloat() < 0.20F) {
            doCapture(player, stack, level, living, spec);
        } else if (size <= Math.ceil(tierSize * 3.0F) && level.random.nextFloat() < 0.10F) {
            doCapture(player, stack, level, living, spec);
        }
    }

    private void doCapture(ServerPlayer player, ItemStack stack, Level level, LivingEntity target,
            AquariumFishSpec spec) {
        Optional<AquariumFishEntry> snapshot = AquariumEntitySnapshot.snapshot(target);
        if (snapshot.isEmpty()) {
            return;
        }

        if (!AquariumInventoryHelper.addFishEntry(player, snapshot.get())) {
            return;
        }

        target.discard();
        NotebookResearchEvents.recordCapture(player, spec);
        player.swing(player.getUsedItemHand());
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FISHING_BOBBER_SPLASH, SoundSource.PLAYERS, 0.5F, 1.0F);
        stack.hurtAndBreak(1, player, player.getUsedItemHand() == InteractionHand.OFF_HAND
                ? net.minecraft.world.entity.EquipmentSlot.OFFHAND
                : net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }

    private static float getFishSize(LivingEntity entity) {
        if (entity instanceof AquariumFish fish) {
            return Math.max(0.1F, fish.getAquariumModelLength()
                    * fish.getAquariumModelWidth()
                    * fish.getAquariumModelHeight());
        }
        return Math.max(0.1F, entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight());
    }
}
