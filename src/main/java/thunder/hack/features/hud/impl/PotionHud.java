package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Formatting;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;

public class PotionHud extends HudElement {
    private final Setting<Boolean> colored = new Setting<>("Colored", false);
    private float vAnimation, hAnimation;

    public PotionHud() {
        super("Potions", 150, 100);
    }

    public static String getDuration(StatusEffectInstance pe) {
        if (pe.isInfinite()) {
            return "*:*";
        } else {
            int var1 = pe.getDuration();
            int mins = var1 / 1200;
            String sec = String.format("%02d", (var1 % 1200) / 20);
            return mins + ":" + sec;
        }
    }

    private String getRomanNumeral(int number) {
        if (number <= 0) return "";
        if (number >= 10) return String.valueOf(number);

        String[] romanNumerals = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};
        return romanNumerals[number];
    }

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        float fixedX = 5;

        if (mc.player.getStatusEffects().isEmpty()) {
            setBounds(fixedX, mc.getWindow().getScaledHeight() * 0.5f, 0, 0);
            return;
        }

        float totalHeight = 0;
        for (StatusEffectInstance potionEffect : mc.player.getStatusEffects()) {
            totalHeight += 24;
        }
        totalHeight -= 2;

        float fixedY = (mc.getWindow().getScaledHeight() * 0.5f) - (totalHeight * 0.5f);
        float yOffset = 0;
        float maxWidth = 100;

        for (StatusEffectInstance potionEffect : mc.player.getStatusEffects()) {
            StatusEffect potion = potionEffect.getEffectType().value();

            String potionName = potion.getName().getString();
            String level = getRomanNumeral(potionEffect.getAmplifier() + 1);
            String duration = getDuration(potionEffect);

            String fullNameWithLevel = potionName + " " + level;

            float nameWidth = FontRenderers.sf_bold_mini.getStringWidth(fullNameWithLevel);
            float timeWidth = FontRenderers.sf_bold_mini.getStringWidth(duration);

            float blockWidth = Math.max(nameWidth, timeWidth) + 24;
            float blockHeight = 22;

            if (blockWidth > maxWidth) maxWidth = blockWidth;

            Render2DEngine.drawHudBase(context.getMatrices(), fixedX, fixedY + yOffset, blockWidth, blockHeight, HudEditor.hudRound.getValue());

            context.getMatrices().push();
            context.getMatrices().translate(fixedX + 3, fixedY + yOffset + 3, 0);
            context.drawSprite(0, 0, 0, 16, 16, mc.getStatusEffectSpriteManager().getSprite(potionEffect.getEffectType()));
            context.getMatrices().pop();

            FontRenderers.sf_bold_mini.drawString(context.getMatrices(),
                    potionName + " " + Formatting.WHITE + level,
                    fixedX + 22, fixedY + yOffset + 4,
                    Color.WHITE.getRGB());

            FontRenderers.sf_bold_mini.drawString(
                    context.getMatrices(),
                    duration,
                    fixedX + 22, fixedY + yOffset + 13,
                    new Color(220, 220, 220).getRGB()
            );

            yOffset += blockHeight + 2;
        }

        setBounds(fixedX, fixedY, maxWidth, yOffset - 2);
    }
}