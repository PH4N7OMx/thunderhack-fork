package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.TntMinecartEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PickFromInventoryC2SPacket;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.SettingGroup;
import thunder.hack.core.Managers;
import thunder.hack.injection.accesors.IMinecraftClient;

public final class AutoTotem extends Module {
    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Matrix);
    private final Setting<Float> healthF = new Setting<>("HP", 16f, 0f, 36f);
    private final Setting<Boolean> checkElytraHP = new Setting<>("CheckElytraHP", true);
    private final Setting<Float> elytraHealth = new Setting<>("ElytraHP", 20f, 0f, 36f);
    private final Setting<Boolean> calcAbsorption = new Setting<>("CalcAbsorption", true);
    private final Setting<Boolean> resetAttackCooldown = new Setting<>("ResetAttackCooldown", false);

    private final Setting<SettingGroup> safety = new Setting<>("Safety", new SettingGroup(false, 0));
    private final Setting<Boolean> onFall = new Setting<>("OnFall", true).addToGroup(safety);
    private final Setting<Boolean> onTnt = new Setting<>("OnTNT", true).addToGroup(safety);
    private final Setting<Boolean> onMinecartTnt = new Setting<>("OnMinecartTNT", true).addToGroup(safety);
    private final Setting<Float> tntRange = new Setting<>("TntRange", 5.0f, 1.0f, 20.0f).addToGroup(safety);
    private final Setting<Boolean> onCrystal = new Setting<>("OnCrystal", true).addToGroup(safety);
    private final Setting<Float> crystalRange = new Setting<>("CrystalRange", 6.0f, 1.0f, 20.0f).addToGroup(safety);
    private final Setting<Boolean> onCrystalInHand = new Setting<>("OnCrystalInHand", false).addToGroup(safety);
    private final Setting<Boolean> onMaceInHand = new Setting<>("OnMaceInHand", false).addToGroup(safety);
    private final Setting<Float> maceRange = new Setting<>("MaceRange", 6.0f, 1.0f, 20.0f).addToGroup(safety);

    private enum Mode { Default, Alternative, Matrix, MatrixPick, NewVersion }

    private int delay;
    private ItemStack prevStack = ItemStack.EMPTY;
    private int prevOffhandSlot = -1;
    private boolean totemSwapped = false;

    public AutoTotem() {
        super("AutoTotem", Category.COMBAT);
    }

    @Override
    public void onEnable() {
        clearSwapState();
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (mc.player == null || mc.player.isDead()) {
            clearSwapState();
            return;
        }

        if (delay-- > 0) return;

        float currentHealth = mc.player.getHealth() + (calcAbsorption.getValue() ? mc.player.getAbsorptionAmount() : 0);
        float triggerHealth = (checkElytraHP.getValue() && isElytraEquipped()) ? elytraHealth.getValue() : healthF.getValue();

        boolean danger = isTntNearby() || isCrystalNearby() || isCrystalInHand() || isMaceInHandNearby();
        boolean shouldTotem = currentHealth <= triggerHealth || (onFall.getValue() && mc.player.fallDistance > 4 && !mc.player.isFallFlying()) || danger;

        if (shouldTotem) {
            if (mc.player.getOffHandStack().getItem() != Items.TOTEM_OF_UNDYING) {
                int slot = findItemInInventory(Items.TOTEM_OF_UNDYING);
                if (slot != -1) {
                    if (!mc.player.getOffHandStack().isEmpty()) {
                        prevStack = mc.player.getOffHandStack().copy();
                        prevOffhandSlot = findItemInInventory(prevStack.getItem());
                    } else {
                        prevStack = ItemStack.EMPTY;
                        prevOffhandSlot = -1;
                    }
                    swapTo(slot);
                    totemSwapped = true;
                    delay = 3;
                }
            }
        } else if (totemSwapped && !mc.player.isUsingItem()) {
            returnPrevItem();
        }
    }

    private void returnPrevItem() {
        if (prevStack.isEmpty()) {
            clearSwapState();
            return;
        }
        if (mc.player.getOffHandStack().getItem() == prevStack.getItem()) {
            clearSwapState();
            return;
        }
        int slotToReturn = (prevOffhandSlot != -1 && mc.player.getInventory().getStack(prevOffhandSlot).getItem() == prevStack.getItem()) ? prevOffhandSlot : findItemInInventory(prevStack.getItem());
        if (slotToReturn == -1) {
            clearSwapState();
            return;
        }
        int guiSlot = slotToReturn < 9 ? slotToReturn + 36 : slotToReturn;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, guiSlot, 40, SlotActionType.SWAP, mc.player);
        clearSwapState();
    }

    private void swapTo(int slot) {
        if (slot == -1 || delay > 0 || mc.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
        int guiSlot = slot < 9 ? slot + 36 : slot;
        int prevCurrentItem = mc.player.getInventory().selectedSlot;
        switch (mode.getValue()) {
            case Default -> {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, guiSlot, 40, SlotActionType.SWAP, mc.player);
                mc.player.resetLastAttackedTicks();
            }
            case Alternative -> {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, guiSlot, findNearestCurrentItem(), SlotActionType.SWAP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 45, findNearestCurrentItem(), SlotActionType.SWAP, mc.player);
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, guiSlot, findNearestCurrentItem(), SlotActionType.SWAP, mc.player);
                mc.player.resetLastAttackedTicks();
            }
            case Matrix -> {
                if (mc.player.currentScreenHandler == null) return;
                if (mc.player.isSprinting()) mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, guiSlot, findNearestCurrentItem(), SlotActionType.SWAP, mc.player);
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(findNearestCurrentItem()));
                mc.player.getInventory().selectedSlot = findNearestCurrentItem();
                ItemStack itemstack = mc.player.getOffHandStack();
                mc.player.setStackInHand(Hand.OFF_HAND, mc.player.getMainHandStack());
                mc.player.setStackInHand(Hand.MAIN_HAND, itemstack);
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prevCurrentItem));
                mc.player.getInventory().selectedSlot = prevCurrentItem;
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, guiSlot, findNearestCurrentItem(), SlotActionType.SWAP, mc.player);
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                if (resetAttackCooldown.getValue()) mc.player.resetLastAttackedTicks();
            }
            case MatrixPick -> {
                mc.player.networkHandler.sendPacket(new PickFromInventoryC2SPacket(guiSlot));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            }
            case NewVersion -> {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, guiSlot, 40, SlotActionType.SWAP, mc.player);
                mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
            }
        }
        delay = 2 + (Managers.SERVER.getPing() / 25);
    }

    private int findNearestCurrentItem() {
        int i = mc.player.getInventory().selectedSlot;
        if (i == 8) return 7;
        if (i == 0) return 1;
        return i - 1;
    }

    private int findItemInInventory(Item item) {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private boolean isElytraEquipped() {
        return mc.player.getInventory().getArmorStack(2).getItem() == Items.ELYTRA;
    }

    private boolean isTntNearby() {
        var box = mc.player.getBoundingBox().expand(tntRange.getValue());
        return onTnt.getValue() && mc.world.getEntitiesByClass(TntEntity.class, box, e -> true).size() > 0
                || onMinecartTnt.getValue() && mc.world.getEntitiesByClass(TntMinecartEntity.class, box, e -> true).size() > 0;
    }

    private boolean isCrystalNearby() {
        var box = mc.player.getBoundingBox().expand(crystalRange.getValue());
        return onCrystal.getValue() && mc.world.getEntitiesByClass(EndCrystalEntity.class, box, e -> true).size() > 0;
    }

    private boolean isCrystalInHand() {
        return onCrystalInHand.getValue() && (mc.player.getMainHandStack().getItem() == Items.END_CRYSTAL || mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL);
    }

    private boolean isMaceInHandNearby() {
        if (!onMaceInHand.getValue()) return false;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && p.distanceTo(mc.player) <= maceRange.getValue() && p.getMainHandStack().getItem() == Items.MACE)
                return true;
        }
        return false;
    }

    private void clearSwapState() {
        prevStack = ItemStack.EMPTY;
        prevOffhandSlot = -1;
        totemSwapped = false;
        delay = 0;
    }
}
