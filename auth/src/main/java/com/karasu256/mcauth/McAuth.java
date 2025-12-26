package com.karasu256.mcauth;

import com.google.gson.Gson;
import com.karasu256.mcauth.processor.*;
import com.karasu256.mcauth.processor.model.AuthSessionResponse;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Main authentication controller with pipeline-based processing.
 * Supports multiple authentication sessions.
 * 
 * <p>This class is designed for testability:
 * <ul>
 *   <li>Use constructor injection for HttpClient and ExecutorService</li>
 *   <li>Override protected methods (createPipeline, buildAuthorizationUri, toAuthResult) for custom behavior</li>
 * </ul>
 */
public class McAuth {
    
    private static final String AUTHORIZE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
    private static final String REDIRECT_URI = "http://localhost:25585/callback";
    
    private MicrosoftAuthenticationProvider provider;
    private final ExecutorService executor;
    private volatile CompletableFuture<String> authCodeFuture;
    private volatile CompletableFuture<AuthResult> currentAuthenticationFuture;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();
    
    // Track current authentication state
    private volatile boolean authenticationInProgress = false;
    
    /**
     * Creates a new McAuth instance with default HttpClient and ExecutorService.
     */
    public McAuth() {
        this(HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build());
    }
    
    /**
     * Creates a new McAuth instance with custom HttpClient.
     * Useful for testing with mocked HTTP responses.
     */
    public McAuth(HttpClient httpClient) {
        this(httpClient, Executors.newCachedThreadPool());
    }
    
    /**
     * Creates a new McAuth instance with custom HttpClient and ExecutorService.
     * Useful for testing with full control over dependencies.
     */
    public McAuth(HttpClient httpClient, ExecutorService executor) {
        this.httpClient = httpClient;
        this.executor = executor;
    }
    
    public void setProvider(MicrosoftAuthenticationProvider provider) {
        this.provider = provider;
    }
    
    public MicrosoftAuthenticationProvider getProvider() {
        return provider;
    }
    
    public void setRequestTimeOut(int timeout) {
        // Reserved for future use
    }
    
    /**
     * Prepares a new authentication session.
     * If authentication is already in progress, cancels the previous one.
     * Must be called before each new startAuthentication() call.
     */
    public void prepareAuthentication() {
        // Cancel any ongoing authentication
        if (authenticationInProgress) {
            cancelCurrentAuthentication();
        }
        
        // Create new future for this authentication session
        authCodeFuture = new CompletableFuture<>();
        currentAuthenticationFuture = null;
        authenticationInProgress = true;
    }
    
    /**
     * Cancels the current authentication if in progress.
     */
    public void cancelCurrentAuthentication() {
        authenticationInProgress = false;
        
        // Cancel the auth code future
        if (authCodeFuture != null && !authCodeFuture.isDone()) {
            authCodeFuture.cancel(true);
        }
        
        // Cancel the current authentication future
        if (currentAuthenticationFuture != null && !currentAuthenticationFuture.isDone()) {
            currentAuthenticationFuture.cancel(true);
        }
    }
    
    /**
     * Completes the current authentication session and cleans up resources.
     * Should be called after authentication completes (success or failure).
     */
    public void completeAuthentication() {
        authenticationInProgress = false;
        currentAuthenticationFuture = null;
    }
    
    /**
     * Checks if authentication is currently in progress.
     */
    public boolean isAuthenticationInProgress() {
        return authenticationInProgress;
    }
    
    /**
     * Starts authentication with default browser action.
     */
    public CompletableFuture<AuthResult> startAuthentication() {
        return startAuthentication(uri -> System.out.println("Please open this URL: " + uri));
    }
    
    /**
     * Starts authentication with custom browser action.
     */
    public CompletableFuture<AuthResult> startAuthentication(Consumer<URI> browserAction) {
        return startAuthentication(browserAction, null);
    }
    
