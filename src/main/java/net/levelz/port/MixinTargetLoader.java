package net.levelz.port;

import java.util.List;
import org.slf4j.Logger;

/**
 * Loads every class targeted by a mixin of this mod, so that the mixin transformer has to apply the mixin for real.
 * {@code MixinEnvironment.audit()} only checks mixins of classes that are already loaded, which misses most of the
 * gameplay classes (blocks, items, containers) on a title-screen run.
 * <p>
 * Only used by the smoke self-checks ({@code -Dlevelz.port.serverSmoke=true} / {@code -Dlevelz.port.exitOnTitle=true}).
 */
public final class MixinTargetLoader {

    private MixinTargetLoader() {
    }

    public static int load(Logger logger, List<String> targets) {
        int failures = 0;
        ClassLoader loader = MixinTargetLoader.class.getClassLoader();
        for (String target : targets) {
            try {
                // initialize=false: load and transform the class, but do not run its static initializers.
                Class.forName(target, false, loader);
            } catch (Throwable throwable) {
                failures++;
                logger.error("[levelz-port] Could not load mixin target {}", target, throwable);
            }
        }
        return failures;
    }

    /** Targets of the mixins in the "mixins" section of levelz.mixins.json. */
    public static final List<String> COMMON = List.of(
            "net.levelz.level.LevelManager",
            "net.minecraft.world.level.block.state.BlockBehaviour",
            "net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity",
            "net.minecraft.world.level.block.Block",
            "net.minecraft.world.level.block.BushBlock",
            "net.minecraft.world.entity.LivingEntity",
            "net.minecraft.world.entity.animal.Animal",
            "net.minecraft.world.entity.boss.enderdragon.EnderDragon",
            "net.minecraft.world.entity.Entity",
            "net.minecraft.world.entity.projectile.ThrownExperienceBottle",
            "net.minecraft.world.entity.projectile.FishingHook",
            "net.minecraft.world.entity.Mob",
            "net.minecraft.world.entity.vehicle.AbstractMinecartContainer",
            "net.minecraft.world.entity.item.PrimedTnt",
            "net.minecraft.world.entity.vehicle.VehicleEntity",
            "net.minecraft.world.entity.npc.Villager",
            "net.minecraft.world.entity.npc.WanderingTrader",
            "net.minecraft.world.item.ArmorItem",
            "net.minecraft.world.item.BowItem",
            "net.minecraft.world.item.CrossbowItem",
            "net.minecraft.world.item.Equipable",
            "net.minecraft.world.item.ItemStack",
            "net.minecraft.world.item.DiggerItem",
            "net.minecraft.world.item.PotionItem",
            "net.minecraft.world.item.SwordItem",
            "net.minecraft.world.inventory.AnvilMenu",
            "net.minecraft.world.item.alchemy.PotionBrewing",
            "net.minecraft.world.inventory.BrewingStandMenu",
            "net.minecraft.world.entity.animal.Bucketable",
            "net.minecraft.world.item.enchantment.EnchantmentHelper",
            "net.minecraft.world.inventory.EnchantmentMenu",
            "net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity",
            "net.minecraft.world.level.BaseSpawner",
            "net.minecraft.world.entity.monster.piglin.PiglinAi",
            "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot",
            "net.minecraft.world.entity.npc.VillagerTrades$ItemsAndEmeraldsToItems",
            "net.minecraft.world.inventory.AbstractContainerMenu",
            "net.minecraft.world.inventory.SmithingMenu",
            "net.minecraft.world.entity.vehicle.ContainerEntity",
            "net.minecraft.network.protocol.game.GameProtocols",
            "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket",
            "net.minecraft.world.inventory.CraftingMenu",
            "net.minecraft.world.food.FoodData",
            "net.minecraft.world.entity.player.Player",
            "net.minecraft.server.players.PlayerList",
            "net.minecraft.server.level.ServerPlayer"
    );

    /** Targets of the mixins in the "client" section of levelz.mixins.json (client runs only). */
    public static final List<String> CLIENT = List.of(
            "net.minecraft.client.multiplayer.ClientPacketListener",
            "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen",
            "net.minecraft.client.renderer.LevelRenderer",
            "net.minecraft.network.protocol.game.ClientGamePacketListener",
            "net.minecraft.client.player.AbstractClientPlayer",
            "net.minecraft.client.multiplayer.PlayerInfo",
            "net.minecraft.client.gui.Gui",
            "net.minecraft.client.gui.screens.inventory.InventoryScreen",
            "net.minecraft.client.Minecraft",
            "net.minecraft.client.renderer.entity.player.PlayerRenderer",
            "net.minecraft.client.gui.components.PlayerTabOverlay"
    );
}
