package net.levelz;

import me.shedaniel.autoconfig.AutoConfig;
import net.levelz.config.LevelzConfig;
import net.levelz.init.KeyInit;
import net.levelz.init.RenderInit;
import net.levelz.network.LevelClientPacket;
import net.levelz.port.ClientSmoke;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = LevelzMain.MOD_ID, dist = Dist.CLIENT)
public class LevelzClient {

    public LevelzClient(IEventBus modBus, ModContainer container) {
        // ModMenu integration of the original -> NeoForge config screen extension point.
        container.registerExtensionPoint(IConfigScreenFactory.class, (mc, parent) -> AutoConfig.getConfigScreen(LevelzConfig.class, parent).get());

        // Same order as the original onInitializeClient(). LevelClientPacket.init() of the original registered the
        // client-bound receivers; on NeoForge they are registered in LevelServerPacket's payload registrar.
        KeyInit.init(modBus, NeoForge.EVENT_BUS);
        RenderInit.init(modBus, NeoForge.EVENT_BUS);

        // Port self-checks: title screen marker, original-assets pack and notice, optional exit (runClientSmoke).
        ClientSmoke.init(modBus, NeoForge.EVENT_BUS);
    }

}
