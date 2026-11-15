package net.levelz.network;

import net.levelz.access.LevelManagerAccess;
import net.levelz.editor.EditorClientData;
import net.levelz.level.LevelManager;
import net.levelz.level.PlayerSkill;
import net.levelz.level.Skill;
import net.levelz.level.restriction.ComponentRestriction;
import net.levelz.network.packet.*;
import net.levelz.registry.EnchantmentRegistry;
import net.levelz.registry.EnchantmentZ;
import net.levelz.screen.LevelScreen;
import net.levelz.screen.RestrictionEditScreen;
import net.levelz.screen.RestrictionEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client-bound payload handlers. Loaded on the client only: the payload registrar calls them through lambdas with a
 * plain static invocation, so a dedicated server never resolves this class.
 */
@OnlyIn(Dist.CLIENT)
public class LevelClientPacket {

    public static void onSkillSync(SkillSyncPacket payload, IPayloadContext context) {
        List<Integer> skillIds = payload.skillIds();
        List<String> skillKeys = payload.skillKeys();
        List<Integer> skillMaxLevels = payload.skillMaxLevels();
        List<SkillSyncPacket.SkillAttributesRecord> skillAttributes = payload.skillAttributes();
        SkillSyncPacket.SkillBonusesRecord skillBonuses = payload.skillBonuses();
        Player player = context.player();

        context.enqueueWork(() -> {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            boolean integrated = Minecraft.getInstance().hasSingleplayerServer();

            if (!integrated) {
                LevelManager.SKILLS.clear();
            }
            for (int i = 0; i < skillIds.size(); i++) {
                if (!integrated) {
                    Skill skill = new Skill(skillIds.get(i), skillKeys.get(i), skillMaxLevels.get(i), skillAttributes.get(i).skillAttributes());
                    LevelManager.SKILLS.put(skillIds.get(i), skill);
                }

                if (!levelManager.getPlayerSkills().containsKey(skillIds.get(i))) {
                    PlayerSkill playerSkill = new PlayerSkill(skillIds.get(i), 0);
                    levelManager.getPlayerSkills().put(skillIds.get(i), playerSkill);
                }
            }
            if (!integrated) {
                LevelManager.BONUSES.clear();
                for (int i = 0; i < skillBonuses.skillBonuses().size(); i++) {
                    String bonusKey = skillBonuses.skillBonuses().get(i).getKey();
                    LevelManager.BONUSES.put(bonusKey, skillBonuses.skillBonuses().get(i));
                }
            }
        });
    }

    public static void onPlayerSkillSync(PlayerSkillSyncPacket payload, IPayloadContext context) {
        List<Integer> playerSkillIds = payload.playerSkillIds();
        List<Integer> playerSkillLevels = payload.playerSkillLevels();
        Player player = context.player();
        context.enqueueWork(() -> {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            for (int i = 0; i < playerSkillIds.size(); i++) {
                levelManager.setSkillLevel(playerSkillIds.get(i), playerSkillLevels.get(i));
            }
        });
    }

    public static void onLevel(LevelPacket payload, IPayloadContext context) {
        int overallLevel = payload.overallLevel();
        int skillPoints = payload.skillPoints();
        int totalLevelExperience = payload.totalLevelExperience();
        float levelProgress = payload.levelProgress();
        Player player = context.player();
        context.enqueueWork(() -> {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            levelManager.setOverallLevel(overallLevel);
            levelManager.setSkillPoints(skillPoints);
            levelManager.setTotalLevelExperience(totalLevelExperience);
            levelManager.setLevelProgress(levelProgress);
        });
    }

