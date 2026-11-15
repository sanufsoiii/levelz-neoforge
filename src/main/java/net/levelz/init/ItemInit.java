package net.levelz.init;

import net.levelz.LevelzMain;
import net.levelz.item.RareCandyItem;
import net.levelz.item.StrangePotionItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

public class ItemInit {

    public static final Item STRANGE_POTION = register("strange_potion", new StrangePotionItem(new Item.Properties().stacksTo(1)));
    public static final Item RARE_CANDY = register("rare_candy", new RareCandyItem(new Item.Properties()));

    private static Item register(String id, Item item) {
        return register(LevelzMain.identifierOf(id), item);
    }

    private static Item register(ResourceLocation id, Item item) {
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void init() {
//        FabricBrewingRecipeRegistryBuilder.BUILD.register((builder) -> {
//            builder.registerItemRecipe(Items.DRAGON_BREATH, Items.NETHER_STAR, STRANGE_POTION);
//        });
    }

    // Fabric ItemGroupEvents.modifyEntriesEvent -> BuildCreativeModeTabContentsEvent (mod bus).
    public static void registerTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            event.accept(STRANGE_POTION);
        } else if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(RARE_CANDY);
        }
    }
}
