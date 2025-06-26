package thunder.hack.features.modules.client;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import net.minecraft.client.network.ServerInfo;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.discord.DiscordEventHandlers;
import thunder.hack.utility.discord.DiscordRPC;
import thunder.hack.utility.discord.DiscordRichPresence;

import java.io.*;
import java.util.Objects;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public final class RPC extends Module {
    private static final DiscordRPC rpc = DiscordRPC.INSTANCE;
    public static Setting<Mode> mode = new Setting<>("Picture", Mode.Recode);
    public static Setting<sMode> smode = new Setting<>("StateMode", sMode.Stats);
    public static Setting<String> state = new Setting<>("State", "1.0");
    public static DiscordRichPresence presence = new DiscordRichPresence();
    public static boolean started;
    static String String1 = "none";
    private final Timer timer_delay = new Timer();
    private static Thread thread;
    String slov;
    String[] rpc_perebor_en = {"Крякает нурлаптон", "Touching grass", "chatgpt request"};
    String[] rpc_perebor_ru = {"Крякает нурлаптон", "Трогает траву", "Спрашивает нейросеть"};
    int randomInt;

    public RPC() {
        super("DiscordRPC", Category.CLIENT);
    }

    public static void readFile() {
        try {
            File file = new File("ThunderHackRecode/misc/RPC.txt");
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    while (reader.ready()) {
                        String1 = reader.readLine();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static void WriteFile(String url1, String url2) {
        File file = new File("ThunderHackRecode/misc/RPC.txt");
        try {
            file.createNewFile();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(url1 + "SEPARATOR" + url2 + '\n');
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDisable() {
        started = false;
        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
        }
        rpc.Discord_Shutdown();
    }

    @Override
    public void onUpdate() {
        startRpc();
    }

    public void startRpc() {
        if (isDisabled()) return;
        if (!started) {
            started = true;
            DiscordEventHandlers handlers = new DiscordEventHandlers();
            rpc.Discord_Initialize("1264857690807468043", handlers, true, "");
            presence.startTimestamp = (System.currentTimeMillis() / 1000L);
            presence.largeImageText = "Kometa" ;
            rpc.Discord_UpdatePresence(presence);

            thread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    rpc.Discord_RunCallbacks();
                    presence.details = getDetails();

                    switch (smode.getValue()) {
                        case Stats ->
                                presence.state = "Otxod Medved AC ";
                        case Custom -> presence.state = state.getValue();
                        case Version -> presence.state = "Test" ;
                    }

                    presence.button_label_1 = "Download";
                    presence.button_url_1 = "https://discord.gg/VDaU6X3emN";

                    switch (mode.getValue()) {
                        case Recode -> presence.largeImageKey = "https://i.imgur.com/LqkcdfT.png";
                        case MegaCute ->
                                presence.largeImageKey = "https://i.imgur.com/Rnv4vEX.gif";
                        case Custom -> {
                            readFile();
                            presence.largeImageKey = String1.split("SEPARATOR")[0];
                            if (!Objects.equals(String1.split("SEPARATOR")[1], "none")) {
                                presence.smallImageKey = String1.split("SEPARATOR")[1];
                            }
                        }
                    }
                    rpc.Discord_UpdatePresence(presence);
                    try {
                        Thread.sleep(2000L);
                    } catch (InterruptedException ignored) {
                    }
                }
            }, "TH-RPC-Handler");
            thread.start();
        }
    }

    private String getDetails() {
        if (mc.currentScreen instanceof MultiplayerScreen
                || mc.currentScreen instanceof AddServerScreen
                || mc.currentScreen instanceof TitleScreen) {
            if (timer_delay.passedMs(60 * 1000)) {
                randomInt = (int) (Math.random() * rpc_perebor_en.length);
                slov = isRu() ? rpc_perebor_ru[randomInt] : rpc_perebor_en[randomInt];
                timer_delay.reset();
            }
            return slov;
        }
        return "Крякаем нурлаптон";
    }

    public enum Mode {Custom, MegaCute, Recode}

    public enum sMode {Custom, Stats, Version}
}
