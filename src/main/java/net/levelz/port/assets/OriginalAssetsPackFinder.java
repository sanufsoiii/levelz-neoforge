package net.levelz.port.assets;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources.SharedZipFileAccess;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * Registers the player's original LevelZ jar as an always-on, top-most built-in resource pack (client only).
 */
public final class OriginalAssetsPackFinder {

    public static final String PACK_ID = "levelz:original_assets";

    private OriginalAssetsPackFinder() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        // Scanned inside loadPacks so that PackRepository.reload() picks up a jar added at runtime.
        event.addRepositorySource(consumer -> {
            Optional<Path> jar = OriginalAssetsLocator.find();
            if (jar.isEmpty()) {
                return;
            }
            Path file = jar.get();
            PackLocationInfo info = new PackLocationInfo(PACK_ID, Component.translatable("levelz.port.assets.pack_name"),
                    PackSource.BUILT_IN, Optional.empty());
            Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
                @Override
                public PackResources openPrimary(PackLocationInfo location) {
                    return new OriginalAssetsPackResources(location, new SharedZipFileAccess(file.toFile()));
                }

                @Override
                public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                    return openPrimary(location);
                }
            };
            Pack.Metadata metadata = new Pack.Metadata(Component.translatable("levelz.port.assets.pack_description"),
                    PackCompatibility.COMPATIBLE, FeatureFlagSet.of(), List.of());
            consumer.accept(new Pack(info, supplier, metadata, new PackSelectionConfig(true, Pack.Position.TOP, true)));
        });
    }
}
