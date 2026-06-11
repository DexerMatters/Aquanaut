package com.dexer.aquanaut.common.notebook;

import com.dexer.aquanaut.core.AttachmentRegistry;
import com.dexer.aquanaut.network.NotebookProgressSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

public final class NotebookProgressHelper {

    private NotebookProgressHelper() {
    }

    public static NotebookProgress getProgress(Player player) {
        return player.getData(AttachmentRegistry.NOTEBOOK_PROGRESS.get());
    }

    public static void setProgress(Player player, NotebookProgress progress) {
        player.setData(AttachmentRegistry.NOTEBOOK_PROGRESS.get(), progress);
        if (player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    public static void copyProgress(Player original, Player replacement) {
        setProgress(replacement, getProgress(original));
    }

    public static void syncToClient(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, NotebookProgressSyncPayload.fromData(getProgress(player)));
    }

    public static boolean unlock(ServerPlayer player, ResourceLocation speciesId, NotebookResearchStage stage) {
        NotebookProgress current = getProgress(player);
        NotebookProgress updated = current.withStage(speciesId, stage);
        if (updated == current) {
            return false;
        }
        setProgress(player, updated);
        return true;
    }

    public static boolean unlockEncounter(ServerPlayer player, ResourceLocation speciesId) {
        return unlock(player, speciesId, NotebookResearchStage.ENCOUNTERED);
    }

    public static boolean unlockCapture(ServerPlayer player, ResourceLocation speciesId) {
        return unlock(player, speciesId, NotebookResearchStage.CAPTURED);
    }

    public static boolean unlockDefeat(ServerPlayer player, ResourceLocation speciesId) {
        return unlock(player, speciesId, NotebookResearchStage.DEFEATED);
    }

    public static Optional<ResourceLocation> canonicalSpeciesId(EntityType<?> type) {
        return com.dexer.aquanaut.common.inventory.aquarium.AquariumFishCatalog.byEntityType(type)
                .map(com.dexer.aquanaut.common.inventory.aquarium.AquariumFishSpec::id);
    }

    public static boolean unlockEncounter(ServerPlayer player, LivingEntity entity) {
        return canonicalSpeciesId(entity.getType()).isPresent()
                && unlockEncounter(player, canonicalSpeciesId(entity.getType()).get());
    }

    public static boolean unlockCapture(ServerPlayer player, LivingEntity entity) {
        return canonicalSpeciesId(entity.getType()).isPresent()
                && unlockCapture(player, canonicalSpeciesId(entity.getType()).get());
    }

    public static boolean unlockDefeat(ServerPlayer player, LivingEntity entity) {
        return canonicalSpeciesId(entity.getType()).isPresent()
                && unlockDefeat(player, canonicalSpeciesId(entity.getType()).get());
    }

    public static String playerKey(UUID uuid) {
        return uuid.toString();
    }
}
