package com.karasu256.mcauth.test;

import com.karasu256.mcauth.AuthenticationPipeline;
import com.karasu256.mcauth.processor.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AuthenticationPipelineTest {

    @Test
    public void testPipelineExecutesProcessorsInOrder() {
        List<String> executionOrder = new ArrayList<>();
        
        IAccessTokenProcessor proc1 = new IAccessTokenProcessor() {
            @Override
            public String getName() { return "Processor1"; }
            @Override
            public CompletableFuture<TokenContext> process(TokenContext context) {
                executionOrder.add("proc1");
                context.setMsAccessToken("ms-token");
                return CompletableFuture.completedFuture(context);
            }
        };
        
        IAccessTokenProcessor proc2 = new IAccessTokenProcessor() {
            @Override
            public String getName() { return "Processor2"; }
            @Override
            public CompletableFuture<TokenContext> process(TokenContext context) {
                executionOrder.add("proc2");
                context.setXboxAccessToken("xbox-token");
                return CompletableFuture.completedFuture(context);
            }
        };
        
        AuthenticationPipeline pipeline = new AuthenticationPipeline()
            .addProcessor(proc1)
            .addProcessor(proc2);
        
        TokenContext initial = new TokenContext();
        initial.setMsAuthCode("auth-code");
        
        TokenContext result = pipeline.execute(initial).join();
        
        assertEquals(2, executionOrder.size());
        assertEquals("proc1", executionOrder.get(0));
        assertEquals("proc2", executionOrder.get(1));
        assertEquals("ms-token", result.getMsAccessToken());
        assertEquals("xbox-token", result.getXboxAccessToken());
    }
    
    @Test
    public void testProgressCallbackInvoked() {
        AtomicInteger callCount = new AtomicInteger(0);
        List<ProcessorProgress> progressList = new ArrayList<>();
        
        IAccessTokenProcessor proc = new IAccessTokenProcessor() {
            @Override
            public String getName() { return "TestProcessor"; }
            @Override
            public CompletableFuture<TokenContext> process(TokenContext context) {
                return CompletableFuture.completedFuture(context);
            }
        };
        
        AuthenticationPipeline pipeline = new AuthenticationPipeline()
            .addProcessor(proc)
            .onProgress(progress -> {
                callCount.incrementAndGet();
                progressList.add(progress);
            });
        
        pipeline.execute(new TokenContext()).join();
        
        assertEquals(2, callCount.get());
        assertEquals("TestProcessor", progressList.get(0).processorName());
        assertEquals(0, progressList.get(0).current());
        assertEquals("Complete", progressList.get(1).processorName());
    }
    
    @Test
    public void testSkippableProcessor() {
        List<String> executionOrder = new ArrayList<>();
        
        IAccessTokenProcessor skippable = new IAccessTokenProcessor() {
            @Override
            public String getName() { return "Skippable"; }
            @Override
            public CompletableFuture<TokenContext> process(TokenContext context) {
                executionOrder.add("skippable");
                return CompletableFuture.completedFuture(context);
            }
            @Override
            public boolean canSkip(TokenContext context) { 
                return context.getMsAccessToken() != null; 
            }
        };
        
        IAccessTokenProcessor normal = new IAccessTokenProcessor() {
            @Override
            public String getName() { return "Normal"; }
            @Override
            public CompletableFuture<TokenContext> process(TokenContext context) {
                executionOrder.add("normal");
                return CompletableFuture.completedFuture(context);
            }
        };
        
        AuthenticationPipeline pipeline = new AuthenticationPipeline()
            .addProcessor(skippable)
            .addProcessor(normal);
        
        TokenContext ctx = new TokenContext();
        ctx.setMsAccessToken("already-have-token");
        
        pipeline.execute(ctx).join();
        
        assertEquals(1, executionOrder.size());
        assertEquals("normal", executionOrder.get(0));
    }
    
    @Test
    public void testTokenContextHelpers() {
        TokenContext ctx = new TokenContext();
        
        assertFalse(ctx.hasAuthCode());
        assertFalse(ctx.hasRefreshToken());
        
        ctx.setMsAuthCode("code");
        assertTrue(ctx.hasAuthCode());
        
        ctx.setMsRefreshToken("refresh");
        assertTrue(ctx.hasRefreshToken());
    }
}
