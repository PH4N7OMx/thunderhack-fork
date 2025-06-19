package thunder.hack.utility.render.animation;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import static thunder.hack.features.modules.Module.mc;

public class CaptureMark {
    private static float espValue = 1f, prevEspValue;
    private static float espSpeed = 1f;
    private static boolean flipSpeed;

    public static void render(Entity target) {
        Camera camera = mc.gameRenderer.getCamera();

        double tPosX = Render2DEngine.interpolate(target.prevX, target.getX(), Render3DEngine.getTickDelta()) - camera.getPos().x;
        double tPosY = Render2DEngine.interpolate(target.prevY, target.getY(), Render3DEngine.getTickDelta()) - camera.getPos().y;
        double tPosZ = Render2DEngine.interpolate(target.prevZ, target.getZ(), Render3DEngine.getTickDelta()) - camera.getPos().z;

        MatrixStack matrices = new MatrixStack();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
        matrices.translate(tPosX, (tPosY + target.getHeight() / 2f), tPosZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        renderArrows(matrices);

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void renderArrows(MatrixStack matrices) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        int primaryColor = HudEditor.getColor(0).getRGB();
        int secondaryColor = HudEditor.getColor(90).getRGB();

        float baseDistance = 0.5f;
        float arrowSpacing = 0.12f;
        int arrowCount = 8;

        for (int i = 0; i < arrowCount; i++) {
            float yPos = -0.48f + (i * arrowSpacing);
            float animationOffset = (float) Math.sin((espValue + i * 20) * 0.05f) * 0.1f;
            renderPixelatedArrow(matrix, -baseDistance - animationOffset, yPos, true, primaryColor, secondaryColor, i);
            renderPixelatedArrow(matrix, baseDistance + animationOffset, yPos, false, primaryColor, secondaryColor, i);
        }
    }

    private static void renderPixelatedArrow(Matrix4f matrix, float centerX, float centerY, boolean leftSide, int color1, int color2, int index) {
        float pixelSize = 0.02f;
        float arrowWidth = 0.25f;
        float arrowHeight = 0.08f;

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        int pixelsWide = (int)(arrowWidth / pixelSize);
        int pixelsHigh = (int)(arrowHeight / pixelSize);

        int centerYpix = pixelsHigh / 2;

        for (int x = 0; x < pixelsWide; x++) {
            for (int y = 0; y < pixelsHigh; y++) {
                boolean shouldRender = false;

                if (leftSide) {
                    if (x < pixelsWide - 1 && Math.abs(y - centerYpix) <= 1) shouldRender = true;
                    if (x == 0 && y == centerYpix) shouldRender = true;
                } else {
                    if (x > 0 && Math.abs(y - centerYpix) <= 1) shouldRender = true;
                    if (x == pixelsWide - 1 && y == centerYpix) shouldRender = true;
                }

                if (shouldRender) {
                    float pixelX = leftSide
                            ? centerX + arrowWidth / 2 - x * pixelSize
                            : centerX + arrowWidth / 2 - x * pixelSize;
                    float pixelY = centerY - arrowHeight / 2 + y * pixelSize;

                    float colorFactor = (float)x / pixelsWide;
                    int finalColor = interpolateColor(color1, color2, colorFactor);
                    float brightness = 0.7f + 0.3f * (float)Math.sin((espValue + index * 30 + x * 10) * 0.08f);
                    finalColor = adjustBrightness(finalColor, brightness);

                    renderPixel(buffer, matrix, pixelX, pixelY, pixelSize, finalColor);
                }
            }
        }

        float protrudeSize = pixelSize * 1.2f;
        float protrudeX = leftSide
                ? centerX - arrowWidth / 2 + protrudeSize / 2f
                : centerX + arrowWidth / 2 + protrudeSize / 2f;
        float protrudeY = centerY;

        int protrudeColor = leftSide ? color1 : color2;
        float protrudeBrightness = 0.7f + 0.3f * (float)Math.sin((espValue + index * 40) * 0.1f);
        protrudeColor = adjustBrightness(protrudeColor, protrudeBrightness);

        renderPixel(buffer, matrix, protrudeX, protrudeY, protrudeSize, protrudeColor);

        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    private static void renderPixel(BufferBuilder buffer, Matrix4f matrix, float x, float y, float size, int color) {
        float halfSize = size / 2f;

        buffer.vertex(matrix, x - halfSize, y - halfSize, 0).color(color);
        buffer.vertex(matrix, x + halfSize, y - halfSize, 0).color(color);
        buffer.vertex(matrix, x + halfSize, y + halfSize, 0).color(color);
        buffer.vertex(matrix, x - halfSize, y + halfSize, 0).color(color);
    }

    private static int interpolateColor(int color1, int color2, float factor) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a1 = (color1 >> 24) & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;

        int r = (int)(r1 + (r2 - r1) * factor);
        int g = (int)(g1 + (g2 - g1) * factor);
        int b = (int)(b1 + (b2 - b1) * factor);
        int a = (int)(a1 + (a2 - a1) * factor);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int adjustBrightness(int color, float brightness) {
        int r = (int)(((color >> 16) & 0xFF) * brightness);
        int g = (int)(((color >> 8) & 0xFF) * brightness);
        int b = (int)((color & 0xFF) * brightness);
        int a = (color >> 24) & 0xFF;

        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void tick() {
        prevEspValue = espValue;
        espValue += espSpeed;
        if (espSpeed > 25) flipSpeed = true;
        if (espSpeed < -25) flipSpeed = false;
        espSpeed = flipSpeed ? espSpeed - 0.5f : espSpeed + 0.5f;
    }
}