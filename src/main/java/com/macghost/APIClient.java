package com.macghost;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

public class APIClient {

    private HttpClient httpClient;
    private APIConfig config;

    public APIClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.config = new APIConfig();
    }

    public String enviarRequisicao(String httpRequest) {

        HttpRequest request = HttpRequest.newBuilder()
                .
    }
    
}
