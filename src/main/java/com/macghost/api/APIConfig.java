package com.macghost.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

public class APIConfig {

    private final String API_KEY;
    private static final String PATH_TO_ENV = ".env";

    public APIConfig() {
        this(() -> {
            try {
                File file = new File(PATH_TO_ENV);
                if (file.exists() && file.isFile()) {
                    return new FileInputStream(file);
                }
                InputStream res = APIConfig.class.getClassLoader().getResourceAsStream(PATH_TO_ENV);
                if (res != null) {
                    return res;
                }
                return null;
            } catch (IOException e) {
                return null;
            }
        });
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

        String chave = propriedades.getProperty("WEATHER_SITE_API_KEY");
        if (chave == null) {
            chave = propriedades.getProperty("api.key");
        }

        if (chave == null || chave.isBlank()) {
            throw new IllegalStateException("A propriedade WEATHER_SITE_API_KEY ou api.key não foi configurada.");
        }

        chave = chave.trim();
        if ((chave.startsWith("\"") && chave.endsWith("\"")) || (chave.startsWith("'") && chave.endsWith("'"))) {
            if (chave.length() >= 2) {
                chave = chave.substring(1, chave.length() - 1).trim();
            }
        }

        if (chave.isBlank()) {
            throw new IllegalStateException("A propriedade WEATHER_SITE_API_KEY ou api.key não foi configurada.");
        }

        this.API_KEY = chave;
    }

    public String getApiKey() {
        return API_KEY;
    }
}
