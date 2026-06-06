package net.modbloc.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.modbloc.network.ModBlocClientPackets;

public class CommunityGoalScreen extends HandledScreen<CommunityGoalScreenHandler> {

    private static final Identifier TEXTURE =
            Identifier.of("modbloc", "textures/gui/community_goal.png");

    private static final int BG_W   = 176;
    private static final int BG_H   = 240;
    private static final int BAR_W  = 120;
    private static final int BAR_H  = 8;
    private static final int MAX_TW = 140;

    private static final int TARGET_X = 80, TARGET_Y = 24;
    private static final int DEP_X0 = 8,   DEP_Y0 = 76;

    private TextFieldWidget amountField;
    private TextFieldWidget priceField;
    private ButtonWidget    confirmButton;
    private ButtonWidget    depositButton;
    private ButtonWidget    withdrawButton;

    public CommunityGoalScreen(CommunityGoalScreenHandler handler,
                                PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth  = BG_W;
        this.backgroundHeight = BG_H;
    }

    @Override
    protected void init() {
        super.init();
        this.playerInventoryTitleY = 156;

        int cx = x + BG_W / 2;

        // Setup widgets: vertically centered in the content area (y=24 to y=150)
        amountField = new TextFieldWidget(textRenderer, cx - 35, y + 65, 70, 12, Text.empty());
        amountField.setMaxLength(9);
        amountField.setText("1000");
        amountField.setVisible(false);
        addDrawableChild(amountField);

        priceField = new TextFieldWidget(textRenderer, cx - 35, y + 95, 70, 12, Text.empty());
        priceField.setMaxLength(9);
        priceField.setText("0");
        priceField.setVisible(false);
        addDrawableChild(priceField);

        confirmButton = ButtonWidget.builder(
                Text.translatable("gui.modbloc.confirm"), btn -> sendConfirm()
        ).dimensions(cx - 50, y + 113, 100, 16).build();
        confirmButton.visible = false;
        addDrawableChild(confirmButton);

        // Play-mode widgets
        depositButton = ButtonWidget.builder(
                Text.translatable("gui.modbloc.deposit"), btn -> sendDeposit()
        ).dimensions(cx - 50, y + 130, 100, 16).build();
        depositButton.visible = false;
        addDrawableChild(depositButton);

        withdrawButton = ButtonWidget.builder(
                Text.translatable("gui.modbloc.withdraw"), btn -> sendWithdraw()
        ).dimensions(cx - 55, y + 130, 110, 16).build();
        withdrawButton.visible = false;
        addDrawableChild(withdrawButton);
    }

    private void sendConfirm() {
        int amount;
        try { amount = Integer.parseInt(amountField.getText().trim()); }
        catch (NumberFormatException e) { return; }
        if (amount <= 0) return;

        int price;
        try { price = Integer.parseInt(priceField.getText().trim()); }
        catch (NumberFormatException e) { price = 0; }
        price = Math.max(0, price);

        ModBlocClientPackets.sendSetupPacket(handler.getBlockPos(), amount, price);
    }

    private void sendDeposit() {
        client.interactionManager.clickButton(handler.syncId, CommunityGoalScreenHandler.BUTTON_DEPOSIT);
    }

    private void sendWithdraw() {
        client.interactionManager.clickButton(handler.syncId, CommunityGoalScreenHandler.BUTTON_WITHDRAW);
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, x, y, 0, 0, BG_W, BG_H);

        // Target slot
        drawSlot(context, x + TARGET_X, y + TARGET_Y);

