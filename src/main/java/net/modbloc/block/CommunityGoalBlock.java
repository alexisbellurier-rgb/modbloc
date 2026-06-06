package net.modbloc.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.modbloc.blockentity.CommunityGoalBlockEntity;
import net.modbloc.registry.ModBlocBlockEntities;
import org.jetbrains.annotations.Nullable;

public class CommunityGoalBlock extends BlockWithEntity {

    public static final MapCodec<CommunityGoalBlock> CODEC = createCodec(CommunityGoalBlock::new);
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    public CommunityGoalBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CommunityGoalBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                  BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CommunityGoalBlockEntity goalBE) {
                if (!goalBE.isSetup() && !player.isCreative()) {
                    // Bloc non configuré : seul le créatif peut ouvrir le setup
                    player.sendMessage(
                        net.minecraft.text.Text.translatable("gui.modbloc.not_configured"),
                        true // actionbar
                    );
                    return ActionResult.FAIL;
                }
                player.openHandledScreen(goalBE);
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        // Unbreakable in survival — strength(-1) already handles this,
        // but this ensures creative-only breaking via the hardness value.
        return player.isCreative() ? 1.0f : 0.0f;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                              BlockEntityType<T> type) {
        if (!world.isClient) return null;
        if (type == ModBlocBlockEntities.COMMUNITY_GOAL_BLOCK_ENTITY) {
            return (BlockEntityTicker<T>) (BlockEntityTicker<CommunityGoalBlockEntity>) CommunityGoalBlockEntity::clientTick;
        }
        return null;
    }
}
