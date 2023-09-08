package com.dci.intellij.dbn.error.jira;

import com.dci.intellij.dbn.common.util.Strings;
import com.dci.intellij.dbn.connection.ConnectionBundle;
import com.dci.intellij.dbn.connection.ConnectionHandler;
import com.dci.intellij.dbn.connection.ConnectionManager;
import com.dci.intellij.dbn.connection.DatabaseType;
import com.dci.intellij.dbn.connection.config.ConnectionDatabaseSettings;
import com.dci.intellij.dbn.connection.info.ConnectionInfo;
import com.dci.intellij.dbn.error.IssueReport;
import com.dci.intellij.dbn.error.IssueReportBuilder;
import com.dci.intellij.dbn.error.MarkupElement;
import com.intellij.diagnostic.AbstractMessage;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.IdeaLoggingEvent;
import com.intellij.openapi.diagnostic.SubmittedReportInfo;
import com.intellij.openapi.project.Project;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JiraIssueReportBuilder implements IssueReportBuilder {
    private static final String LINE_DELIMITER = "\n__________________________________________________________________\n";

    @Override
    public IssueReport buildReport(
            Project project,
            IdeaPluginDescriptor plugin,
            IdeaLoggingEvent[] events,
            String message,
            Consumer<SubmittedReportInfo> consumer){
        IssueReport report = new IssueReport(project, plugin, events, message, consumer);

        initEnvironmentInfo(report);
        initDatabaseInfo(report);
        buildSummary(report);
        buildDetails(report);

        return report;
    }

    private static void initEnvironmentInfo(IssueReport report) {
        report.setOsVersion(System.getProperty("os.name"));
        report.setIdeVersion(ApplicationInfo.getInstance().getBuild().asString());
        report.setJavaVersion(System.getProperty("java.version"));
        report.setPluginVersion(report.getPlugin().getVersion());
    }

    private static void initDatabaseInfo(IssueReport report) {
        Project project = report.getProject();
        ConnectionHandler connection = loadConnection(project);
        if (connection == null) return;

        ConnectionInfo connectionInfo = connection.getConnectionInfo();
        if (connectionInfo != null) {
            DatabaseType databaseType = connectionInfo.getDatabaseType();
            report.setDatabaseType(databaseType.name());
            report.setDatabaseVersion(databaseType.name() + " " + connectionInfo.getProductVersion());
            report.setDatabaseDriver(connectionInfo.getDriverVersion());
        } else {
            ConnectionDatabaseSettings databaseSettings = connection.getSettings().getDatabaseSettings();
            DatabaseType databaseType = DatabaseType.resolve(databaseSettings.getDriver());
            if (databaseType == DatabaseType.GENERIC ) {
                databaseType = databaseSettings.getDatabaseType();
            }
            report.setDatabaseType(databaseType.name());
            report.setDatabaseVersion("NA");
            String driverLibrary = databaseSettings.getDriverLibrary();
            report.setDatabaseDriver(Strings.isEmpty(driverLibrary) ? "NA" : new File(driverLibrary).getName());
        }
    }

    private static void buildSummary(IssueReport report) {
        IdeaLoggingEvent event = report.getEvents()[0];
        String summary = event.getThrowableText();
        summary = summary.substring(0, Math.min(summary.length(), 100));
        report.setSummary(summary);
    }

    private static void buildDetails(IssueReport report) {
        StringBuilder description = new StringBuilder();
        buildEnvironmentInfo(report, description);
        buildAdditionalInfo(report, description);
        buildExceptionInfo(report, description);
        buildAttachmentInfo(report, description);

        report.setDescription(description.toString());
    }

    private static void buildEnvironmentInfo(IssueReport report, StringBuilder description) {
        addEnvironmentInfo(description, "Java Version", report.getJavaVersion());
        addEnvironmentInfo(description, "Operating System", report.getOsVersion());
        addEnvironmentInfo(description, "IDE Version", report.getIdeVersion());
        addEnvironmentInfo(description, "Plugin Version", report.getPluginVersion());
        addEnvironmentInfo(description, "Database Type", report.getDatabaseType());
        addEnvironmentInfo(description, "Database Version", report.getDatabaseVersion());
        addEnvironmentInfo(description, "Database Driver", report.getDatabaseDriver());
        addEnvironmentInfo(description, "Last Action Id", report.getLastActionId());
    }

    private static void buildAdditionalInfo(IssueReport report, StringBuilder description) {
        String message = report.getMessage();
        if (Strings.isNotEmpty(message)) {
            description.append(getMarkupElement(MarkupElement.PANEL, "User Message"));
            description.append(message);
            description.append(getMarkupElement(MarkupElement.PANEL));
        }
    }

    private static void buildExceptionInfo(IssueReport report, StringBuilder description) {
        IdeaLoggingEvent event = report.getEvent();
        String exceptionMessage = event.getMessage();
        if (Strings.isNotEmpty(exceptionMessage) && !"null".equals(exceptionMessage)) {
            description.append("\n\n");
            exceptionMessage = exceptionMessage.replace("{", "\\{").replace("}", "\\}").replace("[", "\\[").replace("]", "\\]");
            description.append(exceptionMessage);
            description.append("\n\n");
        }
        description.append(getMarkupElement(MarkupElement.CODE, event.getThrowable().getClass().getName()));
        String eventDetails = event.getThrowableText();
        if (eventDetails.length() > 30000) {
            eventDetails = eventDetails.substring(0, 30000);
        }
        description.append(eventDetails);
        description.append(getMarkupElement(MarkupElement.CODE));
    }

    private static void buildAttachmentInfo(IssueReport report, StringBuilder description) {
        IdeaLoggingEvent event = report.getEvent();
        Object eventData = event.getData();
        if (eventData instanceof AbstractMessage) {
            List<Attachment> attachments = ((AbstractMessage) eventData).getIncludedAttachments();
            if (attachments.isEmpty()) return;

            Set<String> attachmentTexts = new HashSet<>();
            for (Attachment attachment : attachments) {
                attachmentTexts.add(attachment.getDisplayText().trim());
            }

            description.append("\n\nAttachments:");
            description.append(LINE_DELIMITER);
            int index = 0;
            for (String attachmentText : attachmentTexts) {
                if (index > 0) description.append(LINE_DELIMITER);
                description.append("\n");
                description.append(attachmentText);
                index++;
            }

            description.append(LINE_DELIMITER);
        }
    }

    @Nullable
    private static ConnectionHandler loadConnection(Project project) {
        ConnectionHandler connection = ConnectionManager.getLastUsedConnection();
        if (connection != null) return connection;
        if (project == null) return null;

        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        List<ConnectionHandler> connections = connectionBundle.getConnections();
        if (!connections.isEmpty()) return connections.get(0);

        connections = connectionBundle.getAllConnections();
        if (!connections.isEmpty()) return connections.get(0);

        return null;
    }

    private static void addEnvironmentInfo(StringBuilder description, String label, String value) {
        String bold = getMarkupElement(MarkupElement.BOLD);
        String table = getMarkupElement(MarkupElement.TABLE);
        description.append(table);
        description.append(bold);
        description.append(label);
        description.append(bold);
        description.append(": ");
        description.append(table);
        description.append(value);
        description.append(table);
        description.append('\n');
    }


    private static String getMarkupElement(MarkupElement element) {
        return getMarkupElement(element, null);
    }

    private static String getMarkupElement(MarkupElement element, String title) {
        switch (element) {
            case BOLD: return "*";
            case ITALIC: return "_";
            case TABLE: return "|";
            case CODE: return title == null ? "{code}" : "{code:title=" + title + "}";
            case PANEL: return title == null ? "{panel}" : "{panel:title=" + title + "}";
        }
        return "";
    }
}
