package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Colors;
import org.joml.Matrix4f;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.combat.Aura;
import thunder.hack.features.modules.combat.AutoAnchor;
import thunder.hack.features.modules.combat.AutoCrystal;
import thunder.hack.features.modules.misc.NameProtect;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.gui.hud.HudEditorGui;
import thunder.hack.setting.Setting;
import thunder.hack.utility.math.MathUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.EaseOutBack;
import thunder.hack.utility.render.animation.EaseOutCirc;

import java.awt.*;
import java.util.List;

public class TargetHud extends HudElement {
    private static final float HUD_WIDTH = 140f;
    private static final float HUD_HEIGHT = 32f;
    private static final float HEAD_SIZE = 24f;
    private static final float ITEM_SCALE = 0.8f;
    private static final float HEALTH_BAR_WIDTH = 60f;
    private static final float HEALTH_BAR_HEIGHT = 5f;
    private static final float LERP_SPEED = 3.0f;
    private static final float TARGET_LOST_LERP_SPEED = 0.25f;
    private static final float NORMAL_LERP_SPEED = 0.15f;
    private static final float DEATH_LERP_SPEED = 2.0f;
    private static final Color HEALTH_COLOR_MODERN = new Color(85, 255, 255);
    private static final Color HEALTH_BAR_BG = new Color(80, 80, 80, 180);

    private final Setting<HPmodeEn> hpMode = new Setting<>("HP Mode", HPmodeEn.HP);
    private final Setting<Boolean> funTimeHP = new Setting<>("FunTimeHP", false);

    private final EaseOutBack animation = new EaseOutBack();
    private static final EaseOutCirc healthAnimation = new EaseOutCirc();

    private boolean direction = false;
    private LivingEntity target;
    private float animatedHealth = 0f;
    private LivingEntity previousTarget = null;
    private long lastUpdateTime = System.currentTimeMillis();
    private boolean targetLost = false;
    private boolean targetDead = false;

    private float lastKnownHealth = 0f;

    private String cachedTargetName = "";
    private String cachedHealthText = "";
    private float cachedMaxHealth = 0f;

    public TargetHud() {
        super("TargetHud", 150, 50);
    }

    @Override
    public void onUpdate() {
        animation.update(direction);
        updateHealthAnimation();
        healthAnimation.update();
    }

    private void updateHealthAnimation() {
        if (target != null && !targetLost) {
            updateTargetHealth();
        } else {
            updateNoTargetHealth();
        }
    }

    private void updateTargetHealth() {
        float currentHealth = getHealth();
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        if (target.isDead() || target.getHealth() <= 0) {
            if (!targetDead) {
                targetDead = true;
                cachedHealthText = getHealthDisplayText();
            }
            currentHealth = 0f;
            animatedHealth = 0f;
        } else {
            targetDead = false;
            lastKnownHealth = currentHealth;
        }

        if (previousTarget != target) {
            resetHealthAnimation(currentHealth);
        } else {
            interpolateHealth(currentHealth, deltaTime);
        }
    }

    private void resetHealthAnimation(float currentHealth) {
        animatedHealth = currentHealth;
        previousTarget = target;
        lastKnownHealth = currentHealth;
        updateCachedValues();
    }

    private void interpolateHealth(float currentHealth, float deltaTime) {
        if (targetDead || (target != null && target.isDead()) || currentHealth <= 0) {
            animatedHealth = 0f;
            return;
        }

        float maxChange = LERP_SPEED * deltaTime;
        float healthDifference = currentHealth - animatedHealth;

        if (Math.abs(healthDifference) > 0.01f) {
            if (healthDifference > 0) {
                animatedHealth = Math.min(animatedHealth + maxChange * 2, currentHealth);
            } else {
                animatedHealth = Math.max(animatedHealth + healthDifference * Math.min(deltaTime * 4, 1.0f), currentHealth);
            }
        } else {
            animatedHealth = currentHealth;
        }
    }

