package net.modbloc;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import net.modbloc.client.render.CommunityGoalBlockEntityRenderer;
import net.modbloc.registry.ModBlocBlockEntities;
import net.modbloc.registry.ModBlocBlocks;
import net.modbloc.registry.ModBlocScreenHandlers;
import net.modbloc.screen.CommunityGoalScreen;

public class ModBlocClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModBlocScreenHandlers.COMMUNITY_GOAL, CommunityGoalScreen::new);

        BlockEntityRendererFactories.register(
                ModBlocBlockEntities.COMMUNITY_GOAL_BLOCK_ENTITY,
                CommunityGoalBlockEntityRenderer::new
        );

        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocBlocks.COMMUNITY_GOAL_BLOCK, RenderLayer.getCutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocBlocks.POKEBALL_GOAL_BLOCK, RenderLayer.getCutout());
    }
}
