package net.levelz.port;

import net.levelz.port.assets.AssetNotice;
import net.levelz.port.assets.OriginalAssetsPackFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

/**
 * Client-side self-checks of the port, all off unless their system property is set:
 * <ul>
 * <li>{@code -Dlevelz.port.exitOnTitle=true} (run configuration {@code clientSmoke}): logs {@code TITLE_SCREEN_REACHED}
 * once the title screen opens and stops the game afterwards, so the run exits with code 0;</li>
 * <li>the notice about the All Rights Reserved assets of the original is always active.</li>
 * </ul>
 */
public final class ClientSmoke {

    public static final String EXIT_ON_TITLE = "levelz.port.exitOnTitle";
    private static final Logger LOGGER = LoggerFactory.getLogger("levelz");
    private static boolean titleReached = false;

    private ClientSmoke() {
    }

    public static void init(IEventBus modBus, IEventBus gameBus) {
        // Original jar in resourcepacks/ -> built-in client resource pack. The pack type is checked inside.
        modBus.addListener(AddPackFindersEvent.class, OriginalAssetsPackFinder::onAddPackFinders);

        gameBus.addListener(ScreenEvent.Opening.class, AssetNotice::onScreenOpening);
        gameBus.addListener(ScreenEvent.Opening.class, ClientSmoke::onScreenOpening);
    }

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (titleReached || !(event.getNewScreen() instanceof TitleScreen)) {
            return;
        }
        titleReached = true;
        LOGGER.info("[levelz-port] TITLE_SCREEN_REACHED");
        if (Boolean.getBoolean(EXIT_ON_TITLE)) {
            // Load every mixin target first: audit() only sees classes that are already loaded.
            int targetFailures = MixinTargetLoader.load(LOGGER, MixinTargetLoader.COMMON) + MixinTargetLoader.load(LOGGER, MixinTargetLoader.CLIENT);
            LOGGER.info("[levelz-port] CLIENT_MIXIN_TARGETS targets={} failures={}", MixinTargetLoader.COMMON.size() + MixinTargetLoader.CLIENT.size(), targetFailures);
            // Force every client mixin of the pack to apply now, so a broken target fails here instead of in game.
            MixinEnvironment.getCurrentEnvironment().audit();
            LOGGER.info("[levelz-port] CLIENT_MIXIN_AUDIT_OK");
            Thread exitThread = new Thread(() -> {
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ignored) {
                }
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().stop());
            }, "levelz-port-smoke-exit");
            exitThread.setDaemon(true);
            exitThread.start();
        }
    }
}
