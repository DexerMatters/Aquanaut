package com.dexer.aquanaut.network;

import com.dexer.aquanaut.Aquanaut;
import com.dexer.aquanaut.common.item.DevelopedPhotoItem;
import com.dexer.aquanaut.common.item.PhotoImageFormat;
import com.dexer.aquanaut.common.item.ShellCameraItem;
import com.dexer.aquanaut.core.ItemRegistry;
import com.dexer.aquanaut.core.SoundRegistry;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-validated, bounded photo capture sent after the local camera shutter is released. */
public record TakePhotoPayload(boolean offhand, byte[] png) implements CustomPacketPayload {
    public static final Type<TakePhotoPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Aquanaut.MODID, "take_photo"));

    private static final StreamCodec<ByteBuf, byte[]> IMAGE_CODEC = ByteBufCodecs.byteArray(PhotoImageFormat.MAX_BYTES);
    public static final StreamCodec<ByteBuf, TakePhotoPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TakePhotoPayload decode(ByteBuf buffer) {
            boolean offhand = ByteBufCodecs.BOOL.decode(buffer);
            return new TakePhotoPayload(offhand, IMAGE_CODEC.decode(buffer));
        }

        @Override
        public void encode(ByteBuf buffer, TakePhotoPayload payload) {
            ByteBufCodecs.BOOL.encode(buffer, payload.offhand());
            IMAGE_CODEC.encode(buffer, payload.png());
        }
    };

    public TakePhotoPayload {
        png = png.clone();
    }

    @Override
    public byte[] png() {
        return png.clone();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TakePhotoPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.isSpectator()) {
                return;
            }
            InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack camera = player.getItemInHand(hand);
            long gameTime = player.level().getGameTime();
            if (!camera.is(ItemRegistry.SHELL_CAMERA.get()) || ShellCameraItem.isCoolingDown(camera, gameTime)) {
                return;
            }
            byte[] image = payload.png();
            boolean authorized = ShellCameraItem.consumeCaptureAuthorization(camera, gameTime);
            if (!authorized || !PhotoImageFormat.validPng(image)) {
                Aquanaut.LOGGER.warn("Rejected unauthorized or malformed shell-camera photo from {}",
                        player.getGameProfile().getName());
                return;
            }

            if (!ShellCameraItem.consumeFilm(player)) {
                player.displayClientMessage(Component.translatable("message.aquanaut.camera.no_film"), true);
                return;
            }

            BlockPos pos = player.blockPosition();
            ItemStack exposedFilm = new ItemStack(ItemRegistry.EXPOSED_FILM.get());
            DevelopedPhotoItem.setPhoto(exposedFilm, image, PhotoImageFormat.WIDTH, PhotoImageFormat.HEIGHT,
                    System.currentTimeMillis(),
                    player.level().dimension().location().toString(), pos.getX(), pos.getY(), pos.getZ(),
                    player.getGameProfile().getName());

            if (!player.getInventory().add(exposedFilm)) {
                player.drop(exposedFilm, false);
            }
            player.displayClientMessage(Component.translatable("message.aquanaut.camera.film_exposed"), true);
            ShellCameraItem.startCooldown(camera, gameTime);
            player.awardStat(Stats.ITEM_USED.get(ItemRegistry.SHELL_CAMERA.get()));
            player.level().playSound(player, player.getX(), player.getY(), player.getZ(),
                    SoundRegistry.CAMERA_SHUTTER.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        });
    }

}