    public static void onRestriction(RestrictionPacket payload, IPayloadContext context) {
        RestrictionPacket.RestrictionRecord blockRestrictions = payload.blockRestrictions();
        RestrictionPacket.RestrictionRecord craftingRestrictions = payload.craftingRestrictions();
        RestrictionPacket.RestrictionRecord entityRestrictions = payload.entityRestrictions();
        RestrictionPacket.RestrictionRecord itemRestrictions = payload.itemRestrictions();
        RestrictionPacket.RestrictionRecord miningRestrictions = payload.miningRestrictions();
        RestrictionPacket.RestrictionRecord enchantmentRestrictions = payload.enchantmentRestrictions();
        List<ComponentRestriction> componentRestrictions = payload.componentRestrictions();

        context.enqueueWork(() -> {
            if (Minecraft.getInstance().hasSingleplayerServer()) {
                return;
            }
            LevelManager.BLOCK_RESTRICTIONS.clear();
            LevelManager.CRAFTING_RESTRICTIONS.clear();
            LevelManager.ENTITY_RESTRICTIONS.clear();
            LevelManager.ITEM_RESTRICTIONS.clear();
            LevelManager.MINING_RESTRICTIONS.clear();
            LevelManager.ENCHANTMENT_RESTRICTIONS.clear();

            for (int i = 0; i < blockRestrictions.ids().size(); i++) {
                LevelManager.BLOCK_RESTRICTIONS.put(blockRestrictions.ids().get(i), blockRestrictions.restrictions().get(i));
            }
            for (int i = 0; i < craftingRestrictions.ids().size(); i++) {
                LevelManager.CRAFTING_RESTRICTIONS.put(craftingRestrictions.ids().get(i), craftingRestrictions.restrictions().get(i));
            }
            for (int i = 0; i < entityRestrictions.ids().size(); i++) {
                LevelManager.ENTITY_RESTRICTIONS.put(entityRestrictions.ids().get(i), entityRestrictions.restrictions().get(i));
            }
            for (int i = 0; i < itemRestrictions.ids().size(); i++) {
                LevelManager.ITEM_RESTRICTIONS.put(itemRestrictions.ids().get(i), itemRestrictions.restrictions().get(i));
            }
            for (int i = 0; i < miningRestrictions.ids().size(); i++) {
                LevelManager.MINING_RESTRICTIONS.put(miningRestrictions.ids().get(i), miningRestrictions.restrictions().get(i));
            }
            for (int i = 0; i < enchantmentRestrictions.ids().size(); i++) {
                LevelManager.ENCHANTMENT_RESTRICTIONS.put(enchantmentRestrictions.ids().get(i), enchantmentRestrictions.restrictions().get(i));
            }
            LevelManager.COMPONENT_RESTRICTIONS.clear();
            for (ComponentRestriction componentRestriction : componentRestrictions) {
                LevelManager.COMPONENT_RESTRICTIONS.computeIfAbsent(componentRestriction.getId(), id -> new ArrayList<>()).add(componentRestriction);
            }
        });
    }

    public static void onStat(StatPacket payload, IPayloadContext context) {
        int id = payload.id();
        int level = payload.level();
        Player player = context.player();
        context.enqueueWork(() -> {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            levelManager.setSkillLevel(id, level);
            if (Minecraft.getInstance().screen instanceof LevelScreen levelScreen) {
                levelScreen.updateLevelButtons();
            }
        });
    }

    public static void onEditorData(EditorDataPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            EditorClientData.update(payload.entries(), payload.skills(), payload.problems());
            if (payload.openScreen()) {
                Minecraft.getInstance().setScreen(new RestrictionEditorScreen());
            } else if (Minecraft.getInstance().screen instanceof RestrictionEditorScreen restrictionEditorScreen) {
                restrictionEditorScreen.refresh();
            } else if (Minecraft.getInstance().screen instanceof RestrictionEditScreen restrictionEditScreen) {
                restrictionEditScreen.onDataRefreshed();
            }
        });
    }

    public static void onEditorResult(EditorResultPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            EditorClientData.lastResult = payload;
            if (Minecraft.getInstance().screen instanceof RestrictionEditScreen restrictionEditScreen) {
                restrictionEditScreen.onResult(payload);
            } else if (Minecraft.getInstance().screen instanceof RestrictionEditorScreen restrictionEditorScreen) {
                restrictionEditorScreen.onResult(payload);
            }
        });
    }

    public static void onEnchantmentZ(EnchantmentZPacket payload, IPayloadContext context) {
        Map<String, Integer> indexed = payload.indexed();
        List<Integer> keys = payload.keys();
        List<String> ids = payload.ids();
        List<Integer> levels = payload.levels();
        Player player = context.player();
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().hasSingleplayerServer()) {
                return;
            }
            EnchantmentRegistry.ENCHANTMENTS.clear();
            EnchantmentRegistry.INDEX_ENCHANTMENTS.clear();

            Registry<Enchantment> registry = player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            for (int i = 0; i < keys.size(); i++) {
                int key = keys.get(i);
                Holder<Enchantment> entry = registry.getHolder(ResourceLocation.parse(ids.get(i))).get();
                int level = levels.get(i);
                EnchantmentRegistry.ENCHANTMENTS.put(key, new EnchantmentZ(entry, level));
            }
            EnchantmentRegistry.INDEX_ENCHANTMENTS.putAll(indexed);
        });
    }

    public static void writeC2SAttributeSyncPacket() {
        PacketDistributor.sendToServer(new AttributeSyncPacket());
    }
}
