package thunder.hack.features.modules.player;

import net.minecraft.item.ItemStack;
import net.minecraft.item.MiningToolItem;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;

import static thunder.hack.features.modules.client.ClientSettings.isRu;

public class ToolSaver extends Module {
    public ToolSaver() {
        super("ToolSaver", Category.PLAYER);
    }

    private final Setting<Integer> savePercent = new Setting<>("Save %", 10, 1, 50);

    @Override
    public void onUpdate() {
        if (mc.player == null) return;

        ItemStack tool = mc.player.getMainHandStack();
        if (!(tool.getItem() instanceof MiningToolItem)) return;

        float durability = tool.getMaxDamage() - tool.getDamage();
        int percent = (int) ((durability / (float) tool.getMaxDamage()) * 100F);

        if (percent <= savePercent.getValue()) {
            int slot = findNearestCurrentItem();
            if (slot != mc.player.getInventory().selectedSlot) {
                mc.player.getInventory().selectedSlot = slot;
                sendMessage(isRu() ? "Твой инструмент почти сломался!" : "Your tool is almost broken!");
            }
        }
    }

    public static int findNearestCurrentItem() {
        if (mc.player == null) return 0;

        int currentSlot = mc.player.getInventory().selectedSlot;
        ItemStack currentStack = mc.player.getInventory().getStack(currentSlot);

        for (int i = 0; i < 9; i++) {
            if (i == currentSlot) continue;

            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof MiningToolItem)) continue;

            if (stack.getDamage() >= stack.getMaxDamage() - 1) continue;

            return i;
        }

        return currentSlot;
    }
}
