package net.modbloc.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.BlockWithEntity;

public class PokeBallGoalBlock extends CommunityGoalBlock {

    public static final MapCodec<PokeBallGoalBlock> CODEC = createCodec(PokeBallGoalBlock::new);

    public PokeBallGoalBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }
}
