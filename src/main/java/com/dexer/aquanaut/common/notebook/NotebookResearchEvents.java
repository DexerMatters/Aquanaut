package com.dexer.aquanaut.common.notebook;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishCatalog;
import com.dexer.aquanaut.common.inventory.aquarium.AquariumFishSpec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
@SuppressWarnings("removal")
public final class NotebookResearchEvents {

    private static int tickCounter;

    private NotebookResearchEvents() {
    }

    @SubscribeEvent
    public static void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(NotebookCatalog.reloadListener());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NotebookProgressHelper.syncToClient(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer replacement) || !(event.getOriginal() instanceof ServerPlayer original)) {
            return;
        }

        NotebookProgressHelper.copyProgress(original, replacement);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % 20 != 0) {
            return;
        }

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (player.isSpectator()) {
                continue;
            }

            scanSightedSpecies(player);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }

        ResourceLocation speciesId = speciesId(living).orElse(null);
        if (speciesId == null) {
            return;
        }

        if (event.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
            NotebookProgressHelper.unlockDefeat(serverPlayer, speciesId);
        }
    }

    public static void recordCapture(ServerPlayer player, AquariumFishSpec spec) {
        NotebookProgressHelper.unlockCapture(player, spec.id());
    }

    public static void recordEncounter(ServerPlayer player, LivingEntity entity) {
        speciesId(entity).ifPresent(id -> NotebookProgressHelper.unlockEncounter(player, id));
    }

    public static void recordEncounter(ServerPlayer player, ResourceLocation speciesId) {
        NotebookProgressHelper.unlockEncounter(player, speciesId);
    }

    private static void scanSightedSpecies(ServerPlayer player) {
        var area = player.getBoundingBox().inflate(32.0D);
        List<LivingEntity> nearby = player.level().getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (!AquariumFishCatalog.isAquariumFish(entity.getType())) {
                return false;
            }
            return player.hasLineOfSight(entity);
        });

        for (LivingEntity entity : nearby) {
            speciesId(entity).ifPresent(id -> NotebookProgressHelper.unlockEncounter(player, id));
        }
    }

    private static java.util.Optional<ResourceLocation> speciesId(LivingEntity living) {
        return AquariumFishCatalog.byEntityType(living.getType()).map(AquariumFishSpec::id);
    }
}
