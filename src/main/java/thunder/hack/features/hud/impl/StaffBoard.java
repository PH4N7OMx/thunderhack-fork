package thunder.hack.features.hud.impl;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import thunder.hack.features.cmd.impl.StaffCommand;
import thunder.hack.gui.font.FontRenderers;
import thunder.hack.features.hud.HudElement;
import thunder.hack.features.modules.client.HudEditor;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.animation.AnimationUtility;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StaffBoard extends HudElement {
    private static final Pattern validUserPattern = Pattern.compile("^\\w{3,16}$");
    private List<String> players = new ArrayList<>();
    private List<String> notSpec = new ArrayList<>();
    // Добавляем статический список для хранения администрации с команды /staff
    private static List<String> staffFromCommand = new ArrayList<>();

    private float vAnimation, hAnimation;

    public StaffBoard() {
        super("StaffBoard", 50, 50);
    }

    // Метод для добавления администрации из команды /staff
    public static void addStaffFromCommand(List<String> staffList) {
        staffFromCommand.clear();
        staffFromCommand.addAll(staffList);
    }

    // Метод для очистки списка администрации из команды
    public static void clearStaffFromCommand() {
        staffFromCommand.clear();
    }

    public static List<String> getOnlinePlayer() {
        return mc.player.networkHandler.getPlayerList().stream()
                .map(PlayerListEntry::getProfile)
                .map(GameProfile::getName)
                .filter(profileName -> validUserPattern.matcher(profileName).matches())
                .collect(Collectors.toList());
    }

    public static List<String> getOnlinePlayerD() {
        List<String> S = new ArrayList<>();
        for (PlayerListEntry player : mc.player.networkHandler.getPlayerList()) {
            if (mc.isInSingleplayer() || player.getScoreboardTeam() == null) break;
            String prefix = player.getScoreboardTeam().getPrefix().getString();
            if (check(Formatting.strip(prefix).toLowerCase())
                    || StaffCommand.staffNames.toString().toLowerCase().contains(player.getProfile().getName().toLowerCase())
                    || staffFromCommand.contains(player.getProfile().getName()) // Проверяем список из команды /staff
                    || player.getProfile().getName().toLowerCase().contains("1danil_mansoru1")
                    || player.getProfile().getName().toLowerCase().contains("barslan_")
                    || player.getProfile().getName().toLowerCase().contains("timmings")
                    || player.getProfile().getName().toLowerCase().contains("timings")
                    || player.getProfile().getName().toLowerCase().contains("ruthless")
                    || player.getScoreboardTeam().getPrefix().getString().contains("YT")
                    || (player.getScoreboardTeam().getPrefix().getString().contains("Y") && player.getScoreboardTeam().getPrefix().getString().contains("T"))) {
                String name = Arrays.asList(player.getScoreboardTeam().getPlayerList().toArray()).toString().replace("[", "").replace("]", "");

                if (player.getGameMode() == GameMode.SPECTATOR) {
                    S.add(player.getScoreboardTeam().getPrefix().getString() + name + ":gm3");
                    continue;
                }
                S.add(player.getScoreboardTeam().getPrefix().getString() + name + ":active");
            }
        }
        return S;
    }

    public List<String> getVanish() {
        List<String> list = new ArrayList<>();
        for (Team s : mc.world.getScoreboard().getTeams()) {
            if (s.getPrefix().getString().isEmpty() || mc.isInSingleplayer()) continue;
            String name = Arrays.asList(s.getPlayerList().toArray()).toString().replace("[", "").replace("]", "");

            if (getOnlinePlayer().contains(name) || name.isEmpty())
                continue;
            if (StaffCommand.staffNames.toString().toLowerCase().contains(name.toLowerCase())
                    && check(s.getPrefix().getString().toLowerCase())
                    || check(s.getPrefix().getString().toLowerCase())
                    || staffFromCommand.contains(name) // Проверяем список из команды /staff
                    || name.toLowerCase().contains("1danil_mansoru1")
                    || name.toLowerCase().contains("barslan_")
                    || name.toLowerCase().contains("timmings")
                    || name.toLowerCase().contains("timings")
                    || name.toLowerCase().contains("ruthless")
                    || s.getPrefix().getString().contains("YT")
                    || (s.getPrefix().getString().contains("Y") && s.getPrefix().getString().contains("T"))
            )
                list.add(s.getPrefix().getString() + name + ":vanish");
        }
        return list;
    }

    public static boolean check(String name) {
        if (mc.getCurrentServerEntry() != null && mc.getCurrentServerEntry().address.contains("mcfunny")) {
            return name.contains("helper") || name.contains("moder") || name.contains("модер") || name.contains("хелпер");
        }
        return name.contains("helper") || name.contains("moder") || name.contains("admin") || name.contains("owner") || name.contains("curator") || name.contains("куратор") || name.contains("модер") || name.contains("админ") || name.contains("хелпер") || name.contains("поддержка") || name.contains("сотрудник") || name.contains("зам") || name.contains("стажёр");
    }

    public void onRender2D(DrawContext context) {
        super.onRender2D(context);
        List<String> all = new java.util.ArrayList<>();
        all.addAll(players);
        all.addAll(notSpec);

        int y_offset1 = 0;
        float max_width = 50;

        for (String player : all) {
            if (y_offset1 == 0)
                y_offset1 += 4;

            y_offset1 += 9;

            String playerName = player.split(":")[0];
            String status = player.split(":")[1];
            String statusText = status.equalsIgnoreCase("vanish") ? "VANISH" :
                    status.equalsIgnoreCase("gm3") ? "SPEC" : "ONLINE";

            float nameWidth = FontRenderers.sf_bold_mini.getStringWidth(playerName);
            float statusWidth = FontRenderers.sf_bold_mini.getStringWidth(statusText);

            float width = (nameWidth + statusWidth + 10) * 1.1f; // Добавляем отступ между именем и статусом

            if (width > max_width)
                max_width = width;
        }

        vAnimation = AnimationUtility.fast(vAnimation, 14 + y_offset1, 15);
        hAnimation = AnimationUtility.fast(hAnimation, max_width, 15);

        Render2DEngine.drawHudBase(context.getMatrices(), getPosX(), getPosY(), hAnimation, vAnimation, HudEditor.hudRound.getValue());

        if (HudEditor.hudStyle.is(HudEditor.HudStyle.Glowing)) {
            FontRenderers.sf_bold.drawCenteredString(context.getMatrices(), "Staff", getPosX() + hAnimation / 2, getPosY() + 4, HudEditor.textColor.getValue().getColorObject());
        } else {
            FontRenderers.sf_bold.drawGradientCenteredString(context.getMatrices(), "Staff", getPosX() + hAnimation / 2, getPosY() + 4, 10);
        }

        Render2DEngine.addWindow(context.getMatrices(), getPosX(), getPosY(), getPosX() + hAnimation, getPosY() + vAnimation, 1f);
        int y_offset = 0;

        for (String player : all) {
            String playerName = player.split(":")[0];
            String status = player.split(":")[1];

            // Определяем цвет и текст статуса
            String statusText;
            Color statusColor;

            if (status.equalsIgnoreCase("vanish")) {
                statusText = "VANISH";
                statusColor = Color.RED;
            } else if (status.equalsIgnoreCase("gm3")) {
                statusText = "SPEC";
                statusColor = Color.YELLOW;
            } else {
                statusText = "ONLINE";
                statusColor = Color.GREEN;
            }

            // Рисуем имя игрока
            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), playerName, getPosX() + 5, getPosY() + 19 + y_offset, HudEditor.textColor.getValue().getColor());

            // Рисуем статус справа
            float statusX = getPosX() + hAnimation - FontRenderers.sf_bold_mini.getStringWidth(statusText) - 5;
            FontRenderers.sf_bold_mini.drawString(context.getMatrices(), statusText, statusX, getPosY() + 19 + y_offset, statusColor.getRGB());

            y_offset += 9;
        }
        Render2DEngine.popWindow();
        setBounds(getPosX(), getPosY(), hAnimation, vAnimation);
    }

    @Override
    public void onUpdate() {
        if (mc.player != null && mc.player.age % 10 == 0) {
            players = getVanish();
            notSpec = getOnlinePlayerD();
            players.sort(String::compareTo);
            notSpec.sort(String::compareTo);
        }
    }
}