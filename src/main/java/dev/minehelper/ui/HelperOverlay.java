package dev.minehelper.ui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.minehelper.HelperSession;
import dev.minehelper.MineHelper;
import dev.minehelper.gpt.LearningPrompt;
import static dev.minehelper.ui.UiText.text;
import dev.minehelper.text.TextCapture;
import dev.minehelper.text.TextSelection;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** An overlay, not a Screen: selecting text never replaces the player's current screen. */
public final class HelperOverlay {
    private final MineHelper mod;
    private final Minecraft mc = Minecraft.getInstance();
    private boolean holding, typing, dragging, restoreGrab;
    private int heldKey = -1;
    private double startX, startY;
    private List<TextSelection.Run> dragRuns = List.of();
    private List<TextCapture.TooltipLine> pinnedTooltip = List.of();
    private TextSelection.Run anchor;
    private TextSelection.Selection preview;
    private String question = "";
    private final List<Action> actions = new ArrayList<>();
    private int panelLeft, panelTop, panelRight, panelBottom, maxScroll;
    private record Action(int x1, int y1, int x2, int y2, Runnable callback) {
        boolean contains(double x, double y) { return x >= x1 && x <= x2 && y >= y1 && y <= y2; }
    }
    public HelperOverlay(MineHelper mod) { this.mod = mod; }
    private boolean window(long handle) { return handle == mc.getWindow().getWindow(); }
    public double mouseX() { return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth(); }
    public double mouseY() { return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight(); }
    public boolean active() { return holding || typing; }

