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

    private static final Identifier TEXTURE = Identifier.of("modbloc", "textures/gui/community_goal.png");
    private static final int BG_WIDTH  = 176;
    private static final int BG_HEIGHT = 214;

    // Setup mode widgets
    private TextFieldWidget amountField;
    private ButtonWidget confirmButton;

    // Play mode widgets
    private ButtonWidget depositButton;
    private ButtonWidget withdrawButton;

    public CommunityGoalScreen(CommunityGoalScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth  = BG_WIDTH;
        this.backgroundHeight = BG_HEIGHT;
        this.playerInventoryTitleY = BG_HEIGHT - 94;
    }

    @Override
    protected void init() {
        super.init();

        int cx = x + backgroundWidth / 2;

        // Setup widgets (creative config)
        amountField = new TextFieldWidget(textRenderer, cx - 30, y + 42, 60, 12,
                Text.literal("Amount"));
        amountField.setMaxLength(9);
        amountField.setText("100");
        amountField.setVisible(false);
        addDrawableChild(amountField);

        confirmButton = ButtonWidget.builder(Text.translatable("gui.modbloc.confirm"), btn -> sendConfirm())
                .dimensions(cx - 40, y + 58, 80, 16)
                .build();
        confirmButton.visible = false;
        addDrawableChild(confirmButton);

        // Play widgets
        depositButton = ButtonWidget.builder(Text.translatable("gui.modbloc.deposit"), btn -> sendDeposit())
                .dimensions(cx - 40, y + 118, 80, 16)
                .build();
        depositButton.visible = false;
        addDrawableChild(depositButton);

        withdrawButton = ButtonWidget.builder(Text.translatable("gui.modbloc.withdraw"), btn -> sendWithdraw())
                .dimensions(cx - 50, y + 100, 100, 16)
                .build();
        withdrawButton.visible = false;
        addDrawableChild(withdrawButton);
    }

    private void sendConfirm() {
        int amount;
        try { amount = Integer.parseInt(amountField.getText().trim()); }
        catch (NumberFormatException e) { return; }
        if (amount <= 0) return;

        ModBlocClientPackets.sendSetupPacket(handler.getBlockPos(), amount);
    }

    private void sendDeposit() {
        client.interactionManager.clickButton(handler.syncId, CommunityGoalScreenHandler.BUTTON_DEPOSIT);
    }

    private void sendWithdraw() {
        client.interactionManager.clickButton(handler.syncId, CommunityGoalScreenHandler.BUTTON_WITHDRAW);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // Fallback background (grey panel) when texture is missing
        context.fill(x, y, x + backgroundWidth, y + backgroundHeight, 0xFFC6C6C6);
        context.fill(x + 7, y + 17, x + backgroundWidth - 7, y + backgroundHeight - 7, 0xFF8B8B8B);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Title
        context.drawText(textRenderer, title, titleX, titleY, 0x404040, false);
        // Player inventory label
        context.drawText(textRenderer, playerInventoryTitle, playerInventoryTitleX, playerInventoryTitleY, 0x404040, false);

        boolean setup = handler.isSetup();
        boolean goalReached = handler.isGoalReached();
        int target = handler.getTargetAmount();
        int current = handler.getCurrentAmount();
        ItemStack targetItem = handler.getTargetItem();

        updateWidgetVisibility(setup, goalReached);

        if (!setup) {
            // Setup instructions
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("gui.modbloc.place_item"),
                    backgroundWidth / 2, 30, 0xFFFFFF);
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.translatable("gui.modbloc.set_amount"),
                    backgroundWidth / 2, 32 + textRenderer.fontHeight, 0xFFFFFF);
        } else {
            // Item name
            if (!targetItem.isEmpty()) {
                String itemName = targetItem.getName().getString();
                context.drawCenteredTextWithShadow(textRenderer,
                        Text.literal(itemName), backgroundWidth / 2, 28, 0xFFFFFF);
            }

            // Progress text
            String progress = current + " / " + target;
            context.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(progress), backgroundWidth / 2, 40, goalReached ? 0x55FF55 : 0xFFFFFF);

            // Progress bar
            int barW = 120;
            int barX = (backgroundWidth - barW) / 2;
            int barY = 50;
            float ratio = target > 0 ? Math.min(1f, (float) current / target) : 0f;
            context.fill(barX, barY, barX + barW, barY + 6, 0xFF333333);
            context.fill(barX, barY, barX + (int)(barW * ratio), barY + 6,
                    goalReached ? 0xFF55FF55 : 0xFF5599FF);

            if (goalReached) {
                context.drawCenteredTextWithShadow(textRenderer,
                        Text.translatable("gui.modbloc.goal_reached"),
                        backgroundWidth / 2, 60, 0x55FF55);
            }
        }
    }

    private void updateWidgetVisibility(boolean setup, boolean goalReached) {
        boolean configuring = !setup;
        amountField.setVisible(configuring);
        confirmButton.visible = configuring;
        depositButton.visible = setup && !goalReached;
        withdrawButton.visible = goalReached;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
