package net.levelz.network;

import net.levelz.access.LevelManagerAccess;
import net.levelz.editor.RestrictionEditor;
import net.levelz.init.ConfigInit;
import net.levelz.init.CriteriaInit;
import net.levelz.level.LevelManager;
import net.levelz.level.PlayerSkill;
import net.levelz.level.Skill;
import net.levelz.network.packet.*;
import net.levelz.util.LevelHelper;
import net.levelz.util.PacketHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

public class LevelServerPacket {

    public static final PacketType<OrbPacket> ADD_LEVEL_EXPERIENCE_ORB = s2c("add_level_experience_orb");

    public static void init(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, LevelServerPacket::register);
    }

    private static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Client-bound handlers live in LevelClientPacket (client only). Lambdas with a plain static call, not method
        // references: the call is resolved lazily, so a dedicated server never loads LevelClientPacket.
        registrar.playToClient(SkillSyncPacket.PACKET_ID, SkillSyncPacket.PACKET_CODEC, (payload, context) -> LevelClientPacket.onSkillSync(payload, context));
        registrar.playToClient(PlayerSkillSyncPacket.PACKET_ID, PlayerSkillSyncPacket.PACKET_CODEC, (payload, context) -> LevelClientPacket.onPlayerSkillSync(payload, context));
        registrar.playToClient(LevelPacket.PACKET_ID, LevelPacket.PACKET_CODEC, (payload, context) -> LevelClientPacket.onLevel(payload, context));
        registrar.playToClient(RestrictionPacket.PACKET_ID, RestrictionPacket.PACKET_CODEC, (payload, context) -> LevelClientPacket.onRestriction(payload, context));
        registrar.playToClient(EnchantmentZPacket.PACKET_ID, EnchantmentZPacket.PACKET_CODEC, (payload, context) -> LevelClientPacket.onEnchantmentZ(payload, context));
        registrar.playToClient(EditorDataPacket.PACKET_ID, EditorDataPacket.PACKET_CODEC, (payload, context) -> LevelClientPacket.onEditorData(payload, context));
        registrar.playToClient(EditorResultPacket.PACKET_ID, EditorResultPacket.PACKET_CODEC, (payload, context) -> LevelClientPacket.onEditorResult(payload, context));

        // StatPacket is registered in both directions with one id, exactly like the original.
        registrar.playBidirectional(StatPacket.PACKET_ID, StatPacket.PACKET_CODEC,
                new DirectionalPayloadHandler<>((payload, context) -> LevelClientPacket.onStat(payload, context), LevelServerPacket::handleStat));

        registrar.playToServer(AttributeSyncPacket.PACKET_ID, AttributeSyncPacket.PACKET_CODEC, LevelServerPacket::handleAttributeSync);
        registrar.playToServer(EditorRequestPacket.PACKET_ID, EditorRequestPacket.PACKET_CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> RestrictionEditor.sendEditorData(player, false));
        });
        registrar.playToServer(EditorSavePacket.PACKET_ID, EditorSavePacket.PACKET_CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            String key = payload.key();
            String json = payload.json();
            int baseHash = payload.baseHash();
            context.enqueueWork(() -> RestrictionEditor.save(player, key, json, baseHash));
        });
        registrar.playToServer(EditorDeletePacket.PACKET_ID, EditorDeletePacket.PACKET_CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            String key = payload.key();
            int baseHash = payload.baseHash();
            context.enqueueWork(() -> RestrictionEditor.delete(player, key, baseHash));
        });
        registrar.playToServer(EditorExportPacket.PACKET_ID, EditorExportPacket.PACKET_CODEC, (payload, context) -> {
            ServerPlayer player = (ServerPlayer) context.player();
            context.enqueueWork(() -> RestrictionEditor.export(player));
        });
    }

    private static void handleStat(StatPacket payload, IPayloadContext context) {
        int id = payload.id();
        int level = payload.level();
        ServerPlayer player = (ServerPlayer) context.player();

        context.enqueueWork(() -> {
            LevelManager levelManager = ((LevelManagerAccess) player).getLevelManager();
            if (level > 0 && levelManager.getSkillPoints() - level >= 0) {

                Skill skill = LevelManager.SKILLS.get(id);
                PlayerSkill playerSkill = levelManager.getPlayerSkills().get(id);

                if (skill == null || playerSkill == null) {
                    return;
                }
                if (!ConfigInit.CONFIG.allowHigherSkillLevel && playerSkill.getLevel() + level > skill.getMaxLevel()) {
                    return;
                }
                if (ConfigInit.CONFIG.allowHigherSkillLevel) {
                    if (playerSkill.getLevel() >= skill.getMaxLevel()) {
                        for (Skill skillCheck : LevelManager.SKILLS.values()) {
                            if (skillCheck.getMaxLevel() > levelManager.getSkillLevel(skillCheck.getId())) {
                                return;
                            }
                        }
                    }
                }

                for (int i = 1; i <= level; i++) {
                    CriteriaInit.SKILL_UP.trigger(player, skill.getKey(), playerSkill.getLevel() + i);
                }

                levelManager.setSkillLevel(id, playerSkill.getLevel() + level);
                levelManager.setSkillPoints(levelManager.getSkillPoints() - level);
                LevelHelper.updateSkill(player, skill);
                PacketHelper.updateLevels(player);

                PacketDistributor.sendToPlayer(player, new StatPacket(id, levelManager.getSkillLevel(id)));
            }
        });
    }

    private static void handleAttributeSync(AttributeSyncPacket payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            // Following are already synced
            // Collection<EntityAttributeInstance> collection = player.getAttributes().getAttributesToSend();
            // player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), collection));
            // Is required lul
            player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), List.of(player.getAttribute(Attributes.ATTACK_DAMAGE))));
        });
    }

    private static <T extends Packet<ClientGamePacketListener>> PacketType<T> s2c(String id) {
        return new PacketType<>(PacketFlow.CLIENTBOUND, ResourceLocation.withDefaultNamespace(id));
    }
}
