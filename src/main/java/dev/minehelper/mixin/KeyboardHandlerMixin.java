package dev.minehelper.mixin;

import dev.minehelper.MineHelper;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void minehelper$key(long window, int key, int scan, int action, int mods, CallbackInfo ci) {
        if (MineHelper.instance != null && MineHelper.instance.overlay.key(window, key, scan, action, mods)) ci.cancel();
    }
    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void minehelper$char(long window, int codepoint, int mods, CallbackInfo ci) {
        if (MineHelper.instance != null && MineHelper.instance.overlay.character(window, codepoint)) ci.cancel();
    }
}
