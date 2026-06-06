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

    private static final int BG_W    = 176;
    private static final int BG_H    = 240;
    private static final int BAR_W   = 120;
    private static final int BAR_H   = 8;
    private static final int MAX_TW  = 140; // max text width in content area

    // Slot positions (must match CommunityGoalScreenHandler)
    private static final int TARGET_X = 80, TARGET_Y = 24;
    private static final int DEP_X0 = 61, DEP_Y0 = 76;

    private TextFieldWidget amountField;
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

        amountField = new TextFieldWidget(textRenderer, cx - 35, y + 50, 70, 12,
                Text.empty());
        amountField.setMaxLength(9);
        amountField.setText("100");
        amountField.setVisible(false);
        addDrawableChild(amountField);

        confirmButton = ButtonWidget.builder(
                Text.translatable("gui.modbloc.confirm"), btn -> sendConfirm()
        ).dimensions(cx - 50, y + 66, 100, 16).build();
        confirmButton.visible = false;
        addDrawableChild(confirmButton);

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
        if (amount > 0) ModBlocClientPackets.sendSetupPacket(handler.getBlockPos(), amount);
    }

    private void sendDeposit() {
        client.interactionManager.clickButton(handler.syncId,
                CommunityGoalScreenHandler.BUTTON_DEPOSIT);
    }

    private void sendWithdraw() {
        client.interactionManager.clickButton(handler.syncId,
                CommunityGoalScreenHandler.BUTTON_WITHDRAW);
    }

    // ------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Main background texture
        context.drawTexture(TEXTURE, x, y, 0, 0, BG_W, BG_H);

        // Slot backgrounds (vanilla style: dark top/left, light bottom/right, grey interior)
        drawSlot(context, x + TARGET_X, y + TARGET_Y);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                drawSlot(context, x + DEP_X0 + col * 18, y + DEP_Y0 + row * 18);
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(context, x + 8 + col * 18, y + 166 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(context, x + 8 + col * 18, y + 222);
        }
    }

    /** Draws a single 16×16 slot at absolute screen position (sx, sy). */
    private void drawSlot(DrawContext ctx, int sx, int sy) {
        ctx.fill(sx - 1, sy - 1, sx + 17, sy,     0xFF373737); // top shadow
        ctx.fill(sx - 1, sy,     sx,      sy + 17, 0xFF373737); // left shadow
        ctx.fill(sx,     sy + 16, sx + 17, sy + 17, 0xFFFFFFFF); // bottom highlight
        ctx.fill(sx + 16, sy,    sx + 17,  sy + 16, 0xFFFFFFFF); // right highlight
        ctx.fill(sx, sy, sx + 16, sy + 16, 0xFF8B8B8B);          // interior
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        boolean setup       = handler.isSetup();
        boolean goalReached = handler.isGoalReached();
        int target          = handler.getTargetAmount();
        int current         = handler.getCurrentAmount();
        ItemStack targetItem = handler.getTargetItem();
        int cx              = BG_W / 2;

        // Update widget visibility
        amountField.setVisible(!setup);
        confirmButton.visible = !setup;
        depositButton.visible =  setup && !goalReached;
        withdrawButton.visible = goalReached;

        // Title (trimmed to container width)
        String titleStr = fit(title.getString(), BG_W - 16);
        context.drawText(textRenderer, titleStr,
                cx - textRenderer.getWidth(titleStr) / 2, 6, 0x404040, false);

        // Player inventory label
        context.drawText(textRenderer, playerInventoryTitle,
                playerInventoryTitleX, playerInventoryTitleY, 0x404040, false);

        if (!setup) {
            // ── SETUP MODE ──────────────────────────────────────────────
            // Hint to the right of the target slot
            String hint = fit(Text.translatable("gui.modbloc.place_item").getString(), 68);
            context.drawText(textRenderer, hint, TARGET_X + 18, TARGET_Y + 4, 0x606060, false);

            // Amount label above the text field (field at y=50)
            String amtLabel = fit(Text.translatable("gui.modbloc.amount").getString(), MAX_TW);
            context.drawText(textRenderer, amtLabel,
                    cx - textRenderer.getWidth(amtLabel) / 2, 40, 0x404040, false);

        } else {
            // ── PLAY MODE ───────────────────────────────────────────────

            // Item name below slot (slot bottom = TARGET_Y + 16 = 40)
            if (!targetItem.isEmpty()) {
                String name = fit(targetItem.getName().getString(), MAX_TW);
                context.drawText(textRenderer, name,
                        cx - textRenderer.getWidth(name) / 2, 44, 0x404040, false);
            }

            // Progress bar (y=54)
            drawProgressBar(context, cx, 54, current, target, goalReached);

            // Progress numbers (y=65, below bar)
            String prog = fit(current + " / " + target, MAX_TW);
            context.drawCenteredTextWithShadow(textRenderer, prog, cx, 65,
                    goalReached ? 0x55FF55 : 0xFFFFFF);

            // Section label above deposit slots (DEP_Y0 = 76, label at 68)
            if (goalReached) {
                String reached = fit(Text.translatable("gui.modbloc.goal_reached").getString(), MAX_TW);
                context.drawCenteredTextWithShadow(textRenderer, reached, cx, 68, 0x55FF55);
            } else {
                String depLabel = fit(Text.translatable("gui.modbloc.deposit_section").getString(), MAX_TW);
                context.drawText(textRenderer, depLabel,
                        DEP_X0, 68, 0x404040, false);
            }
        }
    }

    private void drawProgressBar(DrawContext ctx, int cx, int y,
                                  int current, int target, boolean goalReached) {
        int bx = cx - BAR_W / 2;
        float ratio = target > 0 ? Math.min(1f, (float) current / target) : 0f;

        // Border
        ctx.fill(bx - 1, y - 1, bx + BAR_W + 1, y + BAR_H + 1, 0xFF555555);
        // Background
        ctx.fill(bx, y, bx + BAR_W, y + BAR_H, 0xFF222222);
        // Fill
        int fw = (int) (BAR_W * ratio);
        if (fw > 0) {
            ctx.fill(bx, y, bx + fw, y + BAR_H, goalReached ? 0xFF55CC55 : 0xFF4488FF);
        }
    }

    /** Trims text to fit maxWidth, appending "…" if truncated. */
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
