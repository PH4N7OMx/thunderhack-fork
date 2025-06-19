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
    public final Setting<Boolean> showPing = new Setting<>("Ping", true);

    public InfoHud() {
        super("InfoHud", 10, 200);
    }

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        var font = FontRenderers.sf_bold;
        List<String> leftLines = new ArrayList<>();
        List<String> rightLines = new ArrayList<>();

        if (showBPS.getValue()) {
            float bps = Managers.PLAYER.currentPlayerSpeed * 20f;
            leftLines.add("BPS: " + String.format("%.1f", bps));
        }
        if (showCoords.getValue()) {
            int posX = (int) mc.player.getX();
            int posY = (int) mc.player.getY();
            int posZ = (int) mc.player.getZ();
            leftLines.add("XYZ: " + posX + " " + posY + " " + posZ);
        }

        if (showTPS.getValue()) {
            rightLines.add("TPS: " + String.format("%.0f", Managers.SERVER.getTPS()));
        }
        if (showFPS.getValue()) {
            int fps = FrameRateCounter.INSTANCE.getFps();
            rightLines.add("FPS: " + fps);
        }
        if (showPing.getValue()) {
            rightLines.add("PING: " + Managers.SERVER.getPing());
        }

        float lineHeight = font.getFontHeight("A") - 1;
        float fixedLeftX = 5;
        float fixedLeftY = mc.getWindow().getScaledHeight() - (leftLines.size() * lineHeight) - 12;
        float fixedRightY = mc.getWindow().getScaledHeight() - (rightLines.size() * lineHeight) - 12;

        for (int i = 0; i < leftLines.size(); i++) {
            String line = leftLines.get(i);
            String[] parts = line.split(": ");
            String label = parts[0] + ": ";
            String value = parts[1];

            float labelWidth = font.getStringWidth(label);

            font.drawString(context.getMatrices(), label, fixedLeftX, fixedLeftY + i * lineHeight, Color.WHITE.getRGB());
            font.drawString(context.getMatrices(), value, fixedLeftX + labelWidth, fixedLeftY + i * lineHeight, new Color(170, 170, 170).getRGB());
        }

        for (int i = 0; i < rightLines.size(); i++) {
            String line = rightLines.get(i);
            String[] parts = line.split(": ");
            String label = parts[0] + ": ";
            String value = parts[1];

            float lineWidth = font.getStringWidth(line);
            float labelWidth = font.getStringWidth(label);
            float fixedRightX = mc.getWindow().getScaledWidth() - lineWidth - 5;

            font.drawString(context.getMatrices(), label, fixedRightX, fixedRightY + i * lineHeight, Color.WHITE.getRGB());
            font.drawString(context.getMatrices(), value, fixedRightX + labelWidth, fixedRightY + i * lineHeight, new Color(170, 170, 170).getRGB());
        }

        setBounds(fixedLeftX, fixedLeftY, mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight());
    }
}