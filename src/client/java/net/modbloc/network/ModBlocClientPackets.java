package net.modbloc.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.BlockPos;

@Environment(EnvType.CLIENT)
public class ModBlocClientPackets {

    public static void sendSetupPacket(BlockPos pos, int amount, int price, int paymentType, String functionName) {
        ClientPlayNetworking.send(new ModBlocPackets.SetupPayload(pos, amount, price, paymentType, functionName));
    }
}
