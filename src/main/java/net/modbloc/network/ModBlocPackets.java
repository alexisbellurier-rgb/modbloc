package net.modbloc.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.modbloc.blockentity.CommunityGoalBlockEntity;
import net.modbloc.screen.CommunityGoalScreenHandler;

public class ModBlocPackets {

    // -----------------------------------------------------------------------
    // C2S: Setup payload — sent when a creative player confirms the block goal
    // -----------------------------------------------------------------------

    public record SetupPayload(BlockPos pos, int targetAmount) implements CustomPayload {

        public static final Id<SetupPayload> ID = new Id<>(net.minecraft.util.Identifier.of("modbloc", "setup"));

        public static final PacketCodec<RegistryByteBuf, SetupPayload> CODEC =
                PacketCodec.tuple(
                        BlockPos.PACKET_CODEC, SetupPayload::pos,
                        PacketCodecs.VAR_INT,   SetupPayload::targetAmount,
                        SetupPayload::new
                );

        @Override
        public Id<? extends CustomPayload> getId() { return ID; }
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------

    public static void registerServerReceivers() {
        PayloadTypeRegistry.playC2S().register(SetupPayload.ID, SetupPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetupPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            if (!player.isCreative()) return;

            context.server().execute(() -> {
                var world = player.getServerWorld();
                BlockPos pos = payload.pos();
                if (!world.isChunkLoaded(pos)) return;
                if (!(world.getBlockEntity(pos) instanceof CommunityGoalBlockEntity be)) return;
                if (be.isSetup()) return;
                if (!(player.currentScreenHandler instanceof CommunityGoalScreenHandler handler)) return;

                var targetItem = handler.getSlot(CommunityGoalScreenHandler.TARGET_SLOT).getStack();
                if (targetItem.isEmpty()) return;

                int amount = payload.targetAmount();
                if (amount <= 0) return;

                be.setup(targetItem, amount);
                world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_ALL);
                // Close setup screen — player reopens the block to get the play-mode screen
                player.closeHandledScreen();
            });
        });
    }

}
