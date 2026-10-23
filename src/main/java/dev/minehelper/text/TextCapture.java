package dev.minehelper.text;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class TextCapture {
    private static final List<TextSelection.Run> RUNS = new ArrayList<>();
    private static final List<TooltipLine> TOOLTIP = new ArrayList<>();
    private static int tooltipDepth;
    private static ScreenRectangle clip;
    public record TooltipLine(TextSelection.Run run, Font font, FormattedCharSequence text, float scaleX, float scaleY) {}
    private static boolean guiPhase;
    private static int suppressed;
    private TextCapture() {}
    public static void beginFrame() { RUNS.clear(); TOOLTIP.clear(); tooltipDepth = 0; clip = null; guiPhase = false; }
    public static void beginGui() { guiPhase = true; }
    public static void endFrame() { guiPhase = false; }
    public static void suppress(Runnable action) {
        suppressed++;
        try { action.run(); } finally { suppressed--; }
    }
    public static List<TextSelection.Run> snapshot() { return List.copyOf(RUNS); }
    public static List<TooltipLine> tooltip() { return List.copyOf(TOOLTIP); }
    public static void beginTooltip() { tooltipDepth++; }
    public static void endTooltip() { tooltipDepth = Math.max(0, tooltipDepth - 1); }
    public static void setClip(ScreenRectangle rectangle) { clip = rectangle; }
    public static void string(Font font, String text, float x, float y, Matrix4f pose) {
        if (text == null || !active()) return;
        capture(font, sink -> StringDecomposer.iterateFormatted(text, Style.EMPTY, sink), x, y, pose);
    }
    public static void capture(Font font, FormattedCharSequence sequence, float x, float y, Matrix4f pose) {
        if (!active() || sequence == null) return;
        // World-space labels / rotated custom text do not have a reliable 2D hit box.
        if (Math.abs(pose.m01()) > .001 || Math.abs(pose.m10()) > .001
                || pose.m00() <= 0 || pose.m11() <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getGuiScaledWidth(), height = mc.getWindow().getGuiScaledHeight();
        Vector3f origin = pose.transformPosition(x, y, 0, new Vector3f());
        float top = origin.y, bottom = top + font.lineHeight * pose.m11();
        if (bottom < 0 || top > height || origin.x > width || bottom - top < 2) return;
        if (clip != null) {
            top = Math.max(top, clip.top()); bottom = Math.min(bottom, clip.bottom());
            if (bottom <= top) return;
        }
        StringBuilder text = new StringBuilder();
        List<TextSelection.Glyph> glyphs = new ArrayList<>();
        float[] cursor = { origin.x };
        sequence.accept((index, style, cp) -> {
            if (text.length() >= 2048) return false;
            String symbol = new String(Character.toChars(cp));
            float advance = font.getSplitter().stringWidth(FormattedText.of(symbol, style)) * pose.m00();
            int start = text.length();
            text.append(symbol);
            if (clip == null || (cursor[0] + advance > clip.left() && cursor[0] < clip.right()))
                glyphs.add(new TextSelection.Glyph(start, text.length(),
                        clip == null ? cursor[0] : Math.max(cursor[0], clip.left()),
                        clip == null ? cursor[0] + advance : Math.min(cursor[0] + advance, clip.right())));
            cursor[0] += advance;
            return true;
        });
        if (text.toString().isBlank() || cursor[0] < 0 || glyphs.isEmpty()) return;
        var run = new TextSelection.Run(text.toString(), glyphs, top, bottom);
        RUNS.add(run);
        if (tooltipDepth > 0) TOOLTIP.add(new TooltipLine(run, font, sequence, pose.m00(), pose.m11()));
    }
    private static boolean active() { return guiPhase && suppressed == 0 && RUNS.size() < 1800; }
}
