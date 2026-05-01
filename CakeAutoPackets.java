package com.cakeauto;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class CakeAutoPackets {

    // Server → Client: open GUI
    public static final CustomPayload.Id<OpenGuiPayload> OPEN_GUI_ID =
            new CustomPayload.Id<>(Identifier.of(CakeAutoMod.MOD_ID, "open_gui"));

    // Client → Server: toggle auto-craft
    public static final CustomPayload.Id<ToggleAutoPayload> TOGGLE_AUTO_ID =
            new CustomPayload.Id<>(Identifier.of(CakeAutoMod.MOD_ID, "toggle_auto"));

    // Client → Server: request auto-find-and-open crafting table
    public static final CustomPayload.Id<AutoOpenTablePayload> AUTO_OPEN_TABLE_ID =
            new CustomPayload.Id<>(Identifier.of(CakeAutoMod.MOD_ID, "auto_open_table"));

    // ── Payload records ──────────────────────────────────────────────────────

    public record OpenGuiPayload(boolean autoEnabled, int searchRadius) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, OpenGuiPayload> CODEC =
                PacketCodec.of(
                        (value, buf) -> { buf.writeBoolean(value.autoEnabled()); buf.writeInt(value.searchRadius()); },
                        buf -> new OpenGuiPayload(buf.readBoolean(), buf.readInt())
                );
        @Override public Id<OpenGuiPayload> getId() { return OPEN_GUI_ID; }
    }

    public record ToggleAutoPayload(boolean enabled) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, ToggleAutoPayload> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeBoolean(value.enabled()),
                        buf -> new ToggleAutoPayload(buf.readBoolean())
                );
        @Override public Id<ToggleAutoPayload> getId() { return TOGGLE_AUTO_ID; }
    }

    public record AutoOpenTablePayload(int radius) implements CustomPayload {
        public static final PacketCodec<PacketByteBuf, AutoOpenTablePayload> CODEC =
                PacketCodec.of(
                        (value, buf) -> buf.writeInt(value.radius()),
                        buf -> new AutoOpenTablePayload(buf.readInt())
                );
        @Override public Id<AutoOpenTablePayload> getId() { return AUTO_OPEN_TABLE_ID; }
    }

    // ── Registration ─────────────────────────────────────────────────────────

    public static void registerServerPackets() {
        PayloadTypeRegistry.playS2C().register(OPEN_GUI_ID, OpenGuiPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(TOGGLE_AUTO_ID, ToggleAutoPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AUTO_OPEN_TABLE_ID, AutoOpenTablePayload.CODEC);

        // Toggle auto handler
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_AUTO_ID, (payload, context) -> {
            context.server().execute(() -> {
                CakeAutoState.setAutoEnabled(context.player(), payload.enabled());
                CakeAutoMod.LOGGER.info("Player {} auto-craft: {}", context.player().getName().getString(), payload.enabled());
            });
        });

        // Auto-open crafting table handler: find nearest table and open it
        ServerPlayNetworking.registerGlobalReceiver(AUTO_OPEN_TABLE_ID, (payload, context) -> {
            context.server().execute(() -> {
                var player = context.player();
                int radius = payload.radius();
                var world = player.getWorld();
                var playerPos = player.getBlockPos();

                // Search in a cube around the player
                net.minecraft.util.math.BlockPos nearest = null;
                double bestDist = Double.MAX_VALUE;

                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            var pos = playerPos.add(dx, dy, dz);
                            var state = world.getBlockState(pos);
                            if (state.isOf(net.minecraft.block.Blocks.CRAFTING_TABLE)) {
                                double dist = playerPos.getSquaredDistance(pos);
                                if (dist < bestDist) {
                                    bestDist = dist;
                                    nearest = pos;
                                }
                            }
                        }
                    }
                }

                if (nearest != null) {
                    var tableBlock = world.getBlockState(nearest);
                    // Open the crafting table screen for the player
                    tableBlock.onUse(world, player, new net.minecraft.util.hit.BlockHitResult(
                            net.minecraft.util.math.Vec3d.ofCenter(nearest),
                            net.minecraft.util.math.Direction.UP,
                            nearest, false));
                } else {
                    player.sendMessage(
                            net.minecraft.text.Text.literal("§cВерстак не найден в радиусе " + radius + " блоков!"),
                            true);
                }
            });
        });
    }
}
