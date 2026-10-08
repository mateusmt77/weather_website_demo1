package com.macghost;

import java.io.InputStream;
import java.util.Properties;

public class APIConfig {

    private final String API_KEY;
    private final Properties properties; 
    
    public APIConfig() {
        properties = new Properties();

        try {
            InputStream dados = getClass()
                    .getClassLoader()
                    .getResourceAsStream("application.properties");

            if (dados == null) {
                throw new RuntimeException("Arquivo de propiedades não encontrado!");
            }

            properties.load(dados);
            this.API_KEY = properties.getProperty("api.key");

        } catch (Exception exce) {
            throw new RuntimeException("Erro ao carregar arquivo de propriedades: " + exce.getMessage());
        }
    }
    
    public String getApiKey() {
        return API_KEY;
    }
}
