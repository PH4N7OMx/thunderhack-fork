package thunder.hack.features.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class AutoTpAccept extends Module {
    public AutoTpAccept() {
        super("AutoTPaccept", Category.MISC);
    }

    public Setting<Boolean> onlyFriends = new Setting<>("onlyFriends", true);

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (fullNullCheck()) return;

        if (event.getPacket() instanceof GameMessageS2CPacket) {
            GameMessageS2CPacket packet = event.getPacket();
            String message = packet.content().getString();
            if (message.contains("Игрок") && message.contains("отправил запрос на телепортацию!")) {
                String playerName = extractPlayerName(message);

                if (onlyFriends.getValue()) {
                    if (Managers.FRIEND.isFriend(playerName)) {
                        sendTpAccept(playerName);
                    }
                } else {
                    sendTpAccept(playerName);
                }
            }
        }
    }

    private void sendTpAccept(String playerName) {
        if (playerName != null && !playerName.isEmpty()) {
            mc.getNetworkHandler().sendChatCommand("tpaccept " + playerName);
        }
    }

    private String extractPlayerName(String message) {
        String regex = "Игрок (\\w+) отправил запрос на телепортацию!";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
