package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventAttack;
import thunder.hack.events.impl.EventPlayerTravel;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class FireworkBoost extends Module {


    private final Setting<Integer> delay = new Setting<>("Delay", 500, 0, 1000);
    private final Setting<Boolean> log = new Setting<>("Log", false);
    private final Setting<BypassMode> bypass = new Setting<>("Bypass", BypassMode.None);
    private final Setting<DoubleTapMode> doubleTap = new Setting<>("DoubleTap", DoubleTapMode.None);

    private final Setting<SpeedMethod> speedMethod = new Setting<>("Speed Method", SpeedMethod.GRIM);
    private final Setting<Float> speedMultiplier = new Setting<>("Speed Multiplier", 1.5f, 0.1f, 5.0f, v -> speedMethod.getValue() != SpeedMethod.PACKET);
    private final Setting<Float> maxSpeed = new Setting<>("Max Speed", 5.0f, 0.1f, 20.0f, v -> speedMethod.getValue() == SpeedMethod.CUSTOM);
    private final Setting<Float> minDistance = new Setting<>("Min Distance", 2.0f, 0.1f, 10.0f);
    private final Setting<Boolean> useTimer = new Setting<>("Use Timer", false);
    private final Setting<Float> timerSpeed = new Setting<>("Timer Speed", 1.0f, 0.1f, 10.0f, v -> useTimer.getValue());
    private final Setting<Boolean> autoDisable = new Setting<>("AutoDisable", true);
    private final Setting<Integer> disableTime = new Setting<>("Disable Time", 5, 1, 30);

    private final Timer autoDisableTimer = new Timer();
    private final Timer fireworkTimer = new Timer();

    public FireworkBoost() {
        super("ElytraSpeed", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (autoDisable.getValue()) {
            autoDisableTimer.reset();
        }
    }

    @EventHandler
    public void onUpdate() {

        if (autoDisable.getValue() && autoDisableTimer.passedS(disableTime.getValue())) {
            this.disable();
        }

        if (!fireworkTimer.passedMs(delay.getValue())) return;

        if (doubleTap.getValue() == DoubleTapMode.Auto && !hasMaxElytraSpeed()) {
            useDoubleFirework();
            logNotification("Double tap used", Notification.Type.SUCCESS);
        } else {
            useFirework();
        }

        fireworkTimer.reset();
    }

    @EventHandler
    public void onPlayerAttack(EventAttack event) {
        if (autoDisable.getValue() && event.getEntity() instanceof PlayerEntity) {
            this.disable();
        }
    }

    @EventHandler
    public void modifyVelocity(EventPlayerTravel e) {
        if (speedMethod.getValue() == SpeedMethod.GRIM) {
            if (bypass.getValue() == BypassMode.Grim && !e.isPre() && ThunderHack.core.getSetBackTime() > 1000) {
                if (mc.player.isFallFlying()) {
                    Vec3d currentVelocity = mc.player.getVelocity();
                    Vec3d motionVector = new Vec3d(
                            currentVelocity.x * speedMultiplier.getValue(),
                            currentVelocity.y,
                            currentVelocity.z * speedMultiplier.getValue()
                    );

                    if (motionVector.lengthSquared() > maxSpeed.getValue() * maxSpeed.getValue()) {
                        double scaleFactor = maxSpeed.getValue() / motionVector.length();
                        motionVector = new Vec3d(
                                motionVector.x * scaleFactor,
                                motionVector.y,
                                motionVector.z * scaleFactor
                        );
                    }

                    mc.player.setVelocity(motionVector);

                }
            }
        } else if (speedMethod.getValue() == SpeedMethod.PACKET) {
            Vec3d packetVelocity = new Vec3d(
                    mc.player.getVelocity().x * speedMultiplier.getValue(),
                    mc.player.getVelocity().y,
                    mc.player.getVelocity().z * speedMultiplier.getValue()
            );
            mc.player.setVelocity(packetVelocity);
            sendVelocityPacket(packetVelocity);
        } else if (speedMethod.getValue() == SpeedMethod.CUSTOM) {
            Vec3d currentVelocity = mc.player.getVelocity();
            double currentSpeed = Math.sqrt(
                    currentVelocity.x * currentVelocity.x +
                            currentVelocity.z * currentVelocity.z
            );

            if (currentSpeed > maxSpeed.getValue()) {
                double scaleFactor = maxSpeed.getValue() / currentSpeed;
                currentVelocity = new Vec3d(
                        currentVelocity.x * scaleFactor,
                        currentVelocity.y,
                        currentVelocity.z * scaleFactor
                );
                logNotification("Speed capped to Max Speed", Notification.Type.WARNING);
            } else {
                currentVelocity = new Vec3d(
                        currentVelocity.x * speedMultiplier.getValue(),
                        currentVelocity.y,
                        currentVelocity.z * speedMultiplier.getValue()
                );
            }

            mc.player.setVelocity(currentVelocity);
        }

        if (useTimer.getValue()) {
            ThunderHack.TICK_TIMER = timerSpeed.getValue();
        }
    }

    private boolean hasMaxElytraSpeed() {
        double speed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
        return speed >= 33.50;
    }

    private void useDoubleFirework() {
        useFirework();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        useFirework();
    }

    private void useFirework() {
        SearchInvResult hotbarFirework = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET);
        SearchInvResult inventoryFirework = InventoryUtility.findItemInInventory(Items.FIREWORK_ROCKET);

        InventoryUtility.saveSlot();

        if (hotbarFirework.found()) {
            hotbarFirework.switchTo();
        } else if (inventoryFirework.found()) {
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    inventoryFirework.slot(),
                    mc.player.getInventory().selectedSlot,
                    SlotActionType.SWAP,
                    mc.player
            );
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        } else {
            logNotification("No fireworks found!", Notification.Type.ERROR);
            return;
        }

        sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
        sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));

        InventoryUtility.returnSlot();

        if (!hotbarFirework.found() && inventoryFirework.found()) {
            mc.interactionManager.clickSlot(
                    mc.player.currentScreenHandler.syncId,
                    inventoryFirework.slot(),
                    mc.player.getInventory().selectedSlot,
                    SlotActionType.SWAP,
                    mc.player
            );
            sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
        }
    }

    private void sendVelocityPacket(Vec3d velocity) {
        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                mc.player.getX() + velocity.x,
                mc.player.getY(),
                mc.player.getZ() + velocity.z,
                mc.player.isOnGround()
        ));
    }

    @Override
    public void onDisable() {
        if (useTimer.getValue()) {
            ThunderHack.TICK_TIMER = 1.0f;
        }
    }

    private void logNotification(String message, Notification.Type type) {
        if (log.getValue()) {
            Managers.NOTIFICATION.publicity("ElytraSpeed", message, 1, type);
        }
    }

    public enum SpeedMethod {
        PACKET,
        GRIM,
        CUSTOM
    }

    public enum BypassMode {
        None,
        Grim
    }

    public enum DoubleTapMode {
        None,
        Auto
    }
}