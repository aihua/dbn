package com.dci.intellij.dbn.common.notification;

public enum NotificationGroup {
    REPORTING("Reporting"),
    SOFTWARE("Software"),
    TRANSACTION("Transaction"),
    SESSION("Session"),
    SESSION_BROWSER("Session Browser"),
    CONNECTION("Connection"),
    SOURCE_CODE("Source Code"),
    EXECUTION("Execution"),
    DEBUGGER("Debugger"),
    COMPILER("Compiler"),
    LOGGING("Logging"),
    BROWSER("Browser"),
    DATA("Data"),
    ;

    private String title;

    public String getTitle() {
        return title;
    }

    NotificationGroup(String presentable) {
        this.title = presentable;
    }
}