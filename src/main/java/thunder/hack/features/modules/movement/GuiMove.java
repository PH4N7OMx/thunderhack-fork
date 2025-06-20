package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import thunder.hack.events.impl.EventClickSlot;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.MovementUtility;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class GuiMove extends Module {
    public GuiMove() {
        super("GuiMove", Category.MOVEMENT);
    }

    private final Setting<Bypass> clickBypass = new Setting<>("Bypass", Bypass.None);
    private final Setting<Boolean> rotateOnArrows = new Setting<>("RotateOnArrows", true);
    private final Setting<Boolean> sneak = new Setting<>("sneak", false);
    private final Setting<Boolean> noContainers = new Setting<>("NoContainers", false);
    private final Setting<Boolean> onlyGround = new Setting<>("OnlyGround", false);

    private final Queue<ClickSlotC2SPacket> delayedClicks = new LinkedList<>();
    private final AtomicBoolean isFlushing = new AtomicBoolean(false);

    private boolean wasMoving = false;
    private final Timer wait = new Timer();

    private boolean wasOnGround = true;

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        boolean isMovingNow = MovementUtility.isMoving();
        boolean onGroundNow = mc.player.isOnGround();

        if (!isMovingNow && wasMoving) {
            if (clickBypass.getValue() == Bypass.Stop) flushDelayedClicks();
            wait.reset();
        }

        if (onlyGround.getValue()) {
            if (onGroundNow && !wasOnGround) { // игрок только что приземлился
                flushDelayedClicks();
            }
        }

        wasOnGround = onGroundNow;
        wasMoving = isMovingNow;

        if (mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen)) {
            if (noContainers.getValue() && !(mc.currentScreen instanceof InventoryScreen)) {
                return;
            }

            final KeyBinding[] keys = {
                    mc.options.forwardKey,
                    mc.options.backKey,
                    mc.options.leftKey,
                    mc.options.rightKey,
                    mc.options.jumpKey,
                    mc.options.sprintKey
            };

            for (KeyBinding key : keys) {
                boolean pressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), key.getDefaultKey().getCode());
                key.setPressed(pressed);
            }

            if (clickBypass.getValue() != Bypass.Stop) {
                mc.options.sneakKey.setPressed(sneak.getValue());
            }

            if (rotateOnArrows.getValue()) {
                float deltaX = 0;
                float deltaY = 0;
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 264)) deltaY += 30f;
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 265)) deltaY -= 30f;
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 262)) deltaX += 30f;
                if (InputUtil.isKeyPressed(mc.getWindow().getHandle(), 263)) deltaX -= 30f;
                if (deltaX != 0 || deltaY != 0) mc.player.changeLookDirection(deltaX, deltaY);
            }
        }
    }

    @EventHandler
    public void onClickSlot(EventClickSlot e) {
        if (clickBypass.is(Bypass.DisableClicks) && (MovementUtility.isMoving() || mc.options.jumpKey.isPressed()))
            e.cancel();
    }

    @EventHandler
    public void onPacketSend(PacketEvent.Send e) {
        if (e.getPacket() instanceof ClickSlotC2SPacket click) {
            if (noContainers.getValue() && !(mc.currentScreen instanceof InventoryScreen)) return;

            boolean isMoving = MovementUtility.isMoving();

            if (clickBypass.getValue() == Bypass.Stop) {
                if (isMoving) {
                    delayedClicks.add(click);
                    e.cancel();
                    return;
                } else {
                    flushDelayedClicks();
                }
            }

            if (onlyGround.getValue() && !mc.player.isOnGround()) {
                delayedClicks.add(click);
                e.cancel();
                return;
            }

            switch (clickBypass.getValue()) {
                case GrimSwap -> {
                    if (click.getActionType() != SlotActionType.PICKUP && click.getActionType() != SlotActionType.PICKUP_ALL)
                        sendPacket(new CloseHandledScreenC2SPacket(0));
                }
                case StrictNCP -> {
                    if (mc.player.isOnGround() && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, 0.0656, 0.0)).iterator().hasNext()) {
                        if (mc.player.isSprinting())
                            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.0656, mc.player.getZ(), false));
                    }
                }
                case StrictNCP2 -> {
                    if (mc.player.isOnGround() && !mc.world.getBlockCollisions(mc.player, mc.player.getBoundingBox().offset(0.0, 0.000000271875, 0.0)).iterator().hasNext()) {
                        if (mc.player.isSprinting())
                            sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                        sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.000000271875, mc.player.getZ(), false));
                    }
                }
                case MatrixNcp -> {
                    sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    mc.options.forwardKey.setPressed(false);
                    mc.player.input.movementForward = 0;
                    mc.player.input.pressingForward = false;
                }
                case Delay, DelayOnStop -> {
                    if (clickBypass.getValue() == Bypass.DelayOnStop && mc.currentScreen instanceof ChatScreen) return;
                    delayedClicks.add(click);
                    e.cancel();
                }
            }
        }
    }

    @EventHandler
    public void onPacketSendPost(PacketEvent.SendPost e) {
        if (e.getPacket() instanceof ClickSlotC2SPacket) {
            if (mc.player.isSprinting() && clickBypass.is(Bypass.StrictNCP))
                sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    private void flushDelayedClicks() {
        if (isFlushing.getAndSet(true)) return;
        while (!delayedClicks.isEmpty()) {
            sendPacket(delayedClicks.poll());
        }
        isFlushing.set(false);
    }

    private enum Bypass {
        DisableClicks,
        None,
        StrictNCP,
        GrimSwap,
        MatrixNcp,
        Delay,
        DelayOnStop,
        StrictNCP2,
        Stop
    }
}
