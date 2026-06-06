package net.modbloc.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.modbloc.block.CommunityGoalBlock;

public class ModBlocBlocks {

    public static final Block COMMUNITY_GOAL_BLOCK = register(
            "community_goal_block",
            new CommunityGoalBlock(AbstractBlock.Settings.create().strength(-1, 3600000.0f).luminance(state -> 3))
    );

    private static Block register(String name, Block block) {
        Identifier id = Identifier.of("modbloc", name);
        BlockItem blockItem = new BlockItem(block, new Item.Settings());
        Registry.register(Registries.ITEM, id, blockItem);
        return Registry.register(Registries.BLOCK, id, block);
    }

    public static void initialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries ->
                entries.add(COMMUNITY_GOAL_BLOCK));
    }
}
