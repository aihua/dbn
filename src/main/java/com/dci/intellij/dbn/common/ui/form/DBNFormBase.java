package com.dci.intellij.dbn.common.ui.form;

import com.dci.intellij.dbn.common.action.DataProviders;
import com.dci.intellij.dbn.common.dispose.ComponentDisposer;
import com.dci.intellij.dbn.common.environment.options.EnvironmentSettings;
import com.dci.intellij.dbn.common.notification.NotificationSupport;
import com.dci.intellij.dbn.common.ui.component.DBNComponentBase;
import com.dci.intellij.dbn.common.ui.util.UserInterface;
import com.dci.intellij.dbn.options.general.GeneralProjectSettings;
import com.intellij.ide.DataManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class DBNFormBase
        extends DBNComponentBase
        implements DBNForm, NotificationSupport {

    private boolean initialised;

    public DBNFormBase(@Nullable Disposable parent) {
        super(parent);
    }

    public DBNFormBase(@Nullable Disposable parent, @Nullable Project project) {
        super(parent, project);
    }

    @NotNull
    @Override
    public final JComponent getComponent() {
        JComponent component = getMainComponent();
        if (!initialised) {
            initialise();
        }
        return component;
    }

    private void initialise() {
        initialised = true;
        JComponent mainComponent = getMainComponent();
        DataProviders.register(mainComponent, this);
        UserInterface.visitRecursively(mainComponent, component -> {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                UserInterface.updateTitledBorders(panel);
            }
        });

        UserInterface.visitRecursively(mainComponent, component -> {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;
                UserInterface.updateTitledBorders(panel);
            }
        });
        GuiUtils.replaceJSplitPaneWithIDEASplitter(mainComponent);
    }

    protected abstract JComponent getMainComponent();

    public EnvironmentSettings getEnvironmentSettings(Project project) {
        return GeneralProjectSettings.getInstance(project).getEnvironmentSettings();
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        return null;
    }

    @Override
    @Nullable
    public JComponent getPreferredFocusedComponent() {
        return null;
    }

    @Override
    protected void disposeInner() {
        JComponent component = getComponent();
        DataManager.removeDataProvider(component);
        ComponentDisposer.dispose(component);
        nullify();
    }
}