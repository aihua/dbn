package com.dci.intellij.dbn.execution.script;

import com.dci.intellij.dbn.common.options.PersistentConfiguration;
import com.dci.intellij.dbn.common.util.Cloneable;
import com.dci.intellij.dbn.common.util.Lists;
import com.dci.intellij.dbn.connection.DatabaseType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;


@Getter
@Setter
@EqualsAndHashCode
public class CmdLineInterfaceBundle implements Cloneable<CmdLineInterfaceBundle>, PersistentConfiguration {
    private final List<CmdLineInterface> interfaces = new ArrayList<>();

    public void clear() {
        interfaces.clear();
    }

    public void add(CmdLineInterface cmdLineInterface) {
        interfaces.add(cmdLineInterface);
    }

    public void add(int index, CmdLineInterface cmdLineInterface) {
        interfaces.add(index, cmdLineInterface);
    }

    public int size() {
        return interfaces.size();
    }

    public CmdLineInterface get(int index) {
        return interfaces.get(index);
    }

    @Nullable
    public CmdLineInterface getInterface(String id) {
        return Lists.first(interfaces, cli -> Objects.equals(cli.getId(), id));
    }

    public CmdLineInterface remove(int index) {
        return interfaces.remove(index);
    }

    @Override
    public void readConfiguration(Element element) {
        interfaces.clear();
        if (element != null) {
            for (Element child : element.getChildren()) {
                CmdLineInterface cmdLineInterface = new CmdLineInterface();
                cmdLineInterface.readConfiguration(child);
                interfaces.add(cmdLineInterface);
            }
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        for (CmdLineInterface cmdLineInterface : interfaces) {
            Element child = new Element("value");
            cmdLineInterface.writeConfiguration(child);
            element.addContent(child);
        }

    }

    @Override
    public CmdLineInterfaceBundle clone() {
        CmdLineInterfaceBundle cmdLineInterfaces = new CmdLineInterfaceBundle();
        for (CmdLineInterface cmdLineInterface : interfaces) {
            cmdLineInterfaces.interfaces.add(cmdLineInterface.clone());
        }

        return cmdLineInterfaces;
    }

    public List<CmdLineInterface> getInterfaces(DatabaseType databaseType) {
        List<CmdLineInterface> interfaces = new ArrayList<>();
        for (CmdLineInterface cmdLineInterface : this.interfaces) {
            if (cmdLineInterface.getDatabaseType() == databaseType) {
                interfaces.add(cmdLineInterface);
            }
        }
        return interfaces;
    }

    public Set<String> getInterfaceNames() {
        Set<String> names = new HashSet<>();
        for (CmdLineInterface cmdLineInterface : interfaces) {
            names.add(cmdLineInterface.getName());
        }
        return names;
    }
}
