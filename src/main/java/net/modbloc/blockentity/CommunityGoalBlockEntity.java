package net.modbloc.blockentity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.modbloc.registry.ModBlocBlockEntities;
import net.modbloc.screen.CommunityGoalScreenHandler;
import org.jetbrains.annotations.Nullable;

public class CommunityGoalBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<BlockPos> {

    // The target item is stored in this 1-slot inventory (slot syncs automatically via screen handler).
    private final SimpleInventory targetSlot = new SimpleInventory(1) {
        @Override public void markDirty() { CommunityGoalBlockEntity.this.markDirty(); }
    };

    // 9 deposit slots — items placed here are consumed on deposit action.
    private final SimpleInventory depositInventory = new SimpleInventory(9) {
        @Override public void markDirty() { CommunityGoalBlockEntity.this.markDirty(); }
    };

    private int targetAmount = 100;
    private int currentAmount = 0;
    private boolean isSetup = false;

    // Used by the client-side renderer for item rotation animation.
    public float renderAngle = 0f;

    public CommunityGoalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocBlockEntities.COMMUNITY_GOAL_BLOCK_ENTITY, pos, state);
    }

    // --- Tick ---

    public static void clientTick(World world, BlockPos pos, BlockState state, CommunityGoalBlockEntity be) {
        be.renderAngle = (be.renderAngle + 1.5f) % 360f;
    }

    // --- Accessors ---

    public SimpleInventory getTargetSlot() { return targetSlot; }
    public SimpleInventory getDepositInventory() { return depositInventory; }

    public ItemStack getTargetItem() { return targetSlot.getStack(0); }
    public int getTargetAmount() { return targetAmount; }
    public int getCurrentAmount() { return currentAmount; }
    public boolean isSetup() { return isSetup; }
    public boolean isGoalReached() { return currentAmount >= targetAmount; }

    // --- Setup ---

    public void setup(ItemStack targetItem, int amount) {
        targetSlot.setStack(0, targetItem.copyWithCount(1));
        this.targetAmount = Math.max(1, amount);
        this.currentAmount = 0;
        this.isSetup = true;
        markDirty();
    }

    // --- Deposit ---

    /** Called server-side from the screen handler when player clicks Deposit. */
    public void depositItems(PlayerEntity player) {
        if (!isSetup || isGoalReached()) return;
        ItemStack target = getTargetItem();
        if (target.isEmpty()) return;

        for (int i = 0; i < depositInventory.size(); i++) {
            ItemStack stack = depositInventory.getStack(i);
            if (stack.isEmpty()) continue;
            if (stack.isOf(target.getItem())) {
                int add = Math.min(stack.getCount(), targetAmount - currentAmount);
                currentAmount += add;
                stack.decrement(add);
                if (stack.isEmpty()) depositInventory.setStack(i, ItemStack.EMPTY);
            } else {
                // Return non-matching items to player
                player.giveItemStack(stack.copy());
                depositInventory.setStack(i, ItemStack.EMPTY);
            }
        }
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);
    }

    /** Called server-side when player clicks Withdraw (goal reached). */
    public void withdrawItem(PlayerEntity player) {
        if (!isGoalReached()) return;
        ItemStack gift = getTargetItem().copyWithCount(getTargetItem().getMaxCount());
        player.giveItemStack(gift);
    }

    // --- NBT ---

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("TargetAmount", targetAmount);
        nbt.putInt("CurrentAmount", currentAmount);
        nbt.putBoolean("IsSetup", isSetup);

        if (!targetSlot.getStack(0).isEmpty()) {
            nbt.put("TargetItem", targetSlot.getStack(0).encode(registries));
        }

        NbtCompound depositNbt = new NbtCompound();
        for (int i = 0; i < depositInventory.size(); i++) {
            ItemStack s = depositInventory.getStack(i);
            if (!s.isEmpty()) {
                depositNbt.put(String.valueOf(i), s.encode(registries));
            }
        }
        nbt.put("DepositSlots", depositNbt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        targetAmount = nbt.getInt("TargetAmount");
        currentAmount = nbt.getInt("CurrentAmount");
        isSetup = nbt.getBoolean("IsSetup");

        if (nbt.contains("TargetItem", NbtElement.COMPOUND_TYPE)) {
            ItemStack.fromNbt(registries, nbt.getCompound("TargetItem"))
                    .ifPresent(s -> targetSlot.setStack(0, s));
        }

        NbtCompound depositNbt = nbt.getCompound("DepositSlots");
        for (int i = 0; i < depositInventory.size(); i++) {
            String key = String.valueOf(i);
            if (depositNbt.contains(key, NbtElement.COMPOUND_TYPE)) {
                final int slotIndex = i;
                ItemStack.fromNbt(registries, depositNbt.getCompound(key))
                        .ifPresent(s -> depositInventory.setStack(slotIndex, s));
            }
        }
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    }

    public boolean canPlayerUse(PlayerEntity player) {
        if (world == null || world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    // --- Screen ---

    @Override
    public BlockPos getScreenOpeningData(ServerPlayerEntity player) {
        return this.pos;
    }

    @Override
    public @Nullable ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CommunityGoalScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.modbloc.community_goal_block");
    }
}
