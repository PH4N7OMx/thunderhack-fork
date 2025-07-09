package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.client.PacketQueueManager;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.client.PacketQueueManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.util.math.Vec3d;

public class Blink extends Module {
    public Blink() {
        super("Blink", Category.MOVEMENT);
    }

    private final Setting<Boolean> dummy = new Setting<>("Dummy", true);
    private final Setting<Boolean> ambush = new Setting<>("Ambush", false);
    private final Setting<Boolean> autoDisable = new Setting<>("AutoDisable", true);
    private final Setting<Boolean> autoReset = new Setting<>("AutoReset", false);
    private final Setting<Integer> resetAfter = new Setting<>("ResetAfter", 100, 1, 1000, v -> autoReset.getValue());
    private final Setting<ResetAction> resetAction = new Setting<>("ResetAction", ResetAction.RESET, v -> autoReset.getValue());
    private final Setting<Boolean> render = new Setting<>("Render", true);
    private final Setting<ColorSetting> circleColor = new Setting<>("Color", new ColorSetting(0xFF0080FF), v -> render.getValue());

    private enum ResetAction {
        RESET,
        BLINK
    }

    private OtherClientPlayerEntity dummyPlayer;
    private Vec3d originalPosition;
    private boolean isResetting = false;

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null || mc.isIntegratedServerRunning() || mc.getNetworkHandler() == null) {
            disable();
            return;
        }

        isResetting = false;
        originalPosition = mc.player.getPos();

        if (dummy.getValue()) {
            createDummyPlayer();
        }

        PacketQueueManager.startQueueing();
    }

    @Override
    public void onDisable() {
        if (mc.world == null || mc.player == null) return;

        try {
            if (!isResetting) {
                PacketQueueManager.flushOutgoing();
            }
            removeDummyPlayer();
        } catch (Exception e) {
            PacketQueueManager.clear();
        } finally {
            isResetting = false;
        }
    }

    @Override
    public String getDisplayInfo() {
        return String.valueOf(PacketQueueManager.getQueueSize());
    }

    private void createDummyPlayer() {
        if (mc.player == null || mc.world == null) return;

        try {
            OtherClientPlayerEntity clone = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
            clone.headYaw = mc.player.getHeadYaw();
            clone.copyPositionAndRotation(mc.player);
            clone.setUuid(UUID.randomUUID());

            mc.world.addEntity(clone);
            dummyPlayer = clone;
        } catch (Exception e) {
        }
    }

    private void removeDummyPlayer() {
        if (dummyPlayer != null && mc.world != null) {
            try {
                mc.world.removeEntity(dummyPlayer.getId(), Entity.RemovalReason.DISCARDED);
            } catch (Exception e) {
            } finally {
                dummyPlayer = null;
            }
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (fullNullCheck() || isResetting) return;

        if (ambush.getValue() && event.getPacket() instanceof PlayerInteractEntityC2SPacket) {
            disable();
            return;
        }

        if (PacketQueueManager.isQueueing()) {
            PacketQueueManager.queuePacket(event.getPacket());
            event.cancel();
        }
    }

    @EventHandler
    public void onUpdate(EventTick event) {
        if (fullNullCheck() || isResetting) return;

        if (PacketQueueManager.getQueueSize() > 1000) {
            safeDisable();
            return;
        }

        if (autoReset.getValue() && PacketQueueManager.getPositionCount() > resetAfter.getValue()) {
            handleAutoReset();
        }

        if (autoDisable.getValue() && PacketQueueManager.getQueueSize() >= 50) {
            safeDisable();
        }
    }

    private void handleAutoReset() {
        if (isResetting || fullNullCheck()) return;

        isResetting = true;

        try {
            switch (resetAction.getValue()) {
                case RESET:
                    PacketQueueManager.cancel();

                    if (originalPosition != null && mc.player != null) {
                        mc.player.updatePosition(originalPosition.getX(), originalPosition.getY(), originalPosition.getZ());
                        mc.player.setVelocity(0, 0, 0);
                    }

                    if (dummyPlayer != null && originalPosition != null) {
                        dummyPlayer.setPos(originalPosition.getX(), originalPosition.getY(), originalPosition.getZ());
                    }
                    break;

                case BLINK:
                    PacketQueueManager.flushOutgoing();
                    if (dummyPlayer != null && mc.player != null) {
                        dummyPlayer.copyPositionAndRotation(mc.player);
                    }
                    if (mc.player != null) {
                        originalPosition = mc.player.getPos();
                    }
                    PacketQueueManager.startQueueing();
                    break;
            }

            if (autoDisable.getValue()) {
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                        if (this.isEnabled()) {
                            this.disable();
                        }
                    } catch (InterruptedException ignored) {}
                }).start();
            }

        } catch (Exception e) {
            safeDisable();
        } finally {
            isResetting = false;
        }
    }

    private void safeDisable() {
        try {
            PacketQueueManager.clear();
            disable();
        } catch (Exception e) {
            this.setEnabled(false);
        }
    }

    public void onRender3D(MatrixStack stack) {
        if (mc.player == null || mc.world == null || !render.getValue() || originalPosition == null || isResetting) return;

        renderCircle(originalPosition);
    }

    private void renderCircle(Vec3d centerPos) {
        if (centerPos == null) return;

        ArrayList<Vec3d> vecs = new ArrayList<>();
        double x = centerPos.x;
        double y = centerPos.y;
        double z = centerPos.z;

        for (int i = 0; i <= 360; ++i) {
            Vec3d vec = new Vec3d(
                    x + Math.sin(i * Math.PI / 180.0) * 0.5D,
                    y + 0.01,
                    z + Math.cos(i * Math.PI / 180.0) * 0.5D
            );
            vecs.add(vec);
        }

        Color color = new Color(circleColor.getValue().getRed(),
                circleColor.getValue().getGreen(),
                circleColor.getValue().getBlue(),
                circleColor.getValue().getAlpha());

        for (int j = 0; j < vecs.size() - 1; ++j) {
        }
    }
    public void resetQueueFromAura() {
        if (isResetting || fullNullCheck()) return;
        isResetting = true;
        try {
            PacketQueueManager.flushOutgoing();
            if (dummyPlayer != null && mc.player != null) {
                dummyPlayer.copyPositionAndRotation(mc.player);
            }
            if (mc.player != null) {
                originalPosition = mc.player.getPos();
            }
        } catch (Exception e) {
            safeDisable();
        } finally {
            isResetting = false;
        }
    }

    public void startQueueFromAura() {
        if (fullNullCheck()) return;
        PacketQueueManager.startQueueing();
    }
}