package net.modbloc.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.modbloc.network.ModBlocClientPackets;

public class CommunityGoalScreen extends HandledScreen<CommunityGoalScreenHandler> {

    // ── Dimensions ───────────────────────────────────────────────────────────
    private static final int BG_W     = CommunityGoalScreenHandler.BG_W;   // 256
    private static final int BG_H     = 256;
    private static final int BAR_W    = 180;
    private static final int BAR_H    = 10;
    private static final int MAX_TW   = 210;

    // Slot grid positions (mirror the handler)
    private static final int TARGET_X = CommunityGoalScreenHandler.TARGET_SX; // 120
    private static final int TARGET_Y = CommunityGoalScreenHandler.TARGET_SY; // 34
    private static final int DEP_X    = CommunityGoalScreenHandler.GRID_LEFT;  // 47
    private static final int DEP_Y    = CommunityGoalScreenHandler.DEP_SY;     // 82
    private static final int INV_X    = CommunityGoalScreenHandler.GRID_LEFT;  // 47
    private static final int INV_Y    = CommunityGoalScreenHandler.INV_SY;     // 170
    private static final int HOTBAR_Y = CommunityGoalScreenHandler.HOTBAR_SY;  // 226

    // ── Colours ──────────────────────────────────────────────────────────────
    private static final int C_BG         = 0xFF1A1E28;
    private static final int C_HEADER     = 0xFF12151F;
    private static final int C_FOOTER     = 0xFF13161F;
    private static final int C_SEPARATOR  = 0xFF2E3548;
    private static final int C_SLOT_FILL  = 0xFF0F1218;
    private static final int C_SLOT_EDGE  = 0xFF2A3040;
    private static final int C_TEXT       = 0xFFCDD2DE;
    private static final int C_MUTED      = 0xFF6878A0;
    private static final int C_GREEN      = 0xFF44BB66;
    private static final int C_YELLOW     = 0xFFDDA830;
    private static final int C_BAR_TRACK  = 0xFF0C1018;
    private static final int C_BAR_BORDER = 0xFF1E2638;

    // ── Widgets ──────────────────────────────────────────────────────────────
    private TextFieldWidget amountField;
    private TextFieldWidget priceField;
    private ButtonWidget    paymentCycleButton;
    private ButtonWidget    confirmButton;
    private ButtonWidget    depositButton;
    private ButtonWidget    withdrawButton;

    private int selectedPaymentType = CommunityGoalScreenHandler.PAYMENT_FREE;

    public CommunityGoalScreen(CommunityGoalScreenHandler handler,
                                PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth  = BG_W;
        this.backgroundHeight = BG_H;
    }

