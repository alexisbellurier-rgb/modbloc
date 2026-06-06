package net.modbloc.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

public class ModBlocClientPackets {

    public static void sendSetupPacket(BlockPos pos, int amount) {
        ClientPlayNetworking.send(new ModBlocPackets.SetupPayload(pos, amount));
    }
}
