package net.levelz.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.levelz.screen.LevelScreen;
import net.minecraft.client.KeyMapping;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import java.io.FileWriter;
import java.io.IOException;

public class KeyInit {
    public static KeyMapping screenKey = new KeyMapping("key.levelz.openskillscreen", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.levelz.keybind");
    public static KeyMapping devKey = new KeyMapping("key.levelz.dev", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_F8, "category.levelz.keybind");

    public static void init(IEventBus modBus, IEventBus gameBus) {
        // Registering
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> {
            // Only the skill screen key is registered, exactly like the original (devKey is used through matches()).
            event.register(screenKey);
        });
        // Callback
        gameBus.addListener(ClientTickEvent.Post.class, client -> {
            if (screenKey.consumeClick()) {
                net.minecraft.client.Minecraft.getInstance().setScreen(new LevelScreen());
            }
        });
    }

    public static void writeId(String string) {
        try (FileWriter idFile = new FileWriter("idlist.json", true)) {
            idFile.append("\"" + string + "\",");
            idFile.append(System.lineSeparator());
        } catch (IOException ignored) {
        }
    }

}
