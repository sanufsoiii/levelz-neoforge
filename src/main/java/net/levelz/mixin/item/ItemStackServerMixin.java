package net.levelz.mixin.item;

import net.levelz.util.BonusHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackServerMixin {

    // NeoForge turned hurtAndBreak(int, ServerLevel, ServerPlayer, Consumer) into a delegate and moved
    // processDurabilityChange into the LivingEntity overload; the original ServerPlayer-or-null semantics stay.
    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"), cancellable = true)
    private void damageMixin(int amount, ServerLevel world, @Nullable LivingEntity entity, Consumer<Item> breakCallback, CallbackInfo info) {
        if (BonusHelper.itemDamageChanceBonus(entity instanceof net.minecraft.server.level.ServerPlayer player ? player : null)) {
            info.cancel();
        }
    }

}
