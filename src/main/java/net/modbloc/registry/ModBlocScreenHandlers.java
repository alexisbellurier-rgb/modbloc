package net.modbloc.registry;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.modbloc.screen.CommunityGoalScreenHandler;

public class ModBlocScreenHandlers {

    public static final ScreenHandlerType<CommunityGoalScreenHandler> COMMUNITY_GOAL =
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    Identifier.of("modbloc", "community_goal"),
                    new ExtendedScreenHandlerType<>(CommunityGoalScreenHandler::new, BlockPos.PACKET_CODEC)
            );

    public static void initialize() {}
}