        // Deposit grid only in play mode and before goal is reached
        if (handler.isConfigured() && !handler.isGoalReached()) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    drawSlot(context, x + DEP_X0 + col * 18, y + DEP_Y0 + row * 18);
                }
            }
        }

        // Player inventory + hotbar
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(context, x + 8 + col * 18, y + 166 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(context, x + 8 + col * 18, y + 222);
        }
    }

    private void drawSlot(DrawContext ctx, int sx, int sy) {
        ctx.fill(sx - 1, sy - 1, sx + 17, sy,     0xFF373737);
        ctx.fill(sx - 1, sy,     sx,      sy + 17, 0xFF373737);
        ctx.fill(sx,     sy + 16, sx + 17, sy + 17, 0xFFFFFFFF);
        ctx.fill(sx + 16, sy,    sx + 17,  sy + 16, 0xFFFFFFFF);
        ctx.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        boolean configured  = handler.isConfigured();
        boolean goalReached = handler.isGoalReached();
        int target          = handler.getTargetAmount();
        int current         = handler.getCurrentAmount();
        int price           = handler.getPricePerStack();
        ItemStack targetItem = handler.getTargetItem();
        int cx              = BG_W / 2;

        // Widget visibility
        amountField.setVisible(!configured);
        priceField.setVisible(!configured);
        confirmButton.visible   = !configured;
        depositButton.visible   =  configured && !goalReached;
        withdrawButton.visible  =  goalReached;

        // Title
        String titleStr = fit(title.getString(), BG_W - 16);
        context.drawText(textRenderer, titleStr,
                cx - textRenderer.getWidth(titleStr) / 2, 6, 0x404040, false);

        // Player inventory label
        context.drawText(textRenderer, playerInventoryTitle,
                playerInventoryTitleX, playerInventoryTitleY, 0x404040, false);

        if (!configured) {
            // ── SETUP MODE ───────────────────────────────────────────────────
            String hint = fit(Text.translatable("gui.modbloc.place_item").getString(), 68);
            context.drawText(textRenderer, hint, TARGET_X + 18, TARGET_Y + 4, 0x606060, false);

            String amtLabel = fit(Text.translatable("gui.modbloc.amount").getString(), MAX_TW);
            context.drawText(textRenderer, amtLabel,
                    cx - textRenderer.getWidth(amtLabel) / 2, 53, 0x404040, false);

            String priceLabel = fit(Text.translatable("gui.modbloc.price").getString(), MAX_TW);
            context.drawText(textRenderer, priceLabel,
                    cx - textRenderer.getWidth(priceLabel) / 2, 83, 0x404040, false);

        } else {
            // ── PLAY MODE ────────────────────────────────────────────────────

            // Item name below the target slot
            if (!targetItem.isEmpty()) {
                String name = fit(targetItem.getName().getString(), MAX_TW);
                context.drawText(textRenderer, name,
                        cx - textRenderer.getWidth(name) / 2, 34, 0x404040, false);
            }

            // Progress bar
            drawProgressBar(context, cx, 44, current, target, goalReached);

            // Progress numbers
            String prog = fit(current + " / " + target, MAX_TW);
            context.drawCenteredTextWithShadow(textRenderer, prog, cx, 55,
                    goalReached ? 0x55FF55 : 0xFFFFFF);

            // Price — only shown when goal is reached and price is non-zero
            if (goalReached && price > 0) {
                String priceStr = fit(Text.translatable("gui.modbloc.price_display",
                        String.format("%,d", price)).getString(), MAX_TW);
                context.drawCenteredTextWithShadow(textRenderer, priceStr, cx, 65, 0xFFDD44);
            }
        }
    }

    private void drawProgressBar(DrawContext ctx, int cx, int barY,
                                  int current, int target, boolean goalReached) {
        int bx = cx - BAR_W / 2;
        float ratio = target > 0 ? Math.min(1f, (float) current / target) : 0f;

        ctx.fill(bx - 1, barY - 1, bx + BAR_W + 1, barY + BAR_H + 1, 0xFF555555);
        ctx.fill(bx, barY, bx + BAR_W, barY + BAR_H, 0xFF222222);

        int fw = (int) (BAR_W * ratio);
        if (fw > 0) {
            ctx.fill(bx, barY, bx + fw, barY + BAR_H, goalReached ? 0xFF55CC55 : 0xFF4488FF);
        }
    }

    private String fit(String text, int maxWidth) {
        if (textRenderer.getWidth(text) <= maxWidth) return text;
        String ellipsis = "…";
        return textRenderer.trimToWidth(text, maxWidth - textRenderer.getWidth(ellipsis)) + ellipsis;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
