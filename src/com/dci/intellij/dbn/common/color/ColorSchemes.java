package com.dci.intellij.dbn.common.color;

import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.common.ui.LookAndFeel;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.dci.intellij.dbn.common.util.Commons.coalesce;

public class ColorSchemes {
    private static final Latent<EditorColorsScheme> lightScheme = Latent.basic(() -> {
        EditorColorsScheme[] schemes = schemes();
        return coalesce(
                () -> find(schemes, "intellij", "light"),
                () -> find(schemes, "classic", "light"),
                () -> find(schemes, "light"),
                () -> globalScheme());
    });

    private static final Latent<EditorColorsScheme> darkScheme = Latent.basic(() -> {
        EditorColorsScheme[] schemes = schemes();
        return coalesce(
                () -> find(schemes, "darcula"),
                () -> find(schemes, "dark"),
                () -> globalScheme());
    });


    @NotNull
    public static EditorColorsScheme lightScheme() {
        return lightScheme.get();
    }

    @NotNull
    public static EditorColorsScheme darkScheme() {
        return darkScheme.get();
    }


    @NotNull
    public static Color foreground(@Nullable TextAttributesKey textAttributesKey, @Nullable ColorKey colorKey, @NotNull Supplier<Color> fallback) {
        return resolve(textAttributesKey, colorKey, textAttributes -> textAttributes.getForegroundColor(), fallback);
    }

    @NotNull
    public static Color background(@Nullable TextAttributesKey textAttributesKey, @Nullable ColorKey colorKey, @NotNull Supplier<Color> fallback) {
        return resolve(textAttributesKey, colorKey, textAttributes -> textAttributes.getBackgroundColor(), fallback);
    }

    @NotNull
    private static Color resolve(
            @Nullable TextAttributesKey textAttributesKey,
            @Nullable ColorKey colorKey,
            @NotNull Function<TextAttributes, Color> supplier,
            @NotNull Supplier<Color> fallback) {
        EditorColorsScheme lightScheme = ColorSchemes.lightScheme();
        EditorColorsScheme darkScheme = ColorSchemes.darkScheme();

        Color lightColor = null;
        Color darkColor = null;

        if (textAttributesKey != null) {
            TextAttributes lightAttributes = lightScheme.getAttributes(textAttributesKey);
            lightColor = lightAttributes == null ? null : supplier.apply(lightAttributes);

            TextAttributes darkAttributes = darkScheme.getAttributes(textAttributesKey);
            darkColor = darkAttributes == null ? null : supplier.apply(darkAttributes);
        }


        if (colorKey != null) {
            lightColor = lightColor == null ? lightScheme.getColor(colorKey) : lightColor;
            darkColor = darkColor == null ? darkScheme.getColor(colorKey) : darkColor;
        }

        if (lightColor != null && darkColor != null) {
            return lightDarkColor(lightColor, darkColor);
        }

        return new JBColor(() -> fallback.get());
    }

    @NotNull
    private static JBColor lightDarkColor(Color lightColor, Color darkColor) {
        return new JBColor(() -> LookAndFeel.isDarkMode() ? darkColor : lightColor);
    }


    @NotNull
    private static EditorColorsScheme[] schemes() {
        return EditorColorsManager.getInstance().getAllSchemes();
    }

    @NotNull
    private static EditorColorsScheme globalScheme() {
        return EditorColorsManager.getInstance().getGlobalScheme();
    }

    @Nullable
    private static EditorColorsScheme find(EditorColorsScheme[] schemes, String ... tokens) {
        return Arrays.stream(schemes).filter(s -> matches(s.getName(), tokens)).findFirst().orElse(null);
    }

    private static boolean matches(String name, String ... options) {
        for (String option : options) {
            if (!StringUtil.containsIgnoreCase(name, option)) {
                return false;
            }
        }
        return true;
    }
}