    @Override
    protected void init() {
        super.init();
        this.playerInventoryTitleY = 160;

        int cx = x + BG_W / 2;

        // ── Setup widgets — labels are drawn to the left in drawForeground ─────
        // Field/button start at local x=128 (screen x+128)
        amountField = new TextFieldWidget(textRenderer, x + 128, y + 62, 90, 12, Text.empty());
        amountField.setMaxLength(9);
        amountField.setText("1000");
        amountField.setVisible(false);
        addDrawableChild(amountField);

        paymentCycleButton = ButtonWidget.builder(Text.empty(), btn ->
                selectedPaymentType = (selectedPaymentType + 1) % 3
        ).dimensions(x + 128, y + 82, 110, 16).build();
        paymentCycleButton.visible = false;
        addDrawableChild(paymentCycleButton);

        priceField = new TextFieldWidget(textRenderer, x + 128, y + 102, 90, 12, Text.empty());
        priceField.setMaxLength(9);
        priceField.setText("0");
        priceField.setVisible(false);
        addDrawableChild(priceField);

        confirmButton = ButtonWidget.builder(
                Text.translatable("gui.modbloc.confirm"), btn -> sendConfirm()
        ).dimensions(cx - 60, y + 124, 120, 16).build();
        confirmButton.visible = false;
        addDrawableChild(confirmButton);

        // ── Play-mode widgets ─────────────────────────────────────────────────
        depositButton = ButtonWidget.builder(
                Text.translatable("gui.modbloc.deposit"), btn -> sendDeposit()
        ).dimensions(cx - 60, y + 140, 120, 16).build();
        depositButton.visible = false;
        addDrawableChild(depositButton);

        // ── Goal-reached widgets ──────────────────────────────────────────────
        withdrawButton = ButtonWidget.builder(
                Text.translatable("gui.modbloc.withdraw_free"), btn -> sendWithdraw()
        ).dimensions(cx - 70, y + 116, 140, 16).build();
        withdrawButton.visible = false;
        addDrawableChild(withdrawButton);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void sendConfirm() {
        int amount;
        try { amount = Integer.parseInt(amountField.getText().trim()); }
        catch (NumberFormatException e) { return; }
        if (amount <= 0) return;

        int price = 0;
        if (selectedPaymentType != CommunityGoalScreenHandler.PAYMENT_FREE) {
            try { price = Math.max(0, Integer.parseInt(priceField.getText().trim())); }
            catch (NumberFormatException ignored) {}
        }
        ModBlocClientPackets.sendSetupPacket(handler.getBlockPos(), amount, price, selectedPaymentType);
    }

    private void sendDeposit() {
        client.interactionManager.clickButton(handler.syncId, CommunityGoalScreenHandler.BUTTON_DEPOSIT);
    }

    private void sendWithdraw() {
        client.interactionManager.clickButton(handler.syncId, CommunityGoalScreenHandler.BUTTON_WITHDRAW);
    }

    // ── Background ────────────────────────────────────────────────────────────

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        ctx.fill(x, y, x + BG_W, y + BG_H, C_BG);

        // Header bar
        ctx.fill(x, y, x + BG_W, y + 22, C_HEADER);
        ctx.fill(x, y + 21, x + BG_W, y + 22, C_SEPARATOR);

        // Footer / player-inventory section
        ctx.fill(x, y + 154, x + BG_W, y + BG_H, C_FOOTER);
        ctx.fill(x, y + 154, x + BG_W, y + 155, C_SEPARATOR);

        // Target slot
        drawSlot(ctx, x + TARGET_X, y + TARGET_Y);

        // Deposit grid — only while playing and goal not yet reached
        if (handler.isConfigured() && !handler.isGoalReached()) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    drawSlot(ctx, x + DEP_X + col * 18, y + DEP_Y + row * 18);
                }
            }
        }

        // Player inventory (3 rows + hotbar)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(ctx, x + INV_X + col * 18, y + INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(ctx, x + INV_X + col * 18, y + HOTBAR_Y);
        }
    }

    private void drawSlot(DrawContext ctx, int sx, int sy) {
        ctx.fill(sx - 1, sy - 1, sx + 17, sy + 17, C_SLOT_EDGE);
        ctx.fill(sx,     sy,     sx + 16, sy + 16, C_SLOT_FILL);
    }

    // ── Foreground ────────────────────────────────────────────────────────────

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        boolean configured  = handler.isConfigured();
        boolean goalReached = handler.isGoalReached();
        int target          = handler.getTargetAmount();
        int current         = handler.getCurrentAmount();
        int price           = handler.getPricePerStack();
        int paymentType     = handler.getPaymentType();
        int cx              = BG_W / 2;

        boolean showPrice = selectedPaymentType != CommunityGoalScreenHandler.PAYMENT_FREE;

        // ── Widget visibility ─────────────────────────────────────────────────
        amountField.setVisible(!configured);
        paymentCycleButton.visible = !configured;
        priceField.setVisible(!configured && showPrice);
        confirmButton.visible  = !configured;
        depositButton.visible  =  configured && !goalReached;
        withdrawButton.visible =  goalReached;

        if (!configured) paymentCycleButton.setMessage(paymentTypeText(selectedPaymentType));

        // Withdraw button label depends on whether there is a cost
        boolean hasCost = price > 0 && paymentType != CommunityGoalScreenHandler.PAYMENT_FREE;
        withdrawButton.setMessage(Text.translatable(
                hasCost ? "gui.modbloc.withdraw_paid" : "gui.modbloc.withdraw_free"));

        // ── Title (header) ────────────────────────────────────────────────────
        ctx.drawCenteredTextWithShadow(textRenderer,
                fit(title.getString(), BG_W - 24), cx, 8, C_TEXT);

        // ── Player inventory label (footer) ───────────────────────────────────
        String invLabel = playerInventoryTitle.getString();
        ctx.drawText(textRenderer, invLabel,
                cx - textRenderer.getWidth(invLabel) / 2,
                playerInventoryTitleY, C_MUTED, false);

        // ── Mode-specific content ─────────────────────────────────────────────
        if (!configured) {
            drawSetupContent(ctx, showPrice);
        } else if (!goalReached) {
            drawPlayContent(ctx, cx, current, target);
        } else {
            drawGoalReachedContent(ctx, cx, price, paymentType);
        }
    }

    // ── Setup content ─────────────────────────────────────────────────────────
    // Labels are drawn inline to the left of each widget (local x=18).
    // Widgets start at local x=128 (screen x+128).

    private void drawSetupContent(DrawContext ctx, boolean showPrice) {
        separator(ctx, 56);

        // "Quantité :" — centered with the 12px field at local y=62
        ctx.drawText(textRenderer,
                fit(Text.translatable("gui.modbloc.amount").getString(), 104),
                18, 64, C_MUTED, false);

        // "Monnaie :" — centered with the 16px button at local y=82
        ctx.drawText(textRenderer,
                fit(Text.translatable("gui.modbloc.payment_label").getString(), 104),
                18, 87, C_MUTED, false);

        // "Prix par retrait :" — centered with the 12px field at local y=102
        if (showPrice) {
            ctx.drawText(textRenderer,
                    fit(Text.translatable("gui.modbloc.price").getString(), 104),
                    18, 104, C_MUTED, false);
        }

        separator(ctx, 118);
    }

    // ── Play content (deposit phase) ──────────────────────────────────────────

    private void drawPlayContent(DrawContext ctx, int cx, int current, int target) {
        separator(ctx, 56);

        // Progress bar just below the separator, then counter
        drawBar(ctx, cx, 60, current, target, false);
        ctx.drawCenteredTextWithShadow(textRenderer,
                fit(current + " / " + target, MAX_TW), cx, 74, C_MUTED);
    }

    // ── Goal-reached content ──────────────────────────────────────────────────

    private void drawGoalReachedContent(DrawContext ctx, int cx, int price, int paymentType) {
        separator(ctx, 56);

        ctx.drawCenteredTextWithShadow(textRenderer,
                fit(Text.translatable("gui.modbloc.goal_complete").getString(), MAX_TW),
                cx, 72, C_GREEN);

        if (price > 0 && paymentType != CommunityGoalScreenHandler.PAYMENT_FREE) {
            String priceStr = switch (paymentType) {
                case CommunityGoalScreenHandler.PAYMENT_COBBLEDOLLARS ->
                        fit(Text.translatable("gui.modbloc.price_display_dollars",
                                String.format("%,d", price)).getString(), MAX_TW);
                case CommunityGoalScreenHandler.PAYMENT_EMERALDS ->
                        fit(Text.translatable("gui.modbloc.price_display_emeralds",
                                price).getString(), MAX_TW);
                default -> "";
            };
            if (!priceStr.isEmpty()) {
                int color = paymentType == CommunityGoalScreenHandler.PAYMENT_EMERALDS
                        ? C_GREEN : C_YELLOW;
                ctx.drawCenteredTextWithShadow(textRenderer, priceStr, cx, 90, color);
            }
        }

        separator(ctx, 106);
        // Withdraw button is at y+116 (set in init)
    }

    // ── Drawing helpers ───────────────────────────────────────────────────────

    private void separator(DrawContext ctx, int localY) {
        ctx.fill(16, localY, BG_W - 16, localY + 1, C_SEPARATOR);
    }

    private void drawBar(DrawContext ctx, int cx, int barY,
                          int current, int target, boolean goalReached) {
        int bx    = cx - BAR_W / 2;
        float pct = target > 0 ? Math.min(1f, (float) current / target) : 0f;

        ctx.fill(bx - 1, barY - 1, bx + BAR_W + 1, barY + BAR_H + 1, C_BAR_BORDER);
        ctx.fill(bx,     barY,     bx + BAR_W,     barY + BAR_H,     C_BAR_TRACK);

        int fw = (int) (BAR_W * pct);
        if (fw > 0) {
            int fillColor  = goalReached ? 0xFF3AA85E : 0xFF3A78CC;
            int shineColor = goalReached ? 0xFF52CC7C : 0xFF5296E0;
            ctx.fill(bx, barY,     bx + fw, barY + BAR_H, fillColor);
            ctx.fill(bx, barY,     bx + fw, barY + 2,     shineColor);
        }
    }

    private Text paymentTypeText(int type) {
        return switch (type) {
            case CommunityGoalScreenHandler.PAYMENT_COBBLEDOLLARS ->
                    Text.translatable("gui.modbloc.payment_cobbledollars");
            case CommunityGoalScreenHandler.PAYMENT_EMERALDS ->
                    Text.translatable("gui.modbloc.payment_emeralds");
            default -> Text.translatable("gui.modbloc.payment_free");
        };
    }

    private String fit(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        String ell = "…";
        return textRenderer.trimToWidth(text, maxWidth - textRenderer.getWidth(ell)) + ell;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
