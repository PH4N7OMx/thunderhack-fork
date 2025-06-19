package thunder.hack.features.modules.combat;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import thunder.hack.core.manager.client.ModuleManager;
import thunder.hack.events.impl.EventAfterRotate;
import thunder.hack.events.impl.EventPostSync;
import thunder.hack.features.modules.Module;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.BooleanSettingGroup;
import thunder.hack.utility.Timer;

public final class AutoBuff extends Module {
    private final Setting<Boolean> strength = new Setting<>("Strength", true);
    private final Setting<Boolean> speed = new Setting<>("Speed", true);
    private final Setting<Boolean> fire = new Setting<>("FireResistance", true);
    private final Setting<BooleanSettingGroup> heal = new Setting<>("InstantHealing", new BooleanSettingGroup(true));
    private final Setting<Integer> healthH = new Setting<>("Health", 8, 0, 20).addToGroup(heal);
    private final Setting<BooleanSettingGroup> regen = new Setting<>("Regeneration", new BooleanSettingGroup(true));
    private final Setting<TriggerOn> triggerOn = new Setting<>("Trigger", TriggerOn.LackOfRegen).addToGroup(regen);
    private final Setting<Integer> healthR = new Setting<>("HP", 8, 0, 20, v -> triggerOn.is(TriggerOn.Health)).addToGroup(regen);
    private final Setting<Boolean> onDaGround = new Setting<>("OnlyOnGround", true);
    private final Setting<Boolean> pauseAura = new Setting<>("PauseAura", false);

    private boolean spoofed = false;
    private final Timer throwDelay = new Timer();

    public AutoBuff() {
        super("AutoBuff", Category.COMBAT);
    }

    public static int getPotionSlot(Potions potion) {
        for (int i = 0; i < 9; ++i)
            if (isStackPotion(mc.player.getInventory().getStack(i), potion)) return i;
        return -1;
    }

    public static boolean isPotionOnHotBar(Potions potions) {
        return getPotionSlot(potions) != -1;
    }

    public static boolean isStackPotion(ItemStack stack, Potions potion) {
        if (stack == null) return false;

        if (stack.getItem() instanceof SplashPotionItem) {
            PotionContentsComponent potionContentsComponent = stack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT);

            RegistryEntry<StatusEffect> id = switch (potion) {
                case STRENGTH -> StatusEffects.STRENGTH;
                case SPEED -> StatusEffects.SPEED;
                case FIRERES -> StatusEffects.FIRE_RESISTANCE;
                case HEAL -> StatusEffects.INSTANT_HEALTH;
                case REGEN -> StatusEffects.REGENERATION;
            };

            for (StatusEffectInstance effect : potionContentsComponent.getEffects()) {
                if (effect.getEffectType() == id) return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onPostRotationSet(EventAfterRotate event) {
        if (Aura.target != null && mc.player.getAttackCooldownProgress(1) > 0.5f) return;
        if (mc.player.age > 80 && shouldThrow()) {
            spoofed = true;
        }
    }

    private boolean shouldThrow() {
        return (!mc.player.hasStatusEffect(StatusEffects.SPEED) && isPotionOnHotBar(Potions.SPEED) && speed.getValue())
                || (!mc.player.hasStatusEffect(StatusEffects.STRENGTH) && isPotionOnHotBar(Potions.STRENGTH) && strength.getValue())
                || (!mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && isPotionOnHotBar(Potions.FIRERES) && fire.getValue())
                || (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthH.getValue() && isPotionOnHotBar(Potions.HEAL) && heal.getValue().isEnabled())
                || (!mc.player.hasStatusEffect(StatusEffects.REGENERATION) && triggerOn.is(TriggerOn.LackOfRegen) && isPotionOnHotBar(Potions.REGEN) && regen.getValue().isEnabled())
                || (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthR.getValue() && triggerOn.is(TriggerOn.Health) && isPotionOnHotBar(Potions.REGEN) && regen.getValue().isEnabled());
    }

    @EventHandler
    public void onPostSync(EventPostSync e) {
        if (Aura.target != null && mc.player.getAttackCooldownProgress(1) > 0.5f) return;

        if (onDaGround.getValue() && !mc.player.isOnGround()) return;

        if (mc.player.age > 80 && shouldThrow() && spoofed && throwDelay.passedMs(1000)) {
            mc.player.setPitch(90);

            boolean thrownAny = false;

            if (!mc.player.hasStatusEffect(StatusEffects.SPEED) && isPotionOnHotBar(Potions.SPEED) && speed.getValue()) {
                throwPotion(Potions.SPEED);
                thrownAny = true;
            }

            if (!mc.player.hasStatusEffect(StatusEffects.STRENGTH) && isPotionOnHotBar(Potions.STRENGTH) && strength.getValue()) {
                throwPotion(Potions.STRENGTH);
                thrownAny = true;
            }

            if (!mc.player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) && isPotionOnHotBar(Potions.FIRERES) && fire.getValue()) {
                throwPotion(Potions.FIRERES);
                thrownAny = true;
            }

            if (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthH.getValue() && heal.getValue().isEnabled() && isPotionOnHotBar(Potions.HEAL)) {
                throwPotion(Potions.HEAL);
                thrownAny = true;
            }

            if (((!mc.player.hasStatusEffect(StatusEffects.REGENERATION) && triggerOn.is(TriggerOn.LackOfRegen))
                    || (mc.player.getHealth() + mc.player.getAbsorptionAmount() < healthR.getValue() && triggerOn.is(TriggerOn.Health)))
                    && isPotionOnHotBar(Potions.REGEN) && regen.getValue().isEnabled()) {
                throwPotion(Potions.REGEN);
                thrownAny = true;
            }

            if (thrownAny) {
                sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
                throwDelay.reset();
                spoofed = false;
            }
        }
    }

    public void throwPotion(Potions potion) {
        if (pauseAura.getValue()) ModuleManager.aura.pause();
        sendPacket(new UpdateSelectedSlotC2SPacket(getPotionSlot(potion)));
        sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
    }

    public enum Potions {
        STRENGTH, SPEED, FIRERES, HEAL, REGEN
    }

    public enum TriggerOn {
        LackOfRegen, Health
    }
}
