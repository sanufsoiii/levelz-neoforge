package net.levelz;

import net.levelz.init.*;
import net.levelz.network.LevelServerPacket;
import net.levelz.port.ServerSmoke;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(LevelzMain.MOD_ID)
public class LevelzMain {

    public static final String MOD_ID = "levelz";

    private static boolean registered = false;

    public LevelzMain(IEventBus modBus) {
        // ConfigInit first: everything below reads ConfigInit.CONFIG (on Fabric it is the third call, but CommandInit and
        // CompatInit only register callbacks there, so the order of those two does not matter).
        ConfigInit.init();
        CommandInit.init(NeoForge.EVENT_BUS);
        CompatInit.init(NeoForge.EVENT_BUS);
        EventInit.init(NeoForge.EVENT_BUS);
        LoaderInit.init(NeoForge.EVENT_BUS);
        LevelServerPacket.init(modBus);
        TagInit.init();

        // Port self-check for the dedicated server (runServerSmoke), off unless -Dlevelz.port.serverSmoke=true.
        ServerSmoke.register(NeoForge.EVENT_BUS);

        // Vanilla registries are unfrozen for the whole RegisterEvent phase. The *Init classes register their content
        // from static fields (Fabric style), so they must not be class-loaded before this phase.
        modBus.addListener(RegisterEvent.class, LevelzMain::onRegister);
        // Method reference only: calling a static member of ItemInit here would class-load and register it too early.
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, ItemInit::registerTabContents);
    }

    private static void onRegister(RegisterEvent event) {
        if (registered) {
            return;
        }
        registered = true;
        // Same order as the original onInitialize()
        CriteriaInit.init();
        EntityInit.init();
        ItemInit.init();
    }

    public static ResourceLocation identifierOf(String name) {
        return ResourceLocation.fromNamespaceAndPath("levelz", name);
    }
}

// You are LOVED!!!
// Jesus loves you unconditional!
