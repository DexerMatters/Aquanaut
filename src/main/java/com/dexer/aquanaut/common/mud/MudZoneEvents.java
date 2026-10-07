package com.dexer.aquanaut.common.mud;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.worldgen.GlowMushroomBuilder;
import com.dexer.aquanaut.core.BlockRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Mud zone interactions: bone-mealing a glow fungus grows a huge mushroom, and the death cap
 * drags whoever eats it through a slow hallucination. */
@EventBusSubscriber(modid = Aquanaut.MODID)
public final class MudZoneEvents {
    private static final String DEATH_TAG = "aquanaut_death_cap_ticks";
    /** Length of the hallucination before it takes the player: 8 seconds. */
    private static final int DEATH_TICKS = 160;
    private static final ResourceKey<DamageType> HALLUCINATION = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("aquanaut", "hallucination"));
    private MudZoneEvents() {
    }

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        BlockState state = event.getState();
        if (!(state.is(BlockRegistry.GLOW_FUNGUS.get())
                || state.is(BlockRegistry.GLOW_FUNGUS_AMBER.get())
                || state.is(BlockRegistry.GLOW_FUNGUS_VIOLET.get()))) {
            return;
        }
        if (GlowMushroomBuilder.grow(event.getLevel(), event.getPos(), event.getLevel().getRandom())) {
            event.setCanceled(true);
        }
    }

    /** Begins the death cap's hallucination: heavy effects now, death when the timer runs out. */
    public static void beginHallucinationDeath(Player player) {
        player.getPersistentData().putInt(DEATH_TAG, DEATH_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, DEATH_TICKS, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DEATH_TICKS, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, DEATH_TICKS, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DEATH_TICKS, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, DEATH_TICKS, 1, false, false));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        int timer = data.getInt(DEATH_TAG);
        if (timer <= 0) {
            return;
        }
        if (!player.isAlive()) {
            data.putInt(DEATH_TAG, 0);
            return;
        }
        timer--;
        data.putInt(DEATH_TAG, timer);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.WARPED_SPORE, player.getX(), player.getY() + 1.2D, player.getZ(),
                8, 0.6D, 0.9D, 0.6D, 0.02D);
        level.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 0.6D, player.getZ(),
                4, 0.5D, 0.6D, 0.5D, 0.01D);
        if (timer % 20 == 0) {
            player.hurt(hallucinationDamage(level), 1.0F);
        }
        if (timer <= 0) {
            player.hurt(hallucinationDamage(level), Float.MAX_VALUE);
        }
    }

    private static DamageSource hallucinationDamage(ServerLevel level) {
        Holder<DamageType> type = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(HALLUCINATION);
        return new DamageSource(type);
    }
}
