package net.levelz.init;

import net.levelz.LevelzMain;
import net.levelz.entity.render.LevelExperienceOrbEntityRenderer;
import net.levelz.screen.LevelScreen;
import net.levelz.screen.SkillInfoScreen;
import net.levelz.screen.SkillRestrictionScreen;
import net.levelz.screen.widget.LevelzTab;
import net.levelz.screen.widget.VanillaInventoryTab;
import net.levelz.util.TooltipUtil;
import net.libz.registry.TabRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public class RenderInit {

    public static final ResourceLocation SKILL_TAB_ICON = LevelzMain.identifierOf("textures/gui/sprites/skill_tab_icon.png");
    public static final ResourceLocation BAG_TAB_ICON = LevelzMain.identifierOf("textures/gui/sprites/bag_tab_icon.png");

    public static final ResourceLocation MINEABLE_INFO = LevelzMain.identifierOf("mineable_info");
    public static final ResourceLocation MINEABLE_LEVEL_INFO = LevelzMain.identifierOf("mineable_level_info");

    public static void init(IEventBus modBus, IEventBus gameBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerEntityRenderer(EntityInit.LEVEL_EXPERIENCE_ORB, LevelExperienceOrbEntityRenderer::new);
        });

        // LibZ tab lists are plain ArrayLists and client setup of different mods runs in parallel -> enqueueWork.
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
            TabRegistry.registerInventoryTab(new VanillaInventoryTab(Component.translatable("container.crafting"), BAG_TAB_ICON, 0, InventoryScreen.class));
            TabRegistry.registerInventoryTab(new LevelzTab(Component.translatable("screen.levelz.skill_screen"), SKILL_TAB_ICON, 1, LevelScreen.class, SkillInfoScreen.class, SkillRestrictionScreen.class));
        }));

        modBus.addListener(RegisterGuiLayersEvent.class, event -> {
            event.registerAboveAll(LevelzMain.identifierOf("tooltip"), (graphics, deltaTracker) -> {
                TooltipUtil.renderTooltip(Minecraft.getInstance(), graphics);
            });
        });

        gameBus.addListener(ItemTooltipEvent.class, event -> {
            TooltipUtil.renderItemTooltip(Minecraft.getInstance(), event.getItemStack(), event.getToolTip());
        });
    }
}
