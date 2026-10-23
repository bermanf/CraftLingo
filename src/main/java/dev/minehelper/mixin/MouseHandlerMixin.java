package dev.minehelper.mixin;

import dev.minehelper.MineHelper;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void minehelper$click(long window, int button, int action, int mods, CallbackInfo ci) {
        if (MineHelper.instance != null && MineHelper.instance.overlay.click(window, button, action)) ci.cancel();
    }
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void minehelper$scroll(long window, double x, double y, CallbackInfo ci) {
        if (MineHelper.instance != null && MineHelper.instance.overlay.scroll(window, y)) ci.cancel();
    }
}
