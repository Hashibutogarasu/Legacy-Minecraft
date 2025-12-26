package com.karasu256.mcauth.processor;

import com.google.gson.Gson;
import com.karasu256.mcauth.processor.model.MicrosoftTokenResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Exchanges MS auth code or refresh token for MS access token.
 */
public class MicrosoftAccessTokenProcessor implements IAccessTokenProcessor {
    
    private static final String TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
    private static final String REDIRECT_URI = "http://localhost:25585/callback";
    
    private final HttpClient httpClient;
    private final String clientId;
    private final Gson gson = new Gson();
    
    public MicrosoftAccessTokenProcessor(HttpClient httpClient, String clientId) {
        this.httpClient = httpClient;
        this.clientId = clientId;
    }
    
    @Override
    public String getName() {
        return "Microsoft Access Token";
    }
    
    @Override
    public CompletableFuture<TokenContext> process(TokenContext context) {
        Map<String, String> params = new HashMap<>();
        params.put("client_id", clientId);
        params.put("redirect_uri", REDIRECT_URI);
        
        if (context.hasRefreshToken()) {
            params.put("grant_type", "refresh_token");
            params.put("refresh_token", context.getMsRefreshToken());
        } else if (context.hasAuthCode()) {
            params.put("grant_type", "authorization_code");
            params.put("code", context.getMsAuthCode());
        } else {
            return CompletableFuture.failedFuture(
                new IllegalStateException("No auth code or refresh token available"));
        }
        
        String form = params.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" 
                    + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(TOKEN_URL))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(30))
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    MicrosoftTokenResponse tokenResponse = gson.fromJson(
                        response.body(), MicrosoftTokenResponse.class);
                    
                    if (tokenResponse.isSuccess()) {
                        context.setMsAccessToken(tokenResponse.getAccessToken());
                        if (tokenResponse.getRefreshToken() != null) {
                            context.setMsRefreshToken(tokenResponse.getRefreshToken());
                        }
                        return context;
                    }
                    throw new RuntimeException("MS token error: " + tokenResponse.getError() 
                        + " - " + tokenResponse.getErrorDescription());
                }
                throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
            });
    }
}
