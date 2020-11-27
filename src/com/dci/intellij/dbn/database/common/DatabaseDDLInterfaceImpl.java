package com.dci.intellij.dbn.database.common;

import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dci.intellij.dbn.connection.ResourceUtil;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.DatabaseDDLInterface;
import com.dci.intellij.dbn.database.DatabaseInterfaceProvider;
import com.dci.intellij.dbn.database.DatabaseObjectTypeId;
import com.dci.intellij.dbn.editor.code.content.GuardedBlockMarker;
import com.dci.intellij.dbn.editor.code.content.SourceCodeContent;
import com.dci.intellij.dbn.language.common.QuotePair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class DatabaseDDLInterfaceImpl extends DatabaseInterfaceImpl implements DatabaseDDLInterface {
    public static final String TEMP_OBJECT_NAME = "DBN_TEMPORARY_{0}_0001";

    public static String getTempObjectName(String objectType) {
        return MessageFormat.format(TEMP_OBJECT_NAME, objectType.toUpperCase());
    }

    public DatabaseDDLInterfaceImpl(String fileName, DatabaseInterfaceProvider provider) {
        super(fileName, provider);
    }

    @Override
    public boolean includesTypeAndNameInSourceContent(DatabaseObjectTypeId objectTypeId) {
        return
                objectTypeId == DatabaseObjectTypeId.FUNCTION ||
                objectTypeId == DatabaseObjectTypeId.PROCEDURE ||
                objectTypeId == DatabaseObjectTypeId.PACKAGE ||
                objectTypeId == DatabaseObjectTypeId.TRIGGER ||
                objectTypeId == DatabaseObjectTypeId.TYPE;

    }

    protected final void execute(String statementText, DBNConnection connection) throws SQLException {
        Statement statement = connection.createStatement();
        try {
            statement.setQueryTimeout(20);
            statement.execute(statementText);
        } finally {
            ResourceUtil.close(statement);
        }
    }

    protected final String getSingleValue(DBNConnection connection, String loaderId, Object... arguments) throws SQLException {
        ResultSet resultSet = null;
        try {
            resultSet = executeQuery(connection, loaderId, arguments);
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } finally {
            ResourceUtil.close(resultSet);
        }
        return null;
    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createView(String viewName, String code, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "create-view", viewName, code);
    }

    @Override
    public void createObject(String code, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "create-object", code);
    }


   /*********************************************************
    *                   DROP statements                     *
    *********************************************************/
   @Override
   public void dropObject(String objectType, String objectName, DBNConnection connection) throws SQLException {
       executeUpdate(connection, "drop-object", objectType, objectName);
   }

   @Override
   public void dropObjectBody(String objectType, String objectName, DBNConnection connection) throws SQLException {
       executeUpdate(connection, "drop-object-body", objectType, objectName);
   }

    protected String updateNameQualification(String code, boolean qualified, String objectType, String schemaName, String objectName, CodeStyleCaseSettings caseSettings) {
        CodeStyleCaseOption kco = caseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = caseSettings.getObjectCaseOption();

        StringBuffer buffer = new StringBuffer();
        QuotePair quotes = getProvider().getCompatibilityInterface().getDefaultIdentifierQuotes();
        String bq = "\\" + quotes.beginChar() + "?";
        String eq = "\\" + quotes.endChar() + "?";
        String regex = objectType + "\\s+(" + bq + schemaName + eq + "\\s*\\.)?\\s*" + bq + objectName + eq;
        if (qualified) {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(code);
            if (matcher.find()) {
                String replacement = kco.format(objectType) + " " + oco.format(schemaName + "." + objectName);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                matcher.appendTail(buffer);
                code = buffer.toString();
            }
        } else {
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(code);
            if (matcher.find()) {
                String replacement = kco.format(objectType) + " " + oco.format(objectName);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                matcher.appendTail(buffer);
                code = buffer.toString();
            }
        }
        return code;
    }

    @Override
    public void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName) {
        String sourceCode = content.getText().toString();
        int gbEndOffset = sourceCode.indexOf(GuardedBlockMarker.END_OFFSET_IDENTIFIER);
        if (gbEndOffset > -1) {
            content.getOffsets().addGuardedBlock(0, gbEndOffset);
            sourceCode =
                    sourceCode.substring(0, gbEndOffset) +
                    sourceCode.substring(gbEndOffset + GuardedBlockMarker.END_OFFSET_IDENTIFIER.length());
            content.setText(sourceCode);
        }
    }
}
