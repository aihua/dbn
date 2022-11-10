package com.dci.intellij.dbn.database.generic;

import com.dci.intellij.dbn.data.type.GenericDataType;
import com.dci.intellij.dbn.database.common.DatabaseNativeDataTypes;

import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Timestamp;
import java.sql.Types;

public class GenericNativeDataTypes extends DatabaseNativeDataTypes {
    {
        createLiteralDefinition("CHAR", String.class, Types.CHAR);
        createLiteralDefinition("NCHAR", String.class, Types.NCHAR);
        createLiteralDefinition("VARCHAR", String.class, Types.VARCHAR);
        createLiteralDefinition("NVARCHAR", String.class, Types.NVARCHAR);
        createLiteralDefinition("LONGVARCHAR", String.class, Types.LONGVARCHAR);
        createLiteralDefinition("LONGNVARCHAR", String.class, Types.LONGNVARCHAR);
        createLiteralDefinition("BINARY", String.class, Types.BINARY);
        createLiteralDefinition("VARBINARY", String.class, Types.VARBINARY);
        createLiteralDefinition("LONGVARBINARY", String.class, Types.LONGVARBINARY);
        createLiteralDefinition("NATIONAL CHAR", String.class, Types.CHAR);
        createLiteralDefinition("NATIONAL VARCHAR", String.class, Types.VARCHAR);
        createLiteralDefinition("ENUM", String.class, Types.CHAR);
        createLiteralDefinition("SET", String.class, Types.CHAR);

        createNumericDefinition("BIT", Short.class, Types.BIT);
        createNumericDefinition("TINYINT", Short.class, Types.TINYINT);
        createNumericDefinition("BOOL", Boolean.class, Types.BOOLEAN);
        createNumericDefinition("BOOLEAN", Boolean.class, Types.BOOLEAN);
        createNumericDefinition("SMALLINT", Integer.class, Types.SMALLINT);
        createNumericDefinition("MEDIUMINT", Integer.class, Types.INTEGER);
        createNumericDefinition("INT", Integer.class, Types.INTEGER);
        createNumericDefinition("INT UNSIGNED", Integer.class, Types.INTEGER);
        createNumericDefinition("INTEGER", Integer.class, Types.INTEGER);
        createNumericDefinition("BIGINT", Long.class, Types.BIGINT);
        createNumericDefinition("FLOAT", Float.class, Types.FLOAT);
        createNumericDefinition("REAL", Float.class, Types.REAL);
        createNumericDefinition("DOUBLE", Double.class, Types.DOUBLE);
        createNumericDefinition("NUMERIC", Double.class, Types.NUMERIC);
        createNumericDefinition("DOUBLE PRECISION", Double.class, Types.DOUBLE);
        createNumericDefinition("DECIMAL", BigDecimal.class, Types.DECIMAL);
        createNumericDefinition("DEC", BigDecimal.class, Types.DECIMAL);

        createDateTimeDefinition("DATE", Timestamp.class, Types.DATE);
        createDateTimeDefinition("DATETIME", Timestamp.class, Types.TIMESTAMP);
        createDateTimeDefinition("TIMESTAMP", Timestamp.class, Types.TIMESTAMP);
        createDateTimeDefinition("TIME", Timestamp.class, Types.TIME);
        createDateTimeDefinition("YEAR", Timestamp.class, Types.DATE);

        createBasicDefinition("TINYBLOB", Blob.class, Types.BLOB, GenericDataType.BLOB);
        createBasicDefinition("TINYTEXT", Blob.class, Types.CLOB, GenericDataType.CLOB);
        createBasicDefinition("BLOB", Blob.class, Types.BLOB, GenericDataType.BLOB);
        createBasicDefinition("CLOB", Blob.class, Types.CLOB, GenericDataType.CLOB);
        createBasicDefinition("TEXT", Blob.class, Types.CLOB, GenericDataType.CLOB);
        createBasicDefinition("NCLOB", Blob.class, Types.NCLOB, GenericDataType.CLOB);
        createBasicDefinition("MEDIUMBLOB", Blob.class, Types.BLOB, GenericDataType.BLOB);
        createBasicDefinition("MEDIUMTEXT", Blob.class, Types.CLOB, GenericDataType.CLOB);
        createBasicDefinition("LONGBLOB", Blob.class, Types.BLOB, GenericDataType.BLOB);
        createBasicDefinition("LONGTEXT", Blob.class, Types.CLOB, GenericDataType.CLOB);        
    }
}
