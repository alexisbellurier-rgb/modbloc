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

    // ── Synced properties ────────────────────────────────────────────────────
    public static final int PROP_TARGET_AMOUNT   = 0;
    public static final int PROP_CURRENT_AMOUNT  = 1;
    public static final int PROP_IS_SETUP        = 2;
    public static final int PROP_GOAL_REACHED    = 3;
    public static final int PROP_PRICE_PER_STACK = 4;
    public static final int PROP_PAYMENT_TYPE    = 5;
    private static final int PROP_COUNT          = 6;

    // ── Payment type constants ───────────────────────────────────────────────
    public static final int PAYMENT_FREE          = 0;
    public static final int PAYMENT_COBBLEDOLLARS = 1;
    public static final int PAYMENT_EMERALDS      = 2;

    // ── Button IDs ───────────────────────────────────────────────────────────
    public static final int BUTTON_DEPOSIT  = 0;
    public static final int BUTTON_WITHDRAW = 1;

    // ── Slot indices ─────────────────────────────────────────────────────────
    public static final int TARGET_SLOT   = 0;
    public static final int DEPOSIT_START = 1;

    // ── Layout constants (local to background, 256 px wide) ─────────────────
    public static final int BG_W      = 256;
    // Target slot centred: (256 - 16) / 2 = 120
    public static final int TARGET_SX = 120;
    public static final int TARGET_SY = 34;
    // Deposit / player inventory left edge: (256 - 9*18) / 2 = 47
    public static final int GRID_LEFT = 47;
    public static final int DEP_SY    = 82;
    public static final int INV_SY    = 170;
    public static final int HOTBAR_SY = 226;

    // ── Open data sent server → client ───────────────────────────────────────
    public record OpenData(BlockPos pos, boolean configured) {
        public static final PacketCodec<RegistryByteBuf, OpenData> CODEC = PacketCodec.tuple(
                BlockPos.PACKET_CODEC, OpenData::pos,
                PacketCodecs.BOOL,     OpenData::configured,
                OpenData::new
        );
    }

    // ── Dynamic slot boundaries ──────────────────────────────────────────────
    public final int invStart;
    public final int hotbarStart;

    private final SimpleInventory targetSlot;
    private final SimpleInventory depositInventory;
    private final PropertyDelegate propertyDelegate;
    private final BlockPos blockPos;
    private final @Nullable CommunityGoalBlockEntity blockEntity;
    private final boolean configured;

    /** Client-side constructor (via ExtendedScreenHandlerType). */
    public CommunityGoalScreenHandler(int syncId, PlayerInventory playerInventory, OpenData openData) {
        this(syncId, playerInventory,
                new SimpleInventory(1),
                new SimpleInventory(27),
                new ArrayPropertyDelegate(PROP_COUNT),
                openData.pos(),
                null,
                openData.configured());
    }

    /** Server-side constructor (via BlockEntity.createMenu). */
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
        this.targetSlot      = targetSlot;
        this.depositInventory = depositInventory;
        this.propertyDelegate = delegate;
        this.blockPos        = blockPos;
        this.blockEntity     = blockEntity;
        this.configured      = configured;

        checkSize(targetSlot, 1);

        // Slot 0 — target item (centred in the 256 px wide background)
        addSlot(new TargetItemSlot(targetSlot, 0, TARGET_SX, TARGET_SY, this));

        if (configured) {
            checkSize(depositInventory, 27);
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new DepositSlot(depositInventory, col + row * 9,
                            GRID_LEFT + col * 18, DEP_SY + row * 18, this));
                }
            }
            this.invStart    = 28;
            this.hotbarStart = 55;
        } else {
            this.invStart    = 1;
            this.hotbarStart = 28;
        }

        // Player inventory (3 rows)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        GRID_LEFT + col * 18, INV_SY + row * 18));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, GRID_LEFT + col * 18, HOTBAR_SY));
        }

        addProperties(delegate);
    }

    // ── Synced property accessors ────────────────────────────────────────────

    public int getTargetAmount()   { return propertyDelegate.get(PROP_TARGET_AMOUNT); }
    public int getCurrentAmount()  { return propertyDelegate.get(PROP_CURRENT_AMOUNT); }
    public int getPricePerStack()  { return propertyDelegate.get(PROP_PRICE_PER_STACK); }
    public int getPaymentType()    { return propertyDelegate.get(PROP_PAYMENT_TYPE); }
    public boolean isSetup()       { return propertyDelegate.get(PROP_IS_SETUP) == 1; }
    public boolean isGoalReached() { return propertyDelegate.get(PROP_GOAL_REACHED) == 1; }
    public ItemStack getTargetItem() { return slots.get(TARGET_SLOT).getStack(); }
    public BlockPos getBlockPos()  { return blockPos; }
    public boolean isConfigured()  { return configured; }

    // ── Button handling (server-side) ────────────────────────────────────────

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (blockEntity == null) return false;
        return switch (id) {
            case BUTTON_DEPOSIT  -> { blockEntity.depositItems(player); yield true; }
            case BUTTON_WITHDRAW -> { yield blockEntity.withdrawItem(player); }
            default -> false;
        };
    }

    // ── Quick move ───────────────────────────────────────────────────────────

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasStack()) return result;

        ItemStack stack = slot.getStack();
        result = stack.copy();

        if (slotIndex < invStart) {
            if (!insertItem(stack, invStart, slots.size(), true)) return ItemStack.EMPTY;
        } else {
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

    // ── Property delegate ────────────────────────────────────────────────────

    private static PropertyDelegate buildDelegate(CommunityGoalBlockEntity be) {
        return new PropertyDelegate() {
            @Override public int get(int index) {
                return switch (index) {
                    case PROP_TARGET_AMOUNT   -> be.getTargetAmount();
                    case PROP_CURRENT_AMOUNT  -> be.getCurrentAmount();
                    case PROP_IS_SETUP        -> be.isSetup() ? 1 : 0;
                    case PROP_GOAL_REACHED    -> be.isGoalReached() ? 1 : 0;
                    case PROP_PRICE_PER_STACK -> be.getPricePerStack();
                    case PROP_PAYMENT_TYPE    -> be.getPaymentType();
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int size() { return PROP_COUNT; }
        };
    }

    // ── Custom slots ─────────────────────────────────────────────────────────

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
