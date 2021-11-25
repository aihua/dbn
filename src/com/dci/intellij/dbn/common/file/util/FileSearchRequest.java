package com.dci.intellij.dbn.common.file.util;

import com.dci.intellij.dbn.common.util.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.Arrays;

public class FileSearchRequest {
    private final String[] names;
    private final String[] patterns;
    private final String[] extensions;

    private FileSearchRequest(String[] names, String[] patterns, String[] extensions) {
        this.names = names;
        this.patterns = patterns;
        this.extensions = extensions;
    }

    public static FileSearchRequest forNames(String ... names) {
        return new FileSearchRequest(names, null, null);
    }

    public static FileSearchRequest forPatterns(String ... patterns) {
        return new FileSearchRequest(null, patterns, null);
    }

    public static FileSearchRequest forExtensions(String ... extensions) {
        return new FileSearchRequest(null, null, extensions);
    }

    public boolean matches(VirtualFile file) {
        if (names != null) {
            return Arrays.stream(names).anyMatch(name -> StringUtil.equalsIgnoreCase(file.getName(), name));
        } else if (patterns != null) {
            throw new UnsupportedOperationException("Not supported"); // TODO implement
        } else if (extensions != null) {
            return Arrays.stream(extensions).anyMatch(extension -> StringUtil.equalsIgnoreCase(file.getExtension(), extension));
        }
        return false;
    }
}
