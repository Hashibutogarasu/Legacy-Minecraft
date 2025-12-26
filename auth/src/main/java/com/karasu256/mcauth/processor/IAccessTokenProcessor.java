package com.karasu256.mcauth.processor;

import java.util.concurrent.CompletableFuture;

/**
 * Base interface for token processors in the authentication chain.
 */
public interface IAccessTokenProcessor {
    
    /**
     * Returns the display name of this processor for progress reporting.
     */
    String getName();
    
    /**
     * Processes the token context and returns the updated context.
     */
    CompletableFuture<TokenContext> process(TokenContext context);
    
    /**
     * Returns true if this processor can be skipped based on context state.
     */
    default boolean canSkip(TokenContext context) {
        return false;
    }
}
