package net.modbloc.compat;

import fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.math.BigInteger;

/**
 * Wrapper around the CobbleDollars API. Only load this class when
 * FabricLoader.getInstance().isModLoaded("cobbledollars") is true.
 */
public final class CobbleDollarsCompat {

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("cobbledollars");
    }

    public static boolean canAfford(PlayerEntity player, int price) {
        return PlayerExtensionKt.canBuy(player, BigInteger.valueOf(price));
    }

    public static void charge(PlayerEntity player, int price) {
        BigInteger p = BigInteger.valueOf(price);
        PlayerExtensionKt.setCobbleDollars(player, PlayerExtensionKt.getCobbleDollars(player).subtract(p));
        if (player instanceof ServerPlayerEntity sp) {
            PlayerExtensionKt.updateCobbleDollarsAccount(sp);
        }
    }
}
