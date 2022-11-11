package com.dci.intellij.dbn.common.options.setting;

import com.dci.intellij.dbn.common.options.PersistentConfiguration;
import com.intellij.openapi.options.ConfigurationException;
import org.jdom.Element;

import javax.swing.*;

public class IntegerSetting extends Setting<Integer, JTextField> implements PersistentConfiguration {
    public IntegerSetting(String name, Integer value) {
        super(name, value);
    }
    
    @Override
    public void readConfiguration(Element parent) {
        setValue(SettingsSupport.getInteger(parent, getName(), this.value()));
    }

    @Override
    public void writeConfiguration(Element parent) {
        SettingsSupport.setInteger(parent, getName(), this.value());
    }

    @Override
    public boolean to(JTextField component) throws ConfigurationException {
        return setValue(Integer.parseInt(component.getText()));
    }

    @Override
    public void from(JTextField component) {
        component.setText(value().toString());
    }

}
