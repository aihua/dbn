package com.dci.intellij.dbn.database.mysql;

import com.dci.intellij.dbn.code.common.style.DBLCodeStyleManager;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dci.intellij.dbn.code.psql.style.PSQLCodeStyle;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.DatabaseObjectTypeId;
import com.dci.intellij.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dci.intellij.dbn.database.interfaces.DatabaseInterfaces;
import com.dci.intellij.dbn.ddl.options.DDLFileSettings;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.editor.code.content.SourceCodeContent;
import com.dci.intellij.dbn.language.sql.SQLLanguage;
import com.dci.intellij.dbn.object.factory.ArgumentFactoryInput;
import com.dci.intellij.dbn.object.factory.MethodFactoryInput;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;

import java.sql.SQLException;

public class MySqlDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    public MySqlDataDefinitionInterface(DatabaseInterfaces provider) {
        super("mysql_ddl_interface.xml", provider);
    }


    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        if (StringUtil.isEmpty(alternativeDelimiter)) {
            alternativeDelimiter = getInterfaces().getCompatibilityInterface().getDefaultAlternativeStatementDelimiter();
        }

        DDLFileSettings ddlFileSettings = DDLFileSettings.getInstance(project);
        boolean useQualified = ddlFileSettings.getGeneralSettings().isUseQualifiedObjectNames();
        boolean makeRerunnable = ddlFileSettings.getGeneralSettings().isMakeScriptsRerunnable();

        CodeStyleCaseSettings caseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = caseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = caseSettings.getObjectCaseOption();


        if (objectTypeId == DatabaseObjectTypeId.VIEW) {
            return kco.format("create" + (makeRerunnable ? " or replace" : "") + " view ") +
                    oco.format((useQualified ? schemaName + "." : "") + objectName) +
                    kco.format(" as\n") +
                    code;
        }

        if (objectTypeId.isOneOf(DatabaseObjectTypeId.PROCEDURE, DatabaseObjectTypeId.FUNCTION, DatabaseObjectTypeId.DATASET_TRIGGER)) {
            if (objectTypeId == DatabaseObjectTypeId.DATASET_TRIGGER) {
                objectTypeId = DatabaseObjectTypeId.TRIGGER;
            }
            String objectType = objectTypeId.toString().toLowerCase();
            code = updateNameQualification(code, useQualified, objectType, schemaName, objectName, caseSettings);
            String delimiterChange = kco.format("delimiter ") + alternativeDelimiter + "\n";
            String dropStatement =
                    kco.format("drop " + objectType + " if exists ") +
                    oco.format((useQualified ? schemaName + "." : "") + objectName) + alternativeDelimiter + "\n";
            String createStatement = kco.format("create definer=current_user\n") + code + alternativeDelimiter + "\n";
            String delimiterReset = kco.format("delimiter ;");
            return delimiterChange + (makeRerunnable ? dropStatement : "") + createStatement + delimiterReset;
        }
        return code;
    }

    @Override
    public void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName) {
        super.computeSourceCodeOffsets(content, objectTypeId, objectName);
    }

    public String getSessionSqlMode(DBNConnection connection) throws SQLException {
        return getSingleValue(connection, "get-session-sql-mode");
    }

    public void setSessionSqlMode(String sqlMode, DBNConnection connection) throws SQLException {
        if (sqlMode != null) {
            executeCall(connection, null, "set-session-sql-mode", sqlMode);
        }
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    @Override
    public void updateView(String viewName, String code, DBNConnection connection) throws SQLException {
        String sqlMode = getSessionSqlMode(connection);
        setSessionSqlMode("TRADITIONAL", connection);
        try {
            // try instructions
            String tempViewName = getTempObjectName("VIEW");
            dropObjectIfExists("VIEW", tempViewName, connection);
            createView(tempViewName, code, connection);
            dropObjectIfExists("VIEW", tempViewName, connection);

            // instructions
            dropObjectIfExists("VIEW", viewName, connection);
            createView(viewName, code, connection);
        } finally {
            setSessionSqlMode(sqlMode, connection);
        }
    }

    @Override
    public void updateTrigger(String tableOwner, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        // triggers do not support multiple triggers with same event (i.e can not use "try temp" approach)
        String sqlMode = getSessionSqlMode(connection);
        setSessionSqlMode("TRADITIONAL", connection);
        dropObjectIfExists("trigger", triggerName, connection);
        try {
            createObject(newCode, connection);
        } catch (SQLException e) {
            createObject(oldCode, connection);
            throw e;
        } finally {
            setSessionSqlMode(sqlMode, connection);
        }
    }

    @Override
    public void updateObject(String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        String sqlMode = getSessionSqlMode(connection);
        setSessionSqlMode("TRADITIONAL", connection);
        try {
            String tempObjectName = getTempObjectName(objectType);
            dropObjectIfExists(objectType, tempObjectName, connection);
            createObject(newCode.replaceFirst("(?i)" + objectName, tempObjectName), connection);
            dropObjectIfExists(objectType, tempObjectName, connection);

            dropObjectIfExists(objectType, objectName, connection);
            createObject(newCode, connection);
        } finally {
            setSessionSqlMode(sqlMode, connection);
        }
    }

    /*********************************************************
     *                     DROP statements                   *
     *********************************************************/
    private void dropObjectIfExists(String objectType, String objectName, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "drop-object-if-exists", objectType, objectName);
    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createMethod(MethodFactoryInput method, DBNConnection connection) throws SQLException {
        Project project = method.getSchema().getProject();
        CodeStyleCaseSettings caseSettings = PSQLCodeStyle.caseSettings(project);
        CodeStyleCaseOption keywordCaseOption = caseSettings.getKeywordCaseOption();
        CodeStyleCaseOption objectCaseOption = caseSettings.getObjectCaseOption();
        CodeStyleCaseOption dataTypeCaseOption = caseSettings.getDatatypeCaseOption();

        StringBuilder buffer = new StringBuilder();
        String methodType = method.isFunction() ? "function " : "procedure ";
        buffer.append(keywordCaseOption.format(methodType));
        buffer.append(objectCaseOption.format(method.getObjectName()));
        buffer.append("(");

        int maxArgNameLength = 0;
        int maxArgDirectionLength = 0;
        for (ArgumentFactoryInput argument : method.getArguments()) {
            maxArgNameLength = Math.max(maxArgNameLength, argument.getObjectName().length());
            maxArgDirectionLength = Math.max(maxArgDirectionLength,
                    argument.isInput() && argument.isOutput() ? 5 :
                    argument.isInput() ? 2 :
                    argument.isOutput() ? 3 : 0);
        }


        for (ArgumentFactoryInput argument : method.getArguments()) {
            buffer.append("\n    ");
            
            if (!method.isFunction()) {
                String direction =
                        argument.isInput() && argument.isOutput() ? keywordCaseOption.format("inout") :
                                argument.isInput() ? keywordCaseOption.format("in") :
                                        argument.isOutput() ? keywordCaseOption.format("out") : "";
                buffer.append(direction);
                buffer.append(StringUtil.repeatSymbol(' ', maxArgDirectionLength - direction.length() + 1));
            }

            buffer.append(objectCaseOption.format(argument.getObjectName()));
            buffer.append(StringUtil.repeatSymbol(' ', maxArgNameLength - argument.getObjectName().length() + 1));

            buffer.append(dataTypeCaseOption.format(argument.getDataType()));
            if (argument != method.getArguments().get(method.getArguments().size() -1)) {
                buffer.append(",");
            }
        }

        buffer.append(")\n");
        if (method.isFunction()) {
            buffer.append(keywordCaseOption.format("returns "));
            buffer.append(dataTypeCaseOption.format(method.getReturnArgument().getDataType()));
            buffer.append("\n");
        }
        buffer.append(keywordCaseOption.format("begin\n\n"));
        if (method.isFunction()) buffer.append(keywordCaseOption.format("    return null;\n\n"));
        buffer.append("end");
        
        String sqlMode = getSessionSqlMode(connection);
        try {
            setSessionSqlMode("TRADITIONAL", connection);
            createObject(buffer.toString(), connection);
        } finally {
            setSessionSqlMode(sqlMode, connection);
        }

    }

}
