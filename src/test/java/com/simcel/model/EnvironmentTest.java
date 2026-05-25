package com.simcel.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EnvironmentTest {

    @Test
    void defaultConstructorSetsExpectedValues() {
        Environment env = new Environment();
        assertEquals(WindDirection.N, env.getDirection());
        assertEquals(0, env.getWindStrength());
        assertEquals(50, env.getHumidity());
    }

    @Test
    void setWindStrengthValidValue() {
        Environment env = new Environment();
        env.setWindStrength(3);
        assertEquals(3, env.getWindStrength());
    }

    @Test
    void setWindStrengthClampsAboveMax() {
        Environment env = new Environment();
        env.setWindStrength(7);
        assertEquals(5, env.getWindStrength());
    }

    @Test
    void setWindStrengthClampsBelowMin() {
        Environment env = new Environment();
        env.setWindStrength(-2);
        assertEquals(0, env.getWindStrength());
    }

    @Test
    void setHumidityValidValue() {
        Environment env = new Environment();
        env.setHumidity(60);
        assertEquals(60, env.getHumidity());
    }

    @Test
    void setHumidityClampsAboveMax() {
        Environment env = new Environment();
        env.setHumidity(150);
        assertEquals(100, env.getHumidity());
    }

    @Test
    void setHumidityClampsBelowMin() {
        Environment env = new Environment();
        env.setHumidity(-10);
        assertEquals(0, env.getHumidity());
    }

    @Test
    void parametricConstructorAppliesClamp() {
        Environment env = new Environment(WindDirection.SE, 10, 200);
        assertEquals(WindDirection.SE, env.getDirection());
        assertEquals(5, env.getWindStrength());
        assertEquals(100, env.getHumidity());
    }

    @Test
    void setDirectionNullThrowsIllegalArgumentException() {
        Environment env = new Environment();
        assertThrows(IllegalArgumentException.class, () -> env.setDirection(null));
    }
}
