package com.stockvision.entity;

import com.pgvector.PGvector;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;
import org.postgresql.util.PGobject;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

public class PgVectorType implements UserType<float[]> {

    @Override
    public int getSqlType() {
        return SqlTypes.OTHER;
    }

    @Override
    public Class<float[]> returnedClass() {
        return float[].class;
    }

    @Override
    public boolean equals(float[] x, float[] y) {
        return Arrays.equals(x, y);
    }

    @Override
    public int hashCode(float[] x) {
        return Arrays.hashCode(x);
    }

    @Override
    public float[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
        Object obj = rs.getObject(position);
        if (obj == null) {
            return null;
        }
        if (obj instanceof PGvector pgVector) {
            return pgVector.toArray();
        }
        if (obj instanceof PGobject pgObject) {
            return parseVectorString(pgObject.getValue());
        }
        if (obj instanceof String str) {
            return parseVectorString(str);
        }
        if (obj instanceof float[] floats) {
            return floats;
        }
        return parseVectorString(obj.toString());
    }

    @Override
    public void nullSafeSet(PreparedStatement st, float[] value, int index, SharedSessionContractImplementor session) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
            return;
        }
        boolean isPostgres = false;
        try {
            if (st.getConnection() != null && st.getConnection().getMetaData() != null) {
                String dbName = st.getConnection().getMetaData().getDatabaseProductName();
                if (dbName != null && dbName.toLowerCase().contains("postgresql")) {
                    isPostgres = true;
                }
            }
        } catch (Exception ignored) {}

        if (isPostgres) {
            PGobject pgObject = new PGobject();
            pgObject.setType("vector");
            pgObject.setValue(new PGvector(value).getValue());
            st.setObject(index, pgObject);
        } else {
            st.setString(index, new PGvector(value).getValue());
        }
    }

    @Override
    public float[] deepCopy(float[] value) {
        return value == null ? null : Arrays.copyOf(value, value.length);
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(float[] value) {
        return deepCopy(value);
    }

    @Override
    public float[] assemble(Serializable cached, Object owner) {
        return cached instanceof float[] floats ? deepCopy(floats) : null;
    }

    @Override
    public float[] replace(float[] detached, float[] managed, Object owner) {
        return deepCopy(detached);
    }

    private float[] parseVectorString(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String clean = dbData.trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        if (clean.isBlank()) {
            return new float[0];
        }
        String[] parts = clean.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }
}