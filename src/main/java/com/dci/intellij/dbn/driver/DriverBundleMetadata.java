package com.dci.intellij.dbn.driver;

import com.dci.intellij.dbn.common.state.PersistentStateElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dci.intellij.dbn.common.options.setting.Settings.*;


@Slf4j
@Getter
@Setter
@NoArgsConstructor
public class DriverBundleMetadata implements PersistentStateElement {
    private File library;
    private long checksum;
    private Set<String> driverClassNames = new HashSet<>();

    public DriverBundleMetadata(File library) {
        this.library = library;
        this.checksum = calculateChecksum();
    }

    private long calculateChecksum() {
        try {
            // rudimentary checksum logic to detect changes in library bundles
            if (library.isDirectory()) {
                List<String> fileNames = Arrays.stream(library.listFiles()).map(f -> f.getPath()).collect(Collectors.toList());
                return String.join(";", fileNames).hashCode();
            }
        } catch (Throwable e) {
            log.error("Failed to evaluate driver library checksum", e);
        }

        return library.getPath().hashCode();
    }


    public boolean matchesSignature(DriverBundleMetadata metadata) {
        return this.checksum == metadata.checksum;
    }

    public boolean isValid() {
        return library.exists();
    }

    public boolean isEmpty() {
        return driverClassNames.isEmpty();
    }

    public boolean isDriverClass(String className) {
        return driverClassNames.contains(className);
    }

    @Override
    public void readState(Element element) {
        this.library = new File(stringAttribute(element, "path"));
        this.checksum = longAttribute(element, "checksum", 0);
        String[] classNames = stringAttribute(element, "driver-classes").split(",");
        this.driverClassNames.addAll(Arrays.asList(classNames));
    }

    @Override
    public void writeState(Element element) {
        setLongAttribute(element, "checksum", checksum);
        setStringAttribute(element, "path", library.getAbsolutePath());
        setStringAttribute(element, "driver-classes", String.join(",", driverClassNames));

    }
}
