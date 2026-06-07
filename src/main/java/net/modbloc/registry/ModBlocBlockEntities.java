package net.modbloc.registry;

import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.modbloc.blockentity.CommunityGoalBlockEntity;

public class ModBlocBlockEntities {

    public static final BlockEntityType<CommunityGoalBlockEntity> COMMUNITY_GOAL_BLOCK_ENTITY =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of("modbloc", "community_goal_block_entity"),
                    BlockEntityType.Builder.create(CommunityGoalBlockEntity::new,
                            ModBlocBlocks.COMMUNITY_GOAL_BLOCK,
                            ModBlocBlocks.POKEBALL_GOAL_BLOCK).build()
            );

    public static void initialize() {}
}
