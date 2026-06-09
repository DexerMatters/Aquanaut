package com.dexer.aquanaut.common.inventory.aquarium;

import com.dexer.aquanaut.Aquanaut;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = Aquanaut.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class AquariumHealthTracker {

    private static final String ENTERED_AT_KEY = "AquariumEnteredAt";
    private static final String LAST_HURT_AT_KEY = "AquariumLastHurtAt";
    private static final long GRACE_PERIOD_MS = 30_000L;
    private static final long HURT_INTERVAL_MS = 2_000L;
    private static final float DAMAGE_PER_TICK = 0.5F;

    private static long tickCounter;

    private AquariumHealthTracker() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter % 20 != 0) {
            return;
        }

        ServerLevel level = event.getServer().overworld();
        for (ServerPlayer player : level.players()) {
            tickPlayer(player);
        }
    }

    private static void tickPlayer(ServerPlayer player) {
        AquariumInventoryData data = AquariumInventoryHelper.getAquarium(player);
        List<AquariumFishEntry> entries = data.mutableCopy();
        boolean changed = false;

        for (int i = 0; i < entries.size(); i++) {
            AquariumFishEntry entry = entries.get(i);
            if (entry.isEmpty()) {
                continue;
            }

            long now = System.currentTimeMillis();
            long enteredAt = getEnteredAt(entry);
            if (enteredAt <= 0) {
                entry = setEnteredAt(entry, now);
                entries.set(i, entry);
                changed = true;
                continue;
            }

            long elapsed = now - enteredAt;
            if (elapsed < GRACE_PERIOD_MS) {
                continue;
            }

            long lastHurt = getLastHurtAt(entry);
            if (lastHurt > 0 && now - lastHurt < HURT_INTERVAL_MS) {
                continue;
            }

            float currentHealth = getHealth(entry);
            float newHealth = currentHealth - DAMAGE_PER_TICK;

            if (newHealth <= 0.0F) {
                killFish(player, entry, i, entries);
                changed = true;
                break;
            }

            entry = setHealth(entry, newHealth);
            entry = setLastHurtAt(entry, now);
            entries.set(i, entry);
            changed = true;
        }

        if (changed) {
            AquariumInventoryHelper.setAquarium(player, new AquariumInventoryData(entries));
        }
    }

    private static void killFish(ServerPlayer player, AquariumFishEntry entry, int index,
            List<AquariumFishEntry> entries) {
        entries.set(index, AquariumFishEntry.EMPTY);

        Optional<Entity> entityOpt = AquariumEntitySnapshot.createEntity(player.serverLevel(), entry);
        if (entityOpt.isEmpty() || !(entityOpt.get() instanceof LivingEntity living)) {
            return;
        }

        List<ItemStack> drops = generateLoot(living, player.serverLevel());

        for (ItemStack drop : drops) {
            if (!drop.isEmpty()) {
                if (!player.getInventory().add(drop)) {
                    player.level().addFreshEntity(
                            new ItemEntity(player.level(), player.getX(),
                                    player.getY() + 0.5, player.getZ(), drop));
                }
            }
        }
    }

    private static List<ItemStack> generateLoot(LivingEntity entity, ServerLevel level) {
        var lootTableResource = entity.getLootTable();
        LootTable lootTable = level.getServer().reloadableRegistries().getLootTable(lootTableResource);
        if (lootTable == LootTable.EMPTY) {
            return List.of();
        }
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, entity.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, entity.damageSources().generic())
                .create(LootContextParamSets.ENTITY);
        return lootTable.getRandomItems(params);
    }

    public static String setEnteredAt(String entityData, long timestamp) {
        try {
            CompoundTag tag = entityData.isBlank() ? new CompoundTag()
                    : TagParser.parseTag(entityData);
            tag.putLong(ENTERED_AT_KEY, timestamp);
            return tag.toString();
        } catch (Exception e) {
            return entityData;
        }
    }

    public static long getEnteredAt(AquariumFishEntry entry) {
        return getLongFromData(entry.entityData(), ENTERED_AT_KEY);
    }

    public static AquariumFishEntry setEnteredAt(AquariumFishEntry entry, long timestamp) {
        return new AquariumFishEntry(entry.entityId(), setEnteredAt(entry.entityData(), timestamp));
    }

    public static long getLastHurtAt(AquariumFishEntry entry) {
        return getLongFromData(entry.entityData(), LAST_HURT_AT_KEY);
    }

    public static AquariumFishEntry setLastHurtAt(AquariumFishEntry entry, long timestamp) {
        try {
            CompoundTag tag = entry.entityData().isBlank() ? new CompoundTag()
                    : TagParser.parseTag(entry.entityData());
            tag.putLong(LAST_HURT_AT_KEY, timestamp);
            return new AquariumFishEntry(entry.entityId(), tag.toString());
        } catch (Exception e) {
            return entry;
        }
    }

    public static float getHealth(AquariumFishEntry entry) {
        try {
            CompoundTag tag = entry.entityData().isBlank() ? new CompoundTag()
                    : TagParser.parseTag(entry.entityData());
            return tag.contains("Health") ? tag.getFloat("Health") : 20.0F;
        } catch (Exception e) {
            return 20.0F;
        }
    }

    public static AquariumFishEntry setHealth(AquariumFishEntry entry, float health) {
        try {
            CompoundTag tag = entry.entityData().isBlank() ? new CompoundTag()
                    : TagParser.parseTag(entry.entityData());
            tag.putFloat("Health", Math.max(0.0F, health));
            return new AquariumFishEntry(entry.entityId(), tag.toString());
        } catch (Exception e) {
            return entry;
        }
    }

    public static boolean isRecentlyHurt(AquariumFishEntry entry) {
        long lastHurt = getLastHurtAt(entry);
        if (lastHurt <= 0) {
            return false;
        }
        return System.currentTimeMillis() - lastHurt < 500L;
    }

    private static long getLongFromData(String entityData, String key) {
        try {
            CompoundTag tag = entityData.isBlank() ? new CompoundTag()
                    : TagParser.parseTag(entityData);
            return tag.getLong(key);
        } catch (Exception e) {
            return 0L;
        }
    }
}
