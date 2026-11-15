package net.levelz.mixin.misc;

import com.llamalad7.mixinextras.sugar.Local;
import net.levelz.access.MobEntityAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseSpawner.class)
public class MobSpawnerLogicMixin {

    // NeoForge replaced the vanilla Mob#finalizeSpawn call with EventHooks#finalizeMobSpawnSpawner, which is called for
    // every spawner-spawned mob. Its last "def" argument is the vanilla condition under which finalizeSpawn is actually
    // run, so it reproduces the original injection point (which only ran when finalizeSpawn was called).
    @Inject(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/EventHooks;finalizeMobSpawnSpawner(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/DifficultyInstance;Lnet/minecraft/world/entity/MobSpawnType;Lnet/minecraft/world/entity/SpawnGroupData;Lnet/neoforged/neoforge/common/extensions/IOwnedSpawner;Z)Lnet/neoforged/neoforge/event/entity/living/FinalizeSpawnEvent;", shift = At.Shift.AFTER))
    private void serverTickMixin(ServerLevel world, BlockPos pos, CallbackInfo ci, @Local Mob mob, @Local(ordinal = 1) boolean def) {
        if (def) {
            ((MobEntityAccess) mob).setSpawnerMob(true);
        }
    }

}
