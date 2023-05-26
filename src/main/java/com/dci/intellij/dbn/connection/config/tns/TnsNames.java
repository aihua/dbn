package com.dci.intellij.dbn.connection.config.tns;

import com.dci.intellij.dbn.common.filter.Filter;
import com.dci.intellij.dbn.common.list.FilteredList;
import com.dci.intellij.dbn.common.util.Strings;
import lombok.Getter;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dci.intellij.dbn.common.util.Lists.convert;
import static com.dci.intellij.dbn.common.util.Lists.filter;

@Getter
public class TnsNames {
    private final File file;
    private final List<TnsProfile> profiles;
    private final NamesFilter filter = new NamesFilter();

    public TnsNames() {
        this(null, Collections.emptyList());
    }

    public TnsNames(File file, List<TnsProfile> profiles) {
        this.file = file;
        Collections.sort(profiles);
        this.profiles = FilteredList.stateful(filter, profiles);
    }

    public List<String> getProfileNames() {
        return convert(FilteredList.unwrap(profiles), p -> p.getProfile());
    }

    public List<TnsProfile> getSelectedProfiles() {
        return filter(profiles, p -> p.isSelected());
    }

    public String getTnsFolder() {
        return file == null ? "" : file.getParentFile().getAbsolutePath().replaceAll("\\\\", "/");
    }

    public int size() {
        return profiles.size();
    }

    @Getter
    public static class NamesFilter implements Filter<TnsProfile> {
        private String text = "";

        @Override
        public boolean accepts(TnsProfile tnsProfile) {
            if (Strings.isEmptyOrSpaces(text)) return true;
            return matches(tnsProfile.getProfile()) ||
                    matches(tnsProfile.getProtocol()) ||
                    matches(tnsProfile.getHost()) ||
                    matches(tnsProfile.getPort()) ||
                    matches(tnsProfile.getSid()) ||
                    matches(tnsProfile.getServiceName()) ||
                    matches(tnsProfile.getGlobalName());
        }

        private boolean matches(String attribute) {
            return attribute != null && Strings.indexOfIgnoreCase(attribute, text, 0) > -1;
        }

        @Override
        public int getSignature() {
            return Objects.hashCode(text);
        }

        public boolean setText(String text) {
            if (!Objects.equals(this.text, text)) {
                this.text = text;
                return true;
            }
            return false;
        }
    }
}
