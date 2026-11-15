package net.levelz.init;

import net.levelz.compat.TreeChopCompat;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

public class CompatInit {

    public static void init(IEventBus gameBus) {
        // Placeholder API (eu.pb4) has no NeoForge 1.21.1 build; the original branch is kept in _excluded/ for reference.
        if (ModList.get().isLoaded("treechop")) {
            TreeChopCompat.register(gameBus);
        }
    }
}
