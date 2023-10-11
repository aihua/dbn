package com.dci.intellij.dbn.common.load;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public final class ProgressMonitor {

    @Nullable
    public static ProgressIndicator getProgressIndicator() {
        return ProgressManager.getInstance().getProgressIndicator();
    }

    private static ProgressIndicator progress() {
        ProgressIndicator progress = getProgressIndicator();
        return progress == null ? DevNullProgressIndicator.INSTANCE : progress;
    }

    public static void checkCancelled() {
        ProgressManager.checkCanceled();
    }

    public static boolean isProgressCancelled() {
        return progress().isCanceled();
    }

    public static boolean isProgressThread() {
        return getProgressIndicator() != null;
    }

    public static void setProgressIndeterminate(boolean indeterminate) {
        progress().setIndeterminate(indeterminate);
    }

    public static void setProgressFraction(double fraction) {
        progress().setFraction(fraction);
    }

    public static void setProgressText(String text) {
        progress().setText(text);
    }

    public static void setProgressDetail(String subtext) {
        progress().setText2(subtext);
    }

    public static boolean isModal() {
        ProgressIndicator progress = progress();
        return progress != null && progress.isModal();
    }

    public static boolean isProgress() {
        return progress() != null;
    }
}
