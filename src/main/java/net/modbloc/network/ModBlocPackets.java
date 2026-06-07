package net.modbloc.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.Block;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.modbloc.blockentity.CommunityGoalBlockEntity;
import net.modbloc.screen.CommunityGoalScreenHandler;

public class ModBlocPackets {

    // -----------------------------------------------------------------------
    // C2S: Setup payload — sent when a creative player confirms the block goal
    // -----------------------------------------------------------------------

    public record SetupPayload(
            BlockPos pos,
            int targetAmount,
            int pricePerStack,
            int paymentType,
            String functionName
    ) implements CustomPayload {

        public static final Id<SetupPayload> ID = new Id<>(net.minecraft.util.Identifier.of("modbloc", "setup"));

        public static final PacketCodec<RegistryByteBuf, SetupPayload> CODEC = new PacketCodec<>() {
            @Override
            public SetupPayload decode(RegistryByteBuf buf) {
                BlockPos pos        = BlockPos.PACKET_CODEC.decode(buf);
                int amount          = buf.readVarInt();
                int price           = buf.readVarInt();
                int payment         = buf.readVarInt();
                String functionName = buf.readString();
                return new SetupPayload(pos, amount, price, payment, functionName);
            }

            @Override
            public void encode(RegistryByteBuf buf, SetupPayload value) {
                BlockPos.PACKET_CODEC.encode(buf, value.pos());
                buf.writeVarInt(value.targetAmount());
                buf.writeVarInt(value.pricePerStack());
                buf.writeVarInt(value.paymentType());
                buf.writeString(value.functionName());
            }
        };

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

                be.setup(targetItem, amount, payload.pricePerStack(), payload.paymentType(), payload.functionName());
                world.updateListeners(pos, world.getBlockState(pos), world.getBlockState(pos), Block.NOTIFY_ALL);
                player.closeHandledScreen();
            });
        });
    }

}