    public boolean key(long handle, int key, int scan, int action, int mods) {
        if (!window(handle) || mc.screen instanceof SettingsScreen || mc.screen instanceof KeyBindsScreen) return false;
        if (mod.selectKey.matches(key, scan)) {
            if (action == GLFW.GLFW_PRESS && mc.level != null && mc.getOverlay() == null) {
                heldKey = key;
                pinnedTooltip = TextCapture.tooltip();
                if (!active()) {
                    restoreGrab = mc.mouseHandler.isMouseGrabbed();
                    mc.mouseHandler.releaseMouse();
                }
                holding = true;
                return true;
            }
            if (action == GLFW.GLFW_RELEASE && holding) {
                releaseSelectionMode();
                return true;
            }
            if (holding) return true;
        }
        if (typing) {
            if (action == GLFW.GLFW_RELEASE) return true;
            if (key == GLFW.GLFW_KEY_ESCAPE) { stopTyping(); return true; }
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                String submitted = question.strip();
                if (!submitted.isBlank()) { mod.session.ask(submitted, false); question = ""; }
                stopTyping(); return true;
            }
            if (key == GLFW.GLFW_KEY_BACKSPACE && !question.isEmpty()) {
                question = question.substring(0, question.offsetByCodePoints(question.length(), -1));
            }
            if (key == GLFW.GLFW_KEY_V && (mods & GLFW.GLFW_MOD_CONTROL) != 0)
                question = TextSelection.limit(question + mc.keyboardHandler.getClipboard().replaceAll("[\\p{Cntrl}]", " "), 1000);
            return true;
        }
        if (action == GLFW.GLFW_PRESS && mc.level != null) {
            if (mod.settingsKey.matches(key, scan)) {
                reset(); mc.setScreen(new SettingsScreen(mc.screen, mod.config)); return true;
            }
            if (mod.panelKey.matches(key, scan)) { mod.config.showPanel = !mod.config.showPanel; return true; }
            if (holding && key == GLFW.GLFW_KEY_ESCAPE) { reset(); return true; }
        }
        return false;
    }
    public boolean character(long handle, int cp) {
        if (!window(handle) || !typing) return false;
        if (Character.isValidCodePoint(cp) && !Character.isISOControl(cp) && question.length() < 999)
            question += new String(Character.toChars(cp));
        return true;
    }
    private void releaseSelectionMode() {
        if (dragging) finishDrag();
        holding = false; heldKey = -1; preview = null; pinnedTooltip = List.of();
        restoreMouse();
    }
    private void restoreMouse() {
        if (!active() && restoreGrab && mc.screen == null && mc.isWindowActive()) mc.mouseHandler.grabMouse();
        if (!active()) restoreGrab = false;
    }
    private void startTyping() {
        typing = true;
        KeyMapping.releaseAll();
    }
    private void stopTyping() { typing = false; restoreMouse(); }
    public void reset() {
        dragging = false; preview = null; anchor = null; dragRuns = List.of(); pinnedTooltip = List.of();
        holding = false; typing = false; heldKey = -1; question = ""; restoreMouse();
    }
    public void tick() {
        if (mc.level == null || !mc.isWindowActive() || mc.getOverlay() != null) {
            if (active()) reset();
            return;
        }
        // Recover when the OS swallows Alt release (e.g. Alt+Tab).
        if (holding && heldKey >= 0 && !InputConstants.isKeyDown(mc.getWindow().getWindow(), heldKey)) releaseSelectionMode();
    }
    public boolean click(long handle, int button, int action) {
        if (!window(handle) || !active() || mc.screen instanceof SettingsScreen) return false;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        double x = mouseX(), y = mouseY();
        if (action == GLFW.GLFW_PRESS) {
            if (mod.config.showPanel && mod.session.selection != null && overPanel(x, y)) {
                for (Action a : List.copyOf(actions)) if (a.contains(x, y)) { a.callback.run(); break; }
                return true;
            }
            if (typing) stopTyping();
            dragRuns = selectableRuns();
            anchor = TextSelection.hit(dragRuns, x, y);
            if (anchor != null) {
                startX = x; startY = y; dragging = true;
                preview = TextSelection.word(dragRuns, anchor, x);
            }
        } else if (action == GLFW.GLFW_RELEASE && dragging) finishDrag();
        return true;
    }
    private void finishDrag() {
        preview = TextSelection.drag(dragRuns, anchor, startX, startY, mouseX(), mouseY());
        dragging = false;
        mod.session.select(preview);
        dragRuns = List.of(); anchor = null;
    }
    public boolean scroll(long handle, double delta) {
        if (!window(handle) || !active()) return false;
        if (overPanel(mouseX(), mouseY())) mod.session.scroll = Math.clamp(mod.session.scroll - (int)(delta * 3), 0, maxScroll);
        // Never let an overlay scroll change the player's selected hotbar slot.
        return true;
    }
    private boolean overPanel(double x, double y) {
        return x >= panelLeft && x <= panelRight && y >= panelTop && y <= panelBottom;
    }
    private List<TextSelection.Run> selectableRuns() {
        List<TextSelection.Run> live = new ArrayList<>(TextCapture.snapshot());
        // Freeze only the tooltip being inspected, never the world or the rest of the HUD.
        live.addAll(pinnedTooltip.stream().map(TextCapture.TooltipLine::run).toList());
        return live;
    }
    public void render(GuiGraphics g) {
        if (mc.level == null || mc.screen instanceof SettingsScreen || mc.getOverlay() != null) return;
        TextCapture.suppress(() -> draw(g));
    }
    private void draw(GuiGraphics g) {
        int w = mc.getWindow().getGuiScaledWidth(), h = mc.getWindow().getGuiScaledHeight();
        Font font = mc.font;
        g.pose().pushPose();
        g.pose().translate(0, 0, 1000);
        if (holding && !pinnedTooltip.isEmpty()) {
            int left = (int)pinnedTooltip.stream().mapToDouble(t -> t.run().left()).min().orElse(0) - 4;
            int right = (int)Math.ceil(pinnedTooltip.stream().mapToDouble(t -> t.run().right()).max().orElse(0)) + 4;
            int top = (int)pinnedTooltip.stream().mapToDouble(t -> t.run().top()).min().orElse(0) - 4;
            int bottom = (int)Math.ceil(pinnedTooltip.stream().mapToDouble(t -> t.run().bottom()).max().orElse(0)) + 4;
            g.fill(left, top, right, bottom, 0xFF17241E);
            for (TextCapture.TooltipLine line : pinnedTooltip) {
                g.pose().pushPose();
                g.pose().translate(line.run().left(), line.run().top(), 0);
                g.pose().scale(line.scaleX(), line.scaleY(), 1);
                g.drawString(line.font(), line.text(), 0, 0, 0xFFFFFFFF, false);
                g.pose().popPose();
            }
        }
        if (dragging) preview = TextSelection.drag(dragRuns, anchor, startX, startY, mouseX(), mouseY());
        else if (holding && !(mod.config.showPanel && mod.session.selection != null && overPanel(mouseX(), mouseY()))) {
            List<TextSelection.Run> runs = selectableRuns();
            TextSelection.Run hover = TextSelection.hit(runs, mouseX(), mouseY());
            preview = TextSelection.word(runs, hover, mouseX());
        }
        if (active() && preview != null) for (TextSelection.Box box : preview.boxes()) {
            g.fill((int)box.left() - 1, (int)box.top() - 1, (int)Math.ceil(box.right()) + 1,
                    (int)Math.ceil(box.bottom()) + 1, 0x708DDC78);
        }
        actions.clear();
        if (mod.config.showPanel && mod.session.selection != null) drawPanel(g, font, w, h);
        if (active()) {
            String hint = typing ? text("hint.typing") : text("hint.select");
            int hintWidth = Math.min(w - 16, font.width(hint) + 14);
            g.fill(8, h - 49, 8 + hintWidth, h - 31, 0xED17241E);
            g.drawString(font, fit(font, hint, hintWidth - 12), 14, h - 44, 0xFFD8E6D9, false);
        }
        g.flush();
        g.pose().popPose();
    }
    private void drawPanel(GuiGraphics g, Font font, int w, int h) {
        int panelWidth = Math.min(226, Math.max(150, w / 2));
        panelRight = w - 8; panelLeft = Math.max(8, panelRight - panelWidth);
        panelTop = 18; panelBottom = Math.max(panelTop + 145, Math.min(h - 55, panelTop + 300));
        int x = panelLeft + 10, inner = panelRight - x - 10;
        g.fill(panelLeft, panelTop, panelRight, panelBottom, 0xF318241F);
        g.fill(panelLeft, panelTop, panelLeft + 2, panelBottom, 0xFFA9D78E);
        g.drawString(font, "MINE HELPER · " + mod.session.language().code().toUpperCase(java.util.Locale.ROOT), x, panelTop + 9, 0xFFB8D6B8, false);
        button(g, font, "×", panelRight - 23, panelTop + 4, 17, () -> { mod.config.showPanel = false; if (typing) stopTyping(); });
        g.drawString(font, fit(font, mod.session.selection.text(), inner), x, panelTop + 28, 0xFFD1F7AB, false);
        g.drawString(font, fit(font, mod.session.selection.context().replace('\n', ' '), inner), x, panelTop + 42, 0xFFA8B9AE, false);
        int half = (inner - 5) / 2;
        button(g, font, text("action.explain"), x, panelTop + 57, half, () -> mod.session.ask(HelperSession.EXPLAIN, true));
        button(g, font, text("action.translate"), x + half + 5, panelTop + 57, half, () -> mod.session.ask(HelperSession.TRANSLATE, true));
        int answerY = panelTop + 83, answerBottom = panelBottom - 69;
        List<FormattedCharSequence> lines = font.split(Component.literal(mod.session.answer), inner);
        int visible = Math.max(1, (answerBottom - answerY) / 11);
        maxScroll = Math.max(0, lines.size() - visible);
        mod.session.scroll = Math.clamp(mod.session.scroll, 0, maxScroll);
        g.enableScissor(x, answerY, panelRight - 7, Math.max(answerY + 10, answerBottom));
        for (int i = 0; i < visible && i + mod.session.scroll < lines.size(); i++)
            g.drawString(font, lines.get(i + mod.session.scroll), x, answerY + i * 11, 0xFFE8EFE8, false);
        g.disableScissor();
        if (maxScroll > 0) {
            int track = Math.max(10, answerBottom - answerY), thumb = Math.max(5, track * visible / lines.size());
            int y = answerY + (track - thumb) * mod.session.scroll / maxScroll;
            g.fill(panelRight - 5, y, panelRight - 3, y + thumb, 0xFF89A18D);
        }
        button(g, font, text("action.grammar"), x, panelBottom - 62, inner,
                () -> mod.session.ask(LearningPrompt.GRAMMAR, false));
        int inputY = panelBottom - 40;
        g.fill(x, inputY, panelRight - 10, inputY + 18, typing ? 0xFF324B3B : 0xFF101B16);
        String input = question.isEmpty() ? text("action.question") : tail(font, question, inner - 8);
        g.drawString(font, input + (typing && System.currentTimeMillis() / 500 % 2 == 0 ? "|" : ""),
                x + 4, inputY + 5, typing ? 0xFFF0F5EC : 0xFF9BAF9F, false);
        actions.add(new Action(x, inputY, panelRight - 10, inputY + 18, this::startTyping));
        String status = mod.session.busy ? text("status.answering") : mod.session.status;
        g.drawString(font, fit(font, status, inner), x, panelBottom - 13, 0xFFA7B9AB, false);
    }
    private void button(GuiGraphics g, Font font, String label, int x, int y, int width, Runnable callback) {
        boolean hover = active() && mouseX() >= x && mouseX() <= x + width && mouseY() >= y && mouseY() <= y + 18;
        g.fill(x, y, x + width, y + 18, hover ? 0xFF45604B : 0xFF2D4033);
        g.drawString(font, fit(font, label, width - 8), x + 4, y + 5, 0xFFE8F0E4, false);
        actions.add(new Action(x, y, x + width, y + 18, callback));
    }
    private static String fit(Font font, String s, int width) {
        return font.width(s) <= width ? s : font.plainSubstrByWidth(s, Math.max(0, width - font.width("…"))) + "…";
    }
    private static String tail(Font font, String s, int width) {
        return font.width(s) <= width ? s : "…" + font.plainSubstrByWidth(s, Math.max(0, width - font.width("…")), true);
    }
}
