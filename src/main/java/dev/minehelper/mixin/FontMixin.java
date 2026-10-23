package dev.minehelper.mixin;

import dev.minehelper.text.TextCapture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Font.class)
public abstract class FontMixin {
    @Inject(method = "drawInBatch(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;IIZ)I", at = @At("HEAD"))
    private void minehelper$string(String text, float x, float y, int color, boolean shadow, Matrix4f pose,
                                   MultiBufferSource buffers, Font.DisplayMode mode, int bg, int light, boolean rtl,
                                   CallbackInfoReturnable<Integer> ci) {
        TextCapture.string((Font)(Object)this, text, x, y, pose);
    }
    @Inject(method = "drawInBatch(Lnet/minecraft/util/FormattedCharSequence;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/gui/Font$DisplayMode;II)I", at = @At("HEAD"))
    private void minehelper$ordered(FormattedCharSequence text, float x, float y, int color, boolean shadow, Matrix4f pose,
                                    MultiBufferSource buffers, Font.DisplayMode mode, int bg, int light,
                                    CallbackInfoReturnable<Integer> ci) {
        TextCapture.capture((Font)(Object)this, text, x, y, pose);
    }
}
