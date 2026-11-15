package net.levelz.mixin.item;

import net.levelz.util.BonusHelper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.PotionItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PotionItem.class)
public class PotionItemMixin {

    @ModifyVariable(method = "lambda$finishUsingItem$0(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("HEAD"), argsOnly = true)
    private static MobEffectInstance finishUsingMixin(MobEffectInstance original, Player playerEntity, LivingEntity livingEntity, MobEffectInstance statusEffectInstance) {
        return BonusHelper.potionEffectChanceBonus(playerEntity, original);
    }

}
