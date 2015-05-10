package com.dci.intellij.dbn.database.postgres;

import com.dci.intellij.dbn.common.database.AuthenticationInfo;
import com.dci.intellij.dbn.common.database.DatabaseInfo;
import com.dci.intellij.dbn.common.util.StringUtil;
import com.dci.intellij.dbn.database.ScriptExecutionCommand;
import com.dci.intellij.dbn.database.common.DatabaseExecutionInterfaceImpl;
import com.dci.intellij.dbn.database.common.execution.MethodExecutionProcessor;
import com.dci.intellij.dbn.database.postgres.execution.PostgresMethodExecutionProcessor;
import com.dci.intellij.dbn.execution.script.CmdLineInterface;
import com.dci.intellij.dbn.object.DBMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PostgresExecutionInterface extends DatabaseExecutionInterfaceImpl {
    public MethodExecutionProcessor createExecutionProcessor(DBMethod method) {
        return new PostgresMethodExecutionProcessor(method);
    }

    public MethodExecutionProcessor createDebugExecutionProcessor(DBMethod method) {
        return null;
    }

    @Override
    public ScriptExecutionCommand createScriptExecutionInput(@NotNull CmdLineInterface cmdLineInterface, @NotNull String filePath, String content, @Nullable String schema, @NotNull DatabaseInfo databaseInfo, @NotNull AuthenticationInfo authenticationInfo) {
        ScriptExecutionCommand executionInput = new ScriptExecutionCommand(content);

        List<String> command = executionInput.getCommand();
        command.add(cmdLineInterface.getExecutablePath());
        command.add("--echo-all");
        command.add("--host=" + databaseInfo.getHost());

        String port = databaseInfo.getPort();
        if (StringUtil.isNotEmpty(port)) {
            command.add("--port=" + port);
        }

        String database = databaseInfo.getDatabase();
        if (StringUtil.isNotEmpty(database)) {
            command.add("--dbname=" + database);
        }

        command.add("--username=" + authenticationInfo.getUser());
        if (authenticationInfo.isEmptyPassword()) {
            command.add("--no-password");
        } else {
            executionInput.getEnvironmentVars().put("PGPASSWORD", authenticationInfo.getPassword());
        }


        command.add("-f");
        command.add("\"" + filePath + "\"");


        //command.add("< " + filePath);

        StringBuilder contentBuilder = executionInput.getContent();
        if (StringUtil.isNotEmpty(schema)) {
            contentBuilder.insert(0, "set search_path to " + schema + ";\n");
        }
        //contentBuilder.append("\nexit;\n");
        return executionInput;
    }
}