package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import thunder.hack.events.impl.EventKeyboardInput;
import thunder.hack.events.impl.EventMove;
import thunder.hack.events.impl.EventTick;
import thunder.hack.events.impl.SlowWalkingEvent;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class NoSlow extends Module {

    public enum Mode {
        Grim,
        GrimBow,
        Matrix,
        Grim_2_3_71
    }

    public final Setting<Mode> mode = new Setting<>("Mode", Mode.Grim);
    private final Setting<Boolean> mainHand = new Setting<>("MainHand", true);
    private final Setting<SettingGroup> selection = new Setting<>("Selection", new SettingGroup(false, 0));
    private final Setting<Boolean> food = new Setting<>("Food", true).addToGroup(selection);
    private final Setting<Boolean> projectiles = new Setting<>("Projectiles", true).addToGroup(selection);
    private final Setting<Boolean> shield = new Setting<>("Shield", true).addToGroup(selection);
    public final Setting<Boolean> soulSand = new Setting<>("SoulSand", true).addToGroup(selection);
    public final Setting<Boolean> honey = new Setting<>("Honey", true).addToGroup(selection);
    public final Setting<Boolean> slime = new Setting<>("Slime", true).addToGroup(selection);
    public final Setting<Boolean> ice = new Setting<>("Ice", true).addToGroup(selection);
    public final Setting<Boolean> sweetBerryBush = new Setting<>("SweetBerryBush", true).addToGroup(selection);
    public final Setting<Boolean> sneak = new Setting<>("Sneak", false).addToGroup(selection);
    public final Setting<Boolean> crawl = new Setting<>("Crawl", false).addToGroup(selection);

    private boolean returnSneak;
    private final Timer stop = new Timer();
    private boolean eatingLastTick = false;
    private int prevOffhandSlot = -1;
    private int prevMainhandSlot = -1;
    private int ticks = 0;
    private int actionId = 0;

    public NoSlow() {
        super("NoSlow", Category.MOVEMENT);
    }

    private int getNextActionId() {
        actionId++;
        if (actionId > 100000) actionId = 0;
        return actionId;
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (returnSneak) {
            mc.options.sneakKey.setPressed(false);
            mc.player.setSprinting(true);
            returnSneak = false;
        }

        if (mc.player == null || mc.world == null) return;

        if (mode.getValue() == Mode.GrimBow) {
            ItemStack mainHandStack = mc.player.getMainHandStack();
            ItemStack offHandStack = mc.player.getOffHandStack();

            boolean isMainHandEdible = (mainHandStack.contains(DataComponentTypes.FOOD) || isPotion(mainHandStack)) && mc.player.isUsingItem();
            boolean isOffHandEdible = (offHandStack.contains(DataComponentTypes.FOOD) || isPotion(offHandStack)) && mc.player.isUsingItem();
            boolean isHoldingBow = isBowOrCrossbow(offHandStack) || isBowOrCrossbow(mainHandStack);

            if (isMainHandEdible && !eatingLastTick) {
                int bowSlot = findBowOrCrossbowSlot();
                if (bowSlot != -1 && !isBowOrCrossbow(offHandStack)) {
                    swapToOffhand(bowSlot);
                    prevOffhandSlot = bowSlot;
                }
            }

            if (isOffHandEdible && !eatingLastTick) {
                int bowSlot = findBowOrCrossbowSlot();
                if (bowSlot != -1 && !isBowOrCrossbow(mainHandStack)) {
                    swapToMainhand(bowSlot);
                    prevMainhandSlot = bowSlot;
                }
            }

            if (!mc.player.isUsingItem()) {
                if (prevOffhandSlot != -1) {
                    swapToOffhand(prevOffhandSlot);
                    prevOffhandSlot = -1;
                }
                if (prevMainhandSlot != -1) {
                    swapToMainhand(prevMainhandSlot);
                    prevMainhandSlot = -1;
                }
            }

            eatingLastTick = isMainHandEdible || isOffHandEdible;
        }

        if (mode.getValue() == Mode.Grim_2_3_71) {
            if (mc.player.isUsingItem()) {
                ticks++;
            } else {
                ticks = 0;
            }
        }
    }

    @EventHandler
    public void onMove(EventMove e) {
        if (mc.player == null || mc.player.isFallFlying()) return;
    }

    @EventHandler
    public void onKeyboardInput(EventKeyboardInput e) {}

    @EventHandler
    public void onSlowWalking(SlowWalkingEvent e) {
        if (mc.player == null || mc.player.isFallFlying()) return;

        if (mode.getValue() == Mode.Grim_2_3_71 && mc.player.isUsingItem()) {
            mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
                    mc.player.currentScreenHandler.syncId,
                    0,
                    35,
                    getNextActionId(),
                    SlotActionType.PICKUP,
                    ItemStack.EMPTY,
                    new Int2ObjectOpenHashMap<>()
            ));

            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTickNoSlow(EventTick event) {
        if (mc.player == null || mc.world == null) return;
        if (!mc.player.isUsingItem() || mc.player.isRiding() || mc.player.isFallFlying() || !canNoSlow()) return;

        switch (mode.getValue()) {
            case Grim -> {
                mc.player.networkHandler.sendPacket(new ClickSlotC2SPacket(
                        mc.player.currentScreenHandler.syncId,
                        0,
                        35,
                        getNextActionId(),
                        SlotActionType.PICKUP,
                        ItemStack.EMPTY,
                        new Int2ObjectOpenHashMap<>()
                ));
            }
            case GrimBow -> {
                if (isBowOrCrossbow(mc.player.getOffHandStack())) {
                    if (mc.player.getActiveHand() == Hand.OFF_HAND) {
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot % 8 + 1));
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot % 7 + 2));
                        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                    } else if (mainHand.getValue()) {
                        sendSequencedPacket(i -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, i, mc.player.getYaw(), mc.player.getPitch()));
                        if (prevOffhandSlot != -1) {
                            swapToOffhand(prevOffhandSlot);
                            prevOffhandSlot = -1;
                        }
                    }
                }
            }
            case Matrix -> {
                if (mc.player.isOnGround() && !mc.options.jumpKey.isPressed()) {
                    mc.player.setVelocity(mc.player.getVelocity().x * 0.3, mc.player.getVelocity().y, mc.player.getVelocity().z * 0.3);
                } else if (mc.player.fallDistance > 0.2f) {
                    mc.player.setVelocity(mc.player.getVelocity().x * 0.95f, mc.player.getVelocity().y, mc.player.getVelocity().z * 0.95f);
                }
            }
        }
    }

    protected void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(packet);
        }
    }

    private boolean isBowOrCrossbow(ItemStack stack) {
        return stack.getItem() == Items.BOW || stack.getItem() == Items.CROSSBOW;
    }

    private int findBowOrCrossbowSlot() {
        SearchInvResult bow = InventoryUtility.findItemInInventory(Items.BOW);
        if (bow.found()) return bow.slot();
        SearchInvResult crossbow = InventoryUtility.findItemInInventory(Items.CROSSBOW);
        if (crossbow.found()) return crossbow.slot();
        return -1;
    }

    private void swapToOffhand(int slot) {
        if (slot == -1) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                40,
                SlotActionType.SWAP,
                mc.player
        );
    }

    private void swapToMainhand(int slot) {
        if (slot == -1) return;
        mc.interactionManager.clickSlot(
                mc.player.currentScreenHandler.syncId,
                slot,
                mc.player.getInventory().selectedSlot,
                SlotActionType.SWAP,
                mc.player
        );
    }

    private boolean isPotion(ItemStack stack) {
        return stack.getItem() == Items.POTION
                || stack.getItem() == Items.SPLASH_POTION
                || stack.getItem() == Items.LINGERING_POTION;
    }

    public boolean canNoSlow() {
        if (!food.getValue() && mc.player.getActiveItem().getComponents().contains(DataComponentTypes.FOOD))
            return false;

        if (!shield.getValue() && mc.player.getActiveItem().getItem() == Items.SHIELD)
            return false;

        if (!projectiles.getValue() && (
                mc.player.getActiveItem().getItem() == Items.CROSSBOW ||
                        mc.player.getActiveItem().getItem() == Items.BOW ||
                        mc.player.getActiveItem().getItem() == Items.TRIDENT))
            return false;

        if (!mainHand.getValue() && mc.player.getActiveHand() == Hand.MAIN_HAND)
            return false;

        return true;
    }
}
