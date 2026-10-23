package dev.minehelper.mixin;

import dev.minehelper.text.TextCapture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import net.minecraft.client.gui.navigation.ScreenRectangle;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(method = "applyScissor", at = @At("HEAD"))
    private void minehelper$clip(ScreenRectangle rectangle, CallbackInfo ci) { TextCapture.setClip(rectangle); }
    @Inject(method = "renderTooltipInternal", at = @At("HEAD"))
    private void minehelper$tooltipStart(Font font, List<ClientTooltipComponent> parts, int x, int y,
                                         ClientTooltipPositioner positioner, CallbackInfo ci) { TextCapture.beginTooltip(); }
    @Inject(method = "renderTooltipInternal", at = @At("RETURN"))
    private void minehelper$tooltipEnd(Font font, List<ClientTooltipComponent> parts, int x, int y,
                                       ClientTooltipPositioner positioner, CallbackInfo ci) { TextCapture.endTooltip(); }
}
