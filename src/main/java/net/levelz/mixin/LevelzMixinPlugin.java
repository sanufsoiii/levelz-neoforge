package net.levelz.mixin;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.neoforged.fml.loading.LoadingModList;

public class LevelzMixinPlugin implements IMixinConfigPlugin {

    // At mixin time only the loading mod list exists (ModList is not built yet).
    private static boolean isModLoaded(String modId) {
        return LoadingModList.get().getModFileById(modId) != null;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!isModLoaded("trinkets")
                && (mixinClassName.equals("net.levelz.mixin.compat.TrinketItemMixin") || mixinClassName.equals("net.levelz.mixin.compat.SurvivalTrinketSlotMixin")))
            return false;

        if (mixinClassName.contains("LevelManagerCompatMixin") && !isModLoaded("create") && !isModLoaded("computercraft"))
            return false;

        if (mixinClassName.contains("FishingRodItemMixin") && isModLoaded("go-fish"))
            return false;

        if (mixinClassName.contains("ArmorItemMixin") && isModLoaded("cardboard"))
            return false;

        if (mixinClassName.contains("AnvilScreenHandlerMixin") && isModLoaded("limitless"))
            return false;

        if (mixinClassName.contains("BackpackItemMixin") && !isModLoaded("inmis"))
            return false;

        if (mixinClassName.contains("EasyMagicEnchantingTableMixin") && !isModLoaded("easymagic"))
            return false;

        if (mixinClassName.contains("EasyAnvilsAnvilMixin") && !isModLoaded("easyanvils"))
            return false;

        if (mixinClassName.contains("CosmeticArmorMixin") && !isModLoaded("cosmetic-armor"))
            return false;

        if (mixinClassName.contains("ChopResultMixin") && !isModLoaded("treechop"))
            return false;

        if (mixinClassName.contains("SmithingAnvilScreenHandlerMixin") && !isModLoaded("alloygery"))
            return false;

        if (mixinClassName.contains("TieredCompatMixin") && isModLoaded("tiered"))
            return false;

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
