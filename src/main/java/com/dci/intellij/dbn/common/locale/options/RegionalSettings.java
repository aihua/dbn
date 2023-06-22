package com.dci.intellij.dbn.common.locale.options;

import com.dci.intellij.dbn.common.locale.DBDateFormat;
import com.dci.intellij.dbn.common.locale.DBNumberFormat;
import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.locale.options.ui.RegionalSettingsEditorForm;
import com.dci.intellij.dbn.common.options.BasicProjectConfiguration;
import com.dci.intellij.dbn.common.options.setting.BooleanSetting;
import com.dci.intellij.dbn.common.options.setting.StringSetting;
import com.dci.intellij.dbn.common.sign.Signed;
import com.dci.intellij.dbn.options.general.GeneralProjectSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.*;
import static com.dci.intellij.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class RegionalSettings extends BasicProjectConfiguration<GeneralProjectSettings, RegionalSettingsEditorForm> implements Signed {
    private Locale locale = Locale.getDefault();
    private DBDateFormat dateFormatOption = DBDateFormat.MEDIUM;
    private DBNumberFormat numberFormatOption = DBNumberFormat.UNGROUPED;

    private final BooleanSetting useCustomFormats = new BooleanSetting("use-custom-formats", false);
    private final StringSetting customNumberFormat = new StringSetting("custom-number-format", null);
    private final StringSetting customDateFormat = new StringSetting("custom-date-format", null);
    private final StringSetting customTimeFormat = new StringSetting("custom-time-format", null);

    private transient int signature = 0;
    private transient Formatter baseFormatter = createFormatter();

    public RegionalSettings(GeneralProjectSettings parent) {
        super(parent);
    }

    public static RegionalSettings getInstance(@NotNull Project project) {
        return GeneralProjectSettings.getInstance(project).getRegionalSettings();
    }

    @Override
    public void apply() throws ConfigurationException {
        try {
            super.apply();
            signature = hashCode();
            baseFormatter = createFormatter();
        } catch (ConfigurationException e) {
            conditionallyLog(e);
            throw e;
        } catch (Exception e) {
            conditionallyLog(e);
            throw new ConfigurationException(e.getMessage(), "Invalid configuration");
        }
    }

    public Formatter createFormatter() {
        return useCustomFormats.value() ?
                new Formatter(signature, locale, customDateFormat.value(), customTimeFormat.value(), customNumberFormat.value()) :
                new Formatter(signature, locale, dateFormatOption, numberFormatOption);
    }



    /*********************************************************
     *                      Configuration                    *
     *********************************************************/
    @Override
    @NotNull
    public RegionalSettingsEditorForm createConfigurationEditor() {
        return new RegionalSettingsEditorForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "regional-settings";
    }

    @Override
    public void readConfiguration(Element element) {
        String localeString = getString(element, "locale", Locale.getDefault().toString());
        boolean useSystemLocale = Objects.equals(localeString, "SYSTEM_DEFAULT");
        if (useSystemLocale) {
             this.locale = Locale.getDefault();
        } else {
            for (Locale locale : Locale.getAvailableLocales()) {
                if (Objects.equals(locale.toString(), localeString)) {
                    this.locale = locale;
                    break;
                }
            }
        }

        dateFormatOption = getEnum(element, "date-format", DBDateFormat.MEDIUM);
        numberFormatOption = getEnum(element, "number-format", DBNumberFormat.UNGROUPED);
        useCustomFormats.readConfiguration(element);

        if (useCustomFormats.value()) {
            customNumberFormat.readConfiguration(element);
            customDateFormat.readConfiguration(element);
            customTimeFormat.readConfiguration(element);
        }
        signature = hashCode();
        baseFormatter = createFormatter();
    }

    @Override
    public void writeConfiguration(Element element) {
        setEnum(element, "date-format", dateFormatOption);
        setEnum(element, "number-format", numberFormatOption);

        String localeString = this.locale.equals(Locale.getDefault()) ? "SYSTEM_DEFAULT" : locale.toString();
        setString(element, "locale", localeString);

        useCustomFormats.writeConfiguration(element);
        if (useCustomFormats.value()) {
            customNumberFormat.writeConfiguration(element);
            customDateFormat.writeConfiguration(element);
            customTimeFormat.writeConfiguration(element);
        }

    }


}