    private void updateNoTargetHealth() {
        if (targetLost && !targetDead) {
            return;
        }

        if (targetDead) {
            animatedHealth = 0f;
        }

        previousTarget = null;
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        updateTarget();

        if (target == null && animatedHealth <= 0.01f) return;

        context.getMatrices().push();
        float animationFactor = getAnimationFactor();
        if (animationFactor > 0) {
            renderTargetHud(context, animationFactor);
        }
        context.getMatrices().pop();
    }

    private float getAnimationFactor() {
        return (float) MathUtility.clamp(animation.getAnimationd(), 0, 1f);
    }

    private void renderTargetHud(DrawContext context, float animationFactor) {
        renderModern(context, animationFactor);
    }

    private void updateTarget() {
        LivingEntity newTarget = findNewTarget();

        if (newTarget != null) {
            setNewTarget(newTarget);
        } else {
            loseTarget();
        }
    }

    private LivingEntity findNewTarget() {
        if (AutoCrystal.target != null && !AutoCrystal.target.isDead()) {
            return AutoCrystal.target;
        }

        if (Aura.target instanceof LivingEntity livingTarget && !livingTarget.isDead()) {
            return livingTarget;
        }

        if (AutoAnchor.target != null && !AutoAnchor.target.isDead()) {
            return AutoAnchor.target;
        }

        if (isInSpecialScreen()) {
            return mc.player;
        }

        return null;
    }

    private boolean isInSpecialScreen() {
        return mc.currentScreen instanceof ChatScreen || mc.currentScreen instanceof HudEditorGui;
    }

    private void setNewTarget(LivingEntity newTarget) {
        target = newTarget;
        direction = true;
        targetLost = false;
        targetDead = false;
        updateCachedValues();
    }

    private void loseTarget() {
        direction = false;
        targetLost = true;

        if (target != null && !target.isDead() && !targetDead) {
            lastKnownHealth = animatedHealth;
        }

        if (!targetDead) {
            if (animation.getAnimationd() < 0.02) {
                target = null;
                targetLost = false;
                targetDead = false;
                animatedHealth = 0f;
                clearCachedValues();
            }
            return;
        }

        if (animation.getAnimationd() < 0.02) {
            target = null;
            targetLost = false;
            targetDead = false;
            animatedHealth = 0f;
            clearCachedValues();
        }
    }

    private void updateCachedValues() {
        if (target != null) {
            cachedTargetName = getTargetDisplayName();
            cachedHealthText = getHealthDisplayText();
            cachedMaxHealth = target.getMaxHealth();
        }
    }

    private void clearCachedValues() {
        cachedTargetName = "";
        cachedHealthText = "";
        cachedMaxHealth = 0f;
        lastKnownHealth = 0f;
    }

    private String getTargetDisplayName() {
        if (ModuleManager.media.isEnabled()) {
            return "Player520";
        }

        if (ModuleManager.nameProtect.isEnabled() && target == mc.player) {
            return NameProtect.getCustomName();
        }

        return target.getName().getString();
    }

    private float getHealth() {
        if (target == null) return lastKnownHealth;

        if (target.isDead() || target.getHealth() <= 0) {
            return 0f;
        }

        float health = target.getHealth();
        if (funTimeHP.getValue() && target instanceof PlayerEntity pe) {
            health += pe.getAbsorptionAmount();
        }

        return Math.max(0f, health);
    }

    private float getRegularHealth() {
        if (target == null) return 0f;

        if (target.isDead() || target.getHealth() <= 0) {
            return 0f;
        }

        return Math.max(0f, target.getHealth());
    }

    private float getAbsorptionHealth() {
        if (target == null || target.isDead()) return 0f;

        if (target instanceof PlayerEntity pe) {
            return Math.max(0f, pe.getAbsorptionAmount());
        }
        return 0f;
    }

