package net.modbloc.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import net.modbloc.blockentity.CommunityGoalBlockEntity;
import org.joml.Matrix4f;

public class CommunityGoalBlockEntityRenderer implements BlockEntityRenderer<CommunityGoalBlockEntity> {

    private static final float BAR_HALF_W = 0.48f;
    private static final float BAR_HALF_H = 0.048f;
    private static final float BAR_INSET  = 0.007f;
    // Background z-extent; fill extends beyond it so its front face is
    // always closer to the camera than the background's front face.
    private static final float BG_Z   = 0.004f;
    private static final float FILL_Z = 0.008f;

    private final ItemRenderer itemRenderer;
    private final TextRenderer textRenderer;

    public CommunityGoalBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.textRenderer = context.getTextRenderer();
    }

    @Override
    public void render(CommunityGoalBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (!entity.isSetup()) return;
        ItemStack targetItem = entity.getTargetItem();
        if (targetItem.isEmpty()) return;

        int current     = entity.getCurrentAmount();
        int target      = entity.getTargetAmount();
        boolean reached = entity.isGoalReached();
        float angle     = entity.renderAngle + tickDelta * 1.5f;

        // ── Floating rotating item ────────────────────────────────────────────
        matrices.push();
        matrices.translate(0.5, 0.85, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        matrices.scale(1.2f, 1.2f, 1.2f);
        itemRenderer.renderItem(targetItem, ModelTransformationMode.GROUND,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay,
                matrices, vertexConsumers, entity.getWorld(), 0);
        matrices.pop();

        var rotation = MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation();

        // ── Progress bar ──────────────────────────────────────────────────────
        matrices.push();
        matrices.translate(0.5, 1.40, 0.5);
        matrices.multiply(rotation);

        float pct = target > 0 ? Math.min(1f, (float) current / target) : 0f;
        var buf = vertexConsumers.getBuffer(RenderLayer.getDebugFilledBox());

        // Background: z from -BG_Z to +BG_Z
        WorldRenderer.renderFilledBox(matrices, buf,
                -BAR_HALF_W, -BAR_HALF_H, -BG_Z,
                 BAR_HALF_W,  BAR_HALF_H,  BG_Z,
                0.08f, 0.08f, 0.08f, 0.85f);

        // Fill: z from -FILL_Z to +FILL_Z — extends beyond background so its
        // front face is closer to the camera than the background's front face,
        // guaranteeing it passes the LEQUAL depth test and renders on top.
        if (pct > 0) {
            float fillRight = -BAR_HALF_W + BAR_INSET + (2 * BAR_HALF_W - 2 * BAR_INSET) * pct;
            float fr = reached ? 0.27f : 0.23f;
            float fg = reached ? 0.73f : 0.47f;
            float fb = reached ? 0.40f : 0.80f;
            WorldRenderer.renderFilledBox(matrices, buf,
                    -BAR_HALF_W + BAR_INSET, -BAR_HALF_H + BAR_INSET, -FILL_Z,
                     fillRight,               BAR_HALF_H - BAR_INSET,   FILL_Z,
                    fr, fg, fb, 0.95f);
        }

        matrices.pop();

        // ── Progress text ─────────────────────────────────────────────────────
        matrices.push();
        matrices.translate(0.5, 1.54, 0.5);
        matrices.multiply(rotation);
        float scale = 0.025f;
        matrices.scale(-scale, -scale, scale);

        String progressText = current + " / " + target;
        int textColor = reached ? 0x55FF55 : 0xFFFFFF;
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float textX = -textRenderer.getWidth(progressText) / 2f;

        textRenderer.draw(progressText, textX, 0, textColor, false,
                matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0,
                LightmapTextureManager.MAX_LIGHT_COORDINATE);

        matrices.pop();
    }

    @Override
    public int getRenderDistance() {
        return 64;
    }
}
