package net.levelz.init;

import net.levelz.access.LevelManagerAccess;
import net.levelz.data.DatapackErrors;
import net.levelz.level.LevelManager;
import net.levelz.level.PlayerSkill;
import net.levelz.level.Skill;
import net.levelz.mixin.entity.EntityAccessor;
import net.levelz.util.LevelHelper;
import net.levelz.util.PacketHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EventInit {

    // Fabric's COPY_FROM + AFTER_RESPAWN get the old player in one event; NeoForge's PlayerRespawnEvent does not have it,
    // so the original is remembered in PlayerEvent.Clone (fired from ServerPlayer.restoreFrom right before it).
    private static final Map<UUID, ServerPlayer> RESPAWN_ORIGINALS = new ConcurrentHashMap<>();

    public static void init(IEventBus gameBus) {
        // Fabric ServerPlayConnectionEvents.JOIN -> PlayerLoggedInEvent (end of PlayerList.placeNewPlayer).
        gameBus.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            for (Skill skill : LevelManager.SKILLS.values()) {
                LevelHelper.updateSkill(player, skill);
            }
            PacketHelper.syncEnchantments(player);
            PacketHelper.updateSkills(player);
            PacketHelper.updatePlayerSkills(player, null);
            PacketHelper.updateRestrictions(player);
            DatapackErrors.notify(player);
        });

        // Fabric ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.
        gameBus.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> {
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            PacketHelper.updatePlayerSkills(player, null);
            PacketHelper.updateLevels(player);
        });

        // Fabric ServerPlayerEvents.COPY_FROM -> PlayerEvent.Clone (last line of ServerPlayer.restoreFrom).
        gameBus.addListener(PlayerEvent.Clone.class, event -> {
            if (!(event.getEntity() instanceof ServerPlayer newPlayer) || !(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
                return;
            }
            RESPAWN_ORIGINALS.put(newPlayer.getUUID(), oldPlayer);
            if (!event.isWasDeath()) {
                PacketHelper.updatePlayerSkills(newPlayer, oldPlayer);
                PacketHelper.updateLevels(newPlayer);
            }
        });

        // A player can disconnect after dying but before confirming the respawn; do not keep the old entity until then.
        gameBus.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                RESPAWN_ORIGINALS.remove(player.getUUID());
            }
        });

        // Fabric ServerPlayerEvents.AFTER_RESPAWN -> PlayerEvent.PlayerRespawnEvent (end of PlayerList.respawn).
        // "alive" of Fabric == "end conquered" of NeoForge.
        gameBus.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> {
            if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
                return;
            }
            ServerPlayer oldPlayer = RESPAWN_ORIGINALS.remove(newPlayer.getUUID());
            if (oldPlayer == null) {
                return;
            }
            boolean alive = event.isEndConquered();
            LevelManager newLevelManager = ((LevelManagerAccess) newPlayer).getLevelManager();

            // Fabric also fires AFTER_PLAYER_CHANGE_WORLD when a respawn moves the player to another dimension.
            if (oldPlayer.level().dimension() != newPlayer.level().dimension()) {
                PacketHelper.updatePlayerSkills(newPlayer, null);
                PacketHelper.updateLevels(newPlayer);
            }

            if (alive) {
                for (Skill skill : LevelManager.SKILLS.values()) {
                    LevelHelper.updateSkill(newPlayer, skill);
                }
                return;
            }

            if (ConfigInit.CONFIG.levelRetainPercentage < 100) {
                LevelManager oldLevelManager = ((LevelManagerAccess) oldPlayer).getLevelManager();
                float levelRetainPercentageFloat = ConfigInit.CONFIG.levelRetainPercentage / 100;
                int retainedLevel = (int) (oldLevelManager.getOverallLevel() * levelRetainPercentageFloat);

                int pointsToDistribute = retainedLevel * ConfigInit.CONFIG.pointsPerLevel + ConfigInit.CONFIG.startPoints;

                if (oldLevelManager.getSkillPoints() > 0) {
                    int retainingSkillPoints = (int) (oldLevelManager.getSkillPoints() * levelRetainPercentageFloat);
                    newLevelManager.setSkillPoints(retainingSkillPoints);
                    pointsToDistribute -= retainingSkillPoints;
                }
                for (Map.Entry<Integer, PlayerSkill> entry : oldLevelManager.getPlayerSkills().entrySet()) {
                    int retainingLevel = (int) (entry.getValue().getLevel() * levelRetainPercentageFloat);
                    newLevelManager.setSkillLevel(entry.getKey(), retainingLevel);
                    pointsToDistribute -= retainingLevel;
                    if (pointsToDistribute < 0) {
                        break;
                    }
                }
                if (pointsToDistribute > 0) {
                    for (Map.Entry<Integer, PlayerSkill> entry : oldLevelManager.getPlayerSkills().entrySet()) {
                        if (newLevelManager.getSkillLevel(entry.getKey()) < entry.getValue().getLevel()) {
                            int levelDifference = entry.getValue().getLevel() - newLevelManager.getSkillLevel(entry.getKey());
                            if (levelDifference < pointsToDistribute) {
                                newLevelManager.setSkillLevel(entry.getKey(), newLevelManager.getSkillLevel(entry.getKey()) + levelDifference);
                                pointsToDistribute -= levelDifference;
                            } else {
                                newLevelManager.setSkillLevel(entry.getKey(), newLevelManager.getSkillLevel(entry.getKey()) + pointsToDistribute);
                                pointsToDistribute = 0;
                                break;
                            }
                        }
                    }
                    if (pointsToDistribute > 0) {
                        newLevelManager.setSkillPoints(newLevelManager.getSkillPoints() + pointsToDistribute);
                    }
                }

                PacketHelper.updatePlayerSkills(newPlayer, null);

                newLevelManager.setOverallLevel(retainedLevel);
                PacketHelper.updateLevels(newPlayer);
                newPlayer.getServer().getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE, newPlayer));
            } else {
                PacketHelper.updatePlayerSkills(newPlayer, oldPlayer);

                if (ConfigInit.CONFIG.resetCurrentXp) {
                    float lostProgress = newLevelManager.getLevelProgress() * Mth.clamp(ConfigInit.CONFIG.currentXpLossPercentage, 0.0F, 100.0F) / 100.0F;
                    int lostExperience = (int) (lostProgress * newLevelManager.getNextLevelExperience());
                    newLevelManager.setLevelProgress(newLevelManager.getLevelProgress() - lostProgress);
                    newLevelManager.setTotalLevelExperience(Math.max(newLevelManager.getTotalLevelExperience() - lostExperience, 0));
                }

                PacketHelper.updateLevels(newPlayer);
            }
            for (Skill skill : LevelManager.SKILLS.values()) {
                LevelHelper.updateSkill(newPlayer, skill);
            }
        });

        // Fabric UseItemCallback -> PlayerInteractEvent.RightClickItem (both sides, before the item is used).
        gameBus.addListener(PlayerInteractEvent.RightClickItem.class, event -> {
            Player player = event.getEntity();
            if (!player.isCreative() && !player.isSpectator()) {
                LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                if (!levelManager.hasRequiredStackLevel(player.getItemInHand(event.getHand()))) {
                    // player.sendMessage(Text.translatable("item.levelz." + customList.get(customList.indexOf(string) + 1) +
                    // ".tooltip", customList.get(customList.indexOf(string) + 2)).formatted(Formatting.RED), true);
                    player.displayClientMessage(Component.translatable("restriction.levelz.locked.tooltip").withStyle(ChatFormatting.RED), true);
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.FAIL);
                }
            }
        });

        // Fabric UseBlockCallback -> PlayerInteractEvent.RightClickBlock (both sides, before the block is used).
        gameBus.addListener(PlayerInteractEvent.RightClickBlock.class, event -> {
            Player player = event.getEntity();
            if (!player.isCreative() && !player.isSpectator()) {
                BlockPos blockPos = event.getPos();
                if (event.getLevel().mayInteract(player, blockPos)) {
                    LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
                    if (!levelManager.hasRequiredBlockLevel(event.getLevel().getBlockState(blockPos).getBlock())) {
                        player.displayClientMessage(Component.translatable("restriction.levelz.locked.tooltip").withStyle(ChatFormatting.RED), true);
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.CONSUME);
                    }
                }
            }
            return;
        });

        // Fabric UseEntityCallback is called for both interact and interactAt; NeoForge has one event for each.
        gameBus.addListener(PlayerInteractEvent.EntityInteract.class, event -> {
            if (cancelEntityInteraction(event.getEntity(), event.getTarget())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
        });
        gameBus.addListener(PlayerInteractEvent.EntityInteractSpecific.class, event -> {
            if (cancelEntityInteraction(event.getEntity(), event.getTarget())) {
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.CONSUME);
            }
        });
    }

    private static boolean cancelEntityInteraction(Player player, net.minecraft.world.entity.Entity entity) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        if (!entity.hasControllingPassenger() || !((EntityAccessor) entity).callCanAddPassenger(player)) {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            if (!levelManager.hasRequiredEntityLevel(entity.getType())) {
                player.displayClientMessage(Component.translatable("restriction.levelz.locked.tooltip").withStyle(ChatFormatting.RED), true);
                return true;
            }
        }
        return false;
    }
}
