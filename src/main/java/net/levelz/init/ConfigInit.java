package net.levelz.init;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.levelz.config.LevelzConfig;
import net.neoforged.fml.ModList;

public class ConfigInit {

    public static final boolean isOriginsLoaded = ModList.get().isLoaded("origins");

    public static LevelzConfig CONFIG = new LevelzConfig();

    public static void init() {
        AutoConfig.register(LevelzConfig.class, JanksonConfigSerializer::new);
        CONFIG = AutoConfig.getConfigHolder(LevelzConfig.class).getConfig();
    }

}