    private void renderModern(DrawContext context, float animationFactor) {
        renderBaseHud(context, animationFactor, HEALTH_COLOR_MODERN);
    }

    private void renderBaseHud(DrawContext context, float animationFactor, Color healthColor) {
        float hudX = getPosX();
        float hudY = getPosY();

        Render2DEngine.drawHudBase(context.getMatrices(), hudX, hudY, HUD_WIDTH, HUD_HEIGHT, HudEditor.hudRound.getValue());
        setBounds(hudX, hudY, HUD_WIDTH, HUD_HEIGHT);

        if (target != null) {
            renderTargetInfo(context, hudX, hudY, animationFactor, healthColor);
            renderPlayerItems(context, hudX, hudY, animationFactor);
        }

        renderHealthBar(context, hudX, hudY, animationFactor);
    }

    private void renderTargetInfo(DrawContext context, float hudX, float hudY, float animationFactor, Color healthColor) {
        renderEntityHead(context, hudX, hudY, animationFactor);

        FontRenderers.sf_bold_mini.drawString(context.getMatrices(), cachedTargetName, hudX + 32, hudY + 4,
                Render2DEngine.applyOpacity(Colors.WHITE, animationFactor));

        String healthText = targetDead || (target != null && target.isDead()) ?
                getHealthDisplayTextForDead() : cachedHealthText;

        FontRenderers.sf_bold_mini.drawString(context.getMatrices(), healthText, hudX + 32, hudY + 14,
                Render2DEngine.applyOpacity(healthColor.getRGB(), animationFactor));
    }

