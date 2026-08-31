package com.stockvision.entity;

import com.pgvector.PGvector;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;

@Converter(autoApply = false)
public class VectorConverter implements AttributeConverter<float[], Object> {

    @Override
    public Object convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        return new PGvector(attribute);
    }

    @Override
    public float[] convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData instanceof PGvector pgVector) {
            return pgVector.toArray();
        }
        if (dbData instanceof PGobject pgObject) {
            return parseVectorString(pgObject.getValue());
        }
        if (dbData instanceof String str) {
            return parseVectorString(str);
        }
        if (dbData instanceof float[] floats) {
            return floats;
        }
        return parseVectorString(dbData.toString());
    }

    public String toVectorString(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        return new PGvector(attribute).getValue();
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