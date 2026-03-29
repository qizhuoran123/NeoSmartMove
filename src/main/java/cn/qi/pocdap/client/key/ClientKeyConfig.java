package cn.qi.pocdap.client.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class ClientKeyConfig {
    public static final KeyMapping GRAB_KEY = new KeyMapping(
            "key.neosmartmoving.grab",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            KeyMapping.CATEGORY_GAMEPLAY
    );
    public static final KeyMapping VAULT_KEY = new KeyMapping(
            "key.neosmartmoving.vault",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            KeyMapping.CATEGORY_GAMEPLAY
    );
}
