package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;
import thunder.hack.features.modules.client.HudEditor;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class KeyBinds extends HudElement {
    public final Setting<ColorSetting> textColor = new Setting<>("TextColor", new ColorSetting(Color.WHITE.getRGB()));
    public final Setting<Boolean> onlyEnabled = new Setting<>("OnlyEnabled", false);

    private float vAnimation = 0, hAnimation = 0;

    public KeyBinds() {
        super("KeyBinds", 80, 100);
    }

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        List<Module> binds = Managers.MODULE.modules.stream()
                .filter(m -> !Objects.equals(m.getBind().getBind(), "None"))
                .filter(m -> !onlyEnabled.getValue() || m.isOn())
                .filter(m -> m != ModuleManager.clickGui && m != ModuleManager.thunderHackGui)
                .collect(Collectors.toList());

        var headerFont = FontRenderers.sf_bold;
        var entryFont = FontRenderers.sf_bold_mini;

        // --- Вычисление ширины для выравнивания ---
        float leftPad = 8f;
        float rightPad = 8f;
        float minWidth = headerFont.getStringWidth("KeyBinds") + leftPad + rightPad;
        float maxKeyWidth = 0f;
        float maxNameWidth = 0f;
        for (Module m : binds) {
            maxNameWidth = Math.max(maxNameWidth, entryFont.getStringWidth(m.getName()));
            maxKeyWidth = Math.max(maxKeyWidth, entryFont.getStringWidth(getShortKeyName(m)));
        }
        float contentWidth = leftPad + maxNameWidth + 12f + maxKeyWidth + rightPad;
        float boxWidth = Math.max(minWidth, contentWidth);

        // --- Высота ---
        float headerHeight = 19f; // чуть больше для воздуха сверху
        float lineHeight = 9f;
        float listHeight = binds.size() * lineHeight;
        float bottomPad = 4f; // минимальный паддинг снизу
        vAnimation = AnimationUtility.fast(vAnimation, headerHeight + listHeight + bottomPad, 15);
        hAnimation = AnimationUtility.fast(hAnimation, boxWidth, 15);

        float x = getPosX();
        float y = getPosY();

        // --- Фон с закруглениями ---
        Render2DEngine.drawHudBase(context.getMatrices(), x, y, hAnimation, vAnimation, HudEditor.hudRound.getValue());

        // --- Заголовок ---
        headerFont.drawGradientCenteredString(context.getMatrices(), "KeyBinds", x + hAnimation / 2, y + 5, 10); // чуть ниже для воздуха

        // --- Линия ---
        Render2DEngine.horizontalGradient(
                context.getMatrices(),
                x + 6, y + headerHeight - 2,
                x + hAnimation - 6, y + headerHeight - 0.5f,
                new Color(120, 170, 255, 70),
                new Color(255, 255, 255, 0)
        );

        // --- Список биндов ---
        float yStart = y + headerHeight + 3f;
        int i = 0;
        for (Module m : binds) {
            float yAnim = yStart + i * lineHeight;

            // Название слева
            entryFont.drawGradientString(
                    context.getMatrices(),
                    m.getName(),
                    x + leftPad,
                    yAnim,
                    10
            );

            // Клавиша справа
            entryFont.drawGradientString(
                    context.getMatrices(),
                    getShortKeyName(m),
                    x + hAnimation - rightPad - entryFont.getStringWidth(getShortKeyName(m)),
                    yAnim,
                    10
            );
            i++;
        }

        setBounds(x, y, hAnimation, vAnimation);
    }

    private static final Map<Character, Character> RUS_TO_ENG_MAP = Map.ofEntries(
            Map.entry('Ф', 'A'),
            Map.entry('И', 'B'),
            Map.entry('С', 'C'),
            Map.entry('В', 'D'),
            Map.entry('У', 'E'),
            Map.entry('А', 'F'),
            Map.entry('П', 'G'),
            Map.entry('Р', 'H'),
            Map.entry('Ш', 'I'),
            Map.entry('О', 'J'),
            Map.entry('Л', 'K'),
            Map.entry('Д', 'L'),
            Map.entry('Ь', 'M'),
            Map.entry('Т', 'N'),
            Map.entry('Щ', 'O'),
            Map.entry('З', 'P'),
            Map.entry('Й', 'Q'),
            Map.entry('К', 'R'),
            Map.entry('Ы', 'S'),
            Map.entry('Е', 'T'),
            Map.entry('Г', 'U'),
            Map.entry('М', 'V'),
            Map.entry('Ц', 'W'),
            Map.entry('Ч', 'X'),
            Map.entry('Н', 'Y'),
            Map.entry('Я', 'Z')
    );

    @NotNull
    public static String getShortKeyName(Module feature) {
        String sbind = feature.getBind().getBind();

        if (sbind.length() == 1) {
            char ch = Character.toUpperCase(sbind.charAt(0));
            if (RUS_TO_ENG_MAP.containsKey(ch)) {
                sbind = String.valueOf(RUS_TO_ENG_MAP.get(ch));
            } else if (!(ch >= 'A' && ch <= 'Z')) {
                sbind = String.valueOf(ch);
            }
        }

        return switch (sbind.toUpperCase()) {
            case "LEFT_CONTROL" -> "LCtrl";
            case "RIGHT_CONTROL" -> "RCtrl";
            case "LEFT_SHIFT" -> "LShift";
            case "RIGHT_SHIFT" -> "RShift";
            case "LEFT_ALT" -> "LAlt";
            case "RIGHT_ALT" -> "RAlt";
            default -> sbind.toUpperCase();
        };
    }
}
