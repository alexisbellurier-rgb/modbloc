package net.modbloc.blockentity;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.modbloc.compat.CobbleDollarsCompat;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.modbloc.screen.CommunityGoalScreenHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.modbloc.registry.ModBlocBlockEntities;
import org.jetbrains.annotations.Nullable;


public class CommunityGoalBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory<CommunityGoalScreenHandler.OpenData> {

    private final SimpleInventory targetSlot = new SimpleInventory(1) {
        @Override public void markDirty() { CommunityGoalBlockEntity.this.markDirty(); }
    };

    private final SimpleInventory depositInventory = new SimpleInventory(27) {
        @Override public void markDirty() { CommunityGoalBlockEntity.this.markDirty(); }
    };

    private int targetAmount = 100;
    private int currentAmount = 0;
    private int pricePerStack = 0;
    private int paymentType = CommunityGoalScreenHandler.PAYMENT_FREE;
    private boolean isSetup = false;
    private String functionName = "";

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
    public int getPricePerStack() { return pricePerStack; }
    public int getPaymentType()   { return paymentType; }
    public boolean isSetup() { return isSetup; }
    public boolean isGoalReached() { return currentAmount >= targetAmount; }

    // --- Setup ---

    public void setup(ItemStack targetItem, int amount, int price, int payment, String function) {
        targetSlot.setStack(0, targetItem.copyWithCount(1));
        this.targetAmount  = Math.max(1, amount);
        this.pricePerStack = Math.max(0, price);
        this.paymentType   = payment;
        this.functionName  = function == null ? "" : function.trim();
        this.currentAmount = 0;
        this.isSetup       = true;
        markDirty();
    }

    // --- Deposit ---

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
                player.giveItemStack(stack.copy());
                depositInventory.setStack(i, ItemStack.EMPTY);
            }
        }
        markDirty();
        if (world != null) world.updateListeners(pos, getCachedState(), getCachedState(), 3);

        // Execute the configured Minecraft function the moment the goal is first reached
        if (isGoalReached() && !functionName.isEmpty() && world instanceof ServerWorld sw) {
            try {
                var server = sw.getServer();
                var source = server.getCommandSource().withLevel(4).withSilent();
                server.getCommandManager().executeWithPrefix(source, "function " + functionName);
            } catch (Exception ignored) {}
        }
    }

    public boolean withdrawItem(PlayerEntity player) {
        if (!isGoalReached()) return false;
        if (pricePerStack > 0) {
            switch (paymentType) {
                case CommunityGoalScreenHandler.PAYMENT_COBBLEDOLLARS -> {
                    if (CobbleDollarsCompat.isLoaded()) {
                        if (!CobbleDollarsCompat.canAfford(player, pricePerStack)) {
                            player.sendMessage(Text.literal("Solde insuffisant ! Prix : " + pricePerStack + " CCCoins"), true);
                            return false;
                        }
                        CobbleDollarsCompat.charge(player, pricePerStack);
                    }
                }
                case CommunityGoalScreenHandler.PAYMENT_EMERALDS -> {
                    if (countEmeralds(player) < pricePerStack) {
                        player.sendMessage(Text.literal("Pas assez d'émeraudes ! Prix : " + pricePerStack), true);
                        return false;
                    }
                    removeEmeralds(player, pricePerStack);
                }
            }
        }
        player.giveItemStack(getTargetItem().copyWithCount(getTargetItem().getMaxCount()));
        return true;
    }

    private static int countEmeralds(PlayerEntity player) {
        int count = 0;
        for (int i = 0; i < player.getInventory().main.size(); i++) {
            ItemStack s = player.getInventory().main.get(i);
            if (s.isOf(Items.EMERALD)) count += s.getCount();
        }
        return count;
    }

    private static void removeEmeralds(PlayerEntity player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().main.size() && remaining > 0; i++) {
            ItemStack s = player.getInventory().main.get(i);
            if (s.isOf(Items.EMERALD)) {
                int take = Math.min(s.getCount(), remaining);
                s.decrement(take);
                remaining -= take;
            }
        }
    }

    // --- NBT ---

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
        nbt.putInt("TargetAmount",  targetAmount);
        nbt.putInt("CurrentAmount", currentAmount);
        nbt.putInt("PricePerStack", pricePerStack);
        nbt.putInt("PaymentType",   paymentType);
        nbt.putBoolean("IsSetup",   isSetup);
        nbt.putString("FunctionName", functionName);

        if (!targetSlot.getStack(0).isEmpty()) {
            nbt.put("TargetItem", targetSlot.getStack(0).encode(registries));
        }

        NbtCompound depositNbt = new NbtCompound();
        for (int i = 0; i < depositInventory.size(); i++) {
            ItemStack s = depositInventory.getStack(i);
            if (!s.isEmpty()) depositNbt.put(String.valueOf(i), s.encode(registries));
        }
        nbt.put("DepositSlots", depositNbt);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
        targetAmount  = nbt.getInt("TargetAmount");
        currentAmount = nbt.getInt("CurrentAmount");
        pricePerStack = nbt.getInt("PricePerStack");
        paymentType   = nbt.getInt("PaymentType");
        isSetup       = nbt.getBoolean("IsSetup");
        functionName  = nbt.contains("FunctionName") ? nbt.getString("FunctionName") : "";

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
    public CommunityGoalScreenHandler.OpenData getScreenOpeningData(ServerPlayerEntity player) {
        return new CommunityGoalScreenHandler.OpenData(this.pos, this.isSetup, this.isGoalReached());
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
