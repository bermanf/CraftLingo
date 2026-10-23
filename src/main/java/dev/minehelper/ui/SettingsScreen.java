package dev.minehelper.ui;

import dev.minehelper.HelperConfig;
import dev.minehelper.MineHelper;
import dev.minehelper.language.ExplanationLanguage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import java.io.IOException;
import static dev.minehelper.ui.UiText.component;

public final class SettingsScreen extends Screen {
    private final Screen parent;
    private final HelperConfig config;
    private EditBox key, model;
    private Component status = Component.empty();
    private boolean cache;
    private ExplanationLanguage explanationLanguage;
    public SettingsScreen(Screen parent, HelperConfig config) {
        super(component("settings.title"));
        this.parent = parent; this.config = config; cache = config.cacheEnabled;
        explanationLanguage = ExplanationLanguage.fromCode(config.explanationLanguage);
    }
    private int top() { return Math.max(4, (height - 236) / 2); }
    @Override protected void init() {
        int width = Math.min(330, this.width - 32), x = (this.width - width) / 2, y = top();
        String currentKey = key == null ? "" : key.getValue();
        String currentModel = model == null ? config.model : model.getValue();
        key = new EditBox(font, x, y + 34, width, 20, component("settings.api_key"));
        key.setMaxLength(512); key.setValue(currentKey);
        key.setHint(component(config.apiKey().isBlank() ? "settings.key_hint" : "settings.key_configured"));
        key.setFormatter((text, cursor) -> FormattedCharSequence.forward("•".repeat(text.length()), net.minecraft.network.chat.Style.EMPTY));
        addRenderableWidget(key);
        model = new EditBox(font, x, y + 72, width, 20, component("settings.model"));
        model.setMaxLength(120); model.setValue(currentModel); addRenderableWidget(model);
        CycleButton<ExplanationLanguage> languages = CycleButton.<ExplanationLanguage>builder(language ->
                language == ExplanationLanguage.AUTO ? component("settings.language_auto") : Component.literal(language.label()))
                .withValues(ExplanationLanguage.values()).withInitialValue(explanationLanguage).displayOnlyValue()
                .create(x, y + 110, width, 20, component("settings.language"), (button, language) -> explanationLanguage = language);
        languages.setTooltip(Tooltip.create(component("settings.language_tooltip")));
        addRenderableWidget(languages);
        addRenderableWidget(Button.builder(component(cache ? "settings.cache_on" : "settings.cache_off"), b -> {
            cache = !cache; b.setMessage(component(cache ? "settings.cache_on" : "settings.cache_off"));
        }).bounds(x, y + 138, width, 20).build());
        addRenderableWidget(Button.builder(component("settings.save"), b -> {
            if (!model.getValue().matches("[A-Za-z0-9._:-]{1,120}")) { status = component("settings.invalid_model"); return; }
            try {
                if (!key.getValue().isBlank()) config.saveKey(key.getValue());
                config.model = model.getValue(); config.cacheEnabled = cache;
                config.explanationLanguage = explanationLanguage.code(); config.save();
                if (MineHelper.instance != null) MineHelper.instance.session.preferencesChanged();
                onClose();
            } catch (IOException e) { status = component("settings.save_failed"); }
        }).bounds(x, y + 166, (width - 6) / 2, 20).build());
        addRenderableWidget(Button.builder(component("settings.back"), b -> onClose())
                .bounds(x + (width + 6) / 2, y + 166, (width - 6) / 2, 20).build());
    }
    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int width = Math.min(330, this.width - 32), x = (this.width - width) / 2, y = top();
        if (minecraft.level == null) g.fill(0, 0, this.width, height, 0xFF101A15);
        g.fill(x - 12, y - 4, x + width + 12, y + 232, 0xFC17241E);
        g.drawString(font, title, x, y + 4, 0xFFD1F7AB, false);
        g.drawString(font, component("settings.api_key"), x, y + 22, 0xFFDDE8DD, false);
        g.drawString(font, component("settings.model"), x, y + 60, 0xFFDDE8DD, false);
        g.drawString(font, component("settings.language"), x, y + 98, 0xFFDDE8DD, false);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawWordWrap(font, status.getString().isEmpty() ? component("settings.privacy") : status,
                x, y + 196, width, status.getString().isEmpty() ? 0xFFA9BDAE : 0xFFFFB4A9);
    }
    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Keep the world live and do not blur labels rendered before Screen.render.
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
