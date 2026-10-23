package dev.minehelper;

import com.mojang.blaze3d.platform.InputConstants;
import dev.minehelper.text.TextCapture;
import dev.minehelper.ui.HelperOverlay;
import dev.minehelper.ui.SettingsScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

@Mod(value = MineHelper.ID, dist = Dist.CLIENT)
public final class MineHelper {
    public static final String ID = "minehelper";
    public static MineHelper instance;
    public final HelperConfig config;
    public final HelperSession session;
    public final HelperOverlay overlay;
    public final KeyMapping selectKey = new KeyMapping("key.minehelper.select", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT, "key.categories.minehelper");
    public final KeyMapping settingsKey = new KeyMapping("key.minehelper.settings", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F9, "key.categories.minehelper");
    public final KeyMapping panelKey = new KeyMapping("key.minehelper.panel", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8, "key.categories.minehelper");

    public MineHelper(IEventBus modBus, ModContainer container) {
        instance = this;
        config = HelperConfig.load();
        session = new HelperSession(config);
        overlay = new HelperOverlay(this);
        modBus.addListener(this::registerKeys);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (mod, parent) -> new SettingsScreen(parent, config));
        NeoForge.EVENT_BUS.addListener(this::frameStart);
        NeoForge.EVENT_BUS.addListener(this::frameEnd);
        NeoForge.EVENT_BUS.addListener(this::guiStart);
        NeoForge.EVENT_BUS.addListener(this::guiEnd);
        NeoForge.EVENT_BUS.addListener(this::screenStart);
        NeoForge.EVENT_BUS.addListener(this::screenEnd);
        NeoForge.EVENT_BUS.addListener(this::tick);
        NeoForge.EVENT_BUS.addListener(this::logout);
    }
    private void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(selectKey); event.register(settingsKey); event.register(panelKey);
    }
    private void frameStart(RenderFrameEvent.Pre event) { TextCapture.beginFrame(); }
    private void frameEnd(RenderFrameEvent.Post event) { TextCapture.endFrame(); }
    private void guiStart(RenderGuiEvent.Pre event) { TextCapture.beginGui(); }
    private void guiEnd(RenderGuiEvent.Post event) {
        if (Minecraft.getInstance().screen == null) overlay.render(event.getGuiGraphics());
    }
    private void screenStart(ScreenEvent.Render.Pre event) { TextCapture.beginGui(); }
    private void screenEnd(ScreenEvent.Render.Post event) {
        if (Minecraft.getInstance().screen == event.getScreen()) overlay.render(event.getGuiGraphics());
    }
    private void tick(ClientTickEvent.Post event) { overlay.tick(); }
    private void logout(ClientPlayerNetworkEvent.LoggingOut event) { overlay.reset(); session.clear(); }
}
