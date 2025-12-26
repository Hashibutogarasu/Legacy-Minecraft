package com.karasu256.mcauth.processor;

import com.google.gson.Gson;
import com.karasu256.mcauth.processor.model.MinecraftAuthRequest;
import com.karasu256.mcauth.processor.model.MinecraftAuthResponse;
import com.karasu256.mcauth.processor.model.MinecraftProfileResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Exchanges XSTS token for Minecraft access token and fetches profile.
 */
public class MinecraftAccessTokenProcessor implements IAccessTokenProcessor {
    
    private static final String MC_AUTH_URL = "https://api.minecraftservices.com/authentication/login_with_xbox";
    private static final String MC_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile";
    
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    
    public MinecraftAccessTokenProcessor(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
    
    @Override
    public String getName() {
        return "Minecraft Token";
    }
    
    @Override
    public CompletableFuture<TokenContext> process(TokenContext context) {
        if (context.getXboxXstsToken() == null || context.getXboxUserHash() == null) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("Xbox XSTS token and user hash are required"));
        }
        
        MinecraftAuthRequest authRequest = new MinecraftAuthRequest(
            context.getXboxUserHash(), context.getXboxXstsToken());
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MC_AUTH_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(authRequest)))
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenCompose(response -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    MinecraftAuthResponse authResponse = gson.fromJson(
                        response.body(), MinecraftAuthResponse.class);
                    
                    if (authResponse.isSuccess()) {
                        context.setMcAccessToken(authResponse.getAccessToken());
                        return fetchProfile(context);
                    }
                    return CompletableFuture.failedFuture(new RuntimeException(
                        "MC auth error: " + authResponse.getError() 
                            + " - " + authResponse.getErrorMessage()));
                }
                return CompletableFuture.failedFuture(
                    new RuntimeException("Failed to acquire MC token: " + response.body()));
            });
    }
    
    private CompletableFuture<TokenContext> fetchProfile(TokenContext context) {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(MC_PROFILE_URL))
            .header("Authorization", "Bearer " + context.getMcAccessToken())
            .GET()
            .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    MinecraftProfileResponse profile = gson.fromJson(
                        response.body(), MinecraftProfileResponse.class);
                    
                    if (profile.isSuccess()) {
                        context.setMcUuid(profile.getFormattedUuid());
                        context.setMcUsername(profile.getName());
                        return context;
                    }
                    throw new RuntimeException("Profile error: " + profile.getError() 
                        + " - " + profile.getErrorMessage());
                }
                throw new RuntimeException("Failed to fetch profile: " + response.body());
            });
    }
}
