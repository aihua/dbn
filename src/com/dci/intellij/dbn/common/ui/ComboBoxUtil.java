package com.dci.intellij.dbn.common.ui;

import com.intellij.ui.ColoredListCellRenderer;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class ComboBoxUtil {
    public static void addItems(JComboBox comboBox, Iterable items) {
        for (Object item : items) {
            comboBox.addItem(item);
        }
    }

    public static void addItems(DefaultComboBoxModel comboBox, Iterable items) {
        for (Object item : items) {
            comboBox.addElement(item);
        }
    }

    public static <T extends Presentable> void initComboBox(JComboBox<T> comboBox, T... options) {
        initComboBox(comboBox, Arrays.asList(options));
    }

    public static <T extends Presentable> void initComboBox(JComboBox<T> comboBox, Collection<T> options) {
        DefaultComboBoxModel<T> model = new DefaultComboBoxModel<>(new Vector<>(options));
        comboBox.setModel(model);
        comboBox.setRenderer(new ColoredListCellRenderer<T>() {
            @Override
            protected void customizeCellRenderer(JList list, T value, int index, boolean selected, boolean hasFocus) {
                if (value != null) {
                    append(value.getName());
                    setIcon(value.getIcon());
                }
            }
        });
    }

    public static <T> T getSelection(JComboBox<T> comboBox) {
        return (T) comboBox.getSelectedItem();
    }

    public static <T> void setSelection(JComboBox<T> comboBox, T value) {
        comboBox.setSelectedItem(value);
    }

    public static <T> List<T> getElements(JComboBox<T> comboBox) {
        List<T> list = new ArrayList<>();
        ComboBoxModel<T> model = comboBox.getModel();
        for (int i = 0; i< model.getSize(); i++) {
            T element = model.getElementAt(i);
            if (element != null) {
                list.add(element);
            }
        }

        return list;
    }

}
