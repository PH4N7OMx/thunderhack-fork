package thunder.hack.features.hud.impl;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.Module;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.setting.Setting;
import thunder.hack.utility.render.animation.AnimationUtility;
import thunder.hack.utility.render.Render2DEngine;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleList extends HudElement {
    private final Setting<Boolean> showBinds = new Setting<>("ShowBinds", true);
    private final Setting<Boolean> hideRender = new Setting<>("HideRender", true);
    private final Setting<Boolean> hideHud = new Setting<>("HideHud", true);

    private final Map<Module, Float> moduleAnimations = new HashMap<>();
    private final Map<Module, Float> fadeAnimations = new HashMap<>();

    public ModuleList() {
        super("ArrayList", 150, 30);
    }

    @Override
    public void onRender2D(DrawContext context) {
        super.onRender2D(context);

        List<Module> modules = getFilteredModules();
        var font = FontRenderers.modules;

        float screenWidth = mc.getWindow().getScaledWidth();
        float fixedX = screenWidth;
        float fixedY = 5;

        float maxWidth = 0;
        float yOffset = 0;

        List<Module> allModules = new ArrayList<>(Managers.MODULE.modules);

        for (Module m : allModules) {
            if (!moduleAnimations.containsKey(m)) {
                moduleAnimations.put(m, 0f);
            }
            if (!fadeAnimations.containsKey(m)) {
                fadeAnimations.put(m, 0f);
            }

            boolean shouldShow = modules.contains(m) && m.isOn();
            float currentAnim = moduleAnimations.get(m);
            float targetAnim = shouldShow ? 1f : 0f;
            float newAnim = AnimationUtility.fast(currentAnim, targetAnim, 15);
            moduleAnimations.put(m, newAnim);

            float currentFade = fadeAnimations.get(m);
            float targetFade = shouldShow ? 1f : 0f;
            float newFade = AnimationUtility.fast(currentFade, targetFade, 12);
            fadeAnimations.put(m, newFade);
        }

        List<Module> visibleModules = allModules.stream()
                .filter(m -> moduleAnimations.get(m) > 0.01f || fadeAnimations.get(m) > 0.01f)
                .sorted(Comparator.comparing(module -> {
                    String displayName = getModuleDisplayName(module);
                    float textWidth = font.getStringWidth(displayName);
                    return textWidth * -1;
                }))
                .collect(Collectors.toList());

        for (Module m : visibleModules) {
            float newAnim = moduleAnimations.get(m);
            float fadeAnim = fadeAnimations.get(m);

            if (newAnim < 0.01f && fadeAnim < 0.01f) continue;

            String displayName = getModuleDisplayName(m);
            String fullDisplayName = displayName;

            float totalTextWidth;
            if (showBinds.getValue() && !Objects.equals(m.getBind().getBind(), "None")) {
                String bindName = getShortKeyName(m);
                fullDisplayName = bindName + " <- " + displayName;
                totalTextWidth = font.getStringWidth(fullDisplayName);
            } else {
                totalTextWidth = font.getStringWidth(displayName);
            }

            float moduleBoxWidth = totalTextWidth + 2;
            float totalWidth = moduleBoxWidth;

            if (totalWidth > maxWidth) {
                maxWidth = totalWidth;
            }

            float moduleBoxX = fixedX - moduleBoxWidth;
            float textY = fixedY + yOffset;

            Color moduleColor = HudEditor.getColor((int)(yOffset));
            Color fadeColor = new Color(moduleColor.getRed(), moduleColor.getGreen(), moduleColor.getBlue(), (int)(255 * fadeAnim));

            float animatedX = moduleBoxX + (1 - newAnim) * 50;

            if (showBinds.getValue() && !Objects.equals(m.getBind().getBind(), "None")) {
                String bindText = getShortKeyName(m) + " <- ";

                font.drawString(
                        context.getMatrices(),
                        bindText,
                        animatedX + 1,
                        textY + 3,
                        new Color(255, 255, 255, (int)(255 * fadeAnim)).getRGB()
                );

                float bindWidth = font.getStringWidth(bindText);

                font.drawGradientString(
                        context.getMatrices(),
                        displayName,
                        animatedX + 1 + bindWidth,
                        textY + 3,
                        10
                );
            } else {
                font.drawGradientString(
                        context.getMatrices(),
                        displayName,
                        animatedX + 1,
                        textY + 3,
                        10
                );
            }

            yOffset += 11 * fadeAnim;
        }

        setBounds(fixedX - maxWidth, fixedY, maxWidth, yOffset);
    }

    private List<Module> getFilteredModules() {
        return Managers.MODULE.modules.stream()
                .filter(m -> m.isOn())
                .filter(m -> !hideRender.getValue() || m.getCategory() != Module.Category.RENDER)
                .filter(m -> !hideHud.getValue() || m.getCategory() != Module.Category.HUD)
                .filter(m -> m != ModuleManager.clickGui && m != ModuleManager.thunderHackGui)
                .filter(m -> m.isDrawn())
                .sorted(Comparator.comparing(module -> {
                    String displayName = getModuleDisplayName(module);
                    float textWidth = FontRenderers.modules.getStringWidth(displayName);
                    return textWidth * -1;
                }))
                .collect(Collectors.toList());
    }

    private String getModuleDisplayName(Module module) {
        String baseName = module.getDisplayName();
        if (module.getDisplayInfo() != null) {
            return baseName + Formatting.GRAY + " [" + Formatting.WHITE + module.getDisplayInfo() + Formatting.GRAY + "]";
        }
        return baseName;
    }

    private static final Map<Character, Character> RUS_TO_ENG_MAP = Map.ofEntries(
            Map.entry('Ф', 'A'), Map.entry('И', 'B'), Map.entry('С', 'C'), Map.entry('В', 'D'),
            Map.entry('У', 'E'), Map.entry('А', 'F'), Map.entry('П', 'G'), Map.entry('Р', 'H'),
            Map.entry('Ш', 'I'), Map.entry('О', 'J'), Map.entry('Л', 'K'), Map.entry('Д', 'L'),
            Map.entry('Ь', 'M'), Map.entry('Т', 'N'), Map.entry('Щ', 'O'), Map.entry('З', 'P'),
            Map.entry('Й', 'Q'), Map.entry('К', 'R'), Map.entry('Ы', 'S'), Map.entry('Е', 'T'),
            Map.entry('Г', 'U'), Map.entry('М', 'V'), Map.entry('Ц', 'W'), Map.entry('Ч', 'X'),
            Map.entry('Н', 'Y'), Map.entry('Я', 'Z')
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