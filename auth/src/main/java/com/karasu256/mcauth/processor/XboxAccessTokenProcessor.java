package com.karasu256.mcauth.processor;

import com.google.gson.Gson;
import com.karasu256.mcauth.processor.model.XboxAuthRequest;
import com.karasu256.mcauth.processor.model.XboxAuthResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Exchanges MS access token for Xbox Live token.
 */
public class XboxAccessTokenProcessor implements IAccessTokenProcessor {
    
    private static final String XBOX_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate";
    
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    
    public XboxAccessTokenProcessor(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    @Override
    public String getName() {
        return "Xbox Live Token";
    }
    
    @Override
    public CompletableFuture<TokenContext> process(TokenContext context) {
        if (context.getMsAccessToken() == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("MS access token is required"));
        }
        
        XboxAuthRequest authRequest = new XboxAuthRequest(context.getMsAccessToken());
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(XBOX_AUTH_URL))
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(authRequest)))
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() == 200) {
                    XboxAuthResponse authResponse = gson.fromJson(
                        response.body(), XboxAuthResponse.class);
                    
                    if (authResponse.isSuccess()) {
                        context.setXboxAccessToken(authResponse.getToken());
                        return context;
                    }
                    throw new RuntimeException("Xbox auth error: " + authResponse.getError() 
                        + " - " + authResponse.getMessage());
                }
                throw new RuntimeException("Failed to acquire Xbox token: " + response.body());
            });
    }
}
