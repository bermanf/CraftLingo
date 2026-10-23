package dev.minehelper.ui;

import net.minecraft.network.chat.Component;

public final class UiText {
    private UiText() {}
    public static Component component(String key, Object... arguments) {
        return Component.translatable("minehelper." + key, arguments);
    }
    public static String text(String key, Object... arguments) { return component(key, arguments).getString(); }
}
