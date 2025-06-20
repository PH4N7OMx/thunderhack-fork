package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.util.math.Vec3d;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.player.PlayerEntityCopy;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class FakeLag extends Module {
    public FakeLag() {
        super("FakeLagBeta", Category.MOVEMENT);
    }

    private final Setting<Boolean> blink = new Setting<>("Blink", false);
    private final Setting<Boolean> reviveMode = new Setting<>("Player Revive", false);
    private final Setting<Boolean> silent = new Setting<>("Silent", false);
    private final Setting<Boolean> bypass = new Setting<>("Bypass", false);
    private final Setting<Integer> blinkTime = new Setting<>("Blink Time", 100, 0, 10000);

    private long lastBlinkToggle = System.currentTimeMillis();
    private boolean blinkState = false;

    private PlayerEntityCopy blinkPlayer;
    public static Vec3d lastPos = Vec3d.ZERO;
    private Vec3d prevVelocity = Vec3d.ZERO;
    private float prevYaw = 0;
    private boolean prevSprinting = false;
    private final Queue<Packet<?>> storedPackets = new LinkedList<>();
    private final Queue<Packet<?>> storedTransactions = new LinkedList<>();
    private final AtomicBoolean sending = new AtomicBoolean(false);
    private ClientPlayerEntity targetPlayer;
    private final Setting<Boolean> render = new Setting<>("Render", true);
    private final Setting<RenderMode> renderMode = new Setting<>("Render Mode", RenderMode.Circle);
    private final Setting<ColorSetting> circleColor = new Setting<>("Color", new ColorSetting(0xFFda6464));

    private enum RenderMode {
        Circle,
        Model,
        Both
    }

    @Override
    public void onEnable() {
        if (mc.player == null || mc.world == null || mc.isIntegratedServerRunning() || mc.getNetworkHandler() == null) {
            disable();
            return;
        }

        storedTransactions.clear();
        lastPos = mc.player.getPos();
        prevVelocity = mc.player.getVelocity();
        prevYaw = mc.player.getYaw();
        prevSprinting = mc.player.isSprinting();
        mc.world.spawnEntity(new ClientPlayerEntity(mc, mc.world, mc.getNetworkHandler(), mc.player.getStatHandler(), mc.player.getRecipeBook(), mc.player.lastSprinting, mc.player.isSneaking()));
        sending.set(false);
        storedPackets.clear();
        lastBlinkToggle = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        if (mc.world == null || mc.player == null) return;

        while (!storedPackets.isEmpty())
            sendPacket(storedPackets.poll());

        if (blinkPlayer != null) blinkPlayer.deSpawn();
        blinkPlayer = null;
        targetPlayer = null;
    }

    @Override
    public String getDisplayInfo() {
        return Integer.toString(storedPackets.size());
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive event) {
        if (!bypass.getValue() && event.getPacket() instanceof EntityVelocityUpdateS2CPacket vel && vel.getEntityId() == mc.player.getId()) {
            disable(isRu() ? "Выключенно из-за велосити!" : "Disabled due to velocity!");
        }
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (fullNullCheck()) return;

        Packet<?> packet = event.getPacket();

        if (sending.get()) {
            return;
        }

        if (packet instanceof CommonPongC2SPacket) {
            storedTransactions.add(packet);
        }

        if (blink.getValue()) {
            if (packet instanceof PlayerMoveC2SPacket) {
                event.cancel();
                storedPackets.add(packet);
            }
        }
    }

    @EventHandler
    public void onUpdate(EventTick event) {
        if (fullNullCheck()) return;

        long currentTime = System.currentTimeMillis();

        if (blink.getValue() && currentTime - lastBlinkToggle >= blinkTime.getValue()) {
            blinkState = !blinkState;
            lastBlinkToggle = currentTime;
        }

        if (blinkState) {
            sendPackets();
        }

        if (reviveMode.getValue() && blink.getValue()) {
            handlePlayerRevive();
        }
    }

    private void handlePlayerRevive() {
        if (targetPlayer == null || mc.player == null) return;

        double distance = mc.player.getPos().distanceTo(targetPlayer.getPos());

        if (distance < 2.0 && blinkState) {
            Vec3d behindPlayer = targetPlayer.getPos().subtract(targetPlayer.getRotationVector().normalize().multiply(1.5));
            mc.player.updatePosition(behindPlayer.x, behindPlayer.y, behindPlayer.z);
            blinkState = false;
        } else if (!blinkState && targetPlayer.hurtTime > 0) {
            blinkState = true;
        }
    }

    private void sendPackets() {
        if (mc.player == null) return;
        sending.set(true);

        while (!storedPackets.isEmpty()) {
            Packet<?> packet = storedPackets.poll();
            sendPacket(packet);
            if (packet instanceof PlayerMoveC2SPacket && !(packet instanceof PlayerMoveC2SPacket.LookAndOnGround)) {
                lastPos = new Vec3d(((PlayerMoveC2SPacket) packet).getX(mc.player.getX()), ((PlayerMoveC2SPacket) packet).getY(mc.player.getY()), ((PlayerMoveC2SPacket) packet).getZ(mc.player.getZ()));

                if (blinkPlayer != null) {
                    blinkPlayer.deSpawn();
                    blinkPlayer = new PlayerEntityCopy();
                    blinkPlayer.spawn();
                }
            }
        }

        sending.set(false);
        storedPackets.clear();
    }

    public void onRender3D(MatrixStack stack) {
        if (mc.player == null || mc.world == null) return;

        if (render.getValue() && lastPos != null) {
            if (renderMode.getValue() == RenderMode.Circle || renderMode.getValue() == RenderMode.Both) {
                float[] hsb = Color.RGBtoHSB(circleColor.getValue().getRed(), circleColor.getValue().getGreen(), circleColor.getValue().getBlue(), null);
                float hue = (float) (System.currentTimeMillis() % 7200L) / 7200F;
                int rgb = Color.getHSBColor(hue, hsb[1], hsb[2]).getRGB();
                ArrayList<Vec3d> vecs = new ArrayList<>();
                double x = lastPos.x;
                double y = lastPos.y;
                double z = lastPos.z;

                for (int i = 0; i <= 360; ++i) {
                    Vec3d vec = new Vec3d(
                            x + Math.sin(i * Math.PI / 180.0) * 0.5D,
                            y + 0.01,
                            z + Math.cos(i * Math.PI / 180.0) * 0.5D
                    );
                    vecs.add(vec);
                }

                for (int j = 0; j < vecs.size() - 1; ++j) {
                    Render3DEngine.drawLine(vecs.get(j), vecs.get(j + 1), new Color(rgb));
                    hue += (1F / 360F);
                    rgb = Color.getHSBColor(hue, hsb[1], hsb[2]).getRGB();
                }
            }

            if (renderMode.getValue() == RenderMode.Model || renderMode.getValue() == RenderMode.Both) {
                if (blinkPlayer == null) {
                    blinkPlayer = new PlayerEntityCopy();
                    blinkPlayer.spawn();
                }
            }
        }
    }
}