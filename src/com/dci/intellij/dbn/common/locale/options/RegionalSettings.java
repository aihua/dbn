package com.dci.intellij.dbn.common.locale.options;

import com.dci.intellij.dbn.common.locale.DBDateFormat;
import com.dci.intellij.dbn.common.locale.DBNumberFormat;
import com.dci.intellij.dbn.common.locale.Formatter;
import com.dci.intellij.dbn.common.locale.options.ui.RegionalSettingsEditorForm;
import com.dci.intellij.dbn.common.options.BasicProjectConfiguration;
import com.dci.intellij.dbn.common.options.setting.BooleanSetting;
import com.dci.intellij.dbn.common.options.setting.SettingsSupport;
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

    @EqualsAndHashCode.Exclude
    private transient int signature = 0;

    @EqualsAndHashCode.Exclude
    private Formatter baseFormatter = createFormatter();

    public RegionalSettings(GeneralProjectSettings parent) {
        super(parent);
    }

    public static RegionalSettings getInstance(@NotNull Project project) {
        return GeneralProjectSettings.getInstance(project).getRegionalSettings();
    }

    @Override
    public void apply() throws ConfigurationException {
        super.apply();
        signature = hashCode();
        baseFormatter = createFormatter();
    }

    public Formatter createFormatter(){
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
        String localeString = SettingsSupport.getString(element, "locale", Locale.getDefault().toString());
        boolean useSystemLocale = localeString.equals("SYSTEM_DEFAULT");
        if (useSystemLocale) {
             this.locale = Locale.getDefault();
        } else {
            for (Locale locale : Locale.getAvailableLocales()) {
                if (locale.toString().equals(localeString)) {
                    this.locale = locale;
                    break;
                }
            }
        }

        dateFormatOption = SettingsSupport.getEnum(element, "date-format", DBDateFormat.MEDIUM);
        numberFormatOption = SettingsSupport.getEnum(element, "number-format", DBNumberFormat.UNGROUPED);
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
        SettingsSupport.setEnum(element, "date-format", dateFormatOption);
        SettingsSupport.setEnum(element, "number-format", numberFormatOption);

        String localeString = this.locale.equals(Locale.getDefault()) ? "SYSTEM_DEFAULT" : locale.toString();
        SettingsSupport.setString(element, "locale", localeString);

        useCustomFormats.writeConfiguration(element);
        if (useCustomFormats.value()) {
            customNumberFormat.writeConfiguration(element);
            customDateFormat.writeConfiguration(element);
            customTimeFormat.writeConfiguration(element);
        }

    }


}
