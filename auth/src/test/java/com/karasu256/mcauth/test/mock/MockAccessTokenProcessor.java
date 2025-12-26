package com.karasu256.mcauth.test.mock;

import com.karasu256.mcauth.processor.IAccessTokenProcessor;
import com.karasu256.mcauth.processor.TokenContext;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Mock processor for testing authentication pipelines.
 * Allows custom modification of TokenContext without real HTTP requests.
 */
public class MockAccessTokenProcessor implements IAccessTokenProcessor {
    
    private final String name;
    private final Consumer<TokenContext> modifier;
    private boolean shouldFail = false;
    private Exception failureException;
    private long delayMs = 0;
    
    /**
     * Creates a mock processor with a name and context modifier.
     */
    public MockAccessTokenProcessor(String name, Consumer<TokenContext> modifier) {
        this.name = name;
        this.modifier = modifier;
    }
    
    /**
     * Creates a simple pass-through processor.
     */
    public MockAccessTokenProcessor(String name) {
        this(name, ctx -> {});
    }
    
    /**
     * Configures this processor to fail with the given exception.
     */
    public MockAccessTokenProcessor withFailure(Exception e) {
        this.shouldFail = true;
        this.failureException = e;
        return this;
    }
    
    /**
     * Configures this processor to delay execution.
     */
    public MockAccessTokenProcessor withDelay(long delayMs) {
        this.delayMs = delayMs;
        return this;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public CompletableFuture<TokenContext> process(TokenContext context) {
        if (shouldFail) {
            return CompletableFuture.failedFuture(
                failureException != null ? failureException : new RuntimeException("Mock failure")
            );
        }
        
        if (delayMs > 0) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                modifier.accept(context);
                return context;
            });
        }
        
        modifier.accept(context);
        return CompletableFuture.completedFuture(context);
    }
    
    @Override
    public boolean canSkip(TokenContext context) {
        return IAccessTokenProcessor.super.canSkip(context);
    }
}
