package thunder.hack.utility.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.CommonPlayerSpawnInfo;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.HealthUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PacketQueueManager {
    private static final ConcurrentLinkedQueue<PacketSnapshot> packetQueue = new ConcurrentLinkedQueue<>();
    private static boolean isQueueing = false;
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static class PacketSnapshot {
        public final Packet<?> packet;
        public final TransferOrigin origin;
        public final long timestamp;

        public PacketSnapshot(Packet<?> packet, TransferOrigin origin) {
            this.packet = packet;
            this.origin = origin;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public enum TransferOrigin {
        INCOMING,
        OUTGOING
    }

    public static void startQueueing() {
        isQueueing = true;
        packetQueue.clear();
    }

    public static void stopQueueing() {
        isQueueing = false;
    }

    public static void queuePacket(Packet<?> packet) {
        if (!isQueueing) return;

        if (shouldQueuePacket(packet)) {
            packetQueue.add(new PacketSnapshot(packet, TransferOrigin.OUTGOING));
        }
    }

    private static boolean shouldQueuePacket(Packet<?> packet) {
        if (packet instanceof HandshakeC2SPacket) return false;
        if (packet instanceof ChatMessageC2SPacket) return false;
        if (packet instanceof PlayerPositionLookS2CPacket) return false;
        if (packet instanceof GameStateChangeS2CPacket) return false;
        if (packet instanceof HealthUpdateS2CPacket) {
            HealthUpdateS2CPacket healthPacket = (HealthUpdateS2CPacket) packet;
            if (healthPacket.getHealth() <= 0) return false;
        }

        return true;
    }

    public static void flushOutgoing() {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        try {
            while (!packetQueue.isEmpty()) {
                PacketSnapshot snapshot = packetQueue.poll();
                if (snapshot != null && snapshot.origin == TransferOrigin.OUTGOING) {
                    mc.getNetworkHandler().sendPacket(snapshot.packet);
                }
            }
        } catch (Exception e) {
            // Обработка исключений
        }

        stopQueueing();
    }

    public static void flush(int count) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        int counter = 0;

        try {
            PacketSnapshot snapshot;
            while ((snapshot = packetQueue.poll()) != null && counter < count) {
                if (snapshot.packet instanceof PlayerMoveC2SPacket) {
                    PlayerMoveC2SPacket movePacket = (PlayerMoveC2SPacket) snapshot.packet;
                    if (movePacket.changesPosition()) {
                        counter++;
                    }
                }

                if (snapshot.origin == TransferOrigin.OUTGOING) {
                    mc.getNetworkHandler().sendPacket(snapshot.packet);
                }
            }
        } catch (Exception e) {
            // Обработка исключений
        }
    }

    public static void cancel() {
        Vec3d firstPos = getFirstPosition();
        if (firstPos != null && mc.player != null) {
            mc.player.setPos(firstPos.getX(), firstPos.getY(), firstPos.getZ());
        }

        for (PacketSnapshot snapshot : packetQueue) {
            if (!(snapshot.packet instanceof PlayerMoveC2SPacket)) {
                if (snapshot.origin == TransferOrigin.OUTGOING && mc.getNetworkHandler() != null) {
                    try {
                        mc.getNetworkHandler().sendPacket(snapshot.packet);
                    } catch (Exception e) {
                        // Обработка исключений
                    }
                }
            }
        }

        packetQueue.clear();
        stopQueueing();
    }

    public static int getQueueSize() {
        return packetQueue.size();
    }

    public static int getPositionCount() {
        return (int) packetQueue.stream()
                .filter(snapshot -> snapshot.packet instanceof PlayerMoveC2SPacket)
                .filter(snapshot -> ((PlayerMoveC2SPacket) snapshot.packet).changesPosition())
                .count();
    }

    private static Vec3d getFirstPosition() {
        for (PacketSnapshot snapshot : packetQueue) {
            if (snapshot.packet instanceof PlayerMoveC2SPacket) {
                PlayerMoveC2SPacket movePacket = (PlayerMoveC2SPacket) snapshot.packet;
                if (movePacket.changesPosition()) {
                    return new Vec3d(
                            movePacket.getX(mc.player.getX()),
                            movePacket.getY(mc.player.getY()),
                            movePacket.getZ(mc.player.getZ())
                    );
                }
            }
        }
        return null;
    }

    public static boolean isLagging() {
        return !packetQueue.isEmpty();
    }

    public static boolean isQueueing() {
        return isQueueing;
    }

    public static void clear() {
        packetQueue.clear();
        stopQueueing();
    }
}