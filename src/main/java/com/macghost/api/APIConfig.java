package com.macghost.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public class APIConfig {

    private final String API_KEY;

    public APIConfig() {
        this(() -> APIConfig.class.getClassLoader()
                .getResourceAsStream("application.properties"));
    }

    APIConfig(Supplier<InputStream> fonte) {
        Objects.requireNonNull(fonte, "A fonte de propriedades nao pode ser nula.");

        Properties propriedades = new Properties();
        try (InputStream dados = fonte.get()) {
            if (dados == null) {
                throw new IllegalStateException("Arquivo de propriedades não encontrado!");
            }
            propriedades.load(dados);
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao carregar arquivo de propriedades.", e);
        }

        String chave = propriedades.getProperty("api.key");
        if (chave == null || chave.isBlank() || chave.trim().equals("")) {
            throw new IllegalStateException("A propriedade api.key não foi configurada.");
        }

        this.API_KEY = chave.trim();
    }

    public String getApiKey() {
        return API_KEY;
    }
}
