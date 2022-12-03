package com.dci.intellij.dbn.common.util;

import com.dci.intellij.dbn.common.Icons;
import com.dci.intellij.dbn.common.message.Message;
import com.dci.intellij.dbn.common.message.MessageBundle;
import com.dci.intellij.dbn.common.message.MessageCallback;
import com.dci.intellij.dbn.common.option.DoNotAskOption;
import com.dci.intellij.dbn.common.thread.Dispatch;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class Messages {

    public static final String[] OPTIONS_OK = options("OK");
    public static final String[] OPTIONS_YES_NO = options("Yes", "No");

    public static void showErrorDialog(@Nullable Project project, String title, MessageBundle messages) {
        StringBuilder buffer = new StringBuilder();
        for (Message message : messages.getErrorMessages()) {
            buffer.append(message.getText());
            buffer.append("\n");
        }
        showErrorDialog(project, title, buffer.toString());
    }

    public static void showErrorDialog(@Nullable Project project, String message, Exception exception) {
        showErrorDialog(project, null, message, exception);
    }

    public static void showErrorDialog(@Nullable Project project, String title, String message) {
        showErrorDialog(project, title, message, null);
    }

    public static void showErrorDialog(@Nullable Project project, String message) {
        showErrorDialog(project, null, message, null);
    }

    public static void showErrorDialog(@Nullable Project project, @Nullable String title, String message, @Nullable Exception exception) {
        if (project != null && project.isDisposed()) {
            return; // project is disposed
        }

        if (exception != null) {
            if (exception instanceof ProcessCanceledException) {
                return; // process was interrupted
            }

            //String className = NamingUtil.getClassName(exception.getClass());
            //message = message + "\nCause: [" + className + "] " + exception.getMessage();
            String exceptionMessage = exception.getMessage();
            if (exceptionMessage == null) {
                exceptionMessage = exception.getClass().getName();
            }
            message = message + "\n" + exceptionMessage.trim();
        }
        if (title == null) title = "Error";
        showDialog(project, message, title, OPTIONS_OK, 0, Icons.DIALOG_ERROR, null, null);
    }

    public static void showErrorDialog(@Nullable Project project, String title, String message, String[] options, int defaultOptionIndex, MessageCallback callback) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_ERROR, callback, null);
    }

    public static void showQuestionDialog(@Nullable Project project, String title, String message, String[] options, int defaultOptionIndex, MessageCallback callback) {
        showQuestionDialog(project, title, message, options, defaultOptionIndex, callback, null);
    }

    public static void showQuestionDialog(@Nullable Project project, String title, String message, String[] options, int defaultOptionIndex, MessageCallback callback, @Nullable DoNotAskOption doNotAskOption) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_QUESTION, callback, doNotAskOption);
    }


    public static void showWarningDialog(@Nullable Project project, String title, String message) {
        showWarningDialog(project, title, message, OPTIONS_OK, 0, null);
    }

    public static void showWarningDialog(@Nullable Project project, String title, String message, String[] options, int defaultOptionIndex, MessageCallback callback) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_WARNING, callback, null);
    }

    public static void showInfoDialog(@Nullable Project project, String title, String message) {
        showInfoDialog(project, title, message, OPTIONS_OK, 0, null);
    }

    public static void showInfoDialog(@Nullable Project project, String title, String message, String[] options, int defaultOptionIndex, MessageCallback callback) {
        showDialog(project, message, title, options, defaultOptionIndex, Icons.DIALOG_INFORMATION, callback, null);
    }

    private static void showDialog(
            @Nullable Project project,
            String message,
            String title,
            String[] options,
            int defaultOptionIndex,
            @Nullable Icon icon,
            @Nullable MessageCallback callback,
            @Nullable DoNotAskOption doNotAskOption) {

        Dispatch.run(() -> {
            int option = com.intellij.openapi.ui.Messages.showDialog(project, message, Titles.signed(title), options, defaultOptionIndex, icon, doNotAskOption);
            if (callback != null) {
                callback.accept(option);
            }
        });
    }

    public static String[] options(String ... options) {
        return Commons.list(options);
    }

}
