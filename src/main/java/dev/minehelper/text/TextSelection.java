package dev.minehelper.text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Screen-space text geometry, independent of Minecraft and OpenAI. */
public final class TextSelection {
    private TextSelection() {}
    public record Glyph(int start, int end, float left, float right) {}
    public record Run(String text, List<Glyph> glyphs, float top, float bottom) {
        public Run { glyphs = List.copyOf(glyphs); }
        public float left() { return glyphs.isEmpty() ? 0 : glyphs.getFirst().left(); }
        public float right() { return glyphs.isEmpty() ? 0 : glyphs.getLast().right(); }
        public boolean contains(double x, double y) {
            return x >= left() && x <= right() && y >= top - 1 && y <= bottom + 1;
        }
        public int glyphAt(double x) {
            for (int i = 0; i < glyphs.size(); i++) if (x < glyphs.get(i).right()) return i;
            return glyphs.size() - 1;
        }
    }
    public record Box(float left, float top, float right, float bottom) {}
    public record Selection(String text, String context, List<Box> boxes) {
        public Selection { boxes = List.copyOf(boxes); }
    }

    public static Run hit(List<Run> runs, double x, double y) {
        // Last drawn text is the visible top layer (e.g. a tooltip over a menu).
        for (int i = runs.size() - 1; i >= 0; i--) if (runs.get(i).contains(x, y)) return runs.get(i);
        return null;
    }
    public static Selection word(List<Run> runs, Run run, double x) {
        if (run == null || run.glyphs.isEmpty()) return null;
        int index = run.glyphAt(x);
        int first = index, last = index;
        if (isWord(run, index)) {
            while (first > 0 && isWord(run, first - 1)) first--;
            while (last + 1 < run.glyphs.size() && isWord(run, last + 1)) last++;
        }
        return fragment(runs, run, first, last);
    }
    private static boolean isWord(Run run, int index) {
        int cp = run.text.codePointAt(run.glyphs.get(index).start);
        return Character.isLetterOrDigit(cp) || cp == '\'' || cp == '’' || cp == '-';
    }
    public static Selection drag(List<Run> runs, Run anchor, double x1, double y1, double x2, double y2) {
        if (anchor == null || anchor.glyphs.isEmpty()) return null;
        if (Math.hypot(x2 - x1, y2 - y1) < 3) return word(runs, anchor, x1);
        if (y2 >= anchor.top - 3 && y2 <= anchor.bottom + 3) {
            int a = anchor.glyphAt(x1), b = anchor.glyphAt(x2);
            return fragment(runs, anchor, Math.min(a, b), Math.max(a, b));
        }
        float top = (float)Math.min(y1, y2), bottom = (float)Math.max(y1, y2);
        float left = Math.max(0, anchor.left() - 20), right = anchor.right() + 20;
        List<Run> lines = runs.stream().filter(r -> r.bottom >= top && r.top <= bottom
                && r.left() < right && r.right() > left).sorted(Comparator.comparingDouble(Run::top)
                .thenComparingDouble(Run::left)).toList();
        StringBuilder selected = new StringBuilder();
        List<Box> boxes = new ArrayList<>();
        for (Run r : lines) {
            double startX = y1 < y2 ? x1 : x2, endX = y1 < y2 ? x2 : x1;
            int first = r.top <= top && r.bottom >= top ? r.glyphAt(startX) : 0;
            int last = r.top <= bottom && r.bottom >= bottom ? r.glyphAt(endX) : r.glyphs.size() - 1;
            if (last < first || r.glyphs.isEmpty()) continue;
            if (!selected.isEmpty()) selected.append('\n');
            selected.append(r.text, r.glyphs.get(first).start, r.glyphs.get(last).end);
            boxes.add(new Box(r.glyphs.get(first).left, r.top, r.glyphs.get(last).right, r.bottom));
        }
        return new Selection(limit(selected.toString().strip(), 1200), context(runs, anchor), boxes);
    }
    private static Selection fragment(List<Run> runs, Run r, int first, int last) {
        Glyph a = r.glyphs.get(first), b = r.glyphs.get(last);
        return new Selection(r.text.substring(a.start, b.end).strip(), context(runs, r),
                List.of(new Box(a.left, r.top, b.right, r.bottom)));
    }
    private static String context(List<Run> runs, Run anchor) {
        float height = Math.max(9, anchor.bottom - anchor.top);
        String joined = runs.stream().filter(r -> Math.abs(r.top - anchor.top) < height * 3
                && Math.abs(r.left() - anchor.left()) < 45)
                .sorted(Comparator.comparingDouble(Run::top).thenComparingDouble(Run::left))
                .map(Run::text).distinct().reduce((a, b) -> a + "\n" + b).orElse(anchor.text);
        return limit(joined, 1600);
    }
    public static String limit(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        int end = Character.isHighSurrogate(text.charAt(max - 1)) ? max - 1 : max;
        return text.substring(0, end);
    }
}
