package com.karasu256.mcauth.processor;

import com.google.gson.Gson;
import com.karasu256.mcauth.processor.model.XboxAuthResponse;
import com.karasu256.mcauth.processor.model.XboxXstsRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Exchanges Xbox token for XSTS token with user hash.
 */
public class XboxXstsTokenProcessor implements IAccessTokenProcessor {
    
    private static final String XBOX_XSTS_URL = "https://xsts.auth.xboxlive.com/xsts/authorize";
    
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    
    public XboxXstsTokenProcessor(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    @Override
    public String getName() {
        return "Xbox XSTS Token";
    }
    
    @Override
    public CompletableFuture<TokenContext> process(TokenContext context) {
        if (context.getXboxAccessToken() == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Xbox access token is required"));
        }
        
        XboxXstsRequest xstsRequest = new XboxXstsRequest(context.getXboxAccessToken());
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(XBOX_XSTS_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(xstsRequest)))
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    XboxAuthResponse xstsResponse = gson.fromJson(
                        response.body(), XboxAuthResponse.class);
                    
                    if (xstsResponse.isSuccess()) {
                        context.setXboxXstsToken(xstsResponse.getToken());
                        context.setXboxUserHash(xstsResponse.getUserHash());
                        return context;
                    }
                    throw new RuntimeException("XSTS error: " + xstsResponse.getError() 
                        + " - " + xstsResponse.getMessage());
                }
                throw new RuntimeException("Failed to acquire XSTS token: " + response.body());
            });
    }
}
