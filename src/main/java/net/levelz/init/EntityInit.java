package net.levelz.init;

import net.levelz.LevelzMain;
import net.levelz.entity.LevelExperienceOrbEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.fml.ModList;

public class EntityInit {

    public static final boolean isRedstoneBitsLoaded = ModList.get().isLoaded("redstonebits");

    public static final EntityType<LevelExperienceOrbEntity> LEVEL_EXPERIENCE_ORB = EntityType.Builder.<LevelExperienceOrbEntity>of(LevelExperienceOrbEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F).build("level_experience_orb");

    public static void init() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, LevelzMain.identifierOf("level_experience_orb"), LEVEL_EXPERIENCE_ORB);
    }

}
