package thunder.hack.features.hud.impl;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.features.modules.client.Media;
import thunder.hack.features.modules.misc.NameProtect;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.TextureStorage;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WaterMark extends HudElement {

    public WaterMark() {
        super("WaterMark", 120, 20);
    }

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        String time = (new SimpleDateFormat("HH:mm")).format(new Date());
        String aText = "A";
        String glitchText = "glitch";
        String separator = " | ";
        String fullText = aText + glitchText + separator + time;

        float aWidth = FontRenderers.sf_bold.getStringWidth(aText);
        float glitchWidth = FontRenderers.sf_bold.getStringWidth(glitchText);
        float separatorWidth = FontRenderers.sf_bold.getStringWidth(separator);
        float timeWidth = FontRenderers.sf_bold.getStringWidth(time);

        float totalWidth = aWidth + glitchWidth + separatorWidth + timeWidth + 8;
        float totalHeight = FontRenderers.sf_bold.getFontHeight("A") + 3;

        float fixedX = 5;
        float fixedY = 5;

        Render2DEngine.drawHudBase(context.getMatrices(), fixedX, fixedY, totalWidth, totalHeight, HudEditor.hudRound.getValue());

        float textY = fixedY + 3;
        float currentX = fixedX + 4;

        FontRenderers.sf_bold.drawString(context.getMatrices(), aText, currentX, textY, Color.WHITE.getRGB());
        currentX += aWidth;

        FontRenderers.sf_bold.drawGradientString(context.getMatrices(), glitchText, currentX, textY, 10);
        currentX += glitchWidth;

        FontRenderers.sf_bold.drawString(context.getMatrices(), separator, currentX, textY, Color.GRAY.getRGB());
        currentX += separatorWidth;

        FontRenderers.sf_bold.drawString(context.getMatrices(), time, currentX, textY, Color.WHITE.getRGB());

        setBounds(fixedX, fixedY, totalWidth, totalHeight);
    }
}