package com.dci.intellij.dbn.common.color;

import com.dci.intellij.dbn.common.event.ApplicationEvents;
import com.dci.intellij.dbn.common.ui.util.LookAndFeel;
import com.dci.intellij.dbn.common.util.Cancellable;
import com.dci.intellij.dbn.data.grid.color.DataGridTextAttributesKeys;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

import static com.dci.intellij.dbn.common.color.ColorCache.cached;
import static com.dci.intellij.dbn.common.color.ColorSchemes.background;
import static com.dci.intellij.dbn.common.color.ColorSchemes.foreground;

public final class Colors {
    private Colors() {}

    public static Color LIGHT_BLUE = new JBColor(new Color(235, 244, 254), new Color(0x2D3548));
    public static Color BUTTON_BORDER_COLOR = new JBColor(new Color(0x8C8C8C), new Color(0x606060));
    public static Color COMPONENT_BORDER_COLOR = new JBColor(new Color(0x8C8C8C), new Color(0x656565));
    public static Color HINT_COLOR = new JBColor(new Color(-12029286), new Color(-10058060));

    public static Color FAILURE_COLOR = new JBColor(new Color(0xFF0000), new Color(0xBC3F3C));
    public static Color SUCCESS_COLOR = new JBColor(new Color(0x009600), new Color(0x629755));

    public static Color getPanelBackground() {
        return cached(0, () -> UIUtil.getPanelBackground());
    }

    public static Color getLabelForeground() {
        return cached(1, () -> UIUtil.getLabelForeground());
    }

    public static Color getTextFieldBackground() {
        return cached(2, () -> UIUtil.getTableBackground());
    }

    public static Color getTextFieldForeground() {
        return cached(3, () -> UIUtil.getTableForeground());
    }

    public static Color getTableBackground() {
        return cached(4, () -> UIUtil.getTableBackground());
    }

    public static Color getTableForeground() {
        return cached(5, () -> UIUtil.getTableForeground());
    }

    public static Color getListBackground() {
        return cached(6, () -> UIUtil.getListBackground());
    }

    public static Color getListForeground() {
        return cached(7, () -> UIUtil.getListForeground());
    }

    public static Color getListSelectionBackground(boolean focused) {
        return focused ?
                cached(8, () -> UIUtil.getListSelectionBackground(true)) :
                cached(9, () -> UIUtil.getListSelectionBackground(false));

    }

    public static Color getListSelectionForeground(boolean focused) {
        return focused ?
                cached(10, () -> UIUtil.getListSelectionForeground(true)) :
                cached(11, () -> UIUtil.getListSelectionForeground(false));
    }

    public static Color getTableCaretRowColor() {
        return cached(12, () -> background(
                DataGridTextAttributesKeys.CARET_ROW,
                EditorColors.CARET_ROW_COLOR,
                () -> UIUtil.getTableBackground()));
    }

    public static Color getTableSelectionBackground(boolean focused) {
        return focused ?
                cached(13, () -> background(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_BACKGROUND_COLOR,
                        () -> UIUtil.getTableSelectionBackground(true))) :
                cached(14, () -> background(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_BACKGROUND_COLOR,
                        () -> UIUtil.getTableSelectionBackground(false)));
    }

    public static Color getTableSelectionForeground(boolean focused) {
        return focused ?
                cached(15, () -> foreground(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_FOREGROUND_COLOR,
                        () -> UIUtil.getTableSelectionForeground(true))) :
                cached(16, () -> foreground(
                        DataGridTextAttributesKeys.SELECTION,
                        EditorColors.SELECTION_FOREGROUND_COLOR,
                        () -> UIUtil.getTableSelectionForeground(false)));
    }

    public static Color getTableGridColor() {
        return cached(17, () -> new JBColor(() -> lafDarker(Colors.getTableBackground(), 3)));
    }

    public static Color getTableHeaderGridColor() {
        return cached(18, () -> new JBColor(() -> lafDarker(Colors.getPanelBackground(), 3)));
    }

    public static Color getTableGutterBackground() {
        return cached(19, () -> background(null, EditorColors.GUTTER_BACKGROUND, () -> getPanelBackground()));
    }

    public static Color getTableGutterForeground() {
        return cached(20, () -> background(null, EditorColors.LINE_NUMBERS_COLOR, () -> JBColor.GRAY));
    }

    public static Color getEditorBackground() {
        return cached(21, () -> background(HighlighterColors.TEXT, null, () -> JBColor.WHITE));
    }

    public static Color getEditorForeground() {
        return cached(22, () -> foreground(HighlighterColors.TEXT, null, () -> JBColor.BLACK));
    }

    public static Color getEditorCaretRowBackground() {
        return cached(23, () -> foreground(null, EditorColors.CARET_ROW_COLOR, () -> getEditorBackground()));
    }

    public static Color getReadonlyEditorBackground() {
        return cached(24, () -> background(null, EditorColors.READONLY_BACKGROUND_COLOR, () -> Colors.lafDarker(getEditorBackground(), 1)));
    }

    public static Color getReadonlyEditorCaretRowBackground() {
        return cached(25, () -> new JBColor(() -> Colors.lafDarker(getReadonlyEditorBackground(), 1)));
    }

    public static Color getLighterPanelBackground() {
        return cached(26, () -> new JBColor(() -> Colors.lafBrighter(UIUtil.getPanelBackground(), 1)));
    }

    public static Color getLightPanelBackground() {
        return cached(27, () -> new JBColor(() -> Colors.lafBrighter(UIUtil.getPanelBackground(), 2)));
    }

    public static Color getDarkerPanelBackground() {
        return cached(28, () -> new JBColor(() -> Colors.lafDarker(UIUtil.getPanelBackground(), 1)));
    }

    public static Color getDarkPanelBackground() {
        return cached(29, () -> new JBColor(() -> Colors.lafDarker(UIUtil.getPanelBackground(), 2)));
    }


    @NotNull
    public static EditorColorsScheme getGlobalScheme() {
        return EditorColorsManager.getInstance().getGlobalScheme();
    }

    @Deprecated // remove after all colors confirm to be JBColor
    public static void subscribe(@Nullable Disposable parentDisposable,  Runnable runnable) {
        ApplicationEvents.subscribe(parentDisposable, EditorColorsManager.TOPIC, scheme -> runnable.run());

        UIManager.addPropertyChangeListener(evt -> {
            if (Objects.equals(evt.getPropertyName(), "lookAndFeel")) {
                Cancellable.run(runnable);
            }
        });
    }

    public static Color lafBrighter(Color color, int tones) {
        return LookAndFeel.isDarkMode() ?
                darker(color, tones * 2) :
                brighter(color, tones);
    }

    public static Color lafDarker(Color color, int tones) {
        return LookAndFeel.isDarkMode() ?
                brighter(color, tones * 2) :
                darker(color, tones);
    }


    public static Color brighter(Color color, int tones) {
        return ColorAdjustmentCache.adjusted(color, ColorAdjustment.BRIGHTER, tones);
    }

    public static Color darker(Color color, int tones) {
        return ColorAdjustmentCache.adjusted(color, ColorAdjustment.DARKER, tones);
    }

    public static Color softer(Color color, int tones) {
        return ColorAdjustmentCache.adjusted(color, ColorAdjustment.SOFTER, tones);
    }

    public static Color stronger(Color color, int tones) {
        return ColorAdjustmentCache.adjusted(color, ColorAdjustment.STRONGER, tones);
    }

}
