package com.dexer.aquanaut.common.item;

import java.util.List;

import javax.annotation.Nullable;

import com.dexer.aquanaut.common.entity.AbstractTaggableEntity;
import com.dexer.aquanaut.common.entity.TagRules;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for items that place an entity.
 *
 * <p>
 * The point of it is the second placement path. A plain {@code useOn} item needs a block to click,
 * which is useless for anything meant to live in open water: there is often no surface within reach,
 * and even when there is, the interesting space is the water column in front of the diver, not the
 * face they happen to be looking at. So {@link #use} walks the player's look vector and drops the
 * entity into the first free <em>water</em> volume it finds, no surface required.
 *
 * <p>
 * Subclasses supply the entity and may hook the spawn; everything else — the reach, the collision
 * test, the walls you cannot spawn through, consuming the stack and the sound — lives here so the
 * two paths cannot drift apart.
 *
 * <p>
 * One thing beyond the placement itself is shared here too, because it belongs to placing rather than
 * to any one item: a marker's tag travels on the item that represents it. A stack that came from
 * picking a taggable up puts that tag straight back when it is deployed, and a stack with nothing on
 * it is given a fresh one — see {@link AbstractTaggableEntity#adoptTagFrom}. The stack is also
 * unstackable in every subclass, since two items that each carry a different tag are not the same
 * thing.
 */
public abstract class AbstractSpawnerItem extends Item {

    /** Step along the look vector when hunting for a valid water pocket. */
    private static final double SEARCH_STEP = 0.25D;

    /** Offset of the first sample in front of the player's eyes. */
    private static final double SEARCH_START = 0.75D;

    protected AbstractSpawnerItem(Properties properties) {
        super(properties);
    }

    /** The entity this item places. */
    protected abstract EntityType<? extends Entity> entityType();

    /**
     * Hook called once the entity has been positioned but before it is added to the level, so a
     * subclass can stamp whatever state a fresh spawn needs.
     */
    protected void onSpawned(ServerLevel level, Entity entity, @Nullable Player player) {
    }

    /** Sound played when a placement succeeds. */
    protected SoundEvent spawnSound() {
        return SoundEvents.ITEM_FRAME_ADD_ITEM;
    }

    /**
     * Says which marker a stack came from.
     *
     * <p>
     * A spawner item is unstackable precisely because it carries a tag, and a player holding two of
     * them has no other way to tell which is which before putting one down.
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        String name = TagRules.normalizeName(AbstractTaggableEntity.tagNameOf(stack));
        if (!TagRules.isValidName(name)) {
            return;
        }
        // The colour is a palette value, so it is drawn as it is; a stack edited to something else
        // falls back to the default so the line stays legible.
        int color = AbstractTaggableEntity.tagColorOf(stack);
        tooltip.add(Component.translatable("item.aquanaut.tagged", name)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(
                        TagRules.isPaletteColor(color) ? color : 0xFFFFFF))));
    }

    /** How far in front of the player {@link #use} will look for water. */
    protected double waterReach() {
        return 5.0D;
    }

    /**
     * Where the crosshair lands on the entity when it is dropped into open water.
     *
     * <p>
     * Block placement is always anchored {@link SpawnAnchor#BOTTOM}, because there the aim point is
     * a surface the entity should stand on. Aiming into water is different: there is no surface, so
     * the aim point is simply where the player pointed, and an entity anchored at its feet would
     * appear a whole body-height above the crosshair. Subclasses that are placed by aiming override
     * this to {@link SpawnAnchor#CENTER}.
     */
    protected SpawnAnchor aimedAnchor() {
        return SpawnAnchor.BOTTOM;
    }

    /**
     * Whether the entity may sit with the crosshair at {@code aimPoint}. The default demands open
     * water where the player pointed and a collision-free hitbox once the entity is anchored there,
     * which is what makes this a water spawner rather than a "drop it anywhere" spawner.
     */
    protected boolean isValidSpawnPosition(ServerLevel level, Entity entity, Vec3 aimPoint) {
        return level.getFluidState(BlockPos.containing(aimPoint)).is(FluidTags.WATER)
                && level.noCollision(entity);
    }

    /** The entity origin that puts {@code aimPoint} at {@code anchor} on the entity. */
    protected Vec3 anchoredOrigin(Entity entity, Vec3 aimPoint, SpawnAnchor anchor) {
        return new Vec3(aimPoint.x, aimPoint.y - anchor.originOffset(entity.getBbHeight()), aimPoint.z);
    }

    /**
     * Places the entity against the clicked face — the ordinary behaviour, for when the player does
     * have a surface to aim at.
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }

        Entity entity = entityType().create(server);
        if (entity == null) {
            return InteractionResult.FAIL;
        }

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        return place(server, context.getPlayer(), context.getItemInHand(), entity,
                Vec3.atBottomCenterOf(pos), SpawnAnchor.BOTTOM);
    }

    /**
     * Places the entity into the water in front of the player, without needing to click anything.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            // The client only predicts the swing; the server does the placing.
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        Entity entity = entityType().create(server);
        if (entity == null) {
            return InteractionResultHolder.fail(stack);
        }

        Vec3 target = findWaterAhead(server, player, entity);
        if (target == null) {
            return InteractionResultHolder.fail(stack);
        }

        InteractionResult result = place(server, player, stack, entity, target, aimedAnchor());
        return new InteractionResultHolder<>(result, stack);
    }

    /**
     * Walks the look vector for the nearest spot that satisfies {@link #isValidSpawnPosition},
     * stopping early at any block the player could not see past — so an item cannot be used to post
     * an entity through a wall.
     */
    @Nullable
    protected Vec3 findWaterAhead(ServerLevel level, Player player, Entity probe) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double reach = waterReach();

        BlockHitResult obstruction = level.clip(new ClipContext(eye, eye.add(look.scale(reach)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (obstruction.getType() != HitResult.Type.MISS) {
            reach = Math.min(reach, eye.distanceTo(obstruction.getLocation()));
        }

        SpawnAnchor anchor = aimedAnchor();
        for (double distance = SEARCH_START; distance <= reach; distance += SEARCH_STEP) {
            Vec3 aimPoint = eye.add(look.scale(distance));
            Vec3 origin = anchoredOrigin(probe, aimPoint, anchor);
            probe.setPos(origin.x, origin.y, origin.z);
            if (isValidSpawnPosition(level, probe, aimPoint)) {
                return aimPoint;
            }
        }
        return null;
    }

    /** Positions, configures and adds the entity, then charges the stack. */
    protected InteractionResult place(ServerLevel level, @Nullable Player player, ItemStack stack,
            Entity entity, Vec3 aimPoint, SpawnAnchor anchor) {
        Vec3 origin = anchoredOrigin(entity, aimPoint, anchor);
        entity.moveTo(origin.x, origin.y, origin.z,
                player != null ? player.getYRot() : 0.0F, 0.0F);
        onSpawned(level, entity, player);
        // Before the entity joins the level, so the name it is born with is the name everybody sees:
        // a marker that has already been placed and picked up keeps the tag it had.
        if (entity instanceof AbstractTaggableEntity taggable) {
            taggable.adoptTagFrom(level, stack, player);
        }

        if (!level.noCollision(entity)) {
            return InteractionResult.FAIL;
        }

        level.addFreshEntity(entity);
        level.playSound(null, origin.x, origin.y, origin.z,
                spawnSound(), SoundSource.NEUTRAL, 0.9F, 1.0F);

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
