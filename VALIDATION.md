# Validation — Mine Helper 0.2.0

23 October 2026 · Windows · JDK 21 · Minecraft 1.21.1 · NeoForge 21.1.262.

## Results

- Gradle build: passed.
- **37 automatic tests**: passed, no failures or errors.
- Real Minecraft launch with a new, isolated vanilla flat world and only Mine Helper: passed.
- Settings screenshot inspected: the language selector displays `FR · Français`; labels and controls are readable.

## Automatic coverage

- 6 text-selection tests: whole word/context, drag both ways, topmost layer, changing live frames, multi-line order, Unicode.
- 6 HTTP/payload tests: local Responses API server, missing key, parsing/refusal/incomplete responses, bounded history/input, safe error handling. The HTTP test explicitly inspects the configured French language in the transmitted instructions.
- 2 cache tests: separate model/context/language keys and persistence; corrupted-cache handling.
- 18 prompt tests: all 16 explicit response languages, language-neutral built-in actions, and rejection of unresolved Auto before sending a prompt.
- 4 language/config tests: Minecraft locales and fallback, explicit override, invalid/untrusted values, old-config migration, and stable unique codes.
- 1 localization-resource test: matching English/Russian keys and formatting parameters.

## In-game integration

The test JAR included a temporary probe that is **not included in the release JAR**. It used real game keyboard/mouse handlers and real text rendering.

Verified:

- Text and input mixins load successfully; styled and scaled GUI text is captured.
- Drag selects exactly `running out of`; clicks do not reach the source screen.
- The source screen is preserved and world time continues increasing with `Minecraft.isPaused()` false.
- Missing key produces a localized settings hint; F9 opens a non-pausing settings screen and returns to the source screen.
- A pinned tooltip remains selectable after the original disappears.
- Russian follow-up input is accepted, Enter submits, and helper input does not leak to the source screen.
- All 16 explicit languages plus Auto can be cycled through.
- English interface labels resolve to translated text rather than localization keys.
- FR can be saved and read back from the configuration; reopening settings keeps FR.
- Back leaves an unsaved candidate language unapplied.
- Controls fit a 320×240 GUI; resizing preserves the chosen candidate language.
- Saving a language change clears existing discussion history, cancels a simulated pending old-language response, clears its busy state, and removes the old answer without sending a replacement request.

## Not verified

No live request was made to OpenAI because no API key was provided. Tests exercise the HTTP protocol and generated instructions through a local mock server, not response quality across all languages. Any explanation shown in the preview image is explicitly marked demonstration data.

The user's complete IC2 pack, shaders, resource packs, 3D signs, image text, and arbitrary custom renderers are not covered by this test. User world saves were not used or changed.
