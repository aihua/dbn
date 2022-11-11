package com.dci.intellij.dbn.common;

import com.intellij.CommonBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.PropertyKey;

import java.util.ResourceBundle;

public class SQLBundle {
  @NonNls private static final String COM_INTELLIJ_LANG_SQL_BUNDLE = "com.dci.intellij.dbn.common.SQLBundle";
  private static final ResourceBundle ourBundle = ResourceBundle.getBundle(COM_INTELLIJ_LANG_SQL_BUNDLE);

  private  SQLBundle(){}

  public static String message(@PropertyKey(resourceBundle = "com.dci.intellij.dbn.common.SQLBundle") String key, Object... params) {
    return CommonBundle.message(ourBundle, key, params);
  }
}
