package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

public class Freeze extends Module {
    public Freeze() {
        super("Freeze", Category.MOVEMENT);
    }

    private float fakeYaw, fakePitch, prevFakeYaw, prevFakePitch;
    private double fakeX, fakeY, fakeZ, prevFakeX, prevFakeY, prevFakeZ;
    public LivingEntity trackEntity;

    @Override
    public void onEnable() {
        mc.chunkCullingEnabled = false;
        trackEntity = null;

        fakePitch = mc.player.getPitch();
        fakeYaw = mc.player.getYaw();

        prevFakePitch = fakePitch;
        prevFakeYaw = fakeYaw;

        fakeX = mc.player.getX();
        fakeY = mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose());
        fakeZ = mc.player.getZ();

        prevFakeX = mc.player.getX();
        prevFakeY = mc.player.getY();
        prevFakeZ = mc.player.getZ();
    }

    @EventHandler
    public void onAttack(EventAttack e) {
        if (!e.isPre() && e.getEntity() instanceof LivingEntity entity)
            trackEntity = entity;
    }

    @Override
    public void onDisable() {
        if (fullNullCheck()) return;
        mc.chunkCullingEnabled = true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSync(EventSync e) {
        prevFakeYaw = fakeYaw;
        prevFakePitch = fakePitch;

        if (trackEntity != null) {
            fakeYaw = trackEntity.getYaw();
            fakePitch = trackEntity.getPitch();

            prevFakeX = fakeX;
            prevFakeY = fakeY;
            prevFakeZ = fakeZ;

            fakeX = trackEntity.getX();
            fakeY = trackEntity.getY() + trackEntity.getEyeHeight(trackEntity.getPose());
            fakeZ = trackEntity.getZ();
        } else {
            fakeYaw = mc.player.getYaw();
            fakePitch = mc.player.getPitch();
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMove(EventMove e) {
        e.setX(0.);
        e.setY(0.);
        e.setZ(0.);
        e.cancel();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (e.getPacket() instanceof PlayerMoveC2SPacket)
            e.cancel();
    }

    public float getFakeYaw() {
        return (float) Render2DEngine.interpolate(prevFakeYaw, fakeYaw, Render3DEngine.getTickDelta());
    }

    public float getFakePitch() {
        return (float) Render2DEngine.interpolate(prevFakePitch, fakePitch, Render3DEngine.getTickDelta());
    }

    public double getFakeX() {
        return Render2DEngine.interpolate(prevFakeX, fakeX, Render3DEngine.getTickDelta());
    }

    public double getFakeY() {
        return Render2DEngine.interpolate(prevFakeY, fakeY, Render3DEngine.getTickDelta());
    }

    public double getFakeZ() {
        return Render2DEngine.interpolate(prevFakeZ, fakeZ, Render3DEngine.getTickDelta());
    }
}
