package thunder.hack.features.modules.movement;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import thunder.hack.events.impl.EventSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class ChestplateFly extends Module {
    public ChestplateFly() {
        super("ChestplateFly", Category.MOVEMENT);
    }

    private final Setting<Mode> mode = new Setting<>("Mode", Mode.Old);
    private final Setting<Float> timerStartFireWork = new Setting<>("FireworkTimer", 400.0f, 50.0f, 1500.0f);
    private final Setting<Boolean> onlyGrimBypass = new Setting<>("OnlyGrimBypass", false);

    private final Timer timer = new Timer();
    private final Timer timer1 = new Timer();
    private final Timer timer2 = new Timer();

    private int oldItem = -1;

    @EventHandler
    public void onEventSync(EventSync event) {
        if (oldItem != -1 &&
                getChestplateItem() == Items.ELYTRA &&
                mc.player.getInventory().getStack(oldItem).getItem() instanceof ArmorItem &&
                timer2.passedMs(550)) {

            mc.interactionManager.clickSlot(0, 6, oldItem, SlotActionType.SWAP, mc.player);
            sendMessage((isRu() ? "ChestplateFly обнаружил, отключаю и надеваю нагрудник" : "ChestplateFly detected, disabling and equipping chestplate"));
            oldItem = -1;
            timer2.reset();
            disable();
            return;
        }

        if (getFireworkSlot() == -1) {
            return;
        }

        int timeSwap = 550;
        if (onlyGrimBypass.getValue()) {
            timeSwap = 0;
        } else if (mode.is(Mode.Old)) {
            timeSwap = 200;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.ELYTRA &&
                    !mc.player.isOnGround() &&
                    !mc.player.isTouchingWater() &&
                    !mc.player.isInLava() &&
                    !mc.player.isFallFlying()) {

                if (timer1.passedMs(timeSwap)) {
                    timer2.reset();

                    mc.interactionManager.clickSlot(0, 6, i, SlotActionType.SWAP, mc.player);

                    mc.player.startFallFlying();
                    mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));

                    mc.interactionManager.clickSlot(0, 6, i, SlotActionType.SWAP, mc.player);

                    oldItem = i;
                    timer1.reset();
                }

                if (timer.passedMs(timerStartFireWork.getValue().longValue()) && mc.player.isFallFlying()) {
                    useFirework();
                    timer.reset();
                }
            }
        }
    }

    private Item getChestplateItem() {
        ItemStack chestplate = mc.player.getInventory().getArmorStack(2);
        return chestplate.getItem();
    }

    private int getFireworkSlot() {
        SearchInvResult result = InventoryUtility.findItemInHotBar(Items.FIREWORK_ROCKET);
        return result.found() ? result.slot() : -1;
    }

    private void useFirework() {
        int fireworkSlot = getFireworkSlot();
        if (fireworkSlot != -1) {
            int oldSlot = mc.player.getInventory().selectedSlot;
            InventoryUtility.switchTo(fireworkSlot, true);
            mc.interactionManager.interactItem(mc.player, mc.player.getActiveHand());
            InventoryUtility.switchTo(oldSlot, true);
        }
    }

    private boolean getItemNoHotbar(Item item) {
        for (int i = 9; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        if (oldItem != -1) {
            if (getChestplateItem() == Items.ELYTRA &&
                    mc.player.getInventory().getStack(oldItem).getItem() instanceof ArmorItem) {
                mc.interactionManager.clickSlot(0, 6, oldItem, SlotActionType.SWAP, mc.player);
            }
            oldItem = -1;
        }

        mc.options.sneakKey.setPressed(false);
    }

    private enum Mode {
        Old, RW
    }
}