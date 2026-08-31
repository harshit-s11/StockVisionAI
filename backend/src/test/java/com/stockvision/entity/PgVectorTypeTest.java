package com.stockvision.entity;

import com.pgvector.PGvector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class PgVectorTypeTest {

    private PgVectorType vectorType;

    @BeforeEach
    void setUp() {
        vectorType = new PgVectorType();
    }

    @Test
    @DisplayName("Should verify returned class and sql type")
    void testBasicProperties() {
        assertThat(vectorType.returnedClass()).isEqualTo(float[].class);
        assertThat(vectorType.isMutable()).isTrue();
    }

    @Test
    @DisplayName("Should deep copy float arrays accurately")
    void testDeepCopy() {
        float[] sample = new float[]{0.1f, 0.2f, 0.3f};
        float[] copy = vectorType.deepCopy(sample);

        assertThat(copy).containsExactly(sample);
        assertThat(copy).isNotSameAs(sample);
        assertThat(vectorType.deepCopy(null)).isNull();
    }

    @Test
    @DisplayName("Should check equality of float arrays")
    void testEquals() {
        float[] a = new float[]{1.0f, 2.0f};
        float[] b = new float[]{1.0f, 2.0f};
        float[] c = new float[]{1.0f, 3.0f};

        assertThat(vectorType.equals(a, b)).isTrue();
        assertThat(vectorType.equals(a, c)).isFalse();
    }
}