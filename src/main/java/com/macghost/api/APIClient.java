package com.macghost.api;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.exceptions.APIConnectionException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.macghost.entity.Weather;

public class APIClient {

    private static final Path ARQUIVO_LOG
            = Path.of("logs", "requisicoes.log");

    private static final Logger LOGGER
            = Logger.getLogger(APIClient.class.getName());

    private final HttpClient client;
    private final APIConfig config;

    public APIClient() {
            this.client = HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .connectTimeout(Duration.ofSeconds(10))
                            .followRedirects(HttpClient.Redirect.NORMAL)
                            .build();

            this.config = new APIConfig();
    }
    
    // Construtor que também permite injetar dependências simuladas => Tests
    public APIClient(HttpClient client, APIConfig config) {
        this.client = client;
        this.config = config;
    }

    public Weather getWeather(String cidadeBusca) {
        if (cidadeBusca == null || cidadeBusca.isBlank()) {
            throw new IllegalArgumentException(
                    "Informe o nome da cidade!");
        }

        String apiKey = config.getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "A chave da API Indisponível!");
        }

        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                + URLEncoder.encode(
                        cidadeBusca.trim(),
                        StandardCharsets.UTF_8)
                + "&appid=" + URLEncoder.encode(
                        apiKey,
                        StandardCharsets.UTF_8)
                + "&units=metric&lang=pt_br";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;

        try {
            response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

        } catch (HttpConnectTimeoutException e) {
            throw registrarFalha(
                    "Tempo limite para conectar à API excedido.", e);

        } catch (HttpTimeoutException e) {
            throw registrarFalha(
                    "A API demorou demais para responder.", e);

        } catch (ConnectException e) {
            throw registrarFalha(
                    "Não foi possível conectar à API.", e);

        } catch (IOException e) {
            throw registrarFalha(
                    "Falha de comunicação com a API.", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw registrarFalha(
                    "A consulta foi interrompida.", e);
        }

        int status = response.statusCode();

        if (status < 200 || status >= 300) {
            registrarLog(
                    "ERRO",
                    "Consulta não concluída | statusHTTP = " + status);

            throw new APIConnectionException(status);
        }

        Weather weather;

        try {
            weather = converterWeather(response.body());

        } catch (RuntimeException e) {
            registrarLog(
                    "ERRO",
                    "Resposta inválida; statusHTTP=" + status
                    + "; tipo=" + e.getClass().getSimpleName());

            throw new IllegalStateException(
                    "A API retornou dados de clima inválidos.", e);
        }

        registrarLog(
                "INFO",
                "Consulta concluída; statusHTTP=" + status);

        return weather;
    }

    /* Conversão de objeto json (resposta API) => objeto Weather */
    private Weather converterWeather(String corpo) {
        JsonObject json = JsonParser.parseString(corpo)
                .getAsJsonObject();

        String cidade = json.get("name").getAsString();

        double temperatura = json.getAsJsonObject("main")
                .get("temp")
                .getAsDouble();

        String descricao = json.getAsJsonArray("weather")
                .get(0)
                .getAsJsonObject()
                .get("description")
                .getAsString();

        if (cidade.isBlank()
                || descricao.isBlank()
                || !Double.isFinite(temperatura)) {

            throw new IllegalArgumentException(
                    "Dados de clima ausentes ou inválidos.");
        }

        return new Weather(cidade, temperatura, descricao);
    }

    private IllegalStateException registrarFalha(String mensagem, Exception causa) {
        registrarLog(
                "ERRO",
                mensagem + "; tipo=" + causa.getClass().getSimpleName()
        );

        return new IllegalStateException(mensagem, causa);
    }

    private static synchronized void registrarLog(
            String nivel,
            String mensagem
    ) {
        String linha = OffsetDateTime.now()
                + " [" + nivel + "] "
                + mensagem
                + System.lineSeparator();

        try {
            Files.writeString(
                    ARQUIVO_LOG,
                    linha,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );

        } catch (IOException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Não foi possível gravar no arquivo de logs.", e
            );
        }
    }
}
