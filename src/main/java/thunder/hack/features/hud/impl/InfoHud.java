package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import thunder.hack.core.Managers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.setting.Setting;
import thunder.hack.utility.math.FrameRateCounter;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class InfoHud extends HudElement {
    public final Setting<Boolean> showTPS = new Setting<>("TPS", true);
    public final Setting<Boolean> showFPS = new Setting<>("FPS", true);
    public final Setting<Boolean> showBPS = new Setting<>("BPS", true);
    public final Setting<Boolean> showCoords = new Setting<>("Coords", true);
    public final Setting<Boolean> showPing = new Setting<>("Ping", false);

    public InfoHud() {
        super("InfoHud", 10, 200);
    }

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        var font = FontRenderers.sf_bold_mini;
        List<String> lines = new ArrayList<>();

        if (showTPS.getValue()) lines.add("TPS: " + String.format("%.0f", Managers.SERVER.getTPS()));
        if (showFPS.getValue()) lines.add("FPS: " + FrameRateCounter.INSTANCE.getFps());
        if (showPing.getValue()) lines.add("PING: " + Managers.SERVER.getPing());
        if (showBPS.getValue()) lines.add("BPS: " + String.format("%.2f", Managers.PLAYER.currentPlayerSpeed * 20f));
        if (showCoords.getValue()) {
            int posX = (int) mc.player.getX();
            int posY = (int) mc.player.getY();
            int posZ = (int) mc.player.getZ();
            lines.add("XYZ: " + posX + " " + posY + " " + posZ);
        }

        float lineHeight = font.getFontHeight("A") - 1;
        float startX = 5;
        int screenHeight = mc.getWindow().getScaledHeight();

        int bottomMargin = 1;

        float startY = screenHeight - (lines.size() * lineHeight) - bottomMargin;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] parts = line.split(": ");
            if (parts.length < 2) continue;

            String label = parts[0] + ": ";
            String value = parts[1];
            float labelWidth = font.getStringWidth(label);
            float y = startY + i * lineHeight;

            font.drawGradientString(context.getMatrices(), label, startX, y, 10);
            font.drawString(context.getMatrices(), value, startX + labelWidth, y, Color.WHITE.getRGB());
        }

        setBounds(startX, startY, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }
}
