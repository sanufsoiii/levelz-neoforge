package net.levelz.init;

import net.levelz.access.LevelManagerAccess;
import net.levelz.data.DatapackErrors;
import net.levelz.data.RestrictionLoader;
import net.levelz.data.SkillLoader;
import net.levelz.level.LevelManager;
import net.levelz.level.Skill;
import net.levelz.util.LevelHelper;
import net.levelz.util.PacketHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoaderInit {

    public static final Logger LOGGER = LogManager.getLogger("LevelZ");

    public static void init(IEventBus gameBus) {
        // Restriction depends on the skills, so it is registered second (Fabric dependency of the original).
        gameBus.addListener(AddReloadListenerEvent.class, event -> {
            event.addListener(new SkillLoader());
            event.addListener(new RestrictionLoader(event.getRegistryAccess()));
        });

        // Fabric CommonLifecycleEvents.TAGS_LOADED, server side only.
        gameBus.addListener(TagsUpdatedEvent.class, event -> {
            if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
                RestrictionLoader.onTagsLoaded(event.getRegistryAccess());
            }
        });

        // Fabric ServerLifecycleEvents.SERVER_STARTING.
        gameBus.addListener(ServerAboutToStartEvent.class, event -> RestrictionLoader.applyOverrides(event.getServer()));

        // Fabric ServerLifecycleEvents.END_DATA_PACK_RELOAD. NeoForge only fires this after a successful /reload, so the
        // original "Failed to reload" branch cannot happen.
        gameBus.addListener(OnDatapackSyncEvent.class, event -> {
            if (event.getPlayer() != null) {
                return;
            }
            var server = event.getPlayerList().getServer();
            RestrictionLoader.applyOverrides(server);
            for (int i = 0; i < server.getPlayerList().getPlayers().size(); i++) {
                ServerPlayer serverPlayerEntity = server.getPlayerList().getPlayers().get(i);
                ((LevelManagerAccess) serverPlayerEntity).getLevelManager().refreshSkills();
                for (Skill skill : LevelManager.SKILLS.values()) {
                    LevelHelper.updateSkill(serverPlayerEntity, skill);
                }
                PacketHelper.syncEnchantments(serverPlayerEntity);
                PacketHelper.updateSkills(serverPlayerEntity);
                PacketHelper.updatePlayerSkills(serverPlayerEntity, null);
                PacketHelper.updateRestrictions(serverPlayerEntity);
                PacketHelper.updateLevels(serverPlayerEntity);
                DatapackErrors.notify(serverPlayerEntity);
            }
            LOGGER.info("Finished reload on {}", Thread.currentThread());
        });
    }

}
