package com.dci.intellij.dbn.common.options.setting;

import com.dci.intellij.dbn.common.options.PersistentConfiguration;
import com.intellij.openapi.options.ConfigurationException;
import org.jdom.Element;

import javax.swing.text.JTextComponent;

public class EnumSetting extends Setting<String, JTextComponent> implements PersistentConfiguration {
    public EnumSetting(String name, String value) {
        super(name, value);
    }
    
    @Override
    public void readConfiguration(Element parent) {
        setValue(SettingsSupport.getString(parent, getName(), this.value()));
    }

    @Override
    public void writeConfiguration(Element parent) {
        SettingsSupport.setString(parent, getName(), this.value());
    }

    @Override
    public boolean to(JTextComponent component) throws ConfigurationException {
        return setValue(component.getText());
    }

    @Override
    public void from(JTextComponent component) {
        component.setText(value());
    }

}
