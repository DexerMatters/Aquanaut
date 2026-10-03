package com.dexer.aquanaut.common.block;

import com.dexer.aquanaut.common.mud.MudSinkLogic;
import com.dexer.aquanaut.common.mud.MudZoneConfig;
import com.dexer.aquanaut.common.diving.DivingEquipmentHelper;
import com.dexer.aquanaut.common.diving.DivingEquipmentSlotType;
import com.dexer.aquanaut.core.ItemRegistry;
import com.dexer.aquanaut.core.EntityRegistry;
import com.dexer.aquanaut.common.entity.SedimentWormEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.WeakHashMap;

public final class MudBlock extends Block {
    public static final MapCodec<MudBlock> CODEC = simpleCodec(MudBlock::new);
    private static final VoxelShape VISUAL_SHAPE = Shapes.block();
    private static final VoxelShape COLLISION_SHAPE = Shapes.empty();
    private static final Map<Entity, ContactState> CONTACTS = new WeakHashMap<>();

    public MudBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<MudBlock> codec() {
        return CODEC;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!(entity instanceof Player player) || player.isCreative() || player.isSpectator()) {
            synchronized (CONTACTS) {
                CONTACTS.remove(entity);
            }
            return;
        }
        long gameTime = level.getGameTime();
        ContactState contact;
        synchronized (CONTACTS) {
            contact = CONTACTS.computeIfAbsent(entity, ignored -> new ContactState());
            // A player's bounding box can touch several mud blocks in one tick.
            if (contact.lastGameTime == gameTime) {
                // Contact time was already updated by another mud block this tick.
            } else {
                boolean submerged = level.getBlockState(BlockPos.containing(player.getEyePosition()))
                        .getBlock() instanceof MudBlock;
                contact.ticks = MudSinkLogic.submergedTicks(contact.ticks, contact.lastGameTime, gameTime,
                        submerged);
                if (contact.ticks <= 1) {
                    contact.lastDamageGameTime = -1L;
                }
                contact.lastGameTime = gameTime;

                if (!level.isClientSide
                        && MudSinkLogic.shouldDamage(contact.ticks, gameTime, contact.lastDamageGameTime)) {
                    player.hurt(level.damageSources().inWall(), MudZoneConfig.SUFFOCATION_DAMAGE);
                    contact.lastDamageGameTime = gameTime;
                }
            }
        }

        boolean mudwalker = DivingEquipmentHelper.getEquippedStack(player, DivingEquipmentSlotType.FLIPPERS)
                .is(ItemRegistry.MUDWALKER_CHARM.get());
        if (!mudwalker) {
            entity.setDeltaMovement(MudSinkLogic.applySinking(entity.getDeltaMovement()));
            entity.makeStuckInBlock(state, new Vec3(1.0D, 0.15D, 1.0D));
        }
        if (!level.isClientSide && player.tickCount % 40 == 0 && level.random.nextFloat() < 0.01F
                && level.getBlockState(pos.below()).is(this)) {
            SedimentWormEntity worm = new SedimentWormEntity(EntityRegistry.SEDIMENT_WORM.get(), level);
            worm.moveTo(pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            level.addFreshEntity(worm);
        }
        entity.resetFallDistance();
        entity.hasImpulse = true;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        FluidState above = level.getFluidState(pos.above());
        if (!above.is(Fluids.WATER) || random.nextFloat() > 0.18F) {
            return;
        }

        double x = pos.getX() + 0.2D + random.nextDouble() * 0.6D;
        double y = pos.getY() + 1.03D;
        double z = pos.getZ() + 0.2D + random.nextDouble() * 0.6D;
        level.addParticle(ParticleTypes.BUBBLE, x, y, z, 0.0D, 0.035D, 0.0D);
        if (random.nextFloat() < 0.45F) {
            level.addParticle(new DustParticleOptions(new Vector3f(0.30F, 0.25F, 0.18F), 1.2F),
                    x, y, z, 0.0D, 0.005D, 0.0D);
        }
        // Bound the water-column scan; never search an entire deep ocean per particle.
        BlockPos.MutableBlockPos surface = pos.above().mutable();
        for (int i = 0; i < 32 && surface.getY() < level.getMaxBuildHeight() - 1; i++) {
            if (!level.getFluidState(surface).is(Fluids.WATER)) {
                break;
            }
            if (level.getBlockState(surface.above()).isAir()) {
                level.addParticle(ParticleTypes.BUBBLE_POP, x, surface.getY() + 1.0D, z, 0, 0, 0);
                level.addParticle(new DustParticleOptions(new Vector3f(0.30F, 0.25F, 0.18F), 1.2F),
                        x, surface.getY() + 1.02D, z, 0, 0.005D, 0);
                break;
            }
            surface.move(0, 1, 0);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VISUAL_SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos,
            CollisionContext context) {
        if (context instanceof EntityCollisionContext entityContext
                && entityContext.getEntity() instanceof Player player
                && !player.isCreative() && !player.isSpectator()) {
            return COLLISION_SHAPE;
        }
        return VISUAL_SHAPE;
    }

    private static final class ContactState {
        private int ticks;
        private long lastGameTime = Long.MIN_VALUE;
        private long lastDamageGameTime = -1L;
    }
}
