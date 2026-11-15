package net.levelz.compat;

import ht.treechop.api.ChopEvent;
import net.levelz.access.LevelManagerAccess;
import net.levelz.level.LevelManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;

/**
 * TreeChop integration. Lives in its own class so that {@link ChopEvent} is never loaded when TreeChop is absent.
 * The original Fabric callback returned {@code false} to cancel the chop; NeoForge TreeChop uses a cancellable event.
 */
public final class TreeChopCompat {

    private TreeChopCompat() {
    }

    public static void register(IEventBus gameBus) {
        gameBus.addListener(ChopEvent.StartChopEvent.class, event -> {
            if (!(event.getPlayer() instanceof ServerPlayer player)) {
                return;
            }
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            if (!levelManager.hasRequiredStackLevel(player.getMainHandItem())) {
                player.level().destroyBlock(event.getChoppedBlockPos(), false);
                event.setCanceled(true);
            } else if (!levelManager.hasRequiredMiningLevel(event.getLevel().getBlockState(event.getChoppedBlockPos()).getBlock())) {
                player.level().destroyBlock(event.getChoppedBlockPos(), false);
                event.setCanceled(true);
            }
        });
    }
}
