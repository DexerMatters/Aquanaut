package com.dexer.aquanaut.client.renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.dexer.aquanaut.common.entity.BiologicalDetectorEntity;
import com.dexer.aquanaut.common.entity.DetectorScan;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

/**
 * The returns the detector is currently holding, one list per deployed buoy.
 *
 * <p>
 * The client is the only side that can draw this, so the client is the side that reads it: a
 * detector is a client-side projection of what the client already knows about, and asking the server
 * to describe every fish in a forty-eight block sphere twenty times a second would be a great deal
 * of traffic to reproduce something the client is looking at anyway.
 *
 * <p>
 * The membership is re-read every few ticks, because which creatures are in range of a buoy changes
 * on the scale of a swim rather than a frame, and an entity query over a ninety-block box is not
 * something to do at the frame rate. The list is only ever of live entities, and their positions are
 * taken fresh at draw time from their own interpolation, so a fish drifts smoothly across the sphere
 * while the membership behind it is only occasionally re-checked.
 *
 * <p>
 * Kept in a {@link WeakHashMap} keyed by the buoy itself rather than in the renderer, because a
 * renderer is shared by every detector in the world and the picture belongs to one of them. A buoy
 * that is unloaded takes its own snapshot with it.
 */
final class DetectorContacts {

    /** Ticks between re-reads of the water around a buoy. */
    private static final int RESCAN_TICKS = 4;

    private static final Map<BiologicalDetectorEntity, Snapshot> SNAPSHOTS = new WeakHashMap<>();

    private DetectorContacts() {
    }

    static List<LivingEntity> of(BiologicalDetectorEntity detector) {
        Snapshot snapshot = SNAPSHOTS.get(detector);
        if (snapshot == null || detector.tickCount < snapshot.tick
                || detector.tickCount - snapshot.tick >= RESCAN_TICKS) {
            snapshot = new Snapshot(detector.tickCount, scan(detector));
            SNAPSHOTS.put(detector, snapshot);
        }
        return snapshot.entities;
    }

    private static List<LivingEntity> scan(BiologicalDetectorEntity detector) {
        AABB box = detector.getBoundingBox().inflate(DetectorScan.RANGE);
        List<LivingEntity> found = new ArrayList<>();
        for (LivingEntity entity : detector.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (isContact(entity)) {
                found.add(entity);
            }
        }
        return found;
    }

    /**
     * Whether a living thing belongs on the plot at all.
     *
     * <p>
     * Anything alive counts; an armour stand is not a creature, and a corpse that is still ticking
     * its death animation is not worth a dot.
     */
    private static boolean isContact(LivingEntity entity) {
        return entity.isAlive() && !entity.isRemoved() && !(entity instanceof ArmorStand);
    }

    private record Snapshot(int tick, List<LivingEntity> entities) {
    }
}
