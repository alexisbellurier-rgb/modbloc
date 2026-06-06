package net.modbloc.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.modbloc.blockentity.CommunityGoalBlockEntity;
import net.modbloc.registry.ModBlocScreenHandlers;

public class CommunityGoalScreenHandler extends ScreenHandler {

    // Property indices
    public static final int PROP_TARGET_AMOUNT   = 0;
    public static final int PROP_CURRENT_AMOUNT  = 1;
    public static final int PROP_IS_SETUP        = 2;
    public static final int PROP_GOAL_REACHED    = 3;
    private static final int PROP_COUNT          = 4;

    // Button IDs (sent via ButtonClickC2SPacket)
    public static final int BUTTON_DEPOSIT  = 0;
    public static final int BUTTON_WITHDRAW = 1;

    // Slot layout constants
    public static final int TARGET_SLOT     = 0;
    public static final int DEPOSIT_START   = 1;   // slots 1-9
    public static final int INV_START       = 10;  // player inventory 10-36
    public static final int HOTBAR_START    = 37;  // player hotbar 37-45

    private final SimpleInventory targetSlot;
    private final SimpleInventory depositInventory;
    private final PropertyDelegate propertyDelegate;
    private final BlockPos blockPos;

    // Server-side reference (null on client)
    private final CommunityGoalBlockEntity blockEntity;

    /** Client-side constructor called by ExtendedScreenHandlerType. */
    public CommunityGoalScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos blockPos) {
        this(syncId, playerInventory,
                new SimpleInventory(1),
                new SimpleInventory(9),
                new ArrayPropertyDelegate(PROP_COUNT),
                blockPos,
                null);
    }

    /** Server-side constructor called from BlockEntity.createMenu. */
    public CommunityGoalScreenHandler(int syncId, PlayerInventory playerInventory,
                                       CommunityGoalBlockEntity be) {
        this(syncId, playerInventory,
                be.getTargetSlot(),
                be.getDepositInventory(),
                buildDelegate(be),
                be.getPos(),
                be);
    }

    private CommunityGoalScreenHandler(int syncId, PlayerInventory playerInventory,
                                        SimpleInventory targetSlot, SimpleInventory depositInventory,
                                        PropertyDelegate delegate, BlockPos blockPos,
                                        CommunityGoalBlockEntity blockEntity) {
        super(ModBlocScreenHandlers.COMMUNITY_GOAL, syncId);
        this.targetSlot = targetSlot;
        this.depositInventory = depositInventory;
        this.propertyDelegate = delegate;
        this.blockPos = blockPos;
        this.blockEntity = blockEntity;

        checkSize(targetSlot, 1);
        checkSize(depositInventory, 9);

        // Slot 0: target item display / setup slot
        addSlot(new TargetItemSlot(targetSlot, 0, 80, 22, this));

        // Slots 1-9: deposit area (3×3 grid)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new DepositSlot(depositInventory, col + row * 3, 44 + col * 18, 58 + row * 18, this));
            }
        }

        // Player inventory (slots 10-36)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 130 + row * 18));
            }
        }

        // Hotbar (slots 37-45)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 188));
        }

        addProperties(delegate);
    }

    // --- Synced properties ---

    public int getTargetAmount()  { return propertyDelegate.get(PROP_TARGET_AMOUNT); }
    public int getCurrentAmount() { return propertyDelegate.get(PROP_CURRENT_AMOUNT); }
    public boolean isSetup()      { return propertyDelegate.get(PROP_IS_SETUP) == 1; }
    public boolean isGoalReached(){ return propertyDelegate.get(PROP_GOAL_REACHED) == 1; }
    public ItemStack getTargetItem() { return slots.get(TARGET_SLOT).getStack(); }
    public BlockPos getBlockPos() { return blockPos; }

    // --- Button handling (server-side) ---

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (blockEntity == null) return false;
        return switch (id) {
            case BUTTON_DEPOSIT  -> { blockEntity.depositItems(player); yield true; }
            case BUTTON_WITHDRAW -> { blockEntity.withdrawItem(player); yield true; }
            default -> false;
        };
    }

    // --- Quick move (shift-click) ---

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return result;

        ItemStack stack = slot.getStack();
        result = stack.copy();

        if (slotIndex < INV_START) {
            // Move from container to player inventory
            if (!insertItem(stack, INV_START, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Move from player inventory to deposit slots (if setup and matching)
            if (isSetup() && !isGoalReached()
                    && stack.isOf(getTargetItem().getItem())) {
                if (!insertItem(stack, DEPOSIT_START, INV_START, false)) return ItemStack.EMPTY;
            } else if (!insertItem(stack, TARGET_SLOT, DEPOSIT_START, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (blockEntity != null) {
            return blockEntity.canPlayerUse(player);
        }
        return true;
    }

    // --- PropertyDelegate builder ---

    private static PropertyDelegate buildDelegate(CommunityGoalBlockEntity be) {
        return new PropertyDelegate() {
            @Override public int get(int index) {
                return switch (index) {
                    case PROP_TARGET_AMOUNT  -> be.getTargetAmount();
                    case PROP_CURRENT_AMOUNT -> be.getCurrentAmount();
                    case PROP_IS_SETUP       -> be.isSetup() ? 1 : 0;
                    case PROP_GOAL_REACHED   -> be.isGoalReached() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {
                // read-only delegate — client syncs values here
            }
            @Override public int size() { return PROP_COUNT; }
        };
    }

    // --- Custom slot classes ---

    private static class TargetItemSlot extends Slot {
        private final CommunityGoalScreenHandler handler;

        TargetItemSlot(SimpleInventory inv, int index, int x, int y, CommunityGoalScreenHandler handler) {
            super(inv, index, x, y);
            this.handler = handler;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            // Only writable by creative before setup is confirmed
            return !handler.isSetup();
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            // Cannot retrieve the target item once setup
            return !handler.isSetup();
        }
    }

    private static class DepositSlot extends Slot {
        private final CommunityGoalScreenHandler handler;

        DepositSlot(SimpleInventory inv, int index, int x, int y, CommunityGoalScreenHandler handler) {
            super(inv, index, x, y);
            this.handler = handler;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            if (!handler.isSetup() || handler.isGoalReached()) return false;
            ItemStack target = handler.getTargetItem();
            return !target.isEmpty() && stack.isOf(target.getItem());
        }
    }
}
