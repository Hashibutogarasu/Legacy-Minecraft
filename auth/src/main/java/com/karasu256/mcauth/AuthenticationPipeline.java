package com.karasu256.mcauth;

import com.karasu256.mcauth.processor.IAccessTokenProcessor;
import com.karasu256.mcauth.processor.ProcessorProgress;
import com.karasu256.mcauth.processor.TokenContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Chains processors and provides step callbacks.
 */
public class AuthenticationPipeline {
    
    private final List<IAccessTokenProcessor> processors = new ArrayList<>();
    private Consumer<ProcessorProgress> progressCallback;
    private Executor executor;
    
    public AuthenticationPipeline addProcessor(IAccessTokenProcessor processor) {
        processors.add(processor);
        return this;
    }
    
    public AuthenticationPipeline onProgress(Consumer<ProcessorProgress> callback) {
        this.progressCallback = callback;
        return this;
    }
    
    public AuthenticationPipeline withExecutor(Executor executor) {
        this.executor = executor;
        return this;
    }
    
    public CompletableFuture<TokenContext> execute(TokenContext initial) {
        if (processors.isEmpty()) {
            return CompletableFuture.completedFuture(initial);
        }
        
        CompletableFuture<TokenContext> result = CompletableFuture.completedFuture(initial);
        int total = processors.size();
        
        for (int i = 0; i < total; i++) {
            final int step = i;
            final IAccessTokenProcessor processor = processors.get(i);
            
            result = result.thenCompose(context -> {
                if (processor.canSkip(context)) {
                    return CompletableFuture.completedFuture(context);
                }
                
                notifyProgress(processor.getName(), step, total);
                
                CompletableFuture<TokenContext> processFuture = processor.process(context);
                if (executor != null) {
                    return processFuture.thenApplyAsync(ctx -> ctx, executor);
                }
                return processFuture;
            });
        }
        
        return result.thenApply(context -> {
            notifyProgress("Complete", total, total);
            return context;
        });
    }
    
    private void notifyProgress(String name, int current, int total) {
        if (progressCallback != null) {
            progressCallback.accept(new ProcessorProgress(name, current, total));
        }
    }
    
    public List<IAccessTokenProcessor> getProcessors() {
        return new ArrayList<>(processors);
    }
}
