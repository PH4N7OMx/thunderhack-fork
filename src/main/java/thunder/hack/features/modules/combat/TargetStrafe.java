package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

public class TargetStrafe extends Module {
    public Setting<Float> distance = new Setting<>("Distance", 2.5F, 1.0F, 5.0f);
    public Setting<Float> sensitivity = new Setting<>("Sensitivity", 1.0F, 0.1F, 2.0f);
    public Setting<Boolean> jump = new Setting<>("Jump", true);
    public Setting<Boolean> autoJump = new Setting<>("AutoJump", true);
    public Setting<Boolean> onlyOnGround = new Setting<>("OnlyOnGround", false);

    private boolean clockwise = true;
    private double lastX, lastZ;
    private int switchCooldown = 0;

    private static TargetStrafe instance;

    public TargetStrafe() {
        super("TargetStrafe", Category.COMBAT);
        instance = this;
    }

    public static TargetStrafe getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        if (mc.player != null) {
            lastX = mc.player.getX();
            lastZ = mc.player.getZ();
        }
        clockwise = true;
        switchCooldown = 0;
    }

    private boolean canStrafe() {
        if (mc.player == null || mc.world == null) return false;
        if (mc.player.isSneaking()) return false;
        if (mc.player.isInLava()) return false;
        if (ModuleManager.scaffold != null && ModuleManager.scaffold.isEnabled()) return false;
        if (mc.player.isSubmergedInWater()) return false;
        if (mc.player.getAbilities().flying) return false;
        if (onlyOnGround.getValue() && !mc.player.isOnGround()) return false;

        return true;
    }

    private boolean shouldSwitchDirection(Entity target) {
        if (switchCooldown > 0) {
            switchCooldown--;
            return false;
        }

        if (mc.player.horizontalCollision) {
            switchCooldown = 20;
            return true;
        }

        double targetX = target.getX();
        double targetZ = target.getZ();
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        double angle = Math.atan2(playerZ - targetZ, playerX - targetX);
        angle += clockwise ? -0.3 : 0.3;

        double nextX = targetX + distance.getValue() * Math.cos(angle);
        double nextZ = targetZ + distance.getValue() * Math.sin(angle);

        BlockPos nextPos = new BlockPos((int)nextX, (int)mc.player.getY(), (int)nextZ);

        if (!mc.world.getBlockState(nextPos).isAir() ||
                mc.world.getBlockState(nextPos).getBlock() == Blocks.LAVA ||
                mc.world.getBlockState(nextPos).getBlock() == Blocks.FIRE) {
            switchCooldown = 20;
            return true;
        }

        double distToTarget = Math.sqrt(Math.pow(playerX - targetX, 2) + Math.pow(playerZ - targetZ, 2));
        if (distToTarget > distance.getValue() + 1.0) {
            return true;
        }

        return false;
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (!canStrafe()) return;

        Entity target = Aura.target;
        if (target == null || !ModuleManager.aura.isEnabled()) return;

        if (shouldSwitchDirection(target)) {
            clockwise = !clockwise;
        }

        double targetX = target.getX();
        double targetZ = target.getZ();
        double playerX = mc.player.getX();
        double playerZ = mc.player.getZ();

        double angleToTarget = Math.atan2(playerZ - targetZ, playerX - targetX);

        double strafeAngle = angleToTarget + (clockwise ? -0.2 : 0.2);

        double desiredX = targetX + distance.getValue() * Math.cos(strafeAngle);
        double desiredZ = targetZ + distance.getValue() * Math.sin(strafeAngle);

        double dirX = desiredX - playerX;
        double dirZ = desiredZ - playerZ;

        double length = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (length > 0.1) {
            dirX /= length;
            dirZ /= length;

            simulateMovementInput(dirX, dirZ);
        }

        if (autoJump.getValue() && mc.player.isOnGround() && mc.player.horizontalCollision) {
            mc.player.jump();
        }

        lastX = playerX;
        lastZ = playerZ;
    }

    private void simulateMovementInput(double dirX, double dirZ) {
        double playerYaw = Math.toRadians(mc.player.getYaw());

        double forward = dirZ * Math.cos(playerYaw) + dirX * Math.sin(playerYaw);
        double strafe = dirX * Math.cos(playerYaw) - dirZ * Math.sin(playerYaw);

        double threshold = 0.1 / sensitivity.getValue();

        if (Math.abs(forward) > threshold) {
            if (forward > 0) {
                mc.options.forwardKey.setPressed(true);
                mc.options.backKey.setPressed(false);
            } else {
                mc.options.backKey.setPressed(true);
                mc.options.forwardKey.setPressed(false);
            }
        } else {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
        }

        if (Math.abs(strafe) > threshold) {
            if (strafe > 0) {
                mc.options.rightKey.setPressed(true);
                mc.options.leftKey.setPressed(false);
            } else {
                mc.options.leftKey.setPressed(true);
                mc.options.rightKey.setPressed(false);
            }
        } else {
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }
    }

    @EventHandler
    public void onPacketReceive(PacketEvent.Receive e) {
        if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (mc.player != null) {
                lastX = mc.player.getX();
                lastZ = mc.player.getZ();
            }
        }
    }

    @Override
    public void onDisable() {
        if (mc.options != null) {
            mc.options.forwardKey.setPressed(false);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
        }
    }
}