    private void renderEntityHead(DrawContext context, float hudX, float hudY, float animationFactor) {
        setEntityTexture();

        RenderSystem.setShaderColor(1f, 1f, 1f, animationFactor);
        float headX = hudX + 4;
        float headY = hudY + 4;

        Render2DEngine.renderTexture(context.getMatrices(), headX, headY, HEAD_SIZE, HEAD_SIZE, 8, 8, 8, 8, 64, 64);
        Render2DEngine.renderTexture(context.getMatrices(), headX, headY, HEAD_SIZE, HEAD_SIZE, 40, 8, 8, 8, 64, 64);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void setEntityTexture() {
        if (target instanceof PlayerEntity) {
            RenderSystem.setShaderTexture(0, ((AbstractClientPlayerEntity) target).getSkinTextures().texture());
        } else {
            RenderSystem.setShaderTexture(0, mc.getEntityRenderDispatcher().getRenderer(target).getTexture(target));
        }
    }

    private void renderHealthBar(DrawContext context, float hudX, float hudY, float animationFactor) {
        float healthBarX = hudX + 32;
        float healthBarY = hudY + 24;

        Color bgColor = new Color(HEALTH_BAR_BG.getRed(), HEALTH_BAR_BG.getGreen(), HEALTH_BAR_BG.getBlue(),
                (int)(HEALTH_BAR_BG.getAlpha() * animationFactor));
        drawRoundedRect(context.getMatrices(), healthBarX, healthBarY, HEALTH_BAR_WIDTH, HEALTH_BAR_HEIGHT,
                HudEditor.hudRound.getValue(), bgColor);

        float healthProgress = calculateHealthProgress();
        if (healthProgress > 0f) {
            Color healthColor = Render2DEngine.applyOpacity(HudEditor.getColor(0), animationFactor);
            drawRoundedRect(context.getMatrices(), healthBarX, healthBarY, healthProgress, HEALTH_BAR_HEIGHT,
                    HudEditor.hudRound.getValue(), healthColor);
        }
    }

    private float calculateHealthProgress() {
        if (animatedHealth <= 0f || (target != null && (target.isDead() || target.getHealth() <= 0))) return 0f;

        float maxHealth = (target != null) ? cachedMaxHealth : 20f;
        if (maxHealth <= 0) return 0f;

        float healthRatio = MathUtility.clamp(animatedHealth / maxHealth, 0f, 1f);
        return healthRatio * HEALTH_BAR_WIDTH;
    }

    private String getHealthDisplayText() {
        if (target == null) return String.format("%.1fhp", animatedHealth);

        if (target.isDead() || target.getHealth() <= 0) {
            return "0.0hp";
        }

        float regularHealth = getRegularHealth();
        float absorptionHealth = getAbsorptionHealth();

        return switch (hpMode.getValue()) {
            case HP -> formatHealthText(regularHealth, absorptionHealth);
            case PERCENT -> formatPercentText(regularHealth);
        };
    }

    private String getHealthDisplayTextForDead() {
        return switch (hpMode.getValue()) {
            case HP -> "0.0hp";
            case PERCENT -> "0.0%";
        };
    }

    private String formatHealthText(float regularHealth, float absorptionHealth) {
        if (absorptionHealth > 0 && target instanceof PlayerEntity) {
            return String.format("%.1f+%.1fhp", regularHealth, absorptionHealth);
        }
        return String.format("%.1fhp", regularHealth);
    }

    private String formatPercentText(float regularHealth) {
        float maxHealth = cachedMaxHealth;
        if (maxHealth <= 0) return "0%";
        return String.format("%.1f%%", (regularHealth / maxHealth) * 100);
    }

    private void renderPlayerItems(DrawContext context, float hudX, float hudY, float animationFactor) {
        if (!(target instanceof PlayerEntity pe)) return;

        RenderSystem.setShaderColor(1f, 1f, 1f, animationFactor);

        List<ItemStack> armor = pe.getInventory().armor;
        ItemStack mainHand = pe.getMainHandStack();
        ItemStack offHand = pe.getOffHandStack();

        renderArmorItem(context, armor.get(3), hudX + 96, hudY + 2);
        renderArmorItem(context, armor.get(2), hudX + 96, hudY + 16);
        renderArmorItem(context, armor.get(1), hudX + 110, hudY + 2);
        renderArmorItem(context, armor.get(0), hudX + 110, hudY + 16);

        renderArmorItem(context, mainHand, hudX + 124, hudY + 2);
        renderArmorItem(context, offHand, hudX + 124, hudY + 16);

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private void renderArmorItem(DrawContext context, ItemStack item, float x, float y) {
        if (item.isEmpty()) return;

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        context.drawItem(item, 0, 0);
        context.drawItemInSlot(mc.textRenderer, item, 0, 0);
        context.getMatrices().pop();
    }

    private void drawRoundedRect(MatrixStack matrices, float x, float y, float width, float height, float radius, Color color) {
        if (radius > 0) {
            Render2DEngine.drawRoundedBlur(matrices, x, y, width, height, radius, color);
        } else {
            drawRect(matrices, x, y, width, height, color);
        }
    }

    private void drawRect(MatrixStack matrices, float x, float y, float width, float height, Color color) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(color.getRGB());
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(color.getRGB());
        buffer.vertex(matrix, x + width, y, 0.0F).color(color.getRGB());
        buffer.vertex(matrix, x, y, 0.0F).color(color.getRGB());

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        endRender();
    }

    private static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void endRender() {
        RenderSystem.disableBlend();
    }

    public enum HPmodeEn {
        HP, PERCENT
    }

    public static void sizeAnimation(MatrixStack matrices, float x, float y, double scale) {
        matrices.translate(x, y, 0);
        matrices.scale((float) scale, (float) scale, 1f);
        matrices.translate(-x, -y, 0);
    }

    public static void drawRectWithOutline(MatrixStack matrices, float x, float y, float width, float height, Color c, Color c2) {
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        setupRender();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x + width, y, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x, y, 0.0F).color(c.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x + width, y, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x, y, 0.0F).color(c2.getRGB());
        buffer.vertex(matrix, x, y + height, 0.0F).color(c2.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        endRender();
    }
}