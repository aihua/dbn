package com.dci.intellij.dbn.common;

import com.intellij.icons.AllIcons;
import com.intellij.ui.LayeredIcon;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBRectangle;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class CompoundIcons {

    private static final Map<Icon, Icon> modifiedOverlayIcons = new ConcurrentHashMap<>();

    public static Icon addModifiedOverlay(Icon icon) {
        return modifiedOverlayIcons.computeIfAbsent(icon, i -> {
            JBRectangle area = new JBRectangle(3, 3, 7, 7);
            Icon watermark = IconUtil.cropIcon(AllIcons.General.Modified, area);
            LayeredIcon layeredIcon = new LayeredIcon(2);

            layeredIcon.setIcon(i, 0);
            layeredIcon.setIcon(watermark, 1, -(watermark.getIconWidth() / 2) - 3, 0);

            //return JBUIScale.scaleIcon(layeredIcon);
            return layeredIcon;
        });
    }
}
