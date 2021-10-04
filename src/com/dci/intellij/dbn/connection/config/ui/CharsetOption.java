package com.dci.intellij.dbn.connection.config.ui;

import com.dci.intellij.dbn.common.ui.Presentable;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CharsetOption implements Presentable {
    public static List<CharsetOption> ALL = new ArrayList<CharsetOption>();
    static {
        for (Charset charset : Charset.availableCharsets().values()){
            ALL.add(new CharsetOption(charset));
        }
    }

    private final Charset charset;
    public CharsetOption(Charset charset) {
        this.charset = charset;
    }

    @NotNull
    @Override
    public String getName() {
        return charset.name();
    }


    public static CharsetOption get(Charset charset) {
        for (CharsetOption charsetOption : ALL) {
            if (charsetOption.charset.equals(charset)) {
                return charsetOption;
            }
        }
        return null;
    }

    public Charset getCharset() {
        return charset;
    }
}