    /**
     * Starts authentication with progress callback.
     * Note: Call prepareAuthentication() before this method for each new authentication.
     */
    public CompletableFuture<AuthResult> startAuthentication(
            Consumer<URI> browserAction, 
            Consumer<ProcessorProgress> progressCallback) {
        
        if (provider == null) {
            throw new IllegalStateException("Provider not set");
        }
        
        // Ensure authCodeFuture is ready
        if (authCodeFuture == null || authCodeFuture.isDone()) {
            prepareAuthentication();
        }
        
        final CompletableFuture<String> currentAuthFuture = authCodeFuture;
        
        currentAuthenticationFuture = CompletableFuture.supplyAsync(() -> {
            String state = UUID.randomUUID().toString().substring(0, 8);
            URI uri = buildAuthorizationUri(state);
            browserAction.accept(uri);
            return null;
        }, executor).thenCompose(v -> currentAuthFuture.thenComposeAsync(code -> {
            TokenContext context = new TokenContext();
            context.setMsAuthCode(code);
            
            return createPipeline()
                .onProgress(progressCallback != null ? progressCallback : p -> {})
                .withExecutor(executor)
                .execute(context)
                .thenApply(this::toAuthResult);
        }, executor)).whenComplete((result, error) -> {
            // Mark authentication as complete
            authenticationInProgress = false;
        });
        
        return currentAuthenticationFuture;
    }
    
    /**
     * Refreshes authentication using a refresh token.
     */
    public CompletableFuture<AuthResult> refreshAuthentication(String refreshToken) {
        return refreshAuthentication(refreshToken, null);
    }
    
    /**
     * Refreshes authentication with progress callback.
     */
    public CompletableFuture<AuthResult> refreshAuthentication(
            String refreshToken, 
            Consumer<ProcessorProgress> progressCallback) {
        
        if (provider == null) {
            throw new IllegalStateException("Provider not set");
        }
        
        TokenContext context = new TokenContext();
        context.setMsRefreshToken(refreshToken);
        
        return createPipeline()
            .onProgress(progressCallback != null ? progressCallback : p -> {})
            .withExecutor(executor)
            .execute(context)
            .thenApply(this::toAuthResult);
    }
    
    /**
     * Receives the auth code from OAuth callback.
     * This should be called when the web server receives the callback.
     */
    public void processToken(String code) {
        if (authCodeFuture != null && !authCodeFuture.isDone()) {
            authCodeFuture.complete(code);
        }
    }
    
    /**
     * Shuts down executor threads.
     */
    public void cleanUp() {
        cancelCurrentAuthentication();
        executor.shutdownNow();
    }
    
    /**
     * Returns the HTTP client for custom use.
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }
    
    /**
     * Returns the executor service.
     */
    public ExecutorService getExecutor() {
        return executor;
    }
    
    /**
     * Creates the default authentication pipeline.
     * Override this method in tests to provide mock processors.
     */
    protected AuthenticationPipeline createPipeline() {
        return new AuthenticationPipeline()
            .addProcessor(new MicrosoftAccessTokenProcessor(httpClient, provider.clientId()))
            .addProcessor(new XboxAccessTokenProcessor(httpClient))
            .addProcessor(new XboxXstsTokenProcessor(httpClient))
            .addProcessor(new MinecraftAccessTokenProcessor(httpClient));
    }
    
    /**
     * Builds the Microsoft authorization URI.
     * Override this method in tests to customize the URI.
     */
    protected URI buildAuthorizationUri(String state) {
        String sb = AUTHORIZE_URL + "?client_id=" + URLEncoder.encode(provider.clientId(), StandardCharsets.UTF_8) +
                "&response_type=code" +
                "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode("XboxLive.signin offline_access", StandardCharsets.UTF_8) +
                "&state=" + state;
        return URI.create(sb);
    }
    
    /**
     * Converts TokenContext to AuthResult using AuthSessionResponse data class.
     * Override this method in tests to customize the result.
     */
    protected AuthResult toAuthResult(TokenContext context) {
        AuthSessionResponse sessionResponse = new AuthSessionResponse(
            context.getMcUsername(),
            context.getMcUuid(),
            context.getMcAccessToken(),
            context.getMsRefreshToken()
        );
        return new AuthResult(true, context.getMcUsername(), gson.toJson(sessionResponse));
    }
}
