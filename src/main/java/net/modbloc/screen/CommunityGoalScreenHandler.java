package net.modbloc.screen;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import net.modbloc.blockentity.CommunityGoalBlockEntity;
import net.modbloc.registry.ModBlocScreenHandlers;
import org.jetbrains.annotations.Nullable;

public class CommunityGoalScreenHandler extends ScreenHandler {

    public static final int PROP_TARGET_AMOUNT  = 0;
    public static final int PROP_CURRENT_AMOUNT = 1;
    public static final int PROP_IS_SETUP       = 2;
    public static final int PROP_GOAL_REACHED   = 3;
    public static final int PROP_PRICE_PER_STACK = 4;
    private static final int PROP_COUNT          = 5;

    public static final int BUTTON_DEPOSIT  = 0;
    public static final int BUTTON_WITHDRAW = 1;

    public static final int TARGET_SLOT   = 0;
    public static final int DEPOSIT_START = 1;

    // Sent from server to client when opening the screen.
    public record OpenData(BlockPos pos, boolean configured) {
        public static final PacketCodec<RegistryByteBuf, OpenData> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, OpenData::pos,
                PacketCodecs.BOOL,     OpenData::configured,
                OpenData::new
        );
    }

    // Instance-level slot boundaries (depend on whether deposit slots exist).
    public final int invStart;
    public final int hotbarStart;

    private final SimpleInventory targetSlot;
    private final SimpleInventory depositInventory;
    private final PropertyDelegate propertyDelegate;
    private final BlockPos blockPos;
    private final @Nullable CommunityGoalBlockEntity blockEntity;
    private final boolean configured; // true = play mode (deposit slots present)

    /** Client-side constructor called by ExtendedScreenHandlerType. */
    public CommunityGoalScreenHandler(int syncId, PlayerInventory playerInventory, OpenData openData) {
        this(syncId, playerInventory,
                new SimpleInventory(1),
                new SimpleInventory(27),
                new ArrayPropertyDelegate(PROP_COUNT),
                openData.pos(),
                null,
                openData.configured());
    }

    /** Server-side constructor called from BlockEntity.createMenu. */
    public CommunityGoalScreenHandler(int syncId, PlayerInventory playerInventory,
                                       CommunityGoalBlockEntity be) {
        this(syncId, playerInventory,
                be.getTargetSlot(),
                be.getDepositInventory(),
                buildDelegate(be),
                be.getPos(),
                be,
                be.isSetup());
    }

    private CommunityGoalScreenHandler(int syncId, PlayerInventory playerInventory,
                                        SimpleInventory targetSlot, SimpleInventory depositInventory,
                                        PropertyDelegate delegate, BlockPos blockPos,
                                        @Nullable CommunityGoalBlockEntity blockEntity,
                                        boolean configured) {
        super(ModBlocScreenHandlers.COMMUNITY_GOAL, syncId);
        this.targetSlot = targetSlot;
        this.depositInventory = depositInventory;
        this.propertyDelegate = delegate;
        this.blockPos = blockPos;
        this.blockEntity = blockEntity;
        this.configured = configured;

        checkSize(targetSlot, 1);

        // Slot 0: target item (centered horizontally: 176/2 − 8 = 80)
        addSlot(new TargetItemSlot(targetSlot, 0, 80, 24, this));

        if (configured) {
            // Play mode: add 27 deposit slots (9×3 grid)
            checkSize(depositInventory, 27);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new DepositSlot(depositInventory, col + row * 9,
                            8 + col * 18, 76 + row * 18, this));
                }
            }
            this.invStart    = 28;
            this.hotbarStart = 55;
        } else {
            // Setup mode: no deposit slots
            this.invStart    = 1;
            this.hotbarStart = 28;
        }

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 166 + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 222));
        }

        addProperties(delegate);
    }

    // --- Synced properties ---

    public int getTargetAmount()   { return propertyDelegate.get(PROP_TARGET_AMOUNT); }
    public int getCurrentAmount()  { return propertyDelegate.get(PROP_CURRENT_AMOUNT); }
    public int getPricePerStack()  { return propertyDelegate.get(PROP_PRICE_PER_STACK); }
    public boolean isSetup()       { return propertyDelegate.get(PROP_IS_SETUP) == 1; }
    public boolean isGoalReached() { return propertyDelegate.get(PROP_GOAL_REACHED) == 1; }
    public ItemStack getTargetItem() { return slots.get(TARGET_SLOT).getStack(); }
    public BlockPos getBlockPos()  { return blockPos; }
    public boolean isConfigured()  { return configured; }

    // --- Button handling (server-side) ---

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (blockEntity == null) return false;
        return switch (id) {
            case BUTTON_DEPOSIT  -> { blockEntity.depositItems(player); yield true; }
            case BUTTON_WITHDRAW -> { yield blockEntity.withdrawItem(player); }
            default -> false;
        };
    }

    // --- Quick move ---

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return result;

        ItemStack stack = slot.getStack();
        result = stack.copy();

        if (slotIndex < invStart) {
            // Target or deposit → player inventory
            if (!insertItem(stack, invStart, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Player inventory / hotbar → deposit or target
            if (configured && isSetup() && !isGoalReached()
                    && !getTargetItem().isEmpty() && stack.isOf(getTargetItem().getItem())) {
                if (!insertItem(stack, DEPOSIT_START, invStart, false)) return ItemStack.EMPTY;
            } else if (!insertItem(stack, TARGET_SLOT, TARGET_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        return result;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return blockEntity == null || blockEntity.canPlayerUse(player);
    }

    // --- PropertyDelegate ---

    private static PropertyDelegate buildDelegate(CommunityGoalBlockEntity be) {
        return new PropertyDelegate() {
            @Override public int get(int index) {
                return switch (index) {
                    case PROP_TARGET_AMOUNT  -> be.getTargetAmount();
                    case PROP_CURRENT_AMOUNT -> be.getCurrentAmount();
                    case PROP_IS_SETUP       -> be.isSetup() ? 1 : 0;
                    case PROP_GOAL_REACHED   -> be.isGoalReached() ? 1 : 0;
                    case PROP_PRICE_PER_STACK -> be.getPricePerStack();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int size() { return PROP_COUNT; }
        };
    }

    // --- Custom slots ---

    private static class TargetItemSlot extends Slot {
        private final CommunityGoalScreenHandler handler;
        TargetItemSlot(SimpleInventory inv, int index, int x, int y, CommunityGoalScreenHandler h) {
            super(inv, index, x, y);
            this.handler = h;
        }
        @Override public boolean canInsert(ItemStack stack)        { return !handler.isSetup(); }
        @Override public boolean canTakeItems(PlayerEntity player) { return !handler.isSetup(); }
    }

    private static class DepositSlot extends Slot {
        private final CommunityGoalScreenHandler handler;
        DepositSlot(SimpleInventory inv, int index, int x, int y, CommunityGoalScreenHandler h) {
            super(inv, index, x, y);
            this.handler = h;
        }
        @Override public boolean canInsert(ItemStack stack) {
            if (!handler.isSetup() || handler.isGoalReached()) return false;
            ItemStack target = handler.getTargetItem();
            return !target.isEmpty() && stack.isOf(target.getItem());
        }
    }
}
