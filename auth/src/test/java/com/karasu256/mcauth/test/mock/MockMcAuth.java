package com.karasu256.mcauth.test.mock;

import com.karasu256.mcauth.AuthResult;
import com.karasu256.mcauth.AuthenticationPipeline;
import com.karasu256.mcauth.McAuth;
import com.karasu256.mcauth.processor.TokenContext;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Mock McAuth for testing purposes.
 * Allows overriding authentication behavior without making real HTTP requests.
 */
public class MockMcAuth extends McAuth {
    
    private AuthResult mockResult;
    private TokenContext mockContext;
    private boolean useMockPipeline = false;
    private URI customAuthUri;
    
    public MockMcAuth() {
        super();
    }
    
    public MockMcAuth(HttpClient httpClient) {
        super(httpClient);
    }
    
    public MockMcAuth(HttpClient httpClient, ExecutorService executor) {
        super(httpClient, executor);
    }
    
    /**
     * Sets a mock result to be returned instead of running the real pipeline.
     */
    public MockMcAuth withMockResult(AuthResult result) {
        this.mockResult = result;
        this.useMockPipeline = true;
        return this;
    }
    
    /**
     * Sets a mock TokenContext to be used in the pipeline.
     */
    public MockMcAuth withMockContext(TokenContext context) {
        this.mockContext = context;
        this.useMockPipeline = true;
        return this;
    }
    
    /**
     * Sets a custom authorization URI.
     */
    public MockMcAuth withCustomAuthUri(URI uri) {
        this.customAuthUri = uri;
        return this;
    }
    
    /**
     * Resets all mock configurations.
     */
    public void reset() {
        this.mockResult = null;
        this.mockContext = null;
        this.useMockPipeline = false;
        this.customAuthUri = null;
    }
    
    @Override
    protected AuthenticationPipeline createPipeline() {
        if (useMockPipeline && mockContext != null) {
            // Return a pipeline with a single mock processor
            return new AuthenticationPipeline()
                .addProcessor(new MockAccessTokenProcessor("MockProcessor", ctx -> {
                    // Copy mock context values to the actual context
                    ctx.setMsAccessToken(mockContext.getMsAccessToken());
                    ctx.setMsRefreshToken(mockContext.getMsRefreshToken());
                    ctx.setXboxAccessToken(mockContext.getXboxAccessToken());
                    ctx.setXboxUserHash(mockContext.getXboxUserHash());
                    ctx.setXboxXstsToken(mockContext.getXboxXstsToken());
                    ctx.setMcAccessToken(mockContext.getMcAccessToken());
                    ctx.setMcUsername(mockContext.getMcUsername());
                    ctx.setMcUuid(mockContext.getMcUuid());
                }));
        }
        return super.createPipeline();
    }
    
    @Override
    protected URI buildAuthorizationUri(String state) {
        if (customAuthUri != null) {
            return customAuthUri;
        }
        return super.buildAuthorizationUri(state);
    }
    
    @Override
    protected AuthResult toAuthResult(TokenContext context) {
        if (useMockPipeline && mockResult != null) {
            return mockResult;
        }
        return super.toAuthResult(context);
    }
    
    /**
     * Creates a default mock result for testing.
     */
    public static AuthResult createMockResult(String username, String session) {
        return new AuthResult(true, username, session);
    }
    
    /**
     * Creates a default mock TokenContext with test values.
     */
    public static TokenContext createMockContext() {
        TokenContext ctx = new TokenContext();
        ctx.setMcUsername("MockUser");
        ctx.setMcUuid("00000000-0000-0000-0000-000000000000");
        ctx.setMcAccessToken("mock-mc-access-token");
        ctx.setMsRefreshToken("mock-refresh-token");
        return ctx;
    }
}
