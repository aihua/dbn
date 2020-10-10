package com.dci.intellij.dbn.database.sqlite;

import com.dci.intellij.dbn.common.latent.Latent;
import com.dci.intellij.dbn.data.type.GenericDataType;
import com.dci.intellij.dbn.database.common.DatabaseNativeDataTypes;
import com.dci.intellij.dbn.database.common.util.DataTypeParseAdapter;

import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class SqliteNativeDataTypes extends DatabaseNativeDataTypes {
    private static final Latent<List<SimpleDateFormat>> DATE_FORMATS = Latent.thread(() -> {
        ArrayList<SimpleDateFormat> dateFormats = new ArrayList<>();
        dateFormats.add(new SimpleDateFormat("dd.MM.yyyy"));
        dateFormats.add(new SimpleDateFormat("dd.MM.yyyy hh:mm:ss"));
        dateFormats.add(new SimpleDateFormat("dd.MM.yyyy hh:mm:ss:SSS"));
        dateFormats.add(new SimpleDateFormat("yyyy-MM-dd"));
        dateFormats.add(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));
        dateFormats.add(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS"));
        return dateFormats;
    });
    private static final Latent<List<SimpleDateFormat>> TIMESTAMP_FORMATS = Latent.thread(() -> {
        ArrayList<SimpleDateFormat> timestampFormats = new ArrayList<>();
        timestampFormats.add(new SimpleDateFormat("dd.MM.yyyy hh:mm:ss:SSS"));
        timestampFormats.add(new SimpleDateFormat("dd.MM.yyyy hh:mm:ss"));
        timestampFormats.add(new SimpleDateFormat("dd.MM.yyyy"));
        timestampFormats.add(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS"));
        timestampFormats.add(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"));
        timestampFormats.add(new SimpleDateFormat("yyyy-MM-dd"));
        return timestampFormats;
    });

    {
        createNumericDefinition("INT", Long.class, Types.INTEGER);
        createNumericDefinition("INT2", Long.class, Types.INTEGER);
        createNumericDefinition("INT8", Long.class, Types.INTEGER);
        createNumericDefinition("INTEGER", Long.class, Types.INTEGER);
        createNumericDefinition("TINYINT", Integer.class, Types.TINYINT);
        createNumericDefinition("SMALLINT", Integer.class, Types.SMALLINT);
        createNumericDefinition("MEDIUMINT", Integer.class, Types.INTEGER);
        createNumericDefinition("BIGINT", BigInteger.class, Types.BIGINT);
        createNumericDefinition("UNSIGNED BIG INT", BigInteger.class, Types.BIGINT);

        createNumericDefinition("REAL", Double.class, Types.DOUBLE);
        createNumericDefinition("DOUBLE", Double.class, Types.DOUBLE);
        createNumericDefinition("DOUBLE PRECISION", Double.class, Types.DOUBLE);
        createNumericDefinition("FLOAT", Double.class, Types.FLOAT);
        createNumericDefinition("NUMERIC", Double.class, Types.NUMERIC);
        createNumericDefinition("DECIMAL", Double.class, Types.NUMERIC);
        createNumericDefinition("BOOLEAN", Integer.class, Types.INTEGER);

        createLiteralDefinition("CHARACTER", String.class, Types.CHAR);
        createLiteralDefinition("VARCHAR", String.class, Types.VARCHAR);
        createLiteralDefinition("VARYING CHARACTER", String.class, Types.VARCHAR);
        createLiteralDefinition("NCHAR", String.class, Types.NCHAR);
        createLiteralDefinition("NATIVE CHARACTER", String.class, Types.NCHAR);
        createLiteralDefinition("NVARCHAR", String.class, Types.NVARCHAR);
        createLiteralDefinition("TEXT", String.class, Types.VARCHAR);
        createLiteralDefinition("NULL", String.class, Types.VARCHAR);
        createLargeValueDefinition("BLOB", Blob.class, Types.BLOB, GenericDataType.BLOB);
        createLargeValueDefinition("CLOB", Clob.class, Types.CLOB, GenericDataType.CLOB);

        createDateTimeDefinition("DATE", Date.class, Types.DATE, new DataTypeParseAdapter<Date>() {

            @Override
            public String toString(Date object) {
                return object == null ? null : getDateFormats().get(0).format(object);
            }

            @Override
            public Date parse(String string) throws SQLException {
                return string == null ? null : new Date(parseDateTime(string, getDateFormats(), "DATE"));
            }
        });
        createDateTimeDefinition("DATETIME", Timestamp.class, Types.TIMESTAMP, new DataTypeParseAdapter<Timestamp>() {
            @Override
            public String toString(Timestamp object) {
                return object == null ? null : getTimestampFormats().get(0).format(object);
            }

            @Override
            public Timestamp parse(String string) throws SQLException {
                return string == null ? null : new Timestamp(parseDateTime(string, getTimestampFormats(), "TIMESTAMP"));
            }
        });
    }

    private static long parseDateTime(String dateString, List<SimpleDateFormat> dateFormats, String dataName) throws SQLException {
        for (SimpleDateFormat dateFormat : dateFormats) {
            try {
                return dateFormat.parse(dateString).getTime();
            } catch (ParseException ignore) {}
        }
        throw new SQLException("Error parsing value \"" + dateString + "\" into " + dataName);
    }

    private List<SimpleDateFormat> getTimestampFormats() {
        return TIMESTAMP_FORMATS.get();
    }

    private List<SimpleDateFormat> getDateFormats() {
        return DATE_FORMATS.get();
    }
}