package com.macghost.entity;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class WeatherEntityTests {

    static Stream<Weather> listaClima() {
        return Stream.of(
            new Weather("Vitória", 34.7, "Clima úmido"),
            new Weather("Cariacica", 29.7, "Clima Nublado"),
            new Weather("São Paulo", 32.5, "Risco de Tempestade")
        );
    }

    @ParameterizedTest
    @MethodSource("listaClima")
    void deveraCriarObjetoWeatherValido(Weather weather) {
        assertNotNull(weather);
        assertNotNull(weather.getCidade());
        assertNotNull(weather.getDescricao());
        assertTrue(Double.isFinite(weather.getTemperatura()));
    }

    
}