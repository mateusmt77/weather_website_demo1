package com.macghost.api;

import com.exceptions.APIConnectionException;
import com.macghost.entity.Weather;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class APIClientTests {

    private final HttpClient cliente = mock(HttpClient.class);
    private final APIConfig config = mock(APIConfig.class);
    private final APIClient api = new APIClient(cliente, config);

    private static final String JSON_VALIDO = """
            {
              "name": "Vitória",
              "main": {"temp": 28.5},
              "weather": [{"description": "céu limpo"}]
            }
            """;

    @SuppressWarnings("unchecked")
    private HttpResponse<String> resposta(int status, String corpo) {
        HttpResponse<String> mockResposta = mock(HttpResponse.class);
        when(mockResposta.statusCode()).thenReturn(status);
        if (corpo != null) {
            when(mockResposta.body()).thenReturn(corpo);
        }
        return mockResposta;
    }

    private void chaveValida() {
        when(config.getApiKey()).thenReturn("chave-ficticia");
    }

    private void configurarResposta(HttpResponse<String> response) throws Exception {
        when(cliente.<String>send(any(HttpRequest.class), any())).thenReturn(response);
    }

    @Test
    void deveConverterRespostaHttp200EmWeather() throws Exception {
        chaveValida();
        configurarResposta(resposta(200, JSON_VALIDO));

        Weather clima = api.getWeather("Vitória");

        assertAll(
                () -> assertEquals("Vitória", clima.getCidade()),
                () -> assertEquals(28.5, clima.getTemperatura()),
                () -> assertEquals("céu limpo", clima.getDescricao())
        );
        verify(cliente, times(1)).send(any(HttpRequest.class), any());
    }

    @Test
    void deveMontarRequisicaoGetComCidadeCodificada() throws Exception {
        chaveValida();
        configurarResposta(resposta(200, JSON_VALIDO));

        api.getWeather(" São Paulo ");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(cliente).send(captor.capture(), any());
        HttpRequest req = captor.getValue();

        assertEquals("GET", req.method());
        assertTrue(req.uri().toString().contains("q=S%C3%A3o+Paulo"));
        assertTrue(req.uri().toString().contains("appid=chave-ficticia"));
        assertTrue(req.uri().toString().contains("units=metric"));
        assertEquals("application/json", req.headers().firstValue("Accept").orElseThrow());
        assertEquals(Duration.ofSeconds(20), req.timeout().orElseThrow());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 404, 429, 500})
    void devePropagarErroDeStatusHttp(int status) throws Exception {
        chaveValida();
        configurarResposta(resposta(status, null));

        APIConnectionException erro = assertThrows(APIConnectionException.class,
                () -> api.getWeather("Vitória"));

        assertEquals(status, erro.getStatusError());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   ", "\t"})
    void deveRejeitarCidadeNulaOuVaziaSemRequisicao(String cidade) {
        assertThrows(IllegalArgumentException.class,
                () -> api.getWeather(cidade));

        verifyNoInteractions(cliente, config);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void deveRejeitarChaveInvalidaSemRequisicao(String chave) {
        when(config.getApiKey()).thenReturn(chave);

        assertThrows(IllegalStateException.class,
                () -> api.getWeather("Vitória"));

        verifyNoInteractions(cliente);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "isto nao e json",
            "{}",
            "{\"name\":\"Vitória\",\"main\":{\"temp\":28.5},\"weather\":[]}",
            "{\"name\":\"Vitória\",\"main\":{\"temp\":28.5},\"weather\":[{}]}",
            "{\"name\":\" \",\"main\":{\"temp\":28.5},\"weather\":[{\"description\":\"céu limpo\"}]}"
    })
    void deveRejeitarRespostaJsonInvalida(String corpo) throws Exception {
        chaveValida();
        configurarResposta(resposta(200, corpo));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> api.getWeather("Vitória"));

        assertTrue(erro.getMessage().contains("dados de clima inválidos"));
    }

    @Test
    void deveTratarTimeoutDeResposta() throws Exception {
        chaveValida();
        when(cliente.<String>send(any(HttpRequest.class), any()))
                .thenThrow(new HttpTimeoutException("timeout simulado"));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> api.getWeather("Vitória"));

        assertEquals("A API demorou demais para responder.", erro.getMessage());
        assertInstanceOf(HttpTimeoutException.class, erro.getCause());
    }

    @Test
    void deveTratarTimeoutDeConexao() throws Exception {
        chaveValida();
        when(cliente.<String>send(any(HttpRequest.class), any()))
                .thenThrow(new HttpConnectTimeoutException("timeout de conexão simulado"));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> api.getWeather("Vitória"));

        assertEquals("Tempo limite para conectar à API excedido.", erro.getMessage());
        assertInstanceOf(HttpConnectTimeoutException.class, erro.getCause());
    }

    @Test
    void deveTratarFalhaDeConexao() throws Exception {
        chaveValida();
        when(cliente.<String>send(any(HttpRequest.class), any()))
                .thenThrow(new ConnectException("conexão recusada"));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> api.getWeather("Vitória"));

        assertEquals("Não foi possível conectar à API.", erro.getMessage());
        assertInstanceOf(ConnectException.class, erro.getCause());
    }

    @Test
    void deveTratarFalhaDeComunicacao() throws Exception {
        chaveValida();
        when(cliente.<String>send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("falha de rede"));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> api.getWeather("Vitória"));

        assertEquals("Falha de comunicação com a API.", erro.getMessage());
        assertInstanceOf(IOException.class, erro.getCause());
    }

    @Test
    void deveRestaurarFlagDeInterrupcaoDaThread() throws Exception {
        chaveValida();
        when(cliente.<String>send(any(HttpRequest.class), any()))
                .thenThrow(new InterruptedException("interrupção simulada"));

        try {
            IllegalStateException erro = assertThrows(IllegalStateException.class,
                    () -> api.getWeather("Vitória"));

            assertInstanceOf(InterruptedException.class, erro.getCause());
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // limpa a flag para nao afetar os demais testes
        }
    }
}
