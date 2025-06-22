package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.*;
import thunder.hack.injection.accesors.IInteractionManager;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.interfaces.IEntity;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.MovementUtility;
import thunder.hack.utility.player.SearchInvResult;

import static thunder.hack.features.modules.client.ClientSettings.isRu;
import static thunder.hack.utility.player.MovementUtility.isMoving;

public class Speed extends Module {
    public Speed() {
        super("Speed", Category.MOVEMENT);
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.NCP);
    public final Setting<Float> grimEntityDistance = new Setting<>("GrimEntityDist", 2.25f, 0.1f, 5f, v -> mode.is(Mode.GrimEntity));
    public final Setting<Float> grimEntitySpeed = new Setting<>("GrimEntitySpeed", 0.08f, 0.01f, 1f, v -> mode.is(Mode.GrimEntity));
    public final Setting<Float> grimEntity2Distance = new Setting<>("GrimEntity2Dist", 1.0f, 0.1f, 5f, v -> mode.is(Mode.GrimEntity2));
    public final Setting<Float> grimEntity2Speed = new Setting<>("GrimEntity2Speed", 0.08f, 0.01f, 1f, v -> mode.is(Mode.GrimEntity2));
    public Setting<Boolean> useTimer = new Setting<>("Use Timer", false);
    public Setting<Boolean> pauseInLiquids = new Setting<>("PauseInLiquids", false);
    public Setting<Boolean> pauseWhileSneaking = new Setting<>("PauseWhileSneaking", false);
    public final Setting<Integer> hurttime = new Setting<>("HurtTime", 0, 0, 10, v -> mode.is(Mode.MatrixDamage));
    public final Setting<Float> boostFactor = new Setting<>("BoostFactor", 2f, 0f, 10f, v -> mode.is(Mode.MatrixDamage) || mode.is(Mode.Vanilla));
    public final Setting<Boolean> allowOffGround = new Setting<>("AllowOffGround", true, v -> mode.is(Mode.MatrixDamage));
    public final Setting<Integer> shiftTicks = new Setting<>("ShiftTicks", 0, 0, 10, v -> mode.is(Mode.MatrixDamage));
    public final Setting<Integer> fireWorkSlot = new Setting<>("FireSlot", 1, 1, 9, v -> mode.getValue() == Mode.FireWork);
    public final Setting<Integer> delay = new Setting<>("Delay", 8, 1, 20, v -> mode.getValue() == Mode.FireWork);
    public final Setting<Boolean> strict = new Setting<>("Strict", false, v -> mode.is(Mode.GrimIce));
    public final Setting<Float> matrixJBSpeed = new Setting<>("TimerSpeed", 1.088f, 1f, 2f, v -> mode.is(Mode.MatrixJB));
    public final Setting<Boolean> armorStands = new Setting<>("ArmorStands", false, v -> mode.is(Mode.GrimCombo) || mode.is(Mode.GrimEntity2));
    public Setting<Boolean> ignoreOther = new Setting<>("IgnoreOther", true);

    private int ticks;

    @EventHandler
    public void modifyVelocity(EventPlayerTravel e) {
        if (mode.is(Mode.GrimEntity) && !e.isPre() && ThunderHack.core.getSetBackTime() > 1000) {
            for (PlayerEntity ent : Managers.ASYNC.getAsyncPlayers()) {
                if (ent != mc.player && mc.player.squaredDistanceTo(ent) <= grimEntityDistance.getValue() * grimEntityDistance.getValue()) {
                    float p = mc.world.getBlockState(((IEntity) mc.player).thunderHack_Recode$getVelocityBP()).getBlock().getSlipperiness();
                    float f = mc.player.isOnGround() ? p * 0.91f : 0.91f;
                    float f2 = mc.player.isOnGround() ? p : 0.99f;

                    mc.player.setVelocity(
                            mc.player.getVelocity().getX() / f * f2,
                            mc.player.getVelocity().getY(),
                            mc.player.getVelocity().getZ() / f * f2
                    );
                    break;
                }
            }
        }

        if (mode.is(Mode.GrimEntity2) && !e.isPre() && ThunderHack.core.getSetBackTime() > 1000 && MovementUtility.isMoving()) {
            int collisions = 0;
            for (Entity ent : mc.world.getEntities()) {
                if (ent != mc.player &&
                        (!(ent instanceof ArmorStandEntity) || armorStands.getValue()) &&
                        (ent instanceof LivingEntity || ent instanceof BoatEntity) &&
                        mc.player.getBoundingBox().expand(grimEntity2Distance.getValue()).intersects(ent.getBoundingBox())) {
                    collisions++;
                }
            }

            double[] motion = MovementUtility.forward(grimEntity2Speed.getValue() * collisions);
            mc.player.addVelocity(motion[0], 0.0, motion[1]);
        }
    }

    @Override
    public void onEnable() {
        ticks = 0;
    }

    @EventHandler
    public void onTick(EventTick e) {
        ticks++;
    }

    public enum Mode {
        GrimEntity, GrimEntity2, NCP, ElytraLowHop, MatrixDamage, StrictStrafe, MatrixJB, FireWork, Vanilla, GrimIce, GrimCombo
    }
}
