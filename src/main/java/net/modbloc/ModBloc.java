package net.modbloc;

import net.fabricmc.api.ModInitializer;
import net.modbloc.network.ModBlocPackets;
import net.modbloc.registry.ModBlocBlockEntities;
import net.modbloc.registry.ModBlocBlocks;
import net.modbloc.registry.ModBlocScreenHandlers;

public class ModBloc implements ModInitializer {

    public static final String MOD_ID = "modbloc";

    @Override
    public void onInitialize() {
        ModBlocBlocks.initialize();
        ModBlocBlockEntities.initialize();
        ModBlocScreenHandlers.initialize();
        ModBlocPackets.registerServerReceivers();
    }
}
