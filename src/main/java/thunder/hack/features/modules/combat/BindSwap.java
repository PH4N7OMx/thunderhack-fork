package thunder.hack.features.modules.combat;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.Bind;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.MovementUtility;
import thunder.hack.utility.player.SearchInvResult;

public final class BindSwap extends Module {
    private final KeyBinding[] movementKeys = new KeyBinding[6];
    private final Timer bindDelay = new Timer();
    private final Timer wait = new Timer();

    private boolean lastPressed;
    private Item currentItem;
    private int pendingSwapSlot = -1;
    private boolean isStopping = false;
    private boolean initialized = false;

    private boolean queuedSwap = false;
    private int queuedSlot = -1;

    public BindSwap() {
        super("BindSwap", Category.COMBAT);
    }

    private void initKeys() {
        if (mc == null || mc.options == null) return;
        movementKeys[0] = mc.options.forwardKey;
        movementKeys[1] = mc.options.backKey;
        movementKeys[2] = mc.options.leftKey;
        movementKeys[3] = mc.options.rightKey;
        movementKeys[4] = mc.options.jumpKey;
        movementKeys[5] = mc.options.sprintKey;
        initialized = true;
    }

    public enum SwapItem {
        GAPPLE(Items.GOLDEN_APPLE, "Gapple"),
        SHIELD(Items.SHIELD, "Shield"),
        BALL(Items.PLAYER_HEAD, "Ball"),
        TOTEM(Items.TOTEM_OF_UNDYING, "Totem");

        private final Item item;
        private final String displayName;

        SwapItem(Item item, String name) {
            this.item = item;
            this.displayName = name;
        }

        public Item getItem() {
            return item;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    private final Setting<BooleanSettingGroup> bindSwap = new Setting<>("BindSwap", new BooleanSettingGroup(true));
    private final Setting<Bind> swapButton = new Setting<>("SwapButton", new Bind(GLFW.GLFW_KEY_CAPS_LOCK, false, false)).addToGroup(bindSwap);
    private final Setting<SwapItem> swapFrom = new Setting<>("SwapFrom", SwapItem.GAPPLE).addToGroup(bindSwap);
    private final Setting<SwapItem> swapTo = new Setting<>("SwapTo", SwapItem.SHIELD).addToGroup(bindSwap);
    private final Setting<Boolean> autoStop = new Setting<>("AutoStop", true).addToGroup(bindSwap);
    private final Setting<Boolean> onlyGround = new Setting<>("OnlyGround", true).addToGroup(bindSwap);

    @Override
    public void onUpdate() {
        if (!initialized) {
            initKeys();
            if (!initialized) return;
        }

        if (!bindSwap.getValue().isEnabled()) return;
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;

        if (autoStop.getValue() && isStopping) {
            if (!wait.passedMs(400)) {
                for (KeyBinding keyBinding : movementKeys) {
                    if (keyBinding != null) keyBinding.setPressed(false);
                }
                return;
            } else {
                isStopping = false;
                wait.reset();
                for (KeyBinding keyBinding : movementKeys) {
                    if (keyBinding != null) {
                        int keyCode = keyBinding.getDefaultKey().getCode();
                        if (GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS) {
                            keyBinding.setPressed(true);
                        }
                    }
                }
            }
        }

        boolean pressed = isKeyPressed(swapButton);
        if (pressed && !lastPressed && bindDelay.passedMs(250)) {
            ItemStack offHandStack = mc.player.getOffHandStack();
            currentItem = (offHandStack.getItem() == swapFrom.getValue().getItem())
                    ? swapTo.getValue().getItem()
                    : swapFrom.getValue().getItem();

            SearchInvResult result = InventoryUtility.findItemInInventory(currentItem);

            if (result.found()) {
                if (autoStop.getValue()) {
                    isStopping = true;
                    wait.reset();
                    for (KeyBinding keyBinding : movementKeys) {
                        if (keyBinding != null) keyBinding.setPressed(false);
                    }
                }

                if ((!autoStop.getValue() && MovementUtility.isMoving()) || (onlyGround.getValue() && !mc.player.isOnGround())) {
                    queuedSwap = true;
                    queuedSlot = result.slot();
                } else {
                    pendingSwapSlot = result.slot();
                }
            }
        }

        if (!isStopping) {
            if (pendingSwapSlot != -1 && (!autoStop.getValue() || !MovementUtility.isMoving())) {
                doSwapPacket(pendingSwapSlot);
                pendingSwapSlot = -1;
            } else if (queuedSwap && (!autoStop.getValue() || !MovementUtility.isMoving()) && (!onlyGround.getValue() || mc.player.isOnGround())) {
                doSwapPacket(queuedSlot);
                queuedSwap = false;
                queuedSlot = -1;
            }
        }

        lastPressed = pressed;
    }

    private void doSwapPacket(int slot) {
        if (mc.interactionManager == null || mc.player == null || mc.player.currentScreenHandler == null) return;
        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slot, 40, SlotActionType.SWAP, mc.player);
    }

    @Override
    public boolean isKeyPressed(Setting<Bind> bind) {
        if (bind == null || bind.getValue() == null) return false;
        if (mc == null || mc.getWindow() == null) return false;
        int key = bind.getValue().getKey();
        return key != -1 && GLFW.glfwGetKey(mc.getWindow().getHandle(), key) == GLFW.GLFW_PRESS;
    }
}
