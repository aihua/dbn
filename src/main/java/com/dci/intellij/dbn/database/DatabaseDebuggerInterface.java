package com.dci.intellij.dbn.database;

import com.dci.intellij.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dci.intellij.dbn.connection.jdbc.DBNConnection;
import com.dci.intellij.dbn.database.common.debug.*;
import com.dci.intellij.dbn.editor.DBContentType;
import com.dci.intellij.dbn.object.type.DBObjectType;

import java.sql.SQLException;

public interface DatabaseDebuggerInterface extends DatabaseInterface{

    DebuggerSessionInfo initializeSession(DBNConnection connection) throws SQLException;

    void initializeJdwpSession(DBNConnection connection, String host, String port) throws SQLException;
    void disconnectJdwpSession(DBNConnection connection) throws SQLException;

    DebuggerVersionInfo getDebuggerVersion(DBNConnection connection) throws SQLException;

    void enableDebugging(DBNConnection connection) throws SQLException;

    void disableDebugging(DBNConnection connection) throws SQLException;

    void attachSession(DBNConnection connection, String sessionId) throws SQLException;

    void detachSession(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo synchronizeSession(DBNConnection connection) throws SQLException;

    BreakpointInfo addProgramBreakpoint(String programOwner, String programName, String programType, int line, DBNConnection connection) throws SQLException;

    BreakpointInfo addSourceBreakpoint(int line, DBNConnection connection) throws SQLException;

    BreakpointOperationInfo removeBreakpoint(int breakpointId, DBNConnection connection) throws SQLException;

    BreakpointOperationInfo enableBreakpoint(int breakpointId, DBNConnection connection) throws SQLException;

    BreakpointOperationInfo disableBreakpoint(int breakpointId, DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo stepOver(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo stepInto(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo stepOut(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo runToPosition(String programOwner, String programName, String programType, int line, DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo stopExecution(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo resumeExecution(DBNConnection connection) throws SQLException;

    DebuggerRuntimeInfo getRuntimeInfo(DBNConnection connection) throws SQLException;

    ExecutionStatusInfo getExecutionStatusInfo(DBNConnection connection) throws SQLException;

    VariableInfo getVariableInfo(String variableName, Integer frameNumber, DBNConnection connection) throws SQLException;

    BasicOperationInfo setVariableValue(String variableName, Integer frameNumber, String value, DBNConnection connection) throws SQLException;

    ExecutionBacktraceInfo getExecutionBacktraceInfo(DBNConnection connection) throws SQLException;

    String[] getRequiredPrivilegeNames();

    String getDebugConsoleTemplate(CodeStyleCaseSettings settings);

    String getRuntimeEventReason(int code);

    String getJdwpBlockIdentifier();
    String getJdwpProgramIdentifier(DBObjectType objectType, DBContentType contentType, String qualifiedObjectName);

    String getJdwpTypeName(String typeIdentifier);
}
