package net.levelz.port;

import net.levelz.data.DatapackErrors;
import net.levelz.init.ConfigInit;
import net.levelz.level.LevelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Dedicated-server self-check of the port (run configuration {@code serverSmoke}). Active only with
 * {@code -Dlevelz.port.serverSmoke=true}, does nothing otherwise.
 * <p>
 * On {@link ServerStartedEvent}: forces every common mixin of the pack to apply ({@code MixinEnvironment.audit()}),
 * spawns one instance of every {@link EntityType} of the mod at the world spawn, logs the number of skills and
 * restrictions loaded from the datapack, lets the server tick {@value #TICKS} times, logs
 * {@code [levelz-port] SERVER_SMOKE_OK entities=<N>} and stops the server.
 */
public final class ServerSmoke {

    public static final String PROPERTY = "levelz.port.serverSmoke";
    private static final Logger LOGGER = LoggerFactory.getLogger("levelz");
    private static final int TICKS = 100;

    private static int ticksLeft = -1;
    private static int spawned = 0;
    private static int entityTypes = 0;
    private static int itemTypes = 0;
    private static int criteriaTypes = 0;
    private static int mixinTargetFailures = 0;

    private ServerSmoke() {
    }

    public static void register(IEventBus gameBus) {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        gameBus.addListener(ServerStartedEvent.class, ServerSmoke::onServerStarted);
        gameBus.addListener(ServerTickEvent.Post.class, ServerSmoke::onServerTick);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = server.overworld();
        BlockPos spawn = level.getSharedSpawnPos();
        LOGGER.info("[levelz-port] SERVER_SMOKE_START dedicated={} spawn={}", server.isDedicatedServer(), spawn.toShortString());

        // Load every mixin target first: audit() only sees classes that are already loaded.
        mixinTargetFailures = MixinTargetLoader.load(LOGGER, MixinTargetLoader.COMMON);
        LOGGER.info("[levelz-port] SERVER_SMOKE mixin targets={} failures={}", MixinTargetLoader.COMMON.size(), mixinTargetFailures);

        // Apply every pending mixin now, so a broken target fails here and not when some class is first used in game.
        MixinEnvironment.getCurrentEnvironment().audit();
        LOGGER.info("[levelz-port] SERVER_SMOKE mixin audit done");

        // The mod adds a vanilla-style packet to the game protocol; prove that the mixin really inserted it.
        java.util.concurrent.atomic.AtomicInteger orbPacketId = new java.util.concurrent.atomic.AtomicInteger(-1);
        net.minecraft.network.protocol.game.GameProtocols.CLIENTBOUND_TEMPLATE.listPackets((type, id) -> {
            if (type == net.levelz.network.LevelServerPacket.ADD_LEVEL_EXPERIENCE_ORB) {
                orbPacketId.set(id);
            }
        });
        if (orbPacketId.get() < 0) {
            LOGGER.error("[levelz-port] SERVER_SMOKE_FAILED orb packet is not in the clientbound protocol");
        } else {
            LOGGER.info("[levelz-port] SERVER_SMOKE orb_packet_id={}", orbPacketId.get());
        }

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!net.levelz.LevelzMain.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) {
                continue;
            }
            entityTypes++;
            Entity entity = type.spawn(level, spawn.above(), MobSpawnType.COMMAND);
            if (entity != null) {
                spawned++;
            } else {
                LOGGER.error("[levelz-port] SERVER_SMOKE could not spawn {}", BuiltInRegistries.ENTITY_TYPE.getKey(type));
            }
        }
        for (var item : BuiltInRegistries.ITEM) {
            if (net.levelz.LevelzMain.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) {
                itemTypes++;
            }
        }
        for (var criterion : BuiltInRegistries.TRIGGER_TYPES) {
            if (net.levelz.LevelzMain.MOD_ID.equals(BuiltInRegistries.TRIGGER_TYPES.getKey(criterion).getNamespace())) {
                criteriaTypes++;
            }
        }
        LOGGER.info("[levelz-port] SERVER_SMOKE registries: items={} entity_types={} criteria={}", itemTypes, entityTypes, criteriaTypes);

        LOGGER.info("[levelz-port] SERVER_SMOKE data: skills={} bonuses={} block={} crafting={} entity={} item={} mining={} enchantment={} component={} dimension={} datapack_errors={}",
                LevelManager.SKILLS.size(), LevelManager.BONUSES.size(), LevelManager.BLOCK_RESTRICTIONS.size(), LevelManager.CRAFTING_RESTRICTIONS.size(),
                LevelManager.ENTITY_RESTRICTIONS.size(), LevelManager.ITEM_RESTRICTIONS.size(), LevelManager.MINING_RESTRICTIONS.size(),
                LevelManager.ENCHANTMENT_RESTRICTIONS.size(), LevelManager.COMPONENT_RESTRICTIONS.size(), LevelManager.DIMENSION_RESTRICTIONS.size(),
                DatapackErrors.getErrors().size());
        for (String error : DatapackErrors.getErrors()) {
            LOGGER.error("[levelz-port] SERVER_SMOKE datapack error: {}", error);
        }
        LOGGER.info("[levelz-port] SERVER_SMOKE config: startPoints={} levelRetainPercentage={} disableMobFarms={}",
                ConfigInit.CONFIG.startPoints, ConfigInit.CONFIG.levelRetainPercentage, ConfigInit.CONFIG.disableMobFarms);
        ticksLeft = TICKS;
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (ticksLeft < 0) {
            return;
        }
        if (ticksLeft-- > 0) {
            return;
        }
        if (spawned != entityTypes) {
            LOGGER.error("[levelz-port] SERVER_SMOKE_FAILED entity types={} spawned={}", entityTypes, spawned);
        } else if (LevelManager.SKILLS.isEmpty() || LevelManager.BLOCK_RESTRICTIONS.isEmpty()) {
            LOGGER.error("[levelz-port] SERVER_SMOKE_FAILED skills={} block_restrictions={}", LevelManager.SKILLS.size(), LevelManager.BLOCK_RESTRICTIONS.size());
        } else if (!DatapackErrors.getErrors().isEmpty()) {
            LOGGER.error("[levelz-port] SERVER_SMOKE_FAILED datapack_errors={}", DatapackErrors.getErrors().size());
        } else if (itemTypes != 2 || criteriaTypes != 2) {
            LOGGER.error("[levelz-port] SERVER_SMOKE_FAILED items={} criteria={}", itemTypes, criteriaTypes);
        } else if (mixinTargetFailures != 0) {
            LOGGER.error("[levelz-port] SERVER_SMOKE_FAILED mixin_target_failures={}", mixinTargetFailures);
        } else {
            LOGGER.info("[levelz-port] SERVER_SMOKE_OK entities={} skills={} restrictions={} ticks={}", spawned, LevelManager.SKILLS.size(),
                    LevelManager.BLOCK_RESTRICTIONS.size() + LevelManager.CRAFTING_RESTRICTIONS.size() + LevelManager.ENTITY_RESTRICTIONS.size()
                            + LevelManager.ITEM_RESTRICTIONS.size() + LevelManager.MINING_RESTRICTIONS.size() + LevelManager.ENCHANTMENT_RESTRICTIONS.size(),
                    TICKS);
        }
        event.getServer().halt(false);
    }

}
