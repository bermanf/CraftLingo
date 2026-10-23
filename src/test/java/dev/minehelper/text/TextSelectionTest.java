package dev.minehelper.text;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class TextSelectionTest {
    private static TextSelection.Run line(String text, float x, float y) {
        List<TextSelection.Glyph> glyphs = new ArrayList<>();
        float cursor = x;
        for (int i = 0; i < text.length();) {
            int end = i + Character.charCount(text.codePointAt(i));
            glyphs.add(new TextSelection.Glyph(i, end, cursor, cursor + 6)); cursor += 6; i = end;
        }
        return new TextSelection.Run(text, glyphs, y, y + 9);
    }
    @Test void clickSelectsWholeWordAndKeepsSentence() {
        var run = line("Diamond Pickaxe", 100, 20);
        var selection = TextSelection.word(List.of(run), run, 158);
        assertEquals("Pickaxe", selection.text());
        assertEquals("Diamond Pickaxe", selection.context());
        assertEquals(148, selection.boxes().getFirst().left());
    }
    @Test void dragCanSelectAnExpressionInEitherDirection() {
        var run = line("You are running out of supplies.", 0, 0);
        var runs = List.of(run);
        assertEquals("running out of", TextSelection.drag(runs, run, 48, 3, 131, 3).text());
        assertEquals("running out of", TextSelection.drag(runs, run, 131, 3, 48, 3).text());
    }
    @Test void topmostTextWinsAndCoordinatesAreRespected() {
        var background = line("inventory", 10, 10);
        var tooltip = line("tooltip", 10, 10);
        assertSame(tooltip, TextSelection.hit(List.of(background, tooltip), 25, 15));
        assertNull(TextSelection.hit(List.of(tooltip), 500, 15));
    }
    @Test void selectionKeepsItsOwnTextWhenLiveFrameChanges() {
        var run = line("Old message", 0, 0);
        List<TextSelection.Run> live = new ArrayList<>(List.of(run));
        var snapshot = List.copyOf(live);
        live.clear(); live.add(line("New message", 0, 0));
        assertEquals("Old", TextSelection.word(snapshot, run, 3).text());
    }
    @Test void multilineSelectionUsesReadingOrder() {
        var a = line("First line", 10, 10);
        var b = line("Second line", 10, 22);
        assertEquals("First line\nSecond", TextSelection.drag(List.of(b, a), a, 10, 12, 45, 25).text());
    }
    @Test void unicodeCodepointsAreNeverCutInHalf() {
        var run = line("𐐀word", 0, 0);
        assertEquals("𐐀word", TextSelection.word(List.of(run), run, 3).text());
        assertEquals("a", TextSelection.limit("a😀z", 2));
    }
}
