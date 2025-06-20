package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import thunder.hack.ThunderHack;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventMove;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

import static thunder.hack.utility.player.MovementUtility.isMoving;

public class TargetSpeed extends Module {
    public Setting<Boolean> autoJump = new Setting<>("AutoJump", true);
    public Setting<Float> distance = new Setting<>("Distance", 1.3F, 0.2F, 7f);
    public Setting<Float> circleSpeed = new Setting<>("CircleSpeed", 0.15f, 0.05f, 0.5f);

    private static TargetSpeed instance;

    public TargetSpeed() {
        super("TargetHelper", Category.COMBAT);
        instance = this;
    }

    public static TargetSpeed getInstance() {
        return instance;
    }

    @EventHandler
    public void onMove(EventMove event) {
        if (!canMove()) return;

        LivingEntity target = getTarget();
        if (target == null) return;

        if (!isMoving()) return;

        double speedMultiplier = 1.0;
        if (mc.player.squaredDistanceTo(target) > distance.getValue() * distance.getValue()) {
            speedMultiplier = 1.15;
        } else if (mc.player.squaredDistanceTo(target) < (distance.getValue() - 0.5) * (distance.getValue() - 0.5)) {
            speedMultiplier = 0.85;
        }

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        double angle = Math.atan2(dz, dx);

        if (mc.options.leftKey.isPressed()) {
            angle -= Math.PI / 2;
        } else if (mc.options.rightKey.isPressed()) {
            angle += Math.PI / 2;
        }

        double moveX = Math.cos(angle) * circleSpeed.getValue() * speedMultiplier;
        double moveZ = Math.sin(angle) * circleSpeed.getValue() * speedMultiplier;

        double[] moveArray = MovementUtility.forward(0.1 * speedMultiplier);
        Vec3d moveVec = new Vec3d(moveArray[0] + moveX, 0.0, moveArray[1] + moveZ);

        event.setX(moveVec.x);
        event.setZ(moveVec.z);
    }

    @EventHandler
    public void updateValues(EventSync e) {
        if (mc.player.isOnGround() && autoJump.getValue() && getTarget() != null) {
            mc.player.jump();
        }
    }

    private boolean canMove() {
        return mc.player != null && !mc.player.isSneaking() && isMoving();
    }

    private LivingEntity getTarget() {
        if (ModuleManager.aura.isEnabled()) {
            Entity target = ModuleManager.aura.target;
            if (target instanceof LivingEntity) {
                return (LivingEntity) target;
            }
        }
        return null;
    }
}
