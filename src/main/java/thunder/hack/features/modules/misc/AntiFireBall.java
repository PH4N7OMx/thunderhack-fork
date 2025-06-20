package thunder.hack.features.modules.misc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import thunder.hack.core.Managers;
import thunder.hack.features.modules.Module;
import thunder.hack.gui.notification.Notification;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;

public class AntiFireBall extends Module {
    public final Setting<Float> range = new Setting<>("Range", 5.0f, 1.0f, 10.0f);
    public final Setting<Boolean> warning = new Setting<>("Warning", true);
    public final Setting<Float> warningRange = new Setting<>("WarningRange", 50.0f, 1.0f, 100.0f);
    public final Setting<Boolean> rotation = new Setting<>("Rotation", true);
    public final Setting<Boolean> notify = new Setting<>("Notify", true);
    public final Setting<Boolean> song = new Setting<>("Song", false);
    public final Setting<Boolean> render = new Setting<>("Render", false);

    private final Timer notifyTimer = new Timer();
    private final Timer attackTimer = new Timer();
    private final Timer soundTimer = new Timer();

    public AntiFireBall() {
        super("AntiFireBall", Category.MISC);
    }

    @Override
    public void onUpdate() {
        if (mc.player == null || mc.world == null) return;

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof FireballEntity fireball) {
                float distanceToFireball = mc.player.distanceTo(entity);

                if (distanceToFireball <= warningRange.getValue() && warning.getValue()) {
                    sendFireballWarning(distanceToFireball);
                }


                if (notify.getValue() && distanceToFireball <= warningRange.getValue() && notifyTimer.passedMs(100)) {
                    notifyFireball(fireball, distanceToFireball);
                    notifyTimer.reset();
                }


                if (song.getValue() && soundTimer.passedMs(50)) {
                    mc.world.playSound(mc.player, mc.player.getBlockPos(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 1f, 1f);
                    soundTimer.reset();
                }

                if (render.getValue()) {
                    fireball.setGlowing(true);
                }

                if (distanceToFireball <= range.getValue()) {
                    if (rotation.getValue()) {
                        rotateTowardsEntity(fireball);
                    }
                    reflectFireball(fireball);
                }
            }
        }
    }

    private void reflectFireball(FireballEntity fireball) {
        if (!attackTimer.passedMs(40)) return;

        mc.interactionManager.attackEntity(mc.player, fireball);
        mc.player.swingHand(Hand.MAIN_HAND);
        attackTimer.reset();
    }

    private void sendFireballWarning(float distance) {
        String message = Formatting.RED + "Fireball detected " + String.format("%.1f", distance) + " blocks away!";
        mc.player.sendMessage(Text.literal(message), false);
    }


    private void notifyFireball(Entity fireball, float distance) {
        String message = Formatting.RED + "Fireball detected " + String.format("%.1f", distance) + " blocks away!";
        mc.player.sendMessage(Text.literal(message), false);
        Managers.NOTIFICATION.publicity("AntiFireBall", message, 2, Notification.Type.ERROR);
    }


    private void rotateTowardsEntity(Entity entity) {
        Vec3d direction = entity.getPos().subtract(mc.player.getPos());
        double yaw = Math.atan2(direction.z, direction.x) * (180 / Math.PI) - 90.0;
        double pitch = -Math.atan2(direction.y, direction.length()) * (180 / Math.PI);

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);
    }
}
