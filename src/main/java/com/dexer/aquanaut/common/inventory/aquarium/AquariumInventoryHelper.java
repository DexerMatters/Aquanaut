package com.dexer.aquanaut.common.inventory.aquarium;

import com.dexer.aquanaut.core.AttachmentRegistry;
import com.dexer.aquanaut.network.AquariumInventorySyncPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public final class AquariumInventoryHelper {

    private AquariumInventoryHelper() {
    }

    public static AquariumInventoryData getAquarium(Player player) {
        return player.getData(AttachmentRegistry.AQUARIUM_INVENTORY.get());
    }

    public static void setAquarium(Player player, AquariumInventoryData data) {
        player.setData(AttachmentRegistry.AQUARIUM_INVENTORY.get(), data);
        if (player instanceof ServerPlayer serverPlayer) {
            syncToClient(serverPlayer);
        }
    }

    public static void syncToClient(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, AquariumInventorySyncPayload.fromData(getAquarium(player)));
    }

    public static List<AquariumPlacementMath.Placement> placements(AquariumInventoryData data) {
        List<AquariumPlacementMath.Placement> placements = new ArrayList<>();
        for (int index = 0; index < AquariumInventoryData.SLOT_COUNT; index++) {
            int slotIndex = index;
            fishAt(data, slotIndex).ifPresent(spec -> placements.add(spec.placement(slotIndex)));
        }
        return placements;
    }

    public static Optional<AquariumFishSpec> fishAt(AquariumInventoryData data, int index) {
        if (index < 0 || index >= AquariumInventoryData.SLOT_COUNT) {
            return Optional.empty();
        }
        return fishEntryAt(data, index).flatMap(entry -> entry.resourceId().flatMap(AquariumFishCatalog::byId));
    }

    public static Optional<AquariumFishEntry> fishEntryAt(AquariumInventoryData data, int index) {
        if (index < 0 || index >= AquariumInventoryData.SLOT_COUNT) {
            return Optional.empty();
        }
        return data.entryAt(index);
    }

    public static OptionalInt findDensePlacement(AquariumInventoryData data, AquariumFishSpec fish) {
        int anchor = AquariumPlacementMath.findDensePlacement(placements(data), fish.gridWidth(), fish.gridHeight(),
                AquariumContainerMenu.AQUARIUM_COLS, AquariumContainerMenu.AQUARIUM_ROWS);
        return anchor < 0 ? OptionalInt.empty() : OptionalInt.of(anchor);
    }

    public static boolean canPlaceAt(AquariumInventoryData data, AquariumFishSpec fish, int anchorIndex,
            int ignoredAnchorIndex) {
        return AquariumPlacementMath.canPlaceAt(placements(data), fish.gridWidth(), fish.gridHeight(), anchorIndex,
                AquariumContainerMenu.AQUARIUM_COLS, AquariumContainerMenu.AQUARIUM_ROWS, ignoredAnchorIndex);
    }

    public static boolean addFish(ServerPlayer player, AquariumFishSpec fish) {
        Optional<AquariumFishEntry> entry = AquariumEntitySnapshot.createDefault(player.serverLevel(), fish);
        if (entry.isEmpty()) {
            return false;
        }
        return addFishEntry(player, entry.get());
    }

    public static boolean addFishEntry(ServerPlayer player, AquariumFishEntry entry) {
        AquariumInventoryData data = getAquarium(player);
        Optional<AquariumFishSpec> fish = entry.resourceId().flatMap(AquariumFishCatalog::byId);
        if (fish.isEmpty()) {
            return false;
        }
        OptionalInt placement = findDensePlacement(data, fish.get());
        if (placement.isEmpty()) {
            return false;
        }

        List<AquariumFishEntry> fishEntries = data.mutableCopy();
        fishEntries.set(placement.getAsInt(), entry);
        setAquarium(player, new AquariumInventoryData(fishEntries));
        return true;
    }

    public static boolean moveFish(ServerPlayer player, int sourceIndex, int targetIndex) {
        if (targetIndex < 0) {
            return releaseFish(player, sourceIndex);
        }

        AquariumInventoryData data = getAquarium(player);
        Optional<AquariumFishSpec> fish = fishAt(data, sourceIndex);
        if (fish.isEmpty()) {
            return false;
        }

        if (sourceIndex == targetIndex) {
            return true;
        }

        AquariumFishSpec spec = fish.get();
        if (!canPlaceAt(data, spec, targetIndex, sourceIndex)) {
            return false;
        }

        AquariumFishEntry entry = fishEntryAt(data, sourceIndex).orElse(null);
        if (entry == null) {
            return false;
        }

        List<AquariumFishEntry> fishEntries = data.mutableCopy();
        fishEntries.set(sourceIndex, AquariumFishEntry.EMPTY);
        fishEntries.set(targetIndex, entry);
        setAquarium(player, new AquariumInventoryData(fishEntries));
        return true;
    }

    public static boolean releaseFish(ServerPlayer player, int sourceIndex) {
        AquariumInventoryData data = getAquarium(player);
        Optional<AquariumFishSpec> fish = fishAt(data, sourceIndex);
        if (fish.isEmpty()) {
            return false;
        }

        AquariumFishEntry entry = fishEntryAt(data, sourceIndex).orElse(null);
        if (entry == null) {
            return false;
        }

        AquariumFishSpec spec = fish.get();
        if (!spawnReleasedFish(player, entry, spec)) {
            return false;
        }

        List<AquariumFishEntry> fishEntries = data.mutableCopy();
        fishEntries.set(sourceIndex, AquariumFishEntry.EMPTY);
        setAquarium(player, new AquariumInventoryData(fishEntries));
        return true;
    }

    public static Collection<AquariumPlacementMath.Placement> occupiedPlacements(AquariumInventoryData data) {
        return placements(data);
    }

    private static boolean spawnReleasedFish(ServerPlayer player, AquariumFishEntry entry, AquariumFishSpec spec) {
        ServerLevel level = player.serverLevel();
        Entity entity = AquariumEntitySnapshot.createEntity(level, entry).orElse(null);
        if (entity == null || entity.getType() != spec.entityType()) {
            return false;
        }

        float yaw = player.getYRot();
        float yawRad = yaw * ((float) Math.PI / 180.0F);
        double spawnX = player.getX() - Mth.sin(yawRad) * 0.75D;
        double spawnY = player.getY() + 0.25D;
        double spawnZ = player.getZ() + Mth.cos(yawRad) * 0.75D;
        entity.moveTo(spawnX, spawnY, spawnZ, yaw, 0.0F);
        entity.setDeltaMovement(player.getDeltaMovement().scale(0.15D));
        return level.addFreshEntity(entity);
    }
}
