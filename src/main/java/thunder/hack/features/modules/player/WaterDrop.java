package thunder.hack.features.modules.player;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import thunder.hack.events.impl.EventTick;
import thunder.hack.features.modules.Module;
import thunder.hack.utility.player.InventoryUtility;
import thunder.hack.utility.player.SearchInvResult;

public class WaterDrop extends Module {
    private boolean retrieveFlag = false;

    public WaterDrop() {
        super("WaterDrop", Category.PLAYER);
    }

    @EventHandler
    public void onTick(EventTick event) {
        if (mc.player == null || mc.world == null) return;

        if (mc.player.isOnFire() && !retrieveFlag) {
            SearchInvResult waterResult = InventoryUtility.findItemInHotBar(Items.WATER_BUCKET);
            if (waterResult.found()) {
                BlockPos playerPos = mc.player.getBlockPos();
                doWaterDrop(waterResult, playerPos);
            }
        } else if (retrieveFlag) {
            retrieveWater();
        }
    }

    private void doWaterDrop(SearchInvResult waterResult, BlockPos playerPos) {
        if (!mc.world.getBlockState(playerPos.down()).isAir() || !mc.world.getBlockState(playerPos.down().down()).isSolidBlock(mc.world, playerPos.down().down())) {
            return;
        }

        InventoryUtility.saveSlot();
        waterResult.switchTo();

        BlockPos posUnder = playerPos.down();
        BlockHitResult blockHitResult = new BlockHitResult(
                mc.player.getPos().add(0, -1, 0),
                Direction.UP,
                posUnder,
                false
        );

        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult);
        mc.player.swingHand(Hand.MAIN_HAND);
        retrieveFlag = true;
    }

    private void retrieveWater() {
        SearchInvResult bucketResult = InventoryUtility.findItemInHotBar(Items.BUCKET);
        if (bucketResult.found()) {
            bucketResult.switchTo();
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);

            InventoryUtility.returnSlot();
            retrieveFlag = false;
        }
    }
}
