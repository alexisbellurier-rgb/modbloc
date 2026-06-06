package net.modbloc.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
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

        // --- Floating, rotating item above the block ---
        matrices.push();
        matrices.translate(0.5, 1.25, 0.5);
        float angle = entity.renderAngle + tickDelta * 1.5f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        matrices.scale(0.5f, 0.5f, 0.5f);

        itemRenderer.renderItem(targetItem, ModelTransformationMode.GROUND,
                LightmapTextureManager.MAX_LIGHT_COORDINATE, overlay, matrices, vertexConsumers,
                entity.getWorld(), 0);
        matrices.pop();

        // --- Progress text below the item ---
        matrices.push();
        matrices.translate(0.5, 1.65, 0.5);
        // Face the camera by billboarding (rotate opposite to default)
        matrices.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());
        float scale = 0.025f;
        matrices.scale(-scale, -scale, scale);

        String progressText = entity.getCurrentAmount() + " / " + entity.getTargetAmount();
        int textColor = entity.isGoalReached() ? 0x55FF55 : 0xFFFFFF;

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int textLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        float textX = -textRenderer.getWidth(progressText) / 2f;

        textRenderer.draw(progressText, textX, 0, textColor, false,
                matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, textLight);
        matrices.pop();
    }

    @Override
    public int getRenderDistance() {
        return 64;
    }
}
