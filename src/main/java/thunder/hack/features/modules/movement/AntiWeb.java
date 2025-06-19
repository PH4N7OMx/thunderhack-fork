package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.CobwebBlock;
import thunder.hack.ThunderHack;
import thunder.hack.core.Managers;
import thunder.hack.events.impl.EventCollision;
import thunder.hack.events.impl.PlayerUpdateEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.player.MovementUtility;

public class AntiWeb extends Module {
    public AntiWeb() {
        super("AntiWeb", Category.MOVEMENT);
    }

    public static final Setting<Mode> mode = new Setting<>("Mode", Mode.Solid);
    public static final Setting<ReplaceMode> replaceMode = new Setting<>("ReplaceWith", ReplaceMode.Dirt, v -> mode.getValue() == Mode.Solid);
    public static final Setting<Boolean> grim = new Setting<>("Grim", false, v -> mode.is(Mode.Ignore));
    public static final Setting<Float> timer = new Setting<>("Timer", 20f, 1f, 50f, v -> mode.getValue() == Mode.Timer);

    private boolean timerEnabled = false;

    public enum Mode {
        Timer, Solid, Ignore, VanillaFly
    }

    public enum ReplaceMode {
        Dirt, Water
    }

    @EventHandler
    public void onPlayerUpdate(PlayerUpdateEvent e) {
        if (Managers.PLAYER.isInWeb()) {
            if (mode.getValue() == Mode.Timer) {
                if (mc.player.isOnGround()) {
                    ThunderHack.TICK_TIMER = 1f;
                } else {
                    ThunderHack.TICK_TIMER = timer.getValue();
                    timerEnabled = true;
                }
            }
            if (mode.getValue() == Mode.VanillaFly) {
                final double[] dir = MovementUtility.forward(0.6f);
                mc.player.setVelocity(dir[0], 0, dir[1]);
                if (mc.options.jumpKey.isPressed())
                    mc.player.setVelocity(mc.player.getVelocity().add(0, 1.4f, 0));
                if (mc.options.sneakKey.isPressed())
                    mc.player.setVelocity(mc.player.getVelocity().add(0, -1.4f, 0));
            }
        } else if (timerEnabled) {
            timerEnabled = false;
            ThunderHack.TICK_TIMER = 1f;
        }
    }

    @EventHandler
    public void onCollide(EventCollision e) {
        if (e.getState().getBlock() instanceof CobwebBlock && mode.getValue() == Mode.Solid) {
            Block replacementBlock = replaceMode.getValue() == ReplaceMode.Dirt ? Blocks.DIRT : Blocks.WATER;
            e.setState(replacementBlock.getDefaultState());
        }
    }
}
