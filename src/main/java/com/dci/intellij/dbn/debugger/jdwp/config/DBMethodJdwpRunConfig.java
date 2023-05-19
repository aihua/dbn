package com.dci.intellij.dbn.debugger.jdwp.config;

import com.dci.intellij.dbn.debugger.DBDebuggerType;
import com.dci.intellij.dbn.debugger.DatabaseDebuggerManager;
import com.dci.intellij.dbn.debugger.common.config.DBMethodRunConfig;
import com.dci.intellij.dbn.debugger.common.config.DBRunConfigCategory;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.Range;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import java.net.InetAddress;

import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.integerAttribute;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.setIntegerAttribute;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.stringAttribute;
import static com.dci.intellij.dbn.common.options.setting.SettingsSupport.setStringAttribute;

public class DBMethodJdwpRunConfig extends DBMethodRunConfig implements DBJdwpRunConfig {
    private Range<Integer> tcpPortRange = new Range<>(4000, 4999);
    private InetAddress debuggerHostIPAddr;

    public DBMethodJdwpRunConfig(Project project, DBMethodJdwpRunConfigFactory factory, String name, DBRunConfigCategory category) {
        super(project, factory, name, category);
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new DBMethodJdwpRunConfigEditor(this);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        return new DBMethodJdwpRunProfileState(env);
    }

    @Override
    public boolean canRun() {
        return DBDebuggerType.JDWP.isSupported() && super.canRun();
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        DatabaseDebuggerManager.checkJdwpConfiguration();
        super.checkConfiguration();
    }

    @Override
    public Range<Integer> getTcpPortRange() {
        return tcpPortRange;
    }

    public void setTcpPortRange(Range<Integer> tcpPortRange) {
        this.tcpPortRange = tcpPortRange;
    }

    public InetAddress getDebuggerHostIPAddr() { return debuggerHostIPAddr; }

    public void setDebuggerHostIPAddr(InetAddress addr) { this.debuggerHostIPAddr = addr; }

    @Override
    public void readExternal(@NotNull Element element) throws InvalidDataException {
        super.readExternal(element);
        Element rangeElement = element.getChild("tcp-port-range");
        if (rangeElement != null) {
            int fromPortNumber = integerAttribute(rangeElement, "from-number", tcpPortRange.getFrom());
            int toPortNumber = integerAttribute(rangeElement, "to-number", tcpPortRange.getTo());
            tcpPortRange = new Range<>(fromPortNumber, toPortNumber);
        }

        Element hostInfoElement = element.getChild("hostInfo");
        if (hostInfoElement != null) {
            String hostIpAddressStr = stringAttribute(hostInfoElement, "hostIpAddress");
            if (hostIpAddressStr.startsWith("/")) {
                hostIpAddressStr = hostIpAddressStr.substring(1);
            }
            try {
                setDebuggerHostIPAddr(InetAddress.getAllByName(hostIpAddressStr)[0]);
            }
            catch (java.net.UnknownHostException e) {
                try {
                    setDebuggerHostIPAddr(InetAddress.getLocalHost());
                }
                catch (java.net.UnknownHostException e2) {
                    setDebuggerHostIPAddr(null);
                }
            }
        }
    }

    @Override
    public void writeExternal(@NotNull Element element) throws WriteExternalException {
        super.writeExternal(element);
        Element rangeElement = new Element("tcp-port-range");
        element.addContent(rangeElement);
        setIntegerAttribute(rangeElement, "from-number", tcpPortRange.getFrom());
        setIntegerAttribute(rangeElement, "to-number", tcpPortRange.getTo());

        Element hostInfoElement = new Element("hostInfo");
        element.addContent(hostInfoElement);
        setStringAttribute(hostInfoElement, "hostIpAddress",
                debuggerHostIPAddr != null ? debuggerHostIPAddr.toString() : "");
    }
}